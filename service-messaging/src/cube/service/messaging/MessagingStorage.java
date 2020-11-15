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

package cube.service.messaging;

import cell.core.talk.LiteralBase;
import cell.util.json.JSONException;
import cell.util.json.JSONObject;
import cell.util.log.Logger;
import cube.common.Storagable;
import cube.common.entity.Message;
import cube.common.entity.MessageState;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.Storage;
import cube.core.StorageField;
import cube.storage.StorageFactory;
import cube.storage.StorageFields;
import cube.storage.StorageType;
import cube.util.SQLUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * 消息存储器。
 */
public class MessagingStorage implements Storagable {

    private final String version = "1.0";

    private final String messageTablePrefix = "message_";

    private final String stateTablePrefix = "state_";

    /**
     * 消息字段描述。
     */
    private final StorageField[] messageFields = new StorageField[] {
            new StorageField("id", LiteralBase.LONG),
            new StorageField("from", LiteralBase.LONG),
            new StorageField("to", LiteralBase.LONG),
            new StorageField("source", LiteralBase.LONG),
            new StorageField("lts", LiteralBase.LONG),
            new StorageField("rts", LiteralBase.LONG),
            new StorageField("state", LiteralBase.INT),
            new StorageField("device", LiteralBase.STRING),
            new StorageField("payload", LiteralBase.STRING),
            new StorageField("attachment", LiteralBase.STRING)
    };

    /**
     * 消息状态描述。
     */
    private final StorageField[] stateFields = new StorageField[] {
            new StorageField("contact_id", LiteralBase.LONG),
            new StorageField("message_id", LiteralBase.LONG),
            new StorageField("state", LiteralBase.INT),
            new StorageField("timestamp", LiteralBase.LONG)
    };

    private ExecutorService executor;

    private Storage storage;

    private Map<String, String> messageTableNameMap;

//    private Map<String, String> stateTableNameMap;

    public MessagingStorage(ExecutorService executor, Storage storage) {
        this.executor = executor;
        this.storage = storage;
        this.messageTableNameMap = new HashMap<>();
//        this.stateTableNameMap = new HashMap<>();
    }

    public MessagingStorage(ExecutorService executor, StorageType type, JSONObject config) {
        this.executor = executor;
        this.storage = StorageFactory.getInstance().createStorage(type, "MessagingStorage", config);
        this.messageTableNameMap = new HashMap<>();
//        this.stateTableNameMap = new HashMap<>();
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
        String table = "cube";

        StorageField[] fields = new StorageField[] {
                new StorageField("item", LiteralBase.STRING),
                new StorageField("desc", LiteralBase.STRING)
        };

        List<StorageField[]> result = this.storage.executeQuery(table, fields);
        if (result.isEmpty()) {
            // 数据库没有找到表，创建新表
            if (this.storage.executeCreate(table, fields)) {
                // 插入数据
                StorageField[] data = new StorageField[] {
                        new StorageField("item", LiteralBase.STRING, "version"),
                        new StorageField("desc", LiteralBase.STRING, this.version)
                };
                this.storage.executeInsert(table, data);
                Logger.i(this.getClass(), "Insert into 'cube' data");
            }
            else {
                Logger.e(this.getClass(), "Create table 'cube' failed - " + this.storage.getName());
            }
        }
        else {
            // 校验版本
            for (StorageField[] row : result) {
                if (row[0].getString().equals("version")) {
                    Logger.i(this.getClass(), "Message storage version " + row[1].getString());
                }
            }
        }

        // 校验域对应的表
        for (String domain : domainNameList) {
            // 检查消息表
            this.checkMessageTable(domain);

            // 检查状态表
//            this.checkStateTable(domain);
        }
    }

    /**
     *
     * @param message
     */
    public void write(final Message message) {
        this.write(message, null);
    }

    /**
     *
     * @param message
     * @param completed
     */
    public void write(final Message message, final Runnable completed) {
        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                String domain = message.getDomain().getName();
                // 取表名
                String table = messageTableNameMap.get(domain);
                if (null == table) {
                    return;
                }

                StorageField[] fields = new StorageField[] {
                        new StorageField("id", LiteralBase.LONG, message.getId()),
                        new StorageField("from", LiteralBase.LONG, message.getFrom()),
                        new StorageField("to", LiteralBase.LONG, message.getTo()),
                        new StorageField("source", LiteralBase.LONG, message.getSource()),
                        new StorageField("lts", LiteralBase.LONG, message.getLocalTimestamp()),
                        new StorageField("rts", LiteralBase.LONG, message.getRemoteTimestamp()),
                        new StorageField("state", LiteralBase.INT, message.getState().getCode()),
                        new StorageField("device", LiteralBase.STRING, message.getSourceDevice().toJSON().toString()),
                        new StorageField("payload", LiteralBase.STRING, message.getPayload().toString()),
                        new StorageField("attachment", LiteralBase.STRING,
                                (null != message.getAttachment()) ? message.getAttachment().toJSON().toString() : null)
                };

                synchronized (storage) {
                    storage.executeInsert(table, fields);
                }

                if (null != completed) {
                    completed.run();
                }
            }
        });
    }

    public Message read(String domain, Long contactId, Long messageId) {
        String table = this.messageTableNameMap.get(domain);
        if (null == table) {
            return null;
        }

        List<StorageField[]> result = null;

        synchronized (this.storage) {
            result = this.storage.executeQuery(table, this.messageFields, new Conditional[] {
                    Conditional.createEqualTo("id", LiteralBase.LONG, messageId),
                    Conditional.createAnd(),
                    Conditional.createBracket(new Conditional[] {
                            Conditional.createEqualTo(new StorageField("from", LiteralBase.LONG, contactId)),
                            Conditional.createOr(),
                            Conditional.createEqualTo(new StorageField("to", LiteralBase.LONG, contactId))
                    })
            });
        }

        if (result.isEmpty()) {
            return null;
        }

        Map<String, StorageField> map = StorageFields.get(result.get(0));

        JSONObject device = null;
        JSONObject payload = null;
        JSONObject attachment = null;
        try {
            device = new JSONObject(map.get("device").getString());
            payload = new JSONObject(map.get("payload").getString());
            if (!map.get("attachment").isNullValue()) {
                attachment = new JSONObject(map.get("attachment").getString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Message message = new Message(domain, map.get("id").getLong(),
                map.get("from").getLong(), map.get("to").getLong(), map.get("source").getLong(),
                map.get("lts").getLong(), map.get("rts").getLong(), map.get("state").getInt(),
                device, payload, attachment);
        return message;
    }

    /**
     * 读取指定消息 ID 的所有消息。
     *
     * @param domain
     * @param messageId
     * @return
     */
    public List<Message> read(String domain, Long messageId) {
        List<Message> result = new ArrayList<>();

        String table = this.messageTableNameMap.get(domain);

        List<StorageField[]> list = this.storage.executeQuery(table, this.messageFields, new Conditional[] {
                Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, messageId))
        });

        for (StorageField[] row : list) {
            Map<String, StorageField> map = StorageFields.get(row);

            JSONObject device = null;
            JSONObject payload = null;
            JSONObject attachment = null;
            try {
                device = new JSONObject(map.get("device").getString());
                payload = new JSONObject(map.get("payload").getString());
                if (!map.get("attachment").isNullValue()) {
                    attachment = new JSONObject(map.get("attachment").getString());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Message message = new Message(domain, map.get("id").getLong(),
                    map.get("from").getLong(), map.get("to").getLong(), map.get("source").getLong(),
                    map.get("lts").getLong(), map.get("rts").getLong(), map.get("state").getInt(),
                    device, payload, attachment);

            result.add(message);
        }

        return result;
    }

    /**
     * 读取消息。
     *
     * @param domain
     * @param contactId
     * @param messageIdList
     * @return
     */
    public List<Message> read(String domain, Long contactId, List<Long> messageIdList) {
        // 取表名
        String table = this.messageTableNameMap.get(domain);
        if (null == table) {
            return null;
        }

        Object[] values = new Object[messageIdList.size()];
        for (int i = 0; i < values.length; ++i) {
            values[i] = messageIdList.get(i);
        }

        List<StorageField[]> result = null;
        synchronized (this.storage) {
            result = this.storage.executeQuery(table, this.messageFields,
                    new Conditional[] { Conditional.createIN(this.messageFields[0], values) });
        }

        List<Message> messages = new ArrayList<>(result.size());
        for (StorageField[] row : result) {
            Map<String, StorageField> map = StorageFields.get(row);

            JSONObject device = null;
            JSONObject payload = null;
            JSONObject attachment = null;
            try {
                device = new JSONObject(map.get("device").getString());
                payload = new JSONObject(map.get("payload").getString());
                if (!map.get("attachment").isNullValue()) {
                    attachment = new JSONObject(map.get("attachment").getString());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Message message = new Message(domain, map.get("id").getLong(),
                    map.get("from").getLong(), map.get("to").getLong(), map.get("source").getLong(),
                    map.get("lts").getLong(), map.get("rts").getLong(), map.get("state").getInt(),
                    device, payload, attachment);
            if (message.getFrom().longValue() == contactId.longValue()
                    || message.getTo().longValue() == contactId.longValue()) {
                messages.add(message);
            }
        }

        return messages;
    }

    public List<Message> readWithFromOrderByTime(String domain, Long id, long beginning, long ending) {
        // 取表名
        String table = this.messageTableNameMap.get(domain);
        if (null == table) {
            return null;
        }

        List<StorageField[]> result = null;
        synchronized (this.storage) {
            result = this.storage.executeQuery(table, this.messageFields, new Conditional[] {
                    Conditional.createEqualTo(new StorageField("from", LiteralBase.LONG, id)),
                    Conditional.createAnd(),
                    Conditional.createGreaterThan(new StorageField("rts", LiteralBase.LONG, beginning)),
                    Conditional.createAnd(),
                    Conditional.createLessThanEqual(new StorageField("rts", LiteralBase.LONG, ending))
            });
        }

        List<Message> messages = new ArrayList<>(result.size());
        for (StorageField[] row : result) {
            Map<String, StorageField> map = StorageFields.get(row);

            JSONObject device = null;
            JSONObject payload = null;
            JSONObject attachment = null;
            try {
                device = new JSONObject(map.get("device").getString());
                payload = new JSONObject(map.get("payload").getString());
                if (!map.get("attachment").isNullValue()) {
                    attachment = new JSONObject(map.get("attachment").getString());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Message message = new Message(domain, map.get("id").getLong(),
                    map.get("from").getLong(), map.get("to").getLong(), map.get("source").getLong(),
                    map.get("lts").getLong(), map.get("rts").getLong(), map.get("state").getInt(),
                    device, payload, attachment);
            if (message.getState() == MessageState.Read && message.getState() == MessageState.Sent) {
                messages.add(message);
            }
        }

        return messages;
    }

    public List<Message> readWithToOrderByTime(String domain, Long id, long beginning, long ending) {
        // 取表名
        String table = this.messageTableNameMap.get(domain);
        if (null == table) {
            return null;
        }

        List<StorageField[]> result = null;
        synchronized (this.storage) {
            result = this.storage.executeQuery(table, this.messageFields, new Conditional[] {
                    Conditional.createEqualTo(new StorageField("to", LiteralBase.LONG, id)),
                    Conditional.createAnd(),
                    Conditional.createGreaterThan(new StorageField("rts", LiteralBase.LONG, beginning)),
                    Conditional.createAnd(),
                    Conditional.createLessThanEqual(new StorageField("rts", LiteralBase.LONG, ending))
            });
        }

        List<Message> messages = new ArrayList<>(result.size());

        for (StorageField[] row : result) {
            Map<String, StorageField> map = StorageFields.get(row);

            JSONObject device = null;
            JSONObject payload = null;
            JSONObject attachment = null;
            try {
                device = new JSONObject(map.get("device").getString());
                payload = new JSONObject(map.get("payload").getString());
                if (!map.get("attachment").isNullValue()) {
                    attachment = new JSONObject(map.get("attachment").getString());
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Message message = new Message(domain, map.get("id").getLong(),
                    map.get("from").getLong(), map.get("to").getLong(), map.get("source").getLong(),
                    map.get("lts").getLong(), map.get("rts").getLong(), map.get("state").getInt(),
                    device, payload, attachment);
            if (message.getState() == MessageState.Read && message.getState() == MessageState.Sent) {
                messages.add(message);
            }
        }

        return messages;
    }

    public void writeMessageState(String domain, Long messageId, MessageState state) {
        String table = this.messageTableNameMap.get(domain);

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                StorageField[] fields = new StorageField[] {
                        new StorageField("state", LiteralBase.INT, state.getCode())
                };

                synchronized (storage) {
                    storage.executeUpdate(table, fields, new Conditional[] {
                            Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, messageId))
                    });
                }
            }
        });
    }

    public void writeMessageState(String domain, Long contactId, Long messageId, MessageState state) {
        String table = this.messageTableNameMap.get(domain);

        this.executor.execute(new Runnable() {
            @Override
            public void run() {
                StorageField[] fields = new StorageField[] {
                        new StorageField("state", LiteralBase.INT, state.getCode())
                };

                synchronized (storage) {
                    storage.executeUpdate(table, fields, new Conditional[] {
                            Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, messageId)),
                            Conditional.createAnd(),
                            Conditional.createBracket(new Conditional[] {
                                    Conditional.createEqualTo(new StorageField("from", LiteralBase.LONG, contactId)),
                                    Conditional.createOr(),
                                    Conditional.createEqualTo(new StorageField("to", LiteralBase.LONG, contactId))
                            })
                    });
                }
            }
        });
    }

    public MessageState readMessageState(String domain, Long contactId, Long messageId) {
        String table = this.messageTableNameMap.get(domain);
        if (null == table) {
            return null;
        }

        StorageField[] fields = new StorageField[] {
                new StorageField("state", LiteralBase.INT)
        };

        List<StorageField[]> result = null;
        synchronized (this.storage) {
            result = this.storage.executeQuery(table, fields, new Conditional[]{
                    Conditional.createEqualTo(new StorageField("id", LiteralBase.LONG, messageId)),
                    Conditional.createAnd(),
                    Conditional.createBracket(new Conditional[] {
                            Conditional.createEqualTo(new StorageField("from", LiteralBase.LONG, contactId)),
                            Conditional.createOr(),
                            Conditional.createEqualTo(new StorageField("to", LiteralBase.LONG, contactId))
                    })
            });

            if (result.isEmpty()) {
                return null;
            }
        }

        int state = result.get(0)[0].getInt();
        return MessageState.parse(state);
    }

    private void checkMessageTable(String domain) {
        String table = this.messageTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.messageTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            StorageField[] fields = new StorageField[] {
                    new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                            Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
                    }),
                    new StorageField("id", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("from", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("to", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("source", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL, Constraint.DEFAULT_0
                    }),
                    new StorageField("lts", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL, Constraint.DEFAULT_0
                    }),
                    new StorageField("rts", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL, Constraint.DEFAULT_0
                    }),
                    new StorageField("state", LiteralBase.INT, new Constraint[] {
                            Constraint.NOT_NULL, Constraint.DEFAULT_0
                    }),
                    new StorageField("device", LiteralBase.STRING, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("payload", LiteralBase.STRING, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("attachment", LiteralBase.STRING, new Constraint[] {
                            Constraint.DEFAULT_NULL
                    })
            };

            if (this.storage.executeCreate(table, fields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }
        }
    }

    /*private void checkStateTable(String domain) {
        String table = this.stateTablePrefix + domain;

        table = SQLUtils.correctTableName(table);
        this.stateTableNameMap.put(domain, table);

        if (!this.storage.exist(table)) {
            // 表不存在，建表
            StorageField[] fields = new StorageField[] {
                    new StorageField("sn", LiteralBase.LONG, new Constraint[] {
                            Constraint.PRIMARY_KEY, Constraint.AUTOINCREMENT
                    }),
                    new StorageField("contact_id", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("message_id", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("state", LiteralBase.INT, new Constraint[] {
                            Constraint.NOT_NULL
                    }),
                    new StorageField("timestamp", LiteralBase.LONG, new Constraint[] {
                            Constraint.NOT_NULL
                    })
            };

            if (this.storage.executeCreate(table, fields)) {
                Logger.i(this.getClass(), "Created table '" + table + "' successfully");
            }
        }
    }*/
}
