/**
 * This source file is part of Cube.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2020-2021 Shixin Cube Team.
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

package cube.service.contact.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.entity.Group;
import cube.common.state.ContactStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * 获取联系人所在的所有群。
 */
public class ListGroupsTask extends ServiceTask {

    public ListGroupsTask(Cellet cellet, TalkContext talkContext, Primitive primitive) {
        super(cellet, talkContext, primitive);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        JSONObject data = packet.data;

        String tokenCode = this.getTokenCode(action);
        Contact contact = ContactManager.getInstance().getContact(tokenCode);
        if (null == contact) {
            Logger.w(this.getClass(), "Can NOT find contact with token: " + tokenCode);
            return;
        }

        // 发起查询的设备
        Device device = ContactManager.getInstance().getDevice(tokenCode);
        // 域
        String domain = contact.getDomain().getName();

        // 获取查询起始时间
        long beginning = 0;
        long ending = 0;
        try {
            beginning = data.getLong("beginning");
            ending = data.getLong("ending");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (ending == 0) {
            ending = System.currentTimeMillis();
        }

        int pageSize = 4;

        // 查询从指定活跃时间之后的该联系人所在的所有群
        List<Group> list = ContactManager.getInstance().listGroupsWithMember(domain, contact.getId(), beginning, ending);

        try {
            if (list.isEmpty()) {
                JSONObject responseData = new JSONObject();
                responseData.put("beginning", beginning);
                responseData.put("ending", ending);
                responseData.put("total", list.size());
                responseData.put("list", new JSONArray());
                this.cellet.speak(this.talkContext,
                        this.makeAsynResponse(packet, contact.getId(), domain, device,
                                ContactStateCode.Ok.code, responseData));
                return;
            }

            int total = list.size();

            while (!list.isEmpty()) {
                JSONObject responseData = new JSONObject();
                responseData.put("beginning", beginning);
                responseData.put("ending", ending);
                responseData.put("total", total);

                JSONArray array = new JSONArray();
                for (int i = 0; i < pageSize; ++i) {
                    Group group = list.remove(0);
                    array.put(group.toJSON());
                    if (list.isEmpty()) {
                        break;
                    }
                }

                responseData.put("list", array);

                this.cellet.speak(this.talkContext,
                        this.makeAsynResponse(packet, contact.getId(), domain, device,
                                ContactStateCode.Ok.code, responseData));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
