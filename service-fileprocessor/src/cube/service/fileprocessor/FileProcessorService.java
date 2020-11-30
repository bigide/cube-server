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

package cube.service.fileprocessor;

import cube.common.entity.FileLabel;
import cube.core.AbstractModule;
import cube.core.Kernel;
import cube.core.Module;
import cube.service.filestorage.FileStorageService;

import java.util.concurrent.ExecutorService;

/**
 * 文件处理服务。
 */
public class FileProcessorService extends AbstractModule {

    public final static String NAME = "FileProcessor";

    private ExecutorService executor = null;

    public FileProcessorService(ExecutorService executor) {
        super();
        this.executor = executor;
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }

    @Override
    public void onTick(Module module, Kernel kernel) {

    }

    public FileLabel makeThumbnail(String domainName, String fileCode) {
        FileStorageService fileStorage = (FileStorageService) this.getKernel().getModule(FileStorageService.NAME);
        String fullpath = fileStorage.loadFileToDisk(domainName, fileCode);
        if (null == fullpath) {
            return null;
        }

        return null;
    }
}