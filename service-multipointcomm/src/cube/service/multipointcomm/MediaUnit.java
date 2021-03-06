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

package cube.service.multipointcomm;

import cell.api.Speakable;
import cell.util.log.Logger;
import cube.common.Packet;
import cube.common.state.MultipointCommStateCode;
import cube.service.multipointcomm.signaling.Signaling;
import cube.service.multipointcomm.signaling.SignalingFactory;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 媒体单元描述。
 */
public class MediaUnit {

    protected final static String CELLET_NAME = "MediaUnit";

    protected String address;

    protected int port;

    protected Speakable speaker;

    protected MediaUnitListener listener;

    private ConcurrentHashMap<Long, SignalingBundle> sentSignalings;

    /**
     * 构造函数。
     *
     * @param address
     * @param port
     */
    public MediaUnit(String address, int port, MediaUnitListener listener) {
        this.address = address;
        this.port = port;
        this.listener = listener;
        this.sentSignalings = new ConcurrentHashMap<>();
    }

    /**
     * 发送信令数据到该媒体单元。
     *
     * @param packet
     * @param signaling
     * @param signalingCallback
     * @return
     */
    public boolean transmit(Packet packet, Signaling signaling, SignalingCallback signalingCallback) {
        if (this.speaker.speak(CELLET_NAME, packet.toDialect())) {
            SignalingBundle bundle = new SignalingBundle(signaling, signalingCallback);
            this.sentSignalings.put(packet.sn, bundle);
            return true;
        }

        return false;
    }

    /**
     * 接收来自媒体单元的数据包。
     *
     * @param packet
     */
    public void receive(Packet packet) {
        SignalingBundle bundle = this.sentSignalings.remove(packet.sn);
        if (null != bundle) {
            // 处理信令回包
            if (null != bundle.callback) {
                try {
                    int code = packet.data.getInt("code");
                    JSONObject data = packet.data.getJSONObject("data");

                    Signaling signaling = SignalingFactory.getInstance().createSignaling(data);
                    bundle.callback.on(MultipointCommStateCode.match(code), signaling);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
        else {
            // 接收处理数据

            if (MediaUnitAction.Signaling.name.equals(packet.name)) {
                Signaling signaling = SignalingFactory.getInstance().createSignaling(packet.data);
                Signaling response = this.listener.onSignaling(signaling);

                try {
                    int code = MultipointCommStateCode.Ok.code;
                    JSONObject data = response.toCompactJSON();

                    JSONObject payload = new JSONObject();
                    payload.put("code", code);
                    payload.put("data", data);

                    Packet responsePacket = new Packet(packet.sn, MediaUnitAction.SignalingAck.name, payload);
                    this.speaker.speak(CELLET_NAME, responsePacket.toDialect());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            else {
                Logger.e(this.getClass(), "Unknown media unit action: " + packet.name);
            }
        }
    }

    /**
     * 信令束。
     */
    protected class SignalingBundle {

        protected Signaling signaling;

        protected SignalingCallback callback;

        protected SignalingBundle(Signaling signaling, SignalingCallback callback) {
            this.signaling = signaling;
            this.callback = callback;
        }
    }
}
