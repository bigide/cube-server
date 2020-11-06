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
import cube.auth.AuthToken;
import cube.common.Packet;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import cube.common.state.ContactStateCode;
import cube.service.ServiceTask;
import cube.service.contact.ContactManager;

/**
 * 联系人签入。
 */
public class SignInTask extends ServiceTask {

    public SignInTask(Cellet cellet, TalkContext talkContext, Primitive primitive) {
        super(cellet, talkContext, primitive);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        JSONObject contactJson = null;
        JSONObject authTokenJson = null;
        try {
            contactJson = packet.data.getJSONObject("self");
            authTokenJson = packet.data.getJSONObject("token");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // 校验 Token
        AuthToken authToken = new AuthToken(authTokenJson);

        String tokenCode = this.getTokenCode(action);
        if (!tokenCode.equals(authToken.getCode())) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.InconsistentToken.code, packet.data));
            return;
        }

        // 创建联系人对象
        Contact contact = new Contact(contactJson, this.talkContext);

        // 活跃设备
        Device activeDevice = contact.getDevice(this.talkContext);
        if (null == activeDevice) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.Failure.code, packet.data));
            return;
        }

        // 设置终端的对应关系
        Contact newContact = ContactManager.getInstance().signIn(contact, authToken, activeDevice);
        if (null == newContact) {
            this.cellet.speak(this.talkContext,
                    this.makeResponse(action, packet, ContactStateCode.IllegalOperation.code, packet.data));
            return;
        }

        // 应答
        this.cellet.speak(this.talkContext,
                this.makeResponse(action, packet, ContactStateCode.Ok.code, newContact.toJSON()));
    }
}
