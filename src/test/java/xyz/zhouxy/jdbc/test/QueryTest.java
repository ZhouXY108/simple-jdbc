package xyz.zhouxy.jdbc.test;

import static org.junit.jupiter.api.Assertions.*;
import static xyz.zhouxy.jdbc.ParamBuilder.buildParams;
import static xyz.zhouxy.jdbc.test.JdbcTestAssertions.assertLinkedHashMapOrder;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.zhouxy.jdbc.ParamBuilder;
import xyz.zhouxy.jdbc.SimpleJdbcTemplate;

/**
 * 查询 API 测试：query、queryList、queryFirst、queryValues、queryValue、queryBoolean。
 */
@DisplayName("SimpleJdbcTemplate 查询操作")
class QueryTest extends BaseH2Test {

    private static final Logger logger = LoggerFactory.getLogger(QueryTest.class);

    // ================================
    // #region - query(ResultHandler)
    // ================================

    @Test
    @DisplayName("query + ResultHandler：统计总行数")
    void testQueryWithResultHandlerCount() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Integer count = template.query(
                "SELECT COUNT(*) FROM users",
                new Object[0],
                rs -> {
                    rs.next();
                    return rs.getInt(1);
                });

        logger.info("query 返回总行数: {}", count);
        assertEquals(5, count);
    }

    @Test
    @DisplayName("query + ResultHandler：聚合求和")
    void testQueryWithResultHandlerAggregation() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Long totalBalance = template.query(
                "SELECT SUM(balance) FROM users",
                new Object[0],
                rs -> {
                    rs.next();
                    return rs.getLong(1);
                });

        logger.info("query 聚合 balance 总和: {}", totalBalance);
        assertNotNull(totalBalance);
    }

    @Test
    @DisplayName("query：无参数查询（default 方法）")
    void testQueryNoParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Integer count = template.query(
                "SELECT COUNT(*) FROM users",
                rs -> {
                    rs.next();
                    return rs.getInt(1);
                });

        assertEquals(5, count);
    }

    // ================================
    // #endregion - query(ResultHandler)
    // ================================

    // ================================
    // #region - queryList(RowMapper)
    // ================================

    @Test
    @DisplayName("queryList + RowMapper：查询全部用户")
    void testQueryListWithRowMapper() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        UserRowMapper rowMapper = new UserRowMapper();

        List<User> users = template.queryList("SELECT * FROM users ORDER BY id",
                new Object[0], rowMapper);

        logger.info("queryList(RowMapper) 返回 {} 条记录", users.size());
        assertEquals(5, users.size());
        assertEquals("alice", users.get(0).getUsername());
    }

    @Test
    @DisplayName("queryList + RowMapper：带参数条件查询")
    void testQueryListWithParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        UserRowMapper rowMapper = new UserRowMapper();

        List<User> users = template.queryList(
                "SELECT * FROM users WHERE active = ? ORDER BY id",
                new Object[]{true}, rowMapper);

        logger.info("queryList 活跃用户: {} 条", users.size());
        assertEquals(4, users.size());
        users.forEach(u -> assertTrue(u.getActive()));
    }

    @Test
    @DisplayName("queryList + RowMapper：无参数重载")
    void testQueryListRowMapperNoParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        List<User> users = template.queryList(
                "SELECT * FROM users ORDER BY id",
                new UserRowMapper());

        assertEquals(5, users.size());
    }

    // ================================
    // #endregion - queryList(RowMapper)
    // ================================

    // ================================
    // #region - queryValues(Class)
    // ================================

    @Test
    @DisplayName("queryValues(Class)：单列查询返回 String 列表")
    void testQueryValuesWithClassString() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        List<String> usernames = template.queryValues(
                "SELECT username FROM users ORDER BY id",
                String.class);

        logger.info("queryValues(Class) 返回用户名: {}", usernames);
        assertEquals(5, usernames.size());
        assertTrue(usernames.contains("alice"));
    }

    @Test
    @DisplayName("queryValues(Class)：空结果集返回空列表")
    void testQueryValuesEmptyResult() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        List<String> result = template.queryValues(
                "SELECT username FROM users WHERE id = ?",
                buildParams(999), String.class);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ================================
    // #endregion - queryValues(Class)
    // ================================

    // ================================
    // #region - queryList(Map)
    // ================================

    @Test
    @DisplayName("queryList(Map)：返回 List<Map>")
    void testQueryListAsMap() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        List<Map<String, Object>> users = template.queryList(
                "SELECT id, username, email FROM users WHERE id = ?",
                new Object[]{1});

        logger.info("queryList(Map) 返回: {}", users);
        assertEquals(1, users.size());
        assertEquals("alice", users.get(0).get("username"));
        assertLinkedHashMapOrder(users.get(0), "id", "username", "email");
    }

    @Test
    @DisplayName("queryList(Map)：无参数重载")
    void testQueryListMapNoParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        List<Map<String, Object>> users = template.queryList(
                "SELECT id, username FROM users ORDER BY id");

        assertEquals(5, users.size());
        assertLinkedHashMapOrder(users.get(0), "id", "username");
    }

    // ================================
    // #endregion - queryList(Map)
    // ================================

    // ================================
    // #region - queryFirst(RowMapper)
    // ================================

    @Test
    @DisplayName("queryFirst + RowMapper：查询第一条记录")
    void testQueryFirstWithRowMapper() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        UserRowMapper rowMapper = new UserRowMapper();

        Optional<User> user = template.queryFirst(
                "SELECT * FROM users ORDER BY id",
                rowMapper);

        assertTrue(user.isPresent());
        assertEquals("alice", user.get().getUsername());
    }

    @Test
    @DisplayName("queryFirst + RowMapper：空结果返回 Optional.empty()")
    void testQueryFirstEmpty() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Optional<User> user = template.queryFirst(
                "SELECT * FROM users WHERE id = ?",
                buildParams(999), new UserRowMapper());

        assertFalse(user.isPresent());
    }

    @Test
    @DisplayName("queryFirst + RowMapper：null 参数")
    void testQueryFirstWithNullParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Optional<User> user = template.queryFirst(
                "SELECT * FROM users ORDER BY id",
                (Object[]) null, new UserRowMapper());

        assertTrue(user.isPresent());
    }

    // ================================
    // #endregion - queryFirst(RowMapper)
    // ================================

    // ================================
    // #region - queryValue(Class)
    // ================================

    @Test
    @DisplayName("queryValue(Class)：查询第一行第一列")
    void testQueryValueWithClass() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Optional<String> username = template.queryValue(
                "SELECT username FROM users ORDER BY id",
                String.class);

        assertTrue(username.isPresent());
        assertEquals("alice", username.get());
    }

    @Test
    @DisplayName("queryValue(Class)：空结果返回 Optional.empty()")
    void testQueryValueClassEmpty() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Optional<String> result = template.queryValue(
                "SELECT username FROM users WHERE id = ?",
                buildParams(999), String.class);

        assertFalse(result.isPresent());
    }


    @Test
    @DisplayName("queryValue + Class：统计总行数")
    void testQueryValueWithClass_queryCount() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        int count = template.queryValue(
                "SELECT COUNT(*) FROM users",
                new Object[0],
                Integer.class)
                .orElse(0);

        logger.info("query 返回总行数: {}", count);
        assertEquals(5, count);
    }

    @Test
    @DisplayName("queryValue + Class：聚合求和")
    void testQueryValueWithClass_queryAggregation() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Long totalBalance = template.queryValue(
                "SELECT SUM(balance) FROM users",
                new Object[0],
                Long.class)
                .orElse(0L);

        logger.info("query 聚合 balance 总和: {}", totalBalance);
        assertNotNull(totalBalance);
    }

    // ================================
    // #endregion - queryValue(Class)
    // ================================

    // ================================
    // #region - queryValueOrDefault
    // ================================

    @Test
    @DisplayName("queryValueOrDefault：有结果时返回值")
    void testQueryValueOrDefaultWithResult() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        String username = template.queryValueOrDefault(
                "SELECT username FROM users WHERE id = ?",
                new Object[]{1}, String.class, "default");

        assertEquals("alice", username);
    }

    @Test
    @DisplayName("queryValueOrDefault：无结果时返回默认值")
    void testQueryValueOrDefaultWithDefault() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        String username = template.queryValueOrDefault(
                "SELECT username FROM users WHERE id = ?",
                new Object[]{999}, String.class, "unknown");

        assertEquals("unknown", username);
    }

    @Test
    @DisplayName("queryValueOrDefault：COUNT 聚合查询")
    void testQueryValueOrDefaultCount() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        long count = template.queryValueOrDefault(
                "SELECT COUNT(*) FROM users",
                new Object[0], Long.class, 0L);

        assertEquals(5L, count);
    }

    @Test
    @DisplayName("queryValueOrDefault：SUM 聚合查询")
    void testQueryValueOrDefaultSum() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        long totalBalance = template.queryValueOrDefault(
                "SELECT SUM(balance) FROM users",
                new Object[0], Long.class, 0L);

        assertTrue(totalBalance > 0);
        logger.info("queryValueOrDefault SUM 结果: {}", totalBalance);
    }

    @Test
    @DisplayName("queryValueOrDefault：无参数重载")
    void testQueryValueOrDefaultNoParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        long count = template.queryValueOrDefault(
                "SELECT COUNT(*) FROM users",
                Long.class, 0L);

        assertEquals(5L, count);
    }

    @Test
    @DisplayName("queryValueOrDefault：空表 COUNT 返回默认值 0")
    void testQueryValueOrDefaultEmptyTable() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        long count = template.queryValueOrDefault(
                "SELECT COUNT(*) FROM users WHERE id = ?",
                new Object[]{999}, Long.class, 0L);

        assertEquals(0L, count);
    }

    // ================================
    // #endregion - queryValueOrDefault
    // ================================

    // ================================
    // #region - queryFirst(Map)
    // ================================

    @Test
    @DisplayName("queryFirst(Map)：返回 Optional<Map>")
    void testQueryFirstAsMap() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Optional<Map<String, Object>> user = template.queryFirst(
                "SELECT id, username FROM users WHERE id = ?",
                buildParams(2));

        assertTrue(user.isPresent());
        assertEquals("bob", user.get().get("username"));
        assertLinkedHashMapOrder(user.get(), "id", "username");
    }

    @Test
    @DisplayName("queryFirst(Map)：返回 Optional<Map> 无参数重载")
    void testQueryFirstAsMapNoParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Optional<Map<String, Object>> user = template.queryFirst(
                "SELECT id, username FROM users ORDER BY id");

        assertTrue(user.isPresent());
        assertEquals("alice", user.get().get("username"));
        assertLinkedHashMapOrder(user.get(), "id", "username");
    }

    @Test
    @DisplayName("queryFirst(Map)：空结果返回 Optional.empty()")
    void testQueryFirstMapEmpty() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Optional<Map<String, Object>> result = template.queryFirst(
                "SELECT * FROM users WHERE id = ?",
                new Object[]{-1});

        assertFalse(result.isPresent());
    }

    // ================================
    // #endregion - queryFirst(Map)
    // ================================

    // ================================
    // #region - queryBoolean
    // ================================

    @Test
    @DisplayName("queryBoolean：存在返回 true")
    void testQueryBooleanTrue() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        boolean exists = template.queryBoolean(
                "SELECT TRUE FROM users WHERE username = ?",
                new Object[]{"alice"});

        assertTrue(exists);
    }

    @Test
    @DisplayName("queryBoolean：不存在返回 false")
    void testQueryBooleanFalse() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        boolean exists = template.queryBoolean(
                "SELECT TRUE FROM users WHERE username = ?",
                new Object[]{"nobody"});

        assertFalse(exists);
    }

    @Test
    @DisplayName("queryBoolean：无参数重载")
    void testQueryBooleanNoParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        boolean exists = template.queryBoolean(
                "SELECT COUNT(*) > 0 FROM users");

        assertTrue(exists);
    }

    @Test
    @DisplayName("queryBoolean：结果集为空返回 false")
    void testQueryBooleanEmptyResult() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        boolean exists = template.queryBoolean(
                "SELECT active FROM users WHERE id = ?",
                new Object[]{999});

        assertFalse(exists);
    }

    // ================================
    // #endregion - queryBoolean
    // ================================

    // ================================
    // #region - 边界情况
    // ================================

    @Test
    @DisplayName("边界：查询包含 null 字段的数据")
    void testQueryWithNullFields() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Optional<User> user = template.queryFirst(
                "SELECT * FROM users WHERE username = ?",
                new Object[]{"charlie"}, new UserRowMapper());

        assertTrue(user.isPresent());
        assertNull(user.get().getEmail());
        assertNull(user.get().getAge());
        assertNull(user.get().getBirthDate());
        assertNull(user.get().getWorkStartTime());
    }

    @Test
    @DisplayName("边界：查询不存在的表应抛出 SQLException")
    void testQueryInvalidTable() {
        SimpleJdbcTemplate template = createTemplate();

        assertThrows(SQLException.class, () ->
                template.queryValues("SELECT * FROM non_existent_table", String.class));
    }

    @Test
    @DisplayName("边界：查询语法错误应抛出 SQLException")
    void testQueryInvalidSql() {
        SimpleJdbcTemplate template = createTemplate();

        assertThrows(SQLException.class, () ->
                template.queryValues("SELEC * FROM users", String.class));
    }

    @Test
    @DisplayName("边界：重复列标签后者静默覆盖")
    void testDuplicateColumnLabelOverride() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        // 两列标签相同（后者为字面量），验证后者覆盖前者
        List<Map<String, Object>> results = template.queryList(
                "SELECT username, 'aa' AS username FROM users WHERE username = ?",
                new Object[]{"alice"});

        assertEquals(1, results.size());
        assertEquals("aa", results.get(0).get("username"));
    }

    @Test
    @DisplayName("边界：单行查询结果")
    void testQuerySingleRow() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Optional<User> user = template.queryFirst(
                "SELECT * FROM users WHERE id = ?",
                new Object[]{1}, new UserRowMapper());

        assertTrue(user.isPresent());
        assertEquals(Long.valueOf(1), user.get().getId());
    }

    @Test
    @DisplayName("边界：使用 ParamBuilder.EMPTY_OBJECT_ARRAY")
    void testQueryWithEmptyObjectArray() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        List<User> users = template.queryList(
                "SELECT * FROM users ORDER BY id",
                ParamBuilder.EMPTY_OBJECT_ARRAY, new UserRowMapper());

        assertEquals(5, users.size());
    }

    // ================================
    // #endregion - 边界情况
    // ================================
}
