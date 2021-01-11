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

package cube.service.multipointcomm.signaling;

import cube.common.action.MultipointCommAction;
import cube.common.entity.CommField;
import cube.common.entity.Contact;
import cube.common.entity.Device;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Answer 信令。
 */
public class AnswerSignaling extends Signaling {

    private JSONObject sessionDescription;

    private JSONObject mediaConstraint;

    private Contact caller;

    private Contact callee;

    public AnswerSignaling(CommField field, Contact contact, Device device, Long rtcSN) {
        super(MultipointCommAction.Answer.name, field, contact, device, rtcSN);
    }

    public AnswerSignaling(JSONObject json) {
        super(json);

        try {
            this.sessionDescription = json.getJSONObject("description");
            this.mediaConstraint = json.getJSONObject("constraint");

            if (json.has("caller")) {
                this.caller = new Contact(json.getJSONObject("caller"));
            }
            if (json.has("callee")) {
                this.caller = new Contact(json.getJSONObject("callee"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void copy(AnswerSignaling source) {
        this.caller = source.caller;
        this.callee = source.callee;
        this.sessionDescription = source.sessionDescription;
        this.mediaConstraint = source.mediaConstraint;
    }

    public void setCaller(Contact caller) {
        this.caller = caller;
    }

    public Contact getCaller() {
        return this.caller;
    }

    public void setCallee(Contact callee) {
        this.callee = callee;
    }

    public Contact getCallee() {
        return this.callee;
    }

    public JSONObject getSessionDescription() {
        return this.sessionDescription;
    }

    public JSONObject getMediaConstraint() {
        return this.mediaConstraint;
    }

    public String getType() {
        try {
            return this.sessionDescription.getString("type");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getSDP() {
        try {
            return this.sessionDescription.getString("sdp");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = super.toJSON();
        try {
            json.put("description", this.sessionDescription);
            json.put("constraint", this.mediaConstraint);
            if (null != this.caller) {
                json.put("caller", this.caller.toBasicJSON());
            }
            if (null != this.callee) {
                json.put("callee", this.callee.toBasicJSON());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }
}
