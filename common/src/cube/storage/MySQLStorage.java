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

package cube.storage;

import cell.core.talk.LiteralBase;
import cell.util.log.Logger;
import cube.core.AbstractStorage;
import cube.core.Conditional;
import cube.core.Constraint;
import cube.core.StorageField;
import cube.util.SQLUtils;
import org.json.JSONObject;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MySQL 存储器。
 */
public class MySQLStorage extends AbstractStorage {

    private ConnectionPool pool;

    public MySQLStorage(String name) {
        super(name);
    }

    @Override
    public void open() {
        if (null != this.pool) {
            return;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        this.pool = new ConnectionPool(8, this.config);
    }

    @Override
    public void close() {
        if (null == this.pool) {
            return;
        }

        this.pool.close();
    }

    @Override
    public boolean exist(String table) {
        Connection connection = this.pool.get();
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.setQueryTimeout(10);
            statement.executeQuery("SELECT * FROM " + table + " LIMIT 1");
        } catch (SQLException e) {
            return false;
        } finally {
            if (null != statement) {
                try {
                    statement.close();
                } catch (SQLException e) {
                }
            }

            this.pool.returnConn(connection);
        }
        return true;
    }

    @Override
    public boolean executeCreate(String table, StorageField[] fields) {
        for (StorageField field : fields) {
            fixAutoIncrement(field);
        }

        Connection connection = this.pool.get();
        // 拼写 SQL 语句
        String sql = SQLUtils.spellCreateTable(table, fields);
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            Logger.e(this.getClass(), "#executeCreate - SQL: " + sql, e);
            return false;
        } finally {
            if (null != statement) {
                try {
                    statement.close();
                } catch (SQLException e) {
                }
            }

            this.pool.returnConn(connection);
        }

        return true;
    }

    private void fixAutoIncrement(StorageField field) {
        Constraint[] constraints = field.getConstraints();
        for (int i = 0; i < constraints.length; ++i) {
            Constraint constraint = constraints[i];
            if (constraint == Constraint.AUTOINCREMENT) {
                constraints[i] = Constraint.AUTO_INCREMENT;
            }
        }
    }

    @Override
    public boolean executeInsert(String table, StorageField[] fields) {
        Connection connection = this.pool.get();
        // 拼写 SQL 语句
        String sql = SQLUtils.spellInsert(table, fields);
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            Logger.e(this.getClass(), "#executeInsert - SQL: " + sql, e);
            return false;
        } finally {
            if (null != statement) {
                try {
                    statement.close();
                } catch (SQLException e) {
                }
            }

            this.pool.returnConn(connection);
        }
        return true;
    }

    @Override
    public boolean executeInsert(String table, List<StorageField[]> fieldsList) {
        Connection connection = this.pool.get();

        boolean success = true;
        for (StorageField[] fields : fieldsList) {
            // 拼写 SQL 语句
            String sql = SQLUtils.spellInsert(table, fields);

            Statement statement = null;
            try {
                statement = connection.createStatement();
                statement.executeUpdate(sql);
            } catch (SQLException e) {
                Logger.e(this.getClass(), "#executeInsert - SQL: " + sql, e);
                success = false;
                continue;
            } finally {
                if (null != statement) {
                    try {
                        statement.close();
                    } catch (SQLException e) {
                    }
                }
            }
        }

        this.pool.returnConn(connection);

        return success;
    }

    @Override
    public boolean executeUpdate(String table, StorageField[] fields, Conditional[] conditionals) {
        Connection connection = this.pool.get();

        // 拼写 SQL 语句
        String sql = SQLUtils.spellUpdate(table, fields, conditionals);

        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            Logger.e(this.getClass(), "#executeUpdate - SQL: " + sql, e);
            return false;
        } finally {
            if (null != statement) {
                try {
                    statement.close();
                } catch (SQLException e) {
                }
            }

            this.pool.returnConn(connection);
        }
        return true;
    }

    @Override
    public boolean executeDelete(String table, Conditional[] conditionals) {
        Connection connection = this.pool.get();

        // 拼写 SQL 语句
        String sql = SQLUtils.spellDelete(table, conditionals);

        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            Logger.e(this.getClass(), "#executeDelete - SQL: " + sql, e);
            return false;
        } finally {
            if (null != statement) {
                try {
                    statement.close();
                } catch (SQLException e) {
                }
            }

            this.pool.returnConn(connection);
        }
        return true;
    }

    @Override
    public List<StorageField[]> executeQuery(String table, StorageField[] fields) {
        return this.executeQuery(table, fields, null);
    }

    @Override
    public List<StorageField[]> executeQuery(String table, StorageField[] fields, Conditional[] conditionals) {
        Connection connection = this.pool.get();

        ArrayList<StorageField[]> result = new ArrayList<>();

        // 拼写 SQL 语句
        String sql = SQLUtils.spellSelect(table, fields, conditionals);

        Statement statement = null;
        try {
            statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
                StorageField[] row = new StorageField[fields.length];

                for (int i = 0; i < fields.length; ++i) {
                    StorageField sf = fields[i];
                    LiteralBase literal = sf.getLiteralBase();
                    if (literal == LiteralBase.STRING) {
                        String value = rs.getString(sf.getName());
                        row[i] = new StorageField(sf.getName(), sf.getLiteralBase(), value);
                    }
                    else if (literal == LiteralBase.LONG) {
                        long value = rs.getLong(sf.getName());
                        row[i] = new StorageField(sf.getName(), sf.getLiteralBase(), value);
                    }
                    else if (literal == LiteralBase.INT) {
                        int value = rs.getInt(sf.getName());
                        row[i] = new StorageField(sf.getName(), sf.getLiteralBase(), value);
                    }
                    else if (literal == LiteralBase.BOOL) {
                        boolean value = rs.getBoolean(sf.getName());
                        row[i] = new StorageField(sf.getName(), sf.getLiteralBase(), value);
                    }
                }

                result.add(row);
            }
        } catch (SQLException e) {
            Logger.w(this.getClass(), "#executeQuery - SQL: " + sql, e);
        } finally {
            if (null != statement) {
                try {
                    statement.close();
                } catch (SQLException e) {
                }
            }

            this.pool.returnConn(connection);
        }
        return result;
    }

    @Override
    public List<StorageField[]> executeQuery(String[] tables, StorageField[] fields, Conditional[] conditionals) {
        ArrayList<StorageField[]> result = new ArrayList<>();

        Connection connection = this.pool.get();

        // 拼写 SQL 语句
        String sql = SQLUtils.spellSelect(tables, fields, conditionals);

        Statement statement = null;
        try {
            statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
                StorageField[] row = new StorageField[fields.length];

                for (int i = 0; i < fields.length; ++i) {
                    StorageField sf = fields[i];
                    LiteralBase literal = sf.getLiteralBase();

                    if (literal == LiteralBase.STRING) {
                        String value = rs.getString(sf.getName());
                        row[i] = new StorageField(sf.getName(), sf.getLiteralBase(), value);
                    }
                    else if (literal == LiteralBase.LONG) {
                        long value = rs.getLong(sf.getName());
                        row[i] = new StorageField(sf.getName(), sf.getLiteralBase(), value);
                    }
                    else if (literal == LiteralBase.INT) {
                        int value = rs.getInt(sf.getName());
                        row[i] = new StorageField(sf.getName(), sf.getLiteralBase(), value);
                    }
                    else if (literal == LiteralBase.BOOL) {
                        boolean value = rs.getBoolean(sf.getName());
                        row[i] = new StorageField(sf.getName(), sf.getLiteralBase(), value);
                    }
                }

                result.add(row);
            }
        } catch (SQLException e) {
            Logger.w(this.getClass(), "#executeQuery - SQL: " + sql, e);
        } finally {
            if (null != statement) {
                try {
                    statement.close();
                } catch (SQLException e) {
                }
            }

            this.pool.returnConn(connection);
        }

        return result;
    }


    /**
     * 连接池。
     */
    protected class ConnectionPool {

        private int maxConn = 4;

        private JSONObject config;

        private ConcurrentLinkedQueue<Connection> connections;

        private AtomicInteger count;

        protected ConnectionPool(int maxConn, JSONObject config) {
            this.maxConn = maxConn;
            this.config = config;
            this.connections = new ConcurrentLinkedQueue<>();
            this.count = new AtomicInteger(0);
        }

        protected Connection get() {
            if (this.count.get() >= this.maxConn) {
                synchronized (this) {
                    try {
                        this.wait(30000L);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            Connection conn = this.connections.poll();
            if (null == conn) {
                StringBuilder url = new StringBuilder();
                url.append("jdbc:mysql://");
                url.append(this.config.has("host") ? this.config.getString("host") : "127.0.0.1");
                url.append(":");
                url.append(this.config.has("port") ? this.config.getInt("port") : 3306);
                url.append("/");
                url.append(this.config.has("schema") ? this.config.getString("schema") : "cube");
                url.append("?useSSL=false&allowPublicKeyRetrieval=true");

                try {
                   conn = DriverManager.getConnection(url.toString(),
                            this.config.getString("user"), this.config.getString("password"));
                } catch (SQLException e) {
                    Logger.e(this.getClass(), "#open", e);
                }
            }

            this.count.incrementAndGet();
            return conn;
        }

        protected void returnConn(Connection connection) {
            this.connections.offer(connection);

            if (this.count.get() >= this.maxConn) {
                synchronized (this) {
                    this.notifyAll();
                }
            }

            this.count.decrementAndGet();
        }

        protected void close() {
            this.count.set(0);

            synchronized (this) {
                this.notifyAll();
            }

            for (Connection conn : this.connections) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            this.connections.clear();
        }
    }
}
