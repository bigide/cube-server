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

package cube.service.multipointcomm;

import cell.api.Speakable;
import cell.api.TalkListener;
import cell.api.TalkService;
import cell.core.talk.Primitive;
import cell.core.talk.PrimitiveInputStream;
import cell.core.talk.TalkError;
import cube.common.entity.CommField;
import cube.service.multipointcomm.signaling.Signaling;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 媒体单元的主机。
 */
public class MediaUnitLeader implements TalkListener {

    private final static String CELLET_NAME = "MediaUnit";

    private TalkService talkService;

    private List<MediaUnit> mediaUnitList;

    private ConcurrentHashMap<Long, MediaUnitBundle> bundles;

    public MediaUnitLeader() {
        this.mediaUnitList = new ArrayList<>();
        this.bundles = new ConcurrentHashMap<>();
    }

    public void start(TalkService talkService) {
        this.talkService = talkService;
        this.talkService.setListener(CELLET_NAME, this);

        for (MediaUnit mediaUnit : this.mediaUnitList) {
            mediaUnit.speaker = this.talkService.call(mediaUnit.address, mediaUnit.port);
        }
    }

    public void stop() {
        this.talkService.removeListener(CELLET_NAME);

        for (MediaUnit mediaUnit : this.mediaUnitList) {
            this.talkService.hangup(mediaUnit.address, mediaUnit.port, false);
        }
    }

    public void dispatch(CommField commField, Signaling signaling) {
        // 选择媒体单元
        MediaUnit mediaUnit = selectMediaUnit(commField);

        // 向媒体单元发送信令
        this.sendSignaling(mediaUnit, signaling);
    }

    private MediaUnit selectMediaUnit(CommField commField) {
        MediaUnitBundle bundle = this.bundles.get(commField.getId());
        return null;
    }

    private void sendSignaling(MediaUnit mediaUnit, Signaling signaling) {
        mediaUnit.speaker.speak(CELLET_NAME, signaling.toActionDialect());
    }

    protected void readConfig(Properties properties) {
        // 读取 Unit 配置
        for (int i = 1; i <= 50; ++i) {
            String keyAddress = "unit." + i + ".address";
            if (properties.containsKey(keyAddress)) {
//                String address =
            }
        }
    }

    @Override
    public void onListened(Speakable speaker, String cellet, Primitive primitive) {

    }

    @Override
    public void onListened(Speakable speaker, String cellet, PrimitiveInputStream primitiveInputStream) {

    }

    @Override
    public void onSpoke(Speakable speaker, String cellet, Primitive primitive) {

    }

    @Override
    public void onAck(Speakable speaker, String cellet, Primitive primitive) {

    }

    @Override
    public void onSpeakTimeout(Speakable speaker, String cellet, Primitive primitive) {

    }

    @Override
    public void onContacted(Speakable speaker) {

    }

    @Override
    public void onQuitted(Speakable speaker) {

    }

    @Override
    public void onFailed(Speakable speaker, TalkError talkError) {

    }
}
