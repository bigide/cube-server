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

import cube.util.SQLUtils;

import java.util.List;

/**
 * 条件句式。
 */
public class Conditional {

    private String sql;

    protected Conditional(String sql) {
        this.sql = sql;
    }

    @Override
    public String toString() {
        return this.sql;
    }

    public static Conditional createAnd() {
        return new Conditional("AND");
    }

    public static Conditional createOr() {
        return new Conditional("OR");
    }

    public static Conditional createGreaterThan(StorageField field, long value) {
        return new Conditional("[" + field.getName() + "]>" + value);
    }

    public static Conditional createLessThan(StorageField field, long value) {
        return new Conditional("[" + field.getName() + "]<" + value);
    }

    /**
     * 创建 column IN (value1, value2, ...) 条件。
     *
     * @param field
     * @param values
     * @return
     */
    public static Conditional createIN(StorageField field, Object[] values) {
        StringBuilder buf = new StringBuilder();
        buf.append("[").append(field.getName()).append("]");
        buf.append(" IN (");
        for (Object value : values) {
            switch (field.getLiteralBase()) {
                case LONG:
                    buf.append(((Long) value).longValue());
                    break;
                case INT:
                    buf.append(((Integer) value).intValue());
                    break;
                case BOOL:
                    buf.append(((Boolean) value).booleanValue() ? "1" : "0");
                    break;
                case STRING:
                    buf.append("'").append(SQLUtils.correctString(value.toString())).append("'");
                    break;
                default:
                    buf.append("0");
                    break;
            }
            buf.append(",");
        }
        // 修正逗号
        buf.delete(buf.length() - 1, buf.length());

        buf.append(")");
        return new Conditional(buf.toString());
    }
}
