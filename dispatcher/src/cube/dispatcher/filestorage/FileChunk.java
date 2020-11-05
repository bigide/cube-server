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

package cube.dispatcher.filestorage;
/**
 * 文件数据块。
 */
public class FileChunk implements Comparable<FileChunk> {

    protected Long contactId;

    protected String domain;

    protected String token;

    protected String fileName;

    protected long fileSize;

    protected long cursor;

    protected int size;

    protected byte[] data;

    /**
     * 构造函数。
     *
     * @param contactId
     * @param domain
     * @param token
     * @param fileName
     * @param fileSize
     * @param cursor
     * @param size
     * @param data
     */
    public FileChunk(Long contactId, String domain, String token, String fileName, long fileSize, long cursor, int size, byte[] data) {
        this.contactId = contactId;
        this.domain = domain;
        this.token = token;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.cursor = cursor;
        this.size = size;
        this.data = data;
    }

    @Override
    public boolean equals(Object object) {
        if (null != object && object instanceof FileChunk) {
            FileChunk other = (FileChunk) object;
            if (other.cursor == this.cursor) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int compareTo(FileChunk other) {
        return (int) (this.cursor - other.cursor);
    }
}
