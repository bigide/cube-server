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

package cube.common.entity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 群组的附录。
 */
public class GroupAppendix extends Entity {

    private Group owner;

    private HashMap<Long, String> remarkContents;

    private String notice;

    private HashMap<Long, String> memberRemarks;

    /**
     * 构造函数。
     *
     * @param owner
     */
    public GroupAppendix(Group owner) {
        super();
        this.uniqueKey = owner.getUniqueKey() + "_appendix";
        this.owner = owner;
        this.remarkContents = new HashMap<>();
        this.memberRemarks = new HashMap<>();
    }

    /**
     * 构造函数。
     *
     * @param owner
     * @param json
     */
    public GroupAppendix(Group owner, JSONObject json) {
        super();
        this.uniqueKey = owner.getUniqueKey() + "_appendix";
        this.owner = owner;
        this.remarkContents = new HashMap<>();
        this.memberRemarks = new HashMap<>();

        JSONArray remarkNamesArray = json.getJSONArray("remarkContents");
        for (int i = 0; i < remarkNamesArray.length(); ++i) {
            JSONObject item = remarkNamesArray.getJSONObject(i);
            Long id = item.getLong("id");
            String content = item.getString("content");
            this.remarkContents.put(id, content);
        }

        if (json.has("notice")) {
            this.notice = json.getString("notice");
        }

        if (json.has("memberRemarks")) {
            JSONArray array = json.getJSONArray("memberRemarks");
            for (int i = 0; i < array.length(); ++i) {
                JSONObject item = array.getJSONObject(i);
                Long id = item.getLong("id");
                String remark = item.getString("remark");
                this.memberRemarks.put(id, remark);
            }
        }
    }

    /**
     * 返回附件所属的群组。
     *
     * @return 返回附件所属的群组。
     */
    public Group getOwner() {
        return this.owner;
    }

    /**
     * 指定成员备注该群组的信息。
     *
     * @param member
     * @param content
     */
    public void remark(Contact member, String content) {
        this.remarkContents.put(member.getId(), content);
        this.resetTimestamp();
    }

    /**
     * 获取指定成员对该群组的备注信息。
     *
     * @param member
     * @return
     */
    public String getRemark(Contact member) {
        this.resetTimestamp();
        return this.remarkContents.get(member.getId());
    }

    /**
     * 设置群组公告。
     *
     * @param notice
     */
    public void setNotice(String notice) {
        this.resetTimestamp();
        this.notice = notice;
    }

    /**
     * 获取指定成员的备注。
     *
     * @param memberId
     * @return
     */
    public String getMemberRemark(Long memberId) {
        return this.memberRemarks.get(memberId);
    }

    /**
     * 设置成员的备注。
     *
     * @param memberId
     * @param remark
     */
    public void setMemberRemark(Long memberId, String remark) {
        this.memberRemarks.put(memberId, remark);
    }

    public JSONObject packJSON(Contact member) {
        JSONObject json = new JSONObject();
        json.put("owner", this.owner.toCompactJSON());
        String remarkContent = this.remarkContents.get(member.getId());
        if (null == remarkContent) {
            remarkContent = "";
        }
        json.put("remark", remarkContent);
        json.put("notice", (null == this.notice) ? "" : this.notice);

        JSONArray memberRemarkArray = new JSONArray();
        for (Map.Entry<Long, String> e : this.memberRemarks.entrySet()) {
            JSONObject mr = new JSONObject();
            mr.put("id", e.getKey().longValue());
            mr.put("remark", e.getValue());
            memberRemarkArray.put(mr);
        }
        json.put("memberRemarks", memberRemarkArray);

        return json;
    }

    @Override
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("ownerId", this.owner.getId());

        JSONArray remarkContentsArray = new JSONArray();
        Iterator<Map.Entry<Long, String>> iter = this.remarkContents.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Long, String> e = iter.next();
            JSONObject item = new JSONObject();
            item.put("id", e.getKey().longValue());
            item.put("content", e.getValue());
            remarkContentsArray.put(item);
        }
        json.put("remarkContents", remarkContentsArray);

        json.put("notice", (null == this.notice) ? "" : this.notice);

        JSONArray memberRemarkArray = new JSONArray();
        for (Map.Entry<Long, String> e : this.memberRemarks.entrySet()) {
            JSONObject mr = new JSONObject();
            mr.put("id", e.getKey().longValue());
            mr.put("remark", e.getValue());
            memberRemarkArray.put(mr);
        }
        json.put("memberRemarks", memberRemarkArray);

        return json;
    }

    @Override
    public JSONObject toCompactJSON() {
        return this.toJSON();
    }

    /**
     * 生成附录的唯一键。
     *
     * @param group
     * @return
     */
    public static String makeUniqueKey(Group group) {
        return group.getUniqueKey() + "_appendix";
    }
}
