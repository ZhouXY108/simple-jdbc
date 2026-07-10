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

import xyz.zhouxy.jdbc.SimpleJdbcTemplate;
import xyz.zhouxy.jdbc.namedparam.NamedParamJdbcOperations;
import xyz.zhouxy.jdbc.namedparam.NamedParamSql;
import xyz.zhouxy.jdbc.namedparam.PreparedSql;
import xyz.zhouxy.jdbc.test.BaseH2Test;
import xyz.zhouxy.jdbc.test.User;
import xyz.zhouxy.jdbc.test.UserRowMapper;

/**
 * 命名参数查询 API 测试。
 *
 * <p>验证 {@link NamedParamJdbcOperations} 中命名参数（<code>#{paramName}</code>）形式的
 * 查询方法的三种参数模式：直接传参（String + Map）、模板传参（NamedParamSql + Map）、预构建传参（PreparedSql）。</p>
 */
@DisplayName("NamedParamJdbcOperations 命名参数查询")
class NamedParamQueryTest extends BaseH2Test {

    @BeforeEach
    void setUp() throws Exception {
        resetDatabase();
    }

    // ==================== query(ResultHandler) ====================

    @Test
    @DisplayName("query(Map)：查询")
    void testQueryWithNamedParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Integer count = template.query(
                "SELECT COUNT(*) FROM users WHERE active = #{active}",
                Collections.singletonMap("active", true),
                rs -> {
                    rs.next();
                    return rs.getInt(1);
                });

        assertEquals(4, count);
    }

    @Test
    @DisplayName("query(Map)：多命名参数")
    void testQueryWithMultipleNamedParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Map<String, Object> params = new HashMap<>();
        params.put("id", 1);
        params.put("name", "alice");
        String username = template.query(
                "SELECT username FROM users WHERE id = #{id} AND username = #{name}",
                params,
                rs -> {
                    rs.next();
                    return rs.getString(1);
                });

        assertEquals("alice", username);
    }

    @Test
    @DisplayName("query(NamedParamSql)：查询")
    void testQueryWithNamedParamSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamSql tmpl = NamedParamSql.of(
                "SELECT COUNT(*) FROM users WHERE active = #{active}");

        Integer count = template.query(tmpl,
                Collections.singletonMap("active", true),
                rs -> {
                    rs.next();
                    return rs.getInt(1);
                });

        assertEquals(4, count);
    }

    @Test
    @DisplayName("query(NamedParamSql)：同模板多次执行不同参数")
    void testQueryWithNamedParamSqlReuse() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamSql tmpl = NamedParamSql.of(
                "SELECT username FROM users WHERE id = #{id} AND username = #{name}");

        Map<String, Object> params = new HashMap<>();
        params.put("id", 1);
        params.put("name", "alice");
        String alice = template.query(tmpl, params,
                rs -> {
                    rs.next();
                    return rs.getString(1);
                });
        assertEquals("alice", alice);

        params.put("id", 2);
        params.put("name", "bob");
        String bob = template.query(tmpl, params,
                rs -> {
                    rs.next();
                    return rs.getString(1);
                });
        assertEquals("bob", bob);
    }

    @Test
    @DisplayName("query(NamedParamSql)：缺少参数名应抛异常")
    void testQueryHandlerWithMissingParamNameForNamedParamSql() {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamSql tmpl = NamedParamSql.of(
                "SELECT username FROM users WHERE id = #{missing}");
        Map<String, Object> params = Collections.singletonMap("id", 1);
        assertThrows(IllegalArgumentException.class, () ->
            template.query(tmpl, params, rs -> {
                rs.next();
                return rs.getString(1);
            }));
    }

    // ==================== queryList ====================

    @Test
    @DisplayName("queryList(Map)：查询全部")
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
    @DisplayName("queryValues(Map)：单列查询")
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
    @DisplayName("queryList(Map)：返回 List<Map>")
    void testQueryListAsMap() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        List<Map<String, Object>> users = template.queryList(
                "SELECT id, username, email FROM users WHERE id = #{id}",
                Collections.singletonMap("id", 1));

        assertEquals(1, users.size());
        assertEquals("alice", users.get(0).get("username"));
    }

    @Test
    @DisplayName("queryList(Map)：空参数 Map，SQL 中无命名参数")
    void testQueryWithEmptyParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        List<User> users = template.queryList(
                "SELECT * FROM users ORDER BY id",
                Collections.emptyMap(),
                new UserRowMapper());

        assertEquals(5, users.size());
    }

    @Test
    @DisplayName("queryList(NamedParamSql)：查询全部")
    void testQueryListWithNamedParamSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamSql tmpl = NamedParamSql.of(
                "SELECT * FROM users WHERE active = #{active} ORDER BY id");

        List<User> users = template.queryList(tmpl,
                Collections.singletonMap("active", true),
                new UserRowMapper());

        assertEquals(4, users.size());
        users.forEach(u -> assertTrue(u.getActive()));
    }

    @Test
    @DisplayName("queryValues(NamedParamSql)：单列查询")
    void testQueryValuesWithNamedParamSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamSql tmpl = NamedParamSql.of(
                "SELECT username FROM users WHERE age > #{minAge} ORDER BY id");

        List<String> usernames = template.queryValues(tmpl,
                Collections.singletonMap("minAge", 30),
                String.class);

        assertEquals(3, usernames.size());
        assertTrue(usernames.contains("bob"));
        assertTrue(usernames.contains("diana"));
        assertTrue(usernames.contains("eve"));
    }

    @Test
    @DisplayName("queryList(NamedParamSql)：返回 List<Map>")
    void testQueryListAsMapWithNamedParamSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamSql tmpl = NamedParamSql.of(
                "SELECT id, username, email FROM users WHERE id = #{id}");

        List<Map<String, Object>> users = template.queryList(tmpl,
                Collections.singletonMap("id", 1));

        assertEquals(1, users.size());
        assertEquals("alice", users.get(0).get("username"));
    }

    // ==================== queryFirst ====================

    @Test
    @DisplayName("queryFirst(Map)：查询第一条")
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
    @DisplayName("queryFirst(Map)：空结果返回 Optional.empty()")
    void testQueryFirstEmpty() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Optional<User> user = template.queryFirst(
                "SELECT * FROM users WHERE id = #{id}",
                Collections.singletonMap("id", 999),
                new UserRowMapper());

        assertFalse(user.isPresent());
    }

    @Test
    @DisplayName("queryFirst(Map)：参数值为 null")
    void testQueryWithNullParamValue() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        // 用不存在的 id 查无结果，验证 null 参数值不会导致异常
        Optional<User> user = template.queryFirst(
                "SELECT * FROM users WHERE id = #{id}",
                Collections.singletonMap("id", null),
                new UserRowMapper());

        assertFalse(user.isPresent());
    }

    @Test
    @DisplayName("queryFirst(NamedParamSql)：查询第一条")
    void testQueryFirstWithNamedParamSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamSql tmpl = NamedParamSql.of(
                "SELECT * FROM users WHERE username = #{name}");

        Optional<User> user = template.queryFirst(tmpl,
                Collections.singletonMap("name", "bob"),
                new UserRowMapper());

        assertTrue(user.isPresent());
        assertEquals("bob", user.get().getUsername());
    }

    @Test
    @DisplayName("queryFirst(NamedParamSql)：空结果返回 Optional.empty()")
    void testQueryFirstEmptyWithNamedParamSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamSql tmpl = NamedParamSql.of(
                "SELECT * FROM users WHERE id = #{id}");

        Optional<User> user = template.queryFirst(tmpl,
                Collections.singletonMap("id", 999),
                new UserRowMapper());

        assertFalse(user.isPresent());
    }

    // ==================== queryValue / queryValueOrDefault ====================

    @Test
    @DisplayName("queryValue(Map)：单值查询")
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
    @DisplayName("queryValue(Map)：空结果返回 Optional.empty()")
    void testQueryValueEmpty() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Optional<String> result = template.queryValue(
                "SELECT username FROM users WHERE id = #{id}",
                Collections.singletonMap("id", 999),
                String.class);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("queryValue(NamedParamSql)：空结果返回 Optional.empty()")
    void testQueryValueEmptyWithNamedParamSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamSql tmpl = NamedParamSql.of(
                "SELECT username FROM users WHERE id = #{id}");

        Optional<String> result = template.queryValue(tmpl,
                Collections.singletonMap("id", 999),
                String.class);

        assertFalse(result.isPresent());
    }

    @Test
    @DisplayName("queryValueOrDefault(Map)：有结果")
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
    @DisplayName("queryValueOrDefault(Map)：空结果返回默认值")
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
    @DisplayName("queryValue(Map)：缺少参数名应抛异常")
    void testQueryWithMissingParamName() {
        SimpleJdbcTemplate template = createTemplate();
        Map<String, Object> params = Collections.singletonMap("id", 1);
        assertThrows(IllegalArgumentException.class, () ->
            template.queryValue(
                    "SELECT username FROM users WHERE id = #{missing}",
                    params,
                    String.class));
    }

    @Test
    @DisplayName("queryValue(NamedParamSql)：缺少参数名应抛异常")
    void testQueryWithMissingParamNameForNamedParamSql() {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamSql tmpl = NamedParamSql.of(
                "SELECT username FROM users WHERE id = #{missing}");
        Map<String, Object> params = Collections.singletonMap("id", 1);
        assertThrows(IllegalArgumentException.class, () ->
            template.queryValue(tmpl, params, String.class));
    }

    @Test
    @DisplayName("queryValue(NamedParamSql)：异常后可复用模板")
    void testQueryWithNamedParamSqlReuseAfterError() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamSql tmpl = NamedParamSql.of(
                "SELECT username FROM users WHERE id = #{id}");

        assertThrows(IllegalArgumentException.class, () ->
            template.queryValue(tmpl, Collections.singletonMap("wrong", 1), String.class));

        Optional<String> username = template.queryValue(tmpl,
                Collections.singletonMap("id", 1),
                String.class);
        assertTrue(username.isPresent());
        assertEquals("alice", username.get());
    }

    @Test
    @DisplayName("queryValue(NamedParamSql)：单值查询")
    void testQueryValueWithNamedParamSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamSql tmpl = NamedParamSql.of(
                "SELECT username FROM users WHERE id = #{id}");

        Optional<String> username = template.queryValue(tmpl,
                Collections.singletonMap("id", 1),
                String.class);

        assertTrue(username.isPresent());
        assertEquals("alice", username.get());
    }

    @Test
    @DisplayName("queryValueOrDefault(NamedParamSql)：有结果")
    void testQueryValueOrDefaultWithNamedParamSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamSql tmpl = NamedParamSql.of(
                "SELECT COUNT(*) FROM users WHERE active = #{active}");

        Long count = template.queryValueOrDefault(tmpl,
                Collections.singletonMap("active", true),
                Long.class,
                0L);

        assertEquals(4L, count);
    }

    @Test
    @DisplayName("queryValueOrDefault(NamedParamSql)：空结果返回默认值")
    void testQueryValueOrDefaultEmptyWithNamedParamSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamSql tmpl = NamedParamSql.of(
                "SELECT COUNT(*) FROM users WHERE id = #{id}");

        Long count = template.queryValueOrDefault(tmpl,
                Collections.singletonMap("id", 999),
                Long.class,
                0L);

        assertEquals(0L, count);
    }

    // ==================== queryFirst(Map) ====================

    @Test
    @DisplayName("queryFirst(Map)：返回 Optional<Map>")
    void testQueryFirstAsMap() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Optional<Map<String, Object>> user = template.queryFirst(
                "SELECT id, username FROM users WHERE id = #{id}",
                Collections.singletonMap("id", 3));

        assertTrue(user.isPresent());
        assertEquals("charlie", user.get().get("username"));
    }

    @Test
    @DisplayName("queryFirst(NamedParamSql)：返回 Optional<Map>")
    void testQueryFirstAsMapWithNamedParamSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamSql tmpl = NamedParamSql.of(
                "SELECT id, username FROM users WHERE id = #{id}");

        Optional<Map<String, Object>> user = template.queryFirst(tmpl,
                Collections.singletonMap("id", 3));

        assertTrue(user.isPresent());
        assertEquals("charlie", user.get().get("username"));
    }

    // ==================== queryBoolean ====================

    @Test
    @DisplayName("queryBoolean(Map)：true")
    void testQueryBooleanTrue() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        boolean active = template.queryBoolean(
                "SELECT active FROM users WHERE id = #{id}",
                Collections.singletonMap("id", 1));

        assertTrue(active);
    }

    @Test
    @DisplayName("queryBoolean(Map)：false")
    void testQueryBooleanFalse() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        boolean active = template.queryBoolean(
                "SELECT active FROM users WHERE id = #{id}",
                Collections.singletonMap("id", 3));

        assertFalse(active);
    }

    @Test
    @DisplayName("queryBoolean(NamedParamSql)：true")
    void testQueryBooleanTrueWithNamedParamSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamSql tmpl = NamedParamSql.of(
                "SELECT active FROM users WHERE id = #{id}");

        assertTrue(template.queryBoolean(tmpl, Collections.singletonMap("id", 1)));
    }

    @Test
    @DisplayName("queryBoolean(NamedParamSql)：false")
    void testQueryBooleanFalseWithNamedParamSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamSql tmpl = NamedParamSql.of(
                "SELECT active FROM users WHERE id = #{id}");

        assertFalse(template.queryBoolean(tmpl, Collections.singletonMap("id", 3)));
    }

    @Test
    @DisplayName("queryBoolean(NamedParamSql)：同模板多次复用")
    void testQueryBooleanWithNamedParamSqlReuse() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamSql tmpl = NamedParamSql.of(
                "SELECT active FROM users WHERE id = #{id}");

        assertTrue(template.queryBoolean(tmpl, Collections.singletonMap("id", 1L)));
        assertTrue(template.queryBoolean(tmpl, Collections.singletonMap("id", 2L)));
        assertFalse(template.queryBoolean(tmpl, Collections.singletonMap("id", 3L)));
    }

    // ==================== PreparedSql 重载 ====================

    @Test
    @DisplayName("query(PreparedSql)：查询")
    void testQueryWithPreparedSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        PreparedSql ps = PreparedSql
                .sql("SELECT COUNT(*) FROM users WHERE active = #{active}")
                .param("active", true)
                .build();

        Integer count = template.query(ps,
                rs -> {
                    rs.next();
                    return rs.getInt(1);
                });

        assertEquals(4, count);
    }

    @Test
    @DisplayName("queryList(PreparedSql)：查询全部")
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
    @DisplayName("queryValues(PreparedSql)：单列查询")
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
    @DisplayName("queryList(PreparedSql)：返回 List<Map>")
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
    @DisplayName("queryFirst(PreparedSql)：查询第一条")
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
    @DisplayName("queryFirst(PreparedSql)：返回 Optional<Map>")
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
    @DisplayName("queryValue(PreparedSql)：单值查询")
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
    @DisplayName("queryBoolean(PreparedSql)：true")
    void testQueryBooleanWithPreparedSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        PreparedSql ps = PreparedSql
                .sql("SELECT active FROM users WHERE id = #{id}")
                .param("id", 1)
                .build();

        assertTrue(template.queryBoolean(ps));
    }

    @Test
    @DisplayName("queryValueOrDefault(PreparedSql)：查询")
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
