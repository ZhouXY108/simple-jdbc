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

import xyz.zhouxy.jdbc.RowMapper;
import xyz.zhouxy.jdbc.SimpleJdbcTemplate;
import xyz.zhouxy.jdbc.namedparam.NamedParamJdbcOperations;
import xyz.zhouxy.jdbc.namedparam.NamedParamSql;
import xyz.zhouxy.jdbc.namedparam.PreparedSql;
import xyz.zhouxy.jdbc.test.BaseH2Test;

/**
 * 命名参数更新 API 测试。
 *
 * <p>验证 {@link NamedParamJdbcOperations} 中命名参数（<code>#{paramName}</code>）形式的
 * 更新方法（insert / update / delete / updateAndReturnKeys）的三种参数模式：
 * 直接传参（String + Map）、模板传参（NamedParamSql + Map）、预构建传参（PreparedSql）。</p>
 */
@DisplayName("NamedParamJdbcOperations 命名参数更新")
class NamedParamUpdateTest extends BaseH2Test {

    @BeforeEach
    void setUp() throws Exception {
        resetDatabase();
    }

    // ==================== update ====================

    @Test
    @DisplayName("update(Map)：插入")
    void testInsertWithNamedParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Map<String, Object> params = new HashMap<>();
        params.put("name", "namedParamUser");
        params.put("email", "np@test.com");
        params.put("age", 25);

        int rows = template.update(
                "INSERT INTO users(username, email, age) VALUES(#{name}, #{email}, #{age})",
                params);

        assertEquals(1, rows);
    }

    @Test
    @DisplayName("update(Map)：更新")
    void testUpdateWithNamedParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Map<String, Object> params = new HashMap<>();
        params.put("email", "newalice@test.com");
        params.put("name", "alice");

        int rows = template.update(
                "UPDATE users SET email = #{email} WHERE username = #{name}",
                params);

        assertEquals(1, rows);

        // 验证更新结果
        Optional<String> email = template.queryValue(
                "SELECT email FROM users WHERE username = #{name}",
                Collections.singletonMap("name", "alice"),
                String.class);
        assertTrue(email.isPresent());
        assertEquals("newalice@test.com", email.get());
    }

    @Test
    @DisplayName("update(Map)：删除")
    void testDeleteWithNamedParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        int rows = template.update(
                "DELETE FROM users WHERE username = #{name}",
                Collections.singletonMap("name", "eve"));

        assertEquals(1, rows);
    }

    @Test
    @DisplayName("update(NamedParamSql)：更新")
    void testUpdateWithNamedParamSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamSql tmpl = NamedParamSql.of(
                "UPDATE users SET active = #{active} WHERE id = #{id}");

        Map<String, Object> params = new HashMap<>();
        params.put("active", false);
        params.put("id", 2);
        int rows = template.update(tmpl, params);
        assertEquals(1, rows);

        // 验证更新结果
        boolean active = template.queryBoolean(
                NamedParamSql.of("SELECT active FROM users WHERE id = #{id}"),
                Collections.singletonMap("id", 2));
        assertFalse(active);
    }

    @Test
    @DisplayName("update(NamedParamSql)：同模板多次执行")
    void testUpdateWithNamedParamSqlReuse() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamSql tmpl = NamedParamSql.of(
                "UPDATE users SET active = #{active} WHERE id = #{id}");

        int total = 0;
        Map<String, Object> params = new HashMap<>();
        params.put("active", false);
        params.put("id", 1);
        total += template.update(tmpl, params);

        params.put("id", 2);
        total += template.update(tmpl, params);
        assertEquals(2, total);

        // charlie (id=3) 初始即 active=FALSE，加上 alice、bob 一共 3 条 inactive
        Integer count = template.query(
                NamedParamSql.of("SELECT COUNT(*) FROM users WHERE active = #{active}"),
                Collections.singletonMap("active", false),
                (rs) -> {
                    rs.next();
                    return rs.getInt(1);
                });
        assertEquals(Integer.valueOf(3), count);
    }

    // ==================== updateAndReturnKeys ====================

    @Test
    @DisplayName("updateAndReturnKeys(Map)：插入返回主键")
    void testUpdateAndReturnKeys() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        RowMapper<Long> rowMapper = (rs, rowNum) -> rs.getLong(1);

        Map<String, Object> params = new HashMap<>();
        params.put("name", "keyUser");
        params.put("email", "key@test.com");

        List<Long> keys = template.updateAndReturnKeys(
                "INSERT INTO users(username, email) VALUES(#{name}, #{email})",
                params,
                rowMapper);

        assertEquals(1, keys.size());
        assertTrue(keys.get(0) > 0);
    }

    @Test
    @DisplayName("updateAndReturnKeys(NamedParamSql)：插入返回主键")
    void testUpdateAndReturnKeysWithNamedParamSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamSql tmpl = NamedParamSql.of(
                "INSERT INTO users (username, email, age, active) "
                        + "VALUES (#{username}, #{email}, #{age}, #{active})");

        Map<String, Object> params = new HashMap<>();
        params.put("username", "frank");
        params.put("email", "frank@e.com");
        params.put("age", 40);
        params.put("active", true);

        List<Long> keys = template.updateAndReturnKeys(tmpl, params,
                (rs, rowNum) -> rs.getLong(1));

        assertEquals(1, keys.size());
        assertTrue(keys.get(0) > 0);
    }

    @Test
    @DisplayName("updateAndReturnKeys(NamedParamSql)：缺少参数名应抛异常")
    void testUpdateAndReturnKeysWithNamedParamSqlMissingParam() {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamSql tmpl = NamedParamSql.of(
                "INSERT INTO users (username, email) VALUES (#{username}, #{email})");
        Map<String, Object> params = Collections.singletonMap("username", "error");
        assertThrows(IllegalArgumentException.class, () ->
            template.updateAndReturnKeys(tmpl, params, (rs, rowNum) -> rs.getLong(1)));
    }

    // ==================== 参数编码 ====================

    @Test
    @DisplayName("命名参数，处理 ParamBuilder 的编码逻辑（如 LocalDate）")
    void testNamedParamWithLocalDate() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Map<String, Object> params = new HashMap<>();
        params.put("date", java.time.LocalDate.of(2000, 1, 1));
        params.put("name", "alice");

        int rows = template.update(
                "UPDATE users SET birth_date = #{date} WHERE username = #{name}",
                params);

        assertEquals(1, rows);
    }

    // ==================== PreparedSql 重载 ====================

    @Test
    @DisplayName("update(PreparedSql)：更新")
    void testUpdateWithPreparedSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        PreparedSql ps = PreparedSql
                .sql("UPDATE users SET email = #{email} WHERE username = #{name}")
                .param("email", "ps@test.com")
                .param("name", "alice")
                .build();

        int rows = template.update(ps);
        assertEquals(1, rows);
    }

    @Test
    @DisplayName("updateAndReturnKeys(PreparedSql)：插入返回主键")
    void testUpdateAndReturnKeysWithPreparedSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        PreparedSql ps = PreparedSql
                .sql("INSERT INTO users(username, email) VALUES(#{name}, #{email})")
                .param("name", "keyPS")
                .param("email", "keyps@test.com")
                .build();

        List<Long> keys = template.updateAndReturnKeys(ps,
                (rs, rowNum) -> rs.getLong(1));
        assertEquals(1, keys.size());
        assertTrue(keys.get(0) > 0);
    }

}
