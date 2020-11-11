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

package cube.service.messaging.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.json.JSONArray;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.entity.Message;
import cube.common.state.MessagingStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import cube.service.messaging.MessagingService;

import java.util.List;

/**
 * 拉取消息任务。
 */
public class PullTask extends ServiceTask {

    public PullTask(Cellet cellet, TalkContext talkContext, Primitive primitive) {
        super(cellet, talkContext, primitive);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        Long id = null;
        String domainName = null;
        Device device = null;
        long beginning = 0;
        long ending = 0;
        try {
            id = packet.data.getLong("id");
            domainName = packet.data.getString("domain");
            device = new Device(packet.data.getJSONObject("device"));
            beginning = packet.data.getLong("beginning");
            ending = packet.data.getLong("ending");
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        // 获取联系人
        Contact contact = ContactManager.getInstance().getOnlineContact(domainName, id);
        if (null == contact) {
            // 应答
            this.cellet.speak(this.talkContext,
                    this.makeAsynchResponse(packet, id, domainName, device,
                            MessagingStateCode.NoContact.code, packet.data));
            return;
        }

        // 检查设备是否属于该联系人
        if (!contact.hasDevice(device)) {
            // 应答
            this.cellet.speak(this.talkContext,
                    this.makeAsynchResponse(packet, id, domainName, device,
                            MessagingStateCode.NoDevice.code, packet.data));
            return;
        }

        // 修正起始时间
        long now = System.currentTimeMillis();
        if (beginning < now - ONE_MONTH) {
            beginning = now - ONE_MONTH;
        }
        // 修正截止时间
        if (ending == 0 || ending <= beginning) {
            ending = now;
        }

        // 获取指定起始时间的消息列表
        MessagingService messagingService = (MessagingService) this.kernel.getModule(MessagingService.NAME);
        List<Message> messageList = messagingService.pullMessage(domainName, id, beginning, ending);
        int total = messageList.size();
        int count = 0;
        JSONArray messageArray = new JSONArray();

        while (!messageList.isEmpty()) {
            Message message = messageList.remove(0);
            messageArray.put(message.toJSON());

            ++count;
            if (count >= 10) {
                JSONObject payload = this.makePayload(total, beginning, ending, messageArray);
                this.cellet.speak(this.talkContext,
                        this.makeAsynchResponse(packet, id, domainName, device,
                                MessagingStateCode.Ok.code, payload));

                try {
                    Thread.sleep(10L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                count = 0;
                messageArray = new JSONArray();
            }
        }

        JSONObject payload = this.makePayload(total, beginning, ending, messageArray);
        this.cellet.speak(this.talkContext,
                this.makeAsynchResponse(packet, id, domainName, device,
                        MessagingStateCode.Ok.code, payload));
    }

    private JSONObject makePayload(int totalNum, long beginning, long ending, JSONArray messages) {
        JSONObject payload = new JSONObject();
        try {
            payload.put("total", totalNum);
            payload.put("beginning", beginning);
            payload.put("ending", ending);
            payload.put("messages", messages);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return payload;
    }
}
