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

package cube.service.filestorage.task;

import cell.core.talk.Primitive;
import cell.core.talk.TalkContext;
import cell.core.talk.dialect.ActionDialect;
import cell.core.talk.dialect.DialectFactory;
import cube.auth.AuthToken;
import cube.common.Packet;
import cube.common.state.FileStorageStateCode;
import cube.service.ServiceTask;
import cube.service.auth.AuthService;
import cube.service.filestorage.FileStorageService;
import cube.service.filestorage.FileStorageServiceCellet;
import cube.service.filestorage.hierarchy.Directory;
import cube.service.filestorage.hierarchy.FileHierarchy;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

/**
 * 新建文件夹。
 */
public class NewDirectoryTask extends ServiceTask {

    public NewDirectoryTask(FileStorageServiceCellet cellet, TalkContext talkContext, Primitive primitive) {
        super(cellet, talkContext, primitive);
    }

    @Override
    public void run() {
        ActionDialect action = DialectFactory.getInstance().createActionDialect(this.primitive);
        Packet packet = new Packet(action);

        // 获取令牌码
        String tokenCode = this.getTokenCode(action);
        AuthService authService = (AuthService) this.kernel.getModule(AuthService.NAME);
        AuthToken authToken = authService.getToken(tokenCode);
        if (null == authToken) {
            // 发生错误
            this.cellet.speak(this.talkContext
                    , this.makeResponse(action, packet, FileStorageStateCode.InvalidDomain.code, packet.data));
            return;
        }

        // 域
        String domain = authToken.getDomain();

        // 读取参数
        if (!packet.data.has("root") || !packet.data.has("workingId") || !packet.data.has("dirName")) {
            // 发生错误
            this.cellet.speak(this.talkContext
                    , this.makeResponse(action, packet, FileStorageStateCode.Unauthorized.code, packet.data));
            return;
        }

        Long rootId = packet.data.getLong("root");
        Long workingId = packet.data.getLong("workingId");
        String dirName = packet.data.getString("dirName");

        // 获取服务
        FileStorageService service = (FileStorageService) this.kernel.getModule(FileStorageService.NAME);

        // 获取指定 ROOT ID 对应的文件层级描述
        FileHierarchy fileHierarchy = service.getFileHierarchy(domain, rootId);
        if (null == fileHierarchy) {
            // 发生错误
            this.cellet.speak(this.talkContext
                    , this.makeResponse(action, packet, FileStorageStateCode.NotFound.code, packet.data));
            return;
        }

        // 查找指定 ID 的目录
        Directory directory = fileHierarchy.getDirectory(workingId);
        if (null == directory) {
            // 发生错误
            this.cellet.speak(this.talkContext
                    , this.makeResponse(action, packet, FileStorageStateCode.NotFound.code, packet.data));
            return;
        }

        // 创建目录
        Directory newDir = directory.createDirectory(dirName);
        if (null == newDir) {
            // 发生错误
            this.cellet.speak(this.talkContext
                    , this.makeResponse(action, packet, FileStorageStateCode.DuplicationOfName.code, packet.data));
            return;
        }

        // 成功
        this.cellet.speak(this.talkContext
                , this.makeResponse(action, packet, FileStorageStateCode.Ok.code, newDir.toJSON()));
    }
}
