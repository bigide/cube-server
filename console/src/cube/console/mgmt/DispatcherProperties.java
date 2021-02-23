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

package cube.console.mgmt;

import cube.util.ConfigUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 调度机的属性描述。
 */
public class DispatcherProperties {

    public final static String KEY_CELLETS = "cellets";
    public final static String KEY_HTTP_PORT = "http.port";
    public final static String KEY_HTTPS_PORT = "https.port";
    public final static String KEY_KEYSTORE = "keystore";
    public final static String KEY_STORE_PASSWORD = "storePassword";
    public final static String KEY_MANAGER_PASSWORD = "managerPassword";

    public final static String KEY_DIRECTOR_PREFIX = "director.";
    public final static String KEY_DIRECTOR_ADDRESS = ".address";
    public final static String KEY_DIRECTOR_PORT = ".port";
    public final static String KEY_DIRECTOR_CELLETS = ".cellets";
    public final static String KEY_DIRECTOR_WEIGHT = ".weight";

    private String fullPath;

    private Properties properties;

    public DispatcherProperties(String fullPath) {
        this.fullPath = fullPath;
    }

    public List<String> getCellets() {
        String value = this.properties.getProperty(KEY_CELLETS);
        if (null == value) {
            return null;
        }

        List<String> list = new ArrayList<>();
        String[] array = value.trim().split(",");
        for (String name : array) {
            list.add(name);
        }
        return list;
    }

    public void refresh() {
        try {
            this.properties = ConfigUtils.readProperties(fullPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
