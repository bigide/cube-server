/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Shixin Cube Team.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package cube.service.messaging;

import cell.adapter.CelletAdapter;
import cell.adapter.CelletAdapterFactory;
import cell.adapter.CelletAdapterListener;
import cell.adapter.extra.timeseries.SeriesItem;
import cell.adapter.extra.timeseries.SeriesMemory;
import cell.adapter.extra.timeseries.SeriesMemoryConfig;
import cell.core.net.Endpoint;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.util.CachedQueueExecutor;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cell.util.log.Logger;
import cube.common.ModuleEvent;
import cube.common.Packet;
import cube.common.UniqueKey;
import cube.common.action.MessagingActions;
import cube.common.entity.*;
import cube.common.state.MessagingStateCode;
import cube.core.AbstractModule;
import cube.service.Director;
import cube.service.auth.AuthService;
import cube.service.contact.ContactManager;
import cube.storage.StorageType;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * 消息管理器。
 */
public final class MessagingService extends AbstractModule implements CelletAdapterListener {

    public final static String NAME = "Messaging";

    private MessagingServiceCellet cellet;

    /**
     * 多线程执行器。
     */
    private ExecutorService executor;

    /**
     * 消息缓存器。
     */
    private SeriesMemory messageCache;

    /**
     * 消息存储。
     */
    private MessageStorage messageStorage;

    /**
     * 联系人事件适配器。
     */
    private CelletAdapter contactsAdapter;

    /**
     * 消息模块的插件系统。
     */
    private MessagingPluginSystem pluginSystem;

    /**
     * 构造函数。
     *
     * @param cellet
     */
    public MessagingService(MessagingServiceCellet cellet) {
        this.cellet = cellet;
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(16);
    }

    private void initMessageCache() {
        String filepath = "config/messaging-series-memory.properties";
        File file = new File(filepath);
        if (!file.exists()) {
            filepath = "messaging-series-memory.properties";
        }

        SeriesMemoryConfig config = new SeriesMemoryConfig(filepath);
        this.messageCache = new SeriesMemory(config);

        this.messageCache.start();

        int count = 10;
        Logger.i(this.getClass(), "Waiting for message cache ready");
        while (!this.messageCache.isReady()) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            --count;
            if (count == 0) {
                break;
            }
        }
    }

    private void initMessageStorage() {
        JSONObject config = new JSONObject();
        try {
            config.put("file", "storage/messages.db");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        this.messageStorage = new MessageStorage(this.executor, StorageType.SQLite, config);

        this.messageStorage.open();

        (new Thread() {
            @Override
            public void run() {
                // 存储进行自校验
                AuthService authService = (AuthService) getKernel().getModule(AuthService.NAME);
                messageStorage.execSelfChecking(authService.getDomainList());
            }
        }).start();
    }

    /**
     * 启动服务。
     */
    public void start() {
        this.contactsAdapter = CelletAdapterFactory.getInstance().getAdapter("Contacts");
        this.contactsAdapter.addListener(this);

        this.pluginSystem = new MessagingPluginSystem();

        this.initMessageCache();

        this.initMessageStorage();
    }

    /**
     * 停止服务。
     */
    public void stop() {
        this.messageCache.stop();

        if (null != this.contactsAdapter) {
            this.contactsAdapter.removeListener(this);
        }

        this.messageStorage.close();
    }

    /**
     * 将指定消息实体进行推送处理。
     * 
     * @param message
     * @return
     */
    public Message pushMessage(Message message) {
        // 打时间戳
        message.setRemoteTimestamp(System.currentTimeMillis());
        // 更新状态
        message.setState(MessageState.Sent);

        // Hook PrePush
        MessagingHook hook = this.pluginSystem.getPrePushHook();
        hook.apply(message);

        if (message.getTo().longValue() > 0) {
            // 唯一键是接收消息方的唯一键
            String key = UniqueKey.make(message.getTo(), message.getDomain());

            // 将消息写入缓存
            this.messageCache.add(key, message.toJSON(), message.getRemoteTimestamp());

            ModuleEvent event = new ModuleEvent(MessagingService.NAME, MessagingActions.Push.name, message.toJSON());
            this.contactsAdapter.publish(key, event.toJSON());
        }
        else if (message.getSource().longValue() > 0) {
            // 进行消息的群组管理
            Group group = ContactManager.getInstance().getGroup(message.getSource(), message.getDomain());
            if (null != group) {
                List<Contact> list = group.getMembers();
                for (Contact contact : list) {
                    // 创建副本
                    Message copy = new Message(message);
                    // 更新 To 数据
                    copy.setTo(contact.getId());

                    // 生成唯一键
                    String key = UniqueKey.make(contact.getId(), contact.getDomain());

                    // 将消息写入缓存
                    this.messageCache.add(key, copy.toJSON(), message.getRemoteTimestamp());

                    ModuleEvent event = new ModuleEvent(MessagingService.NAME, MessagingActions.Push.name, copy.toJSON());
                    this.contactsAdapter.publish(key, event.toJSON());
                }
            }
            else {
                // 设置为故障状态
                message.setState(MessageState.Fault);
            }
        }
        else {
            // 设置为故障状态
            message.setState(MessageState.Fault);
        }

        // Hook PostPush
        hook = this.pluginSystem.getPostPushHook();
        hook.apply(message);

        return message;
    }

    /**
     * 拉取指定时间戳到当前时间段的所有消息内容。
     * @param contactId
     * @param beginningTime
     * @param endingTime
     * @return
     */
    public List<Message> pullMessage(Long contactId, long beginningTime, long endingTime) {
        LinkedList<Message> result = new LinkedList<>();

        if (beginningTime >= endingTime) {
            // 时间设置错误
            return result;
        }

        // 从缓存里读取数据
        List<SeriesItem> list = this.messageCache.query(contactId.toString(), beginningTime, endingTime);
        for (SeriesItem item : list) {
            Message message = new Message(item.data);
            result.add(message);
        }
        return result;
    }

    private void notifyMessage(TalkContext talkContext, Message message) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("code", MessagingStateCode.Ok.code);
            payload.put("data", message.toJSON());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Packet packet = new Packet(MessagingActions.Notify.name, payload);
        ActionDialect dialect = Director.attachDirector(packet.toDialect(),
                message.getTo().longValue(), message.getDomain().getName());
        this.cellet.speak(talkContext, dialect);
    }

    @Override
    public void onDelivered(String topic, Endpoint endpoint, Primitive primitive) {
        // Nothing
    }

    @Override
    public void onDelivered(String topic, Endpoint endpoint, JSONObject jsonObject) {
        if (MessagingService.NAME.equals(ModuleEvent.getModuleName(jsonObject))) {
            // 消息模块
            ModuleEvent event = new ModuleEvent(jsonObject);
            if (event.getEventName().equals(MessagingActions.Push.name)) {
                Message message = new Message(event.getData());
                Contact contact = ContactManager.getInstance().getOnlineContact(message.getDomain(), message.getTo());
                if (null != contact) {
                    for (Device device : contact.getDeviceList()) {
                        TalkContext talkContext = device.getTalkContext();
                        notifyMessage(talkContext, message);
                    }
                }
            }
        }
    }

    @Override
    public void onDelivered(List<String> list, Endpoint endpoint, Primitive primitive) {

    }

    @Override
    public void onDelivered(List<String> list, Endpoint endpoint, JSONObject jsonObject) {

    }

    @Override
    public void onSubscribeFailed(String topic, Endpoint endpoint) {

    }

    @Override
    public void onUnsubscribeFailed(String topic, Endpoint endpoint) {

    }
}
