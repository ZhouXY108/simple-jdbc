package xyz.zhouxy.jdbc.test;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.zhouxy.jdbc.BatchUpdateResult;
import xyz.zhouxy.jdbc.JdbcConfig;
import xyz.zhouxy.jdbc.JdbcExecutor;

@DisplayName("JdbcExecutor 实例执行器")
class JdbcExecutorTest extends BaseH2Test {

    private Connection conn;
    private JdbcExecutor executor;

    @BeforeEach
    void setUp() throws Exception {
        resetDatabase();
        conn = dataSource.getConnection();
        executor = new JdbcExecutor();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (conn != null) {
            conn.close();
        }
    }

    @Test
    @DisplayName("query + ResultHandler")
    void testQuery() throws SQLException {
        Integer count = executor.query(conn,
                "SELECT COUNT(*) FROM users",
                new Object[0],
                rs -> {
                    rs.next();
                    return rs.getInt(1);
                });
        assertEquals(5, count);
    }

    @Test
    @DisplayName("query 无参数重载")
    void testQueryNoParams() throws SQLException {
        Integer count = executor.query(conn,
                "SELECT COUNT(*) FROM users",
                rs -> {
                    rs.next();
                    return rs.getInt(1);
                });
        assertEquals(5, count);
    }

    @Test
    @DisplayName("queryList + RowMapper")
    void testQueryListWithRowMapper() throws SQLException {
        List<User> users = executor.queryList(conn,
                "SELECT * FROM users ORDER BY id",
                new UserRowMapper());
        assertEquals(5, users.size());
        assertEquals("alice", users.get(0).getUsername());
    }

    @Test
    @DisplayName("queryList 无参数重载 + RowMapper")
    void testQueryListRowMapperNoParams() throws SQLException {
        List<User> users = executor.queryList(conn,
                "SELECT * FROM users ORDER BY id",
                new UserRowMapper());
        assertEquals(5, users.size());
    }

    @Test
    @DisplayName("queryValues")
    void testQueryValues() throws SQLException {
        List<String> usernames = executor.queryValues(conn,
                "SELECT username FROM users ORDER BY id",
                new Object[0], String.class);
        assertEquals(5, usernames.size());
    }

    @Test
    @DisplayName("queryList(Map)")
    void testQueryListAsMap() throws SQLException {
        List<Map<String, Object>> users = executor.queryList(conn,
                "SELECT id, username FROM users WHERE id = ?",
                new Object[]{1});
        assertEquals(1, users.size());
        assertEquals("alice", users.get(0).get("username"));
    }

    @Test
    @DisplayName("queryFirst + RowMapper")
    void testQueryFirstWithRowMapper() throws SQLException {
        Optional<User> user = executor.queryFirst(conn,
                "SELECT * FROM users WHERE id = ?",
                new Object[]{1}, new UserRowMapper());
        assertTrue(user.isPresent());
        assertEquals("alice", user.get().getUsername());
    }

    @Test
    @DisplayName("queryFirst 空结果")
    void testQueryFirstEmpty() throws SQLException {
        Optional<User> user = executor.queryFirst(conn,
                "SELECT * FROM users WHERE id = ?",
                new Object[]{999}, new UserRowMapper());
        assertFalse(user.isPresent());
    }

    @Test
    @DisplayName("queryValue")
    void testQueryValue() throws SQLException {
        Optional<String> username = executor.queryValue(conn,
                "SELECT username FROM users WHERE id = ?",
                new Object[]{1}, String.class);
        assertTrue(username.isPresent());
        assertEquals("alice", username.get());
    }

    @Test
    @DisplayName("queryFirst(Map)")
    void testQueryFirstAsMap() throws SQLException {
        Optional<Map<String, Object>> user = executor.queryFirst(conn,
                "SELECT id, username FROM users WHERE id = ?",
                new Object[]{2});
        assertTrue(user.isPresent());
        assertEquals("bob", user.get().get("username"));
    }

    @Test
    @DisplayName("queryValueOrDefault")
    void testQueryValueOrDefault() throws SQLException {
        long count = executor.queryValueOrDefault(conn,
                "SELECT COUNT(*) FROM users",
                new Object[0], Long.class, 0L);
        assertEquals(5L, count);
    }

    @Test
    @DisplayName("queryValueOrDefault 空结果返回默认值")
    void testQueryValueOrDefaultEmpty() throws SQLException {
        String val = executor.queryValueOrDefault(conn,
                "SELECT username FROM users WHERE id = ?",
                new Object[]{999}, String.class, "default");
        assertEquals("default", val);
    }

    @Test
    @DisplayName("queryBoolean true")
    void testQueryBooleanTrue() throws SQLException {
        boolean exists = executor.queryBoolean(conn,
                "SELECT COUNT(*) > 0 FROM users WHERE username = ?",
                new Object[]{"alice"});
        assertTrue(exists);
    }

    @Test
    @DisplayName("queryBoolean false")
    void testQueryBooleanFalse() throws SQLException {
        boolean exists = executor.queryBoolean(conn,
                "SELECT COUNT(*) > 0 FROM users WHERE username = ?",
                new Object[]{"nobody"});
        assertFalse(exists);
    }

    @Test
    @DisplayName("update INSERT")
    void testUpdateInsert() throws SQLException {
        int rows = executor.update(conn,
                "INSERT INTO users (username, email) VALUES (?, ?)",
                new Object[]{"executorUser", "exec@test.com"});
        assertEquals(1, rows);
    }

    @Test
    @DisplayName("update UPDATE")
    void testUpdateModify() throws SQLException {
        int rows = executor.update(conn,
                "UPDATE users SET email = ? WHERE username = ?",
                new Object[]{"new@test.com", "alice"});
        assertEquals(1, rows);
    }

    @Test
    @DisplayName("update 无参数重载")
    void testUpdateNoParams() throws SQLException {
        int rows = executor.update(conn,
                "UPDATE users SET balance = 9999 WHERE username = 'alice'");
        assertEquals(1, rows);
    }

    @Test
    @DisplayName("updateAndReturnKeys")
    void testUpdateAndReturnKeys() throws SQLException {
        List<Long> keys = executor.updateAndReturnKeys(conn,
                "INSERT INTO users (username) VALUES (?)",
                new Object[]{"keyUser"},
                (rs, rowNum) -> rs.getLong(1));
        assertEquals(1, keys.size());
        assertTrue(keys.get(0) > 0);
    }

    @Test
    @DisplayName("updateAndReturnKeys 无参数重载")
    void testUpdateAndReturnKeysNoParams() throws SQLException {
        List<Long> keys = executor.updateAndReturnKeys(conn,
                "INSERT INTO users (username) VALUES ('noParamKey')",
                (rs, rowNum) -> rs.getLong(1));
        assertEquals(1, keys.size());
    }

    @Test
    @DisplayName("batchUpdate 正常")
    void testBatchUpdate() throws SQLException {
        List<Object[]> params = new ArrayList<>();
        params.add(new Object[]{"b1", "b1@t.com", 20, 100L, true});
        params.add(new Object[]{"b2", "b2@t.com", 21, 200L, false});
        BatchUpdateResult result = executor.batchUpdate(conn,
                "INSERT INTO users (username, email, age, balance, active) VALUES (?, ?, ?, ?, ?)",
                params, 10);
        assertEquals(2, result.getTotal());
    }

    @Test
    @DisplayName("batchUpdate quietly=true")
    void testBatchUpdateQuietly() throws SQLException {
        List<Object[]> params = new ArrayList<>();
        params.add(new Object[]{"q1", "q1@t.com", 20, 100L, true});
        params.add(new Object[]{"q2", "q2@t.com", 21, 200L, false});
        BatchUpdateResult result = executor.batchUpdate(conn,
                "INSERT INTO users (username, email, age, balance, active) VALUES (?, ?, ?, ?, ?)",
                params, 10, true);
        assertEquals(2, result.getTotal());
    }

    @Test
    @DisplayName("null Connection 抛出异常")
    void testNullConnection() {
        //noinspection DataFlowIssue
        assertThrows(Exception.class, () ->
            executor.query(null, "SELECT 1", rs -> rs.getInt(1)));
    }

    @Test
    @DisplayName("执行后连接不被关闭")
    void testConnectionNotClosed() throws SQLException {
        executor.query(conn, "SELECT 1", rs -> {
            rs.next();
            return rs.getInt(1);
        });
        assertFalse(conn.isClosed());
    }

    @Test
    @DisplayName("null params 走 Statement 路径")
    void testNullParams() throws SQLException {
        @SuppressWarnings("RedundantCast")
        int rows = executor.update(conn, "DELETE FROM users", (Object[]) null);
        assertEquals(5, rows);
    }

    @Test
    @DisplayName("自定义 JdbcConfig 的 ResultSetType 生效")
    void testCustomConfigResultSetType() throws SQLException {
        JdbcConfig config = JdbcConfig.builder()
                .resultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE)
                .build();
        JdbcExecutor customExecutor = new JdbcExecutor(config);
        Integer type = customExecutor.query(conn,
                "SELECT 1",
                rs -> rs.getType());
        assertEquals(ResultSet.TYPE_SCROLL_INSENSITIVE, type);
    }
}
