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

package cube.service.contact.task;

import cell.core.cellet.Cellet;
import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.common.entity.Group;
import cube.common.state.ContactStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;

/**
 * 修改群组信息任务。
 */
public class ModifyGroupTask extends ServiceTask {

    public ModifyGroupTask(Cellet cellet, TalkContext talkContext, Primitive primitive) {
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
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.NoSignIn.code, data));
            return;
        }

        // 域
        String domain = contact.getDomain().getName();

        Long groupId = null;
        Contact newOwner = null;
        String newName = null;
        JSONObject newContext = null;
        try {
            groupId = data.getLong("groupId");
            if (data.has("owner")) {
                newOwner = new Contact(data.getJSONObject("owner"), domain);
            }
            if (data.has("name")) {
                newName = data.getString("name");
            }
            if (data.has("context")) {
                newContext = data.getJSONObject("context");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 获取群组
        Group group = ContactManager.getInstance().getGroup(groupId, domain);
        if (null == group) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.Failure.code, data));
            return;
        }

        if (null != newOwner) {
            // 新的群组必须是当前的群成员
            if (group.hasMember(newOwner.getId())) {
                group.setOwner(newOwner);
            }
        }
        if (null != newName) {
            group.setName(newName);
        }
        if (null != newContext) {
            group.setContext(newContext);
        }

        // 修改群组信息
        Group result = ContactManager.getInstance().modifyGroup(group);

        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, ContactStateCode.Ok.code, result.toJSON()));
    }
}