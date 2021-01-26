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

package cube.service.filestorage.recycle;

import cube.service.filestorage.FileStructStorage;
import cube.service.filestorage.hierarchy.Directory;
import cube.service.filestorage.hierarchy.FileHierarchyTool;

import java.util.LinkedList;

/**
 * 文件回收站。
 */
public class RecycleBin {

    private FileStructStorage structStorage;

    public RecycleBin(FileStructStorage structStorage) {
        this.structStorage = structStorage;
    }

    public void put(Directory root, Directory directory) {
        // 遍历目录结构
        LinkedList<Directory> list = new LinkedList<>();
        FileHierarchyTool.recurse(list, directory);

        // 创建数据实体
        RecycleChain chain = new RecycleChain(list);
        DirectoryTrash directoryTrash = new DirectoryTrash(root.getId(), chain, directory);


    }

    public void recover(Directory root, Long directoryId) {

    }
}
