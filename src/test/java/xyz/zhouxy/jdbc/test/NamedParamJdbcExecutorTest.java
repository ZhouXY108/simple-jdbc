package xyz.zhouxy.jdbc.test;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.zhouxy.jdbc.BatchUpdateResult;
import xyz.zhouxy.jdbc.namedparam.NamedParamJdbcExecutor;
import xyz.zhouxy.jdbc.namedparam.NamedParamSql;
import xyz.zhouxy.jdbc.namedparam.PreparedSql;

@DisplayName("NamedParamJdbcExecutor 命名参数静态执行器")
class NamedParamJdbcExecutorTest extends BaseH2Test {

    private Connection conn;

    @BeforeEach
    void setUp() throws Exception {
        resetDatabase();
        conn = dataSource.getConnection();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (conn != null) {
            conn.close();
        }
    }

    // ==================== query ====================

    @Test
    @DisplayName("query(String + Map)")
    void testQueryWithString() throws SQLException {
        Integer count = NamedParamJdbcExecutor.query(conn,
                "SELECT COUNT(*) FROM users WHERE active = #{active}",
                Collections.singletonMap("active", true),
                rs -> {
                    rs.next();
                    return rs.getInt(1);
                });
        assertEquals(4, count);
    }

    @Test
    @DisplayName("query(NamedParamSql + Map)")
    void testQueryWithNamedParamSql() throws SQLException {
        NamedParamSql tmpl = NamedParamSql.of(
                "SELECT COUNT(*) FROM users WHERE active = #{active}");
        Integer count = NamedParamJdbcExecutor.query(conn, tmpl,
                Collections.singletonMap("active", true),
                rs -> {
                    rs.next();
                    return rs.getInt(1);
                });
        assertEquals(4, count);
    }

    @Test
    @DisplayName("query(PreparedSql)")
    void testQueryWithPreparedSql() throws SQLException {
        PreparedSql ps = PreparedSql
                .sql("SELECT COUNT(*) FROM users WHERE active = #{active}")
                .param("active", true)
                .build();
        Integer count = NamedParamJdbcExecutor.query(conn, ps,
                rs -> {
                    rs.next();
                    return rs.getInt(1);
                });
        assertEquals(4, count);
    }

    // ==================== queryList ====================

    @Test
    @DisplayName("queryList(String + Map + RowMapper)")
    void testQueryListWithRowMapper() throws SQLException {
        List<User> users = NamedParamJdbcExecutor.queryList(conn,
                "SELECT * FROM users WHERE active = #{active} ORDER BY id",
                Collections.singletonMap("active", true),
                new UserRowMapper());
        assertEquals(4, users.size());
    }

    @Test
    @DisplayName("queryList(PreparedSql + RowMapper)")
    void testQueryListWithPreparedSql() throws SQLException {
        PreparedSql ps = PreparedSql
                .sql("SELECT * FROM users WHERE active = #{active} ORDER BY id")
                .param("active", true)
                .build();
        List<User> users = NamedParamJdbcExecutor.queryList(conn, ps, new UserRowMapper());
        assertEquals(4, users.size());
    }

    @Test
    @DisplayName("queryValues(String + Map)")
    void testQueryValues() throws SQLException {
        List<String> usernames = NamedParamJdbcExecutor.queryValues(conn,
                "SELECT username FROM users WHERE age > #{minAge} ORDER BY id",
                Collections.singletonMap("minAge", 30),
                String.class);
        assertEquals(3, usernames.size());
    }

    // ==================== queryFirst / queryValue ====================

    @Test
    @DisplayName("queryFirst(String + Map + RowMapper)")
    void testQueryFirstWithRowMapper() throws SQLException {
        Optional<User> user = NamedParamJdbcExecutor.queryFirst(conn,
                "SELECT * FROM users WHERE id = #{id}",
                Collections.singletonMap("id", 1),
                new UserRowMapper());
        assertTrue(user.isPresent());
        assertEquals("alice", user.get().getUsername());
    }

    @Test
    @DisplayName("queryValue(NamedParamSql + Map)")
    void testQueryValueWithNamedParamSql() throws SQLException {
        NamedParamSql tmpl = NamedParamSql.of(
                "SELECT username FROM users WHERE id = #{id}");
        Optional<String> name = NamedParamJdbcExecutor.queryValue(conn, tmpl,
                Collections.singletonMap("id", 1), String.class);
        assertTrue(name.isPresent());
        assertEquals("alice", name.get());
    }

    @Test
    @DisplayName("queryValueOrDefault(PreparedSql)")
    void testQueryValueOrDefaultWithPreparedSql() throws SQLException {
        PreparedSql ps = PreparedSql
                .sql("SELECT COUNT(*) FROM users WHERE active = #{active}")
                .param("active", true)
                .build();
        long count = NamedParamJdbcExecutor.queryValueOrDefault(conn, ps, Long.class, 0L);
        assertEquals(4L, count);
    }

    @Test
    @DisplayName("queryValueOrDefault 空结果返回默认值")
    void testQueryValueOrDefaultEmpty() throws SQLException {
        long count = NamedParamJdbcExecutor.queryValueOrDefault(conn,
                "SELECT COUNT(*) FROM users WHERE id = #{id}",
                Collections.singletonMap("id", 999),
                Long.class, 0L);
        assertEquals(0L, count);
    }

    // ==================== queryBoolean ====================

    @Test
    @DisplayName("queryBoolean(String + Map) true")
    void testQueryBooleanTrue() throws SQLException {
        boolean active = NamedParamJdbcExecutor.queryBoolean(conn,
                "SELECT active FROM users WHERE id = #{id}",
                Collections.singletonMap("id", 1));
        assertTrue(active);
    }

    @Test
    @DisplayName("queryBoolean(NamedParamSql + Map) false")
    void testQueryBooleanFalseWithNamedParamSql() throws SQLException {
        NamedParamSql tmpl = NamedParamSql.of(
                "SELECT active FROM users WHERE id = #{id}");
        boolean active = NamedParamJdbcExecutor.queryBoolean(conn, tmpl,
                Collections.singletonMap("id", 3));
        assertFalse(active);
    }

    // ==================== update ====================

    @Test
    @DisplayName("update(String + Map)")
    void testUpdateWithString() throws SQLException {
        HashMap<String, Object> params = new HashMap<>();
        params.put("name", "npUser");
        params.put("email", "np@test.com");
        int rows = NamedParamJdbcExecutor.update(conn,
                "INSERT INTO users (username, email) VALUES(#{name}, #{email})",
                params);
        assertEquals(1, rows);
    }

    @Test
    @DisplayName("update(NamedParamSql + Map)")
    void testUpdateWithNamedParamSql() throws SQLException {
        NamedParamSql tmpl = NamedParamSql.of(
                "UPDATE users SET email = #{email} WHERE username = #{name}");
        HashMap<String, Object> params = new HashMap<>();
        params.put("email", "updated@test.com");
        params.put("name", "alice");
        int rows = NamedParamJdbcExecutor.update(conn, tmpl, params);
        assertEquals(1, rows);
    }

    @Test
    @DisplayName("update(PreparedSql)")
    void testUpdateWithPreparedSql() throws SQLException {
        PreparedSql ps = PreparedSql
                .sql("UPDATE users SET email = #{email} WHERE username = #{name}")
                .param("email", "ps@test.com")
                .param("name", "bob")
                .build();
        int rows = NamedParamJdbcExecutor.update(conn, ps);
        assertEquals(1, rows);
    }

    @Test
    @DisplayName("updateAndReturnKeys(String + Map)")
    void testUpdateAndReturnKeys() throws SQLException {
        List<Long> keys = NamedParamJdbcExecutor.updateAndReturnKeys(conn,
                "INSERT INTO users (username) VALUES(#{name})",
                Collections.singletonMap("name", "keyNpUser"),
                (rs, rowNum) -> rs.getLong(1));
        assertEquals(1, keys.size());
        assertTrue(keys.get(0) > 0);
    }

    // ==================== batchUpdate ====================

    @Test
    @DisplayName("batchUpdate(String + List<Map>)")
    void testBatchUpdateWithString() throws SQLException {
        List<Map<String, ?>> batchParams = new ArrayList<>();
        HashMap<String, Object> row1 = new HashMap<>();
        row1.put("name", "batchNp1");
        row1.put("email", "bn1@t.com");
        batchParams.add(row1);
        HashMap<String, Object> row2 = new HashMap<>();
        row2.put("name", "batchNp2");
        row2.put("email", "bn2@t.com");
        batchParams.add(row2);
        BatchUpdateResult result = NamedParamJdbcExecutor.batchUpdate(conn,
                "INSERT INTO users (username, email) VALUES(#{name}, #{email})",
                batchParams, 10);
        assertEquals(2, result.getTotal());
    }

    @Test
    @DisplayName("batchUpdate(NamedParamSql + List<Map>) quietly")
    void testBatchUpdateWithNamedParamSqlQuietly() throws SQLException {
        NamedParamSql tmpl = NamedParamSql.of(
                "INSERT INTO users (username, email) VALUES(#{name}, #{email})");
        List<Map<String, ?>> batchParams = new ArrayList<>();
        HashMap<String, Object> q1 = new HashMap<>();
        q1.put("name", "qNp1");
        q1.put("email", "qn1@t.com");
        batchParams.add(q1);
        HashMap<String, Object> q2 = new HashMap<>();
        q2.put("name", "qNp2");
        q2.put("email", "qn2@t.com");
        batchParams.add(q2);
        BatchUpdateResult result = NamedParamJdbcExecutor.batchUpdate(conn, tmpl,
                batchParams, 10, true);
        assertEquals(2, result.getTotal());
    }

    // ==================== 边界 ====================

    @Test
    @DisplayName("缺失参数名抛 IllegalArgumentException")
    void testMissingParamName() {
        assertThrows(IllegalArgumentException.class, () ->
            NamedParamJdbcExecutor.queryValue(conn,
                    "SELECT username FROM users WHERE id = #{missing}",
                    Collections.singletonMap("id", 1),
                    String.class));
    }

    @Test
    @DisplayName("null Connection 抛出异常")
    void testNullConnection() {
        assertThrows(Exception.class, () ->
            NamedParamJdbcExecutor.query(null,
                    "SELECT #{a}", Collections.singletonMap("a", 1),
                    rs -> rs.getInt(1)));
    }

    @Test
    @DisplayName("执行后连接不被关闭")
    void testConnectionNotClosed() throws SQLException {
        NamedParamJdbcExecutor.query(conn,
                "SELECT #{a}", Collections.singletonMap("a", 1),
                rs -> {
                    rs.next();
                    return rs.getInt(1);
                });
        assertFalse(conn.isClosed());
    }
}
