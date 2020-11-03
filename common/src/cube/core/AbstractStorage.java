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

package cube.core;

import cell.util.json.JSONObject;

import java.util.List;

/**
 * 存储器的抽象层。
 */
public abstract class AbstractStorage implements Storage {

    /**
     * 存储名称。
     */
    private String name;

    /**
     * 配置信息。
     */
    protected JSONObject config;

    /**
     * 构造函数。
     *
     * @param name 指定存储名称。
     */
    public AbstractStorage(String name) {
        this.name = name;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return this.name;
    }

    /**
     * {@inheritDoc}
     */
    public JSONObject getConfig() {
        return this.config;
    }

    /**
     * {@inheritDoc}
     */
    public void configure(JSONObject config) {
        this.config = config;
    }

}
