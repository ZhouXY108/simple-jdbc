package xyz.zhouxy.jdbc.test.namedparam;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.zhouxy.jdbc.ResultHandler;
import xyz.zhouxy.jdbc.SimpleJdbcTemplate;
import xyz.zhouxy.jdbc.namedparam.NamedParamJdbcOperations;
import xyz.zhouxy.jdbc.namedparam.PreparedSql;
import xyz.zhouxy.jdbc.test.BaseH2Test;
import xyz.zhouxy.jdbc.test.User;
import xyz.zhouxy.jdbc.test.UserRowMapper;

/**
 * 命名参数查询 API 测试。
 *
 * <p>验证 {@link NamedParamJdbcOperations} 中命名参数（<code>#{paramName}</code>）形式的
 * 查询方法是否通过 {@link SimpleJdbcTemplate} 正确委托到 {@link xyz.zhouxy.jdbc.JdbcOperations}。</p>
 */
@DisplayName("NamedParamJdbcOperations 命名参数查询")
class NamedParamQueryTest extends BaseH2Test {

    @BeforeEach
    void setUp() throws Exception {
        resetDatabase();
    }

    // ==================== query(ResultHandler) ====================

    @Test
    @DisplayName("query + ResultHandler：命名参数查询")
    void testQueryWithNamedParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Integer count = template.query(
                "SELECT COUNT(*) FROM users WHERE active = #{active}",
                Collections.singletonMap("active", true),
                (ResultHandler<Integer>) rs -> {
                    rs.next();
                    return rs.getInt(1);
                });

        assertEquals(4, count);
    }

    @Test
    @DisplayName("query + ResultHandler：多命名参数")
    void testQueryWithMultipleNamedParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("id", 1);
        params.put("name", "alice");
        String username = template.query(
                "SELECT username FROM users WHERE id = #{id} AND username = #{name}",
                params,
                (ResultHandler<String>) rs -> {
                    rs.next();
                    return rs.getString(1);
                });

        assertEquals("alice", username);
    }

    // ==================== queryList ====================

    @Test
    @DisplayName("queryList + RowMapper：命名参数查询全部")
    void testQueryListWithRowMapper() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        List<User> users = template.queryList(
                "SELECT * FROM users WHERE active = #{active} ORDER BY id",
                Collections.singletonMap("active", true),
                new UserRowMapper());

        assertEquals(4, users.size());
        users.forEach(u -> assertTrue(u.getActive()));
    }

    @Test
    @DisplayName("queryValues：命名参数单列查询")
    void testQueryValues() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        List<String> usernames = template.queryValues(
                "SELECT username FROM users WHERE age > #{minAge} ORDER BY id",
                Collections.singletonMap("minAge", 30),
                String.class);

        assertEquals(3, usernames.size());
        assertTrue(usernames.contains("bob"));
        assertTrue(usernames.contains("diana"));
        assertTrue(usernames.contains("eve"));
    }

    @Test
    @DisplayName("queryList：命名参数返回 List<Map>")
    void testQueryListAsMap() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        List<Map<String, Object>> users = template.queryList(
                "SELECT id, username, email FROM users WHERE id = #{id}",
                Collections.singletonMap("id", 1));

        assertEquals(1, users.size());
        assertEquals("alice", users.get(0).get("username"));
    }

    @Test
    @DisplayName("queryList：空参数 Map，SQL 中无命名参数")
    void testQueryWithEmptyParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        List<User> users = template.queryList(
                "SELECT * FROM users ORDER BY id",
                Collections.<String, Object>emptyMap(),
                new UserRowMapper());

        assertEquals(5, users.size());
    }

    // ==================== queryFirst ====================

    @Test
    @DisplayName("queryFirst + RowMapper：命名参数查询第一条")
    void testQueryFirstWithRowMapper() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Optional<User> user = template.queryFirst(
                "SELECT * FROM users WHERE username = #{name}",
                Collections.singletonMap("name", "bob"),
                new UserRowMapper());

        assertTrue(user.isPresent());
        assertEquals("bob", user.get().getUsername());
    }

    @Test
    @DisplayName("queryFirst：空结果返回 Optional.empty()")
    void testQueryFirstEmpty() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Optional<User> user = template.queryFirst(
                "SELECT * FROM users WHERE id = #{id}",
                Collections.singletonMap("id", 999),
                new UserRowMapper());

        assertFalse(user.isPresent());
    }

    @Test
    @DisplayName("queryFirst：参数值为 null，验证 ParamBuilder 处理 null")
    void testQueryWithNullParamValue() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        // 用不存在的 id 查无结果，验证 null 参数值不会导致异常
        Optional<User> user = template.queryFirst(
                "SELECT * FROM users WHERE id = #{id}",
                Collections.singletonMap("id", null),
                new UserRowMapper());

        assertFalse(user.isPresent());
    }

    // ==================== queryValue / queryValueOrDefault ====================

    @Test
    @DisplayName("queryValue：命名参数单值查询")
    void testQueryValue() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Optional<String> username = template.queryValue(
                "SELECT username FROM users WHERE id = #{id}",
                Collections.singletonMap("id", 1),
                String.class);

        assertTrue(username.isPresent());
        assertEquals("alice", username.get());
    }

    @Test
    @DisplayName("queryValue：空结果返回 Optional.empty()")
    void testQueryValueEmpty() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Optional<String> result = template.queryValue(
                "SELECT username FROM users WHERE id = #{id}",
                Collections.singletonMap("id", 999),
                String.class);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("queryValueOrDefault：存在结果返回实际值")
    void testQueryValueOrDefaultPresent() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Long count = template.queryValueOrDefault(
                "SELECT COUNT(*) FROM users WHERE active = #{active}",
                Collections.singletonMap("active", true),
                Long.class,
                0L);

        assertEquals(4L, count);
    }

    @Test
    @DisplayName("queryValueOrDefault：空结果返回默认值")
    void testQueryValueOrDefaultEmpty() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Long count = template.queryValueOrDefault(
                "SELECT COUNT(*) FROM users WHERE id = #{id}",
                Collections.singletonMap("id", 999),
                Long.class,
                0L);

        assertEquals(0L, count);
    }

    @Test
    @DisplayName("queryValue：SQL 引用未提供的参数名，应抛异常")
    void testQueryWithMissingParamName() {
        SimpleJdbcTemplate template = createTemplate();
        Map<String, Object> params = Collections.singletonMap("id", 1);
        assertThrows(IllegalArgumentException.class, () ->
            template.queryValue(
                    "SELECT username FROM users WHERE id = #{missing}",
                    params,
                    String.class));
    }

    // ==================== queryFirst(Map) ====================

    @Test
    @DisplayName("queryFirst：命名参数返回 Map")
    void testQueryFirstAsMap() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Optional<Map<String, Object>> user = template.queryFirst(
                "SELECT id, username FROM users WHERE id = #{id}",
                Collections.singletonMap("id", 3));

        assertTrue(user.isPresent());
        assertEquals("charlie", user.get().get("username"));
    }

    // ==================== queryBoolean ====================

    @Test
    @DisplayName("queryBoolean：命名参数布尔查询")
    void testQueryBooleanTrue() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        boolean active = template.queryBoolean(
                "SELECT active FROM users WHERE id = #{id}",
                Collections.singletonMap("id", 1));

        assertTrue(active);
    }

    @Test
    @DisplayName("queryBoolean：返回 false")
    void testQueryBooleanFalse() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        boolean active = template.queryBoolean(
                "SELECT active FROM users WHERE id = #{id}",
                Collections.singletonMap("id", 3));

        assertFalse(active);
    }

    // ==================== PreparedSql 重载：查询 ====================

    @Test
    @DisplayName("PreparedSql 重载：query + ResultHandler")
    void testQueryWithPreparedSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        PreparedSql ps = PreparedSql
                .sql("SELECT COUNT(*) FROM users WHERE active = #{active}")
                .param("active", true)
                .build();

        Integer count = template.query(ps,
                (ResultHandler<Integer>) rs -> {
                    rs.next();
                    return rs.getInt(1);
                });

        assertEquals(4, count);
    }

    @Test
    @DisplayName("PreparedSql 重载：queryList + RowMapper")
    void testQueryListWithPreparedSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        PreparedSql ps = PreparedSql
                .sql("SELECT * FROM users WHERE active = #{active} ORDER BY id")
                .param("active", true)
                .build();

        List<User> users = template.queryList(ps, new UserRowMapper());
        assertEquals(4, users.size());
    }

    @Test
    @DisplayName("PreparedSql 重载：queryValues(Class) 单列列表")
    void testQueryValuesWithPreparedSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        PreparedSql ps = PreparedSql
                .sql("SELECT username FROM users WHERE age > #{minAge} ORDER BY id")
                .param("minAge", 30)
                .build();

        List<String> usernames = template.queryValues(ps, String.class);

        assertEquals(3, usernames.size());
        assertTrue(usernames.contains("bob"));
        assertTrue(usernames.contains("diana"));
        assertTrue(usernames.contains("eve"));
    }

    @Test
    @DisplayName("PreparedSql 重载：queryList 返回 List<Map>")
    void testQueryListMapWithPreparedSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        PreparedSql ps = PreparedSql
                .sql("SELECT id, username, email FROM users WHERE id = #{id}")
                .param("id", 1)
                .build();

        List<Map<String, Object>> users = template.queryList(ps);

        assertEquals(1, users.size());
        assertEquals("alice", users.get(0).get("username"));
    }

    @Test
    @DisplayName("PreparedSql 重载：queryFirst + RowMapper")
    void testQueryFirstWithPreparedSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        PreparedSql ps = PreparedSql
                .sql("SELECT * FROM users WHERE username = #{name}")
                .param("name", "bob")
                .build();

        Optional<User> user = template.queryFirst(ps, new UserRowMapper());

        assertTrue(user.isPresent());
        assertEquals("bob", user.get().getUsername());
    }

    @Test
    @DisplayName("PreparedSql 重载：queryFirst 返回 Optional<Map>")
    void testQueryFirstMapWithPreparedSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        PreparedSql ps = PreparedSql
                .sql("SELECT id, username FROM users WHERE id = #{id}")
                .param("id", 3)
                .build();

        Optional<Map<String, Object>> user = template.queryFirst(ps);

        assertTrue(user.isPresent());
        assertEquals("charlie", user.get().get("username"));
    }

    @Test
    @DisplayName("PreparedSql 重载：queryValue")
    void testQueryValueWithPreparedSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        PreparedSql ps = PreparedSql
                .sql("SELECT username FROM users WHERE id = #{id}")
                .param("id", 1)
                .build();

        Optional<String> result = template.queryValue(ps, String.class);
        assertTrue(result.isPresent());
        assertEquals("alice", result.get());
    }

    @Test
    @DisplayName("PreparedSql 重载：queryBoolean")
    void testQueryBooleanWithPreparedSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        PreparedSql ps = PreparedSql
                .sql("SELECT active FROM users WHERE id = #{id}")
                .param("id", 1)
                .build();

        assertTrue(template.queryBoolean(ps));
    }

    @Test
    @DisplayName("PreparedSql 重载：queryValueOrDefault")
    void testQueryValueOrDefaultWithPreparedSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        PreparedSql ps = PreparedSql
                .sql("SELECT COUNT(*) FROM users WHERE active = #{active}")
                .param("active", true)
                .build();

        Long count = template.queryValueOrDefault(ps, Long.class, 0L);
        assertEquals(4L, count);
    }

    // ==================== 结构性 / 多态验证 ====================

    @Test
    @DisplayName("getJdbcOperations：返回 this，可回退到位置参数操作")
    void testGetJdbcOperations() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamJdbcOperations nops = template;

        // 通过 getJdbcOperations() 回退到 JdbcOperations
        List<User> users = nops.getJdbcOperations()
                .queryList("SELECT * FROM users ORDER BY id", new UserRowMapper());

        assertEquals(5, users.size());
    }

    @Test
    @DisplayName("多态：template 可直接赋值给 NamedParamJdbcOperations")
    void testPolymorphicView() {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamJdbcOperations nops = template;

        assertNotNull(nops);
        assertSame(template, nops.getJdbcOperations());
    }

}
