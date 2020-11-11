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

import cell.util.CachedQueueExecutor;

import java.util.List;
import java.util.concurrent.ExecutorService;

/**
 * 管理器守护线程。
 * 不使用系统的定时器机制，而使用线程自旋方式，让整个任务始终持有时间片。
 */
public class ManagementDaemon extends Thread {

    private Kernel kernel;

    private boolean spinning = true;

    private final long spinningSleep = 60L * 1000L;

    private ExecutorService executor;

    public ManagementDaemon(Kernel kernel) {
        setName("ManagementDaemon");
        setDaemon(true);
        this.kernel = kernel;
        this.executor = CachedQueueExecutor.newCachedQueueThreadPool(4);
    }

    @Override
    public void run() {
        while (this.spinning) {
            try {
                Thread.sleep(this.spinningSleep);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            List<AbstractModule> list = this.kernel.getModules();
            for (int i = 0, size = list.size(); i < size; ++i) {
                final AbstractModule module = list.get(i);
                this.executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        module.onTick(module, kernel);
                    }
                });
            }
        }
    }

    public final void terminate() {
        this.spinning = false;
        this.executor.shutdown();
    }
}
