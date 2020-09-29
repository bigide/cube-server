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

package cube.common;

import cell.util.json.JSONException;
import cell.util.json.JSONObject;

/**
 * 模块事件描述。
 */
public class ModuleEvent implements JSONable {

    /**
     * 模块名。
     */
    private String moduleName;

    /**
     * 事件名。
     */
    private String eventName;

    /**
     * 事件的数据负载。
     */
    private JSONObject data;

    /**
     * 构造函数。
     *
     * @param moduleName 模块名。
     * @param eventName 事件名。
     * @param data 对应的数据。
     */
    public ModuleEvent(String moduleName, String eventName, JSONObject data) {
        this.moduleName = moduleName;
        this.eventName = eventName;
        this.data = data;
    }

    /**
     * 构造函数。
     *
     * @param json
     */
    public ModuleEvent(JSONObject json) {
        try {
            this.moduleName = json.getString("module");
            this.eventName = json.getString("event");
            this.data = json.getJSONObject("data");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public String getModuleName() {
        return this.moduleName;
    }

    public String getEventName() {
        return this.eventName;
    }

    public JSONObject getData() {
        return this.data;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        try {
            json.put("module", this.moduleName);
            json.put("event", this.eventName);
            json.put("data", this.data);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static String getModuleName(JSONObject json) {
        String mod = null;
        try {
            mod = json.getString("module");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return mod;
    }
}
