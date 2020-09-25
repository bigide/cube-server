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

package cube.dispatcher;

import cell.api.*;
import cell.core.cellet.Cellet;
import cell.core.net.Endpoint;
import cell.core.talk.Primitive;
import cell.core.talk.PrimitiveInputStream;
import cell.core.talk.TalkContext;
import cell.core.talk.TalkError;
import cell.core.talk.dialect.ActionDialect;
import cell.util.Utils;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cell.util.log.Logger;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.dispatcher.auth.AuthCellet;
import cube.dispatcher.contact.ContactCellet;
import cube.dispatcher.messaging.MessagingCellet;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Performer 接入层连接器。
 */
public class Performer implements TalkListener {

    private TalkService talkService;

    private CelletService celletService;

    private final String performerKey = "_performer";

    private final String directorKey = "_director";

    /**
     * 服务主机列表。
     */
    private List<Director> directorList;

    /**
     * Cellet 对应的 Director 列表。
     */
    private HashMap<String, List<Director>> celletDirectorMap;

    /**
     * 会话上下文对应的服务主机。
     */
    private HashMap<TalkContext, Director> talkDirectorMap;

    private ConcurrentHashMap<String, PerformerListener> listenerMap;

    /**
     * 在线的联系人。
     */
    private ConcurrentHashMap<Long, Contact> onlineContacts;

    /**
     * 数据传输记录。
     */
    protected ConcurrentHashMap<Long, Transmission> transmissionMap;

    /**
     * 阻塞块记录。
     */
    private ConcurrentHashMap<Long, Block> blockMap;

    /**
     * 阻塞超时时长。
     */
    private long blockTimeout = 6000L;

    /**
     * 构造函数。
     *
     * @param nucleus
     */
    public Performer(Nucleus nucleus) {
        this.talkService = nucleus.getTalkService();
        this.celletService = nucleus.getCelletService();
        this.directorList = new ArrayList<>();
        this.celletDirectorMap = new HashMap<>();
        this.talkDirectorMap = new HashMap<>();
        this.listenerMap = new ConcurrentHashMap<>();
        this.onlineContacts = new ConcurrentHashMap<>();
        this.transmissionMap = new ConcurrentHashMap<>();
        this.blockMap = new ConcurrentHashMap<>();
    }

    /**
     * 添加 Director 节点。
     * @param address
     * @param port
     * @param scope
     * @return
     */
    public Director addDirector(String address, int port, Scope scope) {
        Endpoint endpoint = new Endpoint(address, port);
        Director director = new Director(endpoint, scope);

        if (this.directorList.contains(director)) {
            return null;
        }

        this.directorList.add(director);

        for (String celletName : scope.cellets) {
            List<Director> list = this.celletDirectorMap.get(celletName);
            if (null == list) {
                list = new ArrayList<>();
                this.celletDirectorMap.put(celletName, list);
            }
            list.add(director);
        }

        return director;
    }

    /**
     * 选择节点。
     * @param talkContext
     * @param celletName
     * @return
     */
    private synchronized Director selectDirector(TalkContext talkContext, String celletName) {
        Director director = this.talkDirectorMap.get(talkContext);
        if (null != director) {
            return director;
        }

        List<Director> directors = this.celletDirectorMap.get(celletName);
        if (null == directors) {
            director = this.directorList.get(0);
            this.talkDirectorMap.put(talkContext, director);
            return director;
        }

        int totalWeight = directors.get(0).getSection(celletName).totalWeight;
        int anchor = Utils.randomInt(0, totalWeight - 1);
        for (int i = 0, size = directors.size(); i < size; ++i) {
            Director cur = directors.get(i);
            Director.Section section = cur.getSection(celletName);
            if (section.contains(anchor)) {
                director = cur;
                break;
            }
        }

        if (null != director) {
            this.talkDirectorMap.put(talkContext, director);
            return director;
        }

        Logger.w(this.getClass(), "Can NOT find director : " + celletName);
        return this.directorList.get(0);
    }

    /**
     *
     * @param contact
     * @return
     */
    public void addContact(Contact contact) {
        Contact current = this.onlineContacts.get(contact.getId());
        if (null == current) {
            this.onlineContacts.put(contact.getId(), contact);
        }
        else {
            current.setName(contact.getName());
            current.setContext(contact.getContext());
            for (Device device : contact.getDeviceList()) {
                current.addDevice(device);
            }
        }
    }

    public void removeContact(Long id) {
        this.onlineContacts.remove(id);
    }

    public Map<Long, Contact> getOnlineContacts() {
        return this.onlineContacts;
    }

    public void setListener(String celletName, PerformerListener listener) {
        this.listenerMap.put(celletName, listener);
    }

    public void removeTalkContext(TalkContext context) {
        this.talkDirectorMap.remove(context);

        // 删除记录
        Iterator<Transmission> iter = this.transmissionMap.values().iterator();
        while (iter.hasNext()) {
            Transmission transmission = iter.next();
            if (transmission.source == context) {
                iter.remove();
            }
        }
    }

    public void start() {
        this.talkService.setListener(AuthCellet.NAME, this);
        this.talkService.setListener(ContactCellet.NAME, this);
        this.talkService.setListener(MessagingCellet.NAME, this);

        Iterator<Map.Entry<String, List<Director>>> iter = this.celletDirectorMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, List<Director>> e = iter.next();
            String celletName = e.getKey();
            List<Director> directors = e.getValue();

            // 计算 Cellet 对应的每个连接的权重
            int totalWeight = 0;
            int cursor = 0;

            for (Director director : directors) {
                totalWeight += director.scope.weight;

                Director.Section section = director.getSection(celletName);
                section.begin = cursor;
                section.end = cursor + director.scope.weight - 1;

                // 更新游标
                cursor += section.end + 1;
            }

            for (Director director : directors) {
                Director.Section section = director.getSection(celletName);
                section.totalWeight = totalWeight;
            }
        }

        // 启动所有 Director
        for (Director director : this.directorList) {
            Endpoint ep = director.endpoint;
            Speakable speakable = this.talkService.call(ep.getHost(), ep.getPort());
            director.speaker = speakable;
        }
    }

    public void stop() {
        for (Director director : this.directorList) {
            Endpoint ep = director.endpoint;
            this.talkService.hangup(ep.getHost(), ep.getPort(), false);
        }
    }

    /**
     * 向服务单元发送数据，不等待应答。
     * @param talkContext
     * @param celletName
     * @param actionDialect
     */
    public void transmit(TalkContext talkContext, String celletName, ActionDialect actionDialect) {
        long sn = actionDialect.containsParam("sn") ?
                actionDialect.getParamAsLong("sn") : Utils.generateSerialNumber();

        Director director = this.selectDirector(talkContext, celletName);
        if (null == director) {
            Logger.e(this.getClass(), "Can not connect '" + celletName + "'");
            return;
        }

        // 添加 P-KEY 记录
        actionDialect.addParam(this.performerKey, createPerformer(sn));

        director.speaker.speak(celletName, actionDialect);
    }

    /**
     * 向服务单元发送数据，不等待应答。
     * @param talkContext
     * @param cellet
     * @param actionDialect
     */
    public void transmit(TalkContext talkContext, Cellet cellet, ActionDialect actionDialect) {
        long sn = actionDialect.containsParam("sn") ?
                actionDialect.getParamAsLong("sn") : Utils.generateSerialNumber();

        Director director = this.selectDirector(talkContext, cellet.getName());
        if (null == director) {
            Logger.e(this.getClass(), "Can not connect '" + cellet.getName() + "'");
            return;
        }

        // 增加 P-KEY 记录
        actionDialect.addParam(this.performerKey, createPerformer(sn));

        // 绑定关系
        Transmission tran = new Transmission(sn, cellet, talkContext);
        this.transmissionMap.put(tran.sn, tran);

        director.speaker.speak(cellet.getName(), actionDialect);
    }

    /**
     * 向服务单元发送数据，并阻塞当前线程直到应答或超时。
     * @param talkContext
     * @param cellet
     * @param actionDialect
     * @return
     */
    public ActionDialect syncTransmit(TalkContext talkContext, String cellet, ActionDialect actionDialect) {
        long sn = actionDialect.containsParam("sn") ?
                actionDialect.getParamAsLong("sn") : Utils.generateSerialNumber();

        Director director = this.selectDirector(talkContext, cellet);
        if (null == director) {
            Logger.e(this.getClass(), "Can not connect '" + cellet + "'");
            return null;
        }

        // 添加 Performer 信息
        actionDialect.addParam(this.performerKey, createPerformer(sn));

        Block block = new Block(sn);
        this.blockMap.put(block.sn, block);

        if (!director.speaker.speak(cellet, actionDialect)) {
            this.blockMap.remove(block.sn);
            return null;
        }

        synchronized (block) {
            try {
                block.wait(this.blockTimeout);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        this.blockMap.remove(block.sn);

        if (null != block.dialect) {
            // 删除不需要返回的参数 P-KEY
            block.dialect.removeParam(this.performerKey);
            return block.dialect;
        }
        else {
            Logger.e(this.getClass(), "Service timeout '" + cellet + "'");
            return null;
        }
    }

    private JSONObject createPerformer(long sn) {
        JSONObject json = new JSONObject();
        try {
            json.put("sn", sn);
            json.put("ts", System.currentTimeMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public void onListened(Speakable speakable, String celletName, Primitive primitive) {
        try {
            ActionDialect actionDialect = new ActionDialect(primitive);
            if (actionDialect.containsParam(this.performerKey)) {
                JSONObject performer = actionDialect.getParamAsJson(this.performerKey);
                Long sn = performer.getLong("sn");
                Block block = this.blockMap.remove(sn);
                if (null != block) {
                    block.dialect = actionDialect;
                    // 唤醒阻塞的线程
                    synchronized (block) {
                        block.notify();
                    }
                }
                else {
                    Transmission transmission = this.transmissionMap.get(sn);
                    if (null != transmission) {
                        // 移除 P-KEY
                        actionDialect.removeParam(this.performerKey);
                        // 向客户端发送数据
                        transmission.cellet.speak(transmission.source, actionDialect);
                    }
                }
            }
            else if (actionDialect.containsParam(this.directorKey)) {
                JSONObject director = actionDialect.getParamAsJson(this.directorKey);
                // 移除 D-KEY
                actionDialect.removeParam(this.directorKey);

                Long id = director.getLong("id");
                Contact contact = this.onlineContacts.get(id);
                if (null != contact) {
                    for (Device device : contact.getDeviceList()) {
                        if (device.isOnline()) {
                            Cellet cellet = this.celletService.getCellet(celletName);
                            if (null != cellet) {
                                cellet.speak(device.getTalkContext(), actionDialect);
                            }
                        }
                    }
                }
                else {
                    // TODO 日志报告
                }
            }
            else {
                PerformerListener listener = this.listenerMap.get(celletName);
                if (null != listener) {
                    listener.onReceived(celletName, primitive);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onListened(Speakable speakable, String cellet, PrimitiveInputStream primitiveInputStream) {

    }

    @Override
    public void onSpoke(Speakable speakable, String cellet, Primitive primitive) {

    }

    @Override
    public void onAck(Speakable speakable, String cellet, Primitive primitive) {

    }

    @Override
    public void onSpeakTimeout(Speakable speakable, String cellet, Primitive primitive) {

    }

    @Override
    public void onContacted(Speakable speakable) {
        Logger.i(this.getClass(), "Contacted " + speakable.getRemoteAddress().getHostString() + ":" +
                speakable.getRemoteAddress().getPort());
    }

    @Override
    public void onQuitted(Speakable speakable) {
        Logger.i(this.getClass(), "Quitted " + speakable.getRemoteAddress().getHostString() + ":" +
                speakable.getRemoteAddress().getPort());
    }

    @Override
    public void onFailed(Speakable speakable, TalkError talkError) {

    }


    /**
     * 阻塞信息描述。
     */
    public class Block {

        public Long sn;

        public ActionDialect dialect;

        public Block(Long sn) {
            this.sn = sn;
        }
    }

    /**
     * 用于记录客户端经过网关通信后对应的由服务节点回送的数据的映射关系。
     */
    public class Transmission {

        public Long sn;

        public Cellet cellet;

        public TalkContext source;

        public long timestamp;

        public Transmission(Long sn, Cellet cellet, TalkContext source) {
            this.sn = sn;
            this.cellet = cellet;
            this.source = source;
            this.timestamp = System.currentTimeMillis();
        }
    }
}