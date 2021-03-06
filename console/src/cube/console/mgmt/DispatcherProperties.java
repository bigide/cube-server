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
    public final static String KEY_HTTP_HOST = "http.host";
    public final static String KEY_HTTP_PORT = "http.port";
    public final static String KEY_HTTPS_HOST = "https.host";
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

    private List<DirectorProperties> directorProperties;

    public DispatcherProperties(String fullPath) {
        this.fullPath = fullPath;
        this.directorProperties = new ArrayList<>();
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

    public String getHttpHost() {
        return this.properties.getProperty(KEY_HTTP_HOST);
    }

    public int getHttpPort() {
        return Integer.parseInt(this.properties.getProperty(KEY_HTTP_PORT, "7010"));
    }

    public AccessPoint getHttpAccessPoint() {
        return new AccessPoint(this.getHttpHost(), this.getHttpPort(), 0);
    }

    public String getHttpsHost() {
        return this.properties.getProperty(KEY_HTTPS_HOST);
    }

    public int getHttpsPort() {
        return Integer.parseInt(this.properties.getProperty(KEY_HTTPS_PORT, "7017"));
    }

    public AccessPoint getHttpsAccessPoint() {
        return new AccessPoint(this.getHttpsHost(), this.getHttpsPort(), 0);
    }

    public String getKeystore() {
        return this.properties.getProperty(KEY_KEYSTORE);
    }

    public String getStorePassword() {
        return this.properties.getProperty(KEY_STORE_PASSWORD);
    }

    public String getManagerPassword() {
        return this.properties.getProperty(KEY_MANAGER_PASSWORD);
    }

    public List<DirectorProperties> getDirectorProperties() {
        return this.directorProperties;
    }

    public void refresh() {
        try {
            this.properties = ConfigUtils.readProperties(this.fullPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int i = 1; i <= 50; ++i) {
            String key = KEY_DIRECTOR_PREFIX + i + KEY_DIRECTOR_ADDRESS;
            if (!this.properties.containsKey(key)) {
                continue;
            }

            String address = this.properties.getProperty(key);
            int port = Integer.parseInt(this.properties.getProperty(KEY_DIRECTOR_PREFIX + i + KEY_DIRECTOR_PORT));
            int weight = Integer.parseInt(this.properties.getProperty(KEY_DIRECTOR_PREFIX + i + KEY_DIRECTOR_WEIGHT));

            String celletsValue = this.properties.getProperty(KEY_DIRECTOR_PREFIX + i + KEY_DIRECTOR_CELLETS);
            String[] array = celletsValue.split(",");
            List<String> cellets = new ArrayList<>();
            for (String cellet : array) {
                cellets.add(cellet);
            }

            DirectorProperties dp = new DirectorProperties(address, port, cellets, weight);
            this.directorProperties.add(dp);
        }
    }
}
