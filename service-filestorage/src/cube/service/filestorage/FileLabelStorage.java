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

package cube.service.filestorage;

import cell.core.talk.LiteralBase;
import cell.util.json.JSONObject;
import cube.common.Storagable;
import cube.common.entity.FileLabel;
import cube.core.Storage;
import cube.core.StorageField;
import cube.service.filestorage.system.FileDescriptor;
import cube.storage.StorageFactory;
import cube.storage.StorageType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * 文件码存储器。
 * 用于管理文件码存储。
 */
public class FileLabelStorage implements Storagable {

    private final String version = "1.0";

    private final String labelTablePrefix = "filelabel_";

    private final StorageField[] labelFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG)
    };

    private ExecutorService executor;

    private Storage storage;

    private Map<String, String> tableNameMap;

    public FileLabelStorage(ExecutorService executorService, StorageType type, JSONObject config) {
        this.executor = executorService;
        this.storage = StorageFactory.getInstance().createStorage(type, "FileLabelStorage", config);
        this.tableNameMap = new HashMap<>();
    }

    @Override
    public void open() {
        this.storage.open();
    }

    @Override
    public void close() {
        this.storage.close();
    }

    @Override
    public void execSelfChecking(List<String> domainNameList) {

    }

    public void writeFileDescriptor(String fileCode, FileDescriptor descriptor) {

    }

    public void writeFileLabel(FileLabel fileLabel) {

    }

    public FileLabel readFileLabel(String fileCode) {
        return null;
    }
}
