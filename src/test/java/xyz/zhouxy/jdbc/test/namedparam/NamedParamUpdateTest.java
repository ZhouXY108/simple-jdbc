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
import xyz.zhouxy.jdbc.namedparam.PreparedSql;
import xyz.zhouxy.jdbc.test.BaseH2Test;

/**
 * 命名参数更新 API 测试。
 *
 * <p>验证 {@link NamedParamJdbcOperations} 中命名参数（<code>#{paramName}</code>）形式的
 * 更新方法（insert / update / delete / updateAndReturnKeys）是否通过
 * {@link SimpleJdbcTemplate} 正确委托到 {@link xyz.zhouxy.jdbc.JdbcOperations}。</p>
 */
@DisplayName("NamedParamJdbcOperations 命名参数更新")
class NamedParamUpdateTest extends BaseH2Test {

    @BeforeEach
    void setUp() throws Exception {
        resetDatabase();
    }

    // ==================== update（Map 参数） ====================

    @Test
    @DisplayName("update：命名参数插入")
    void testInsertWithNamedParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("name", "namedParamUser");
        params.put("email", "np@test.com");
        params.put("age", 25);

        int rows = template.update(
                "INSERT INTO users(username, email, age) VALUES(#{name}, #{email}, #{age})",
                params);

        assertEquals(1, rows);
    }

    @Test
    @DisplayName("update：命名参数更新")
    void testUpdateWithNamedParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Map<String, Object> params = new HashMap<String, Object>();
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
    @DisplayName("update：命名参数删除")
    void testDeleteWithNamedParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        int rows = template.update(
                "DELETE FROM users WHERE username = #{name}",
                Collections.singletonMap("name", "eve"));

        assertEquals(1, rows);
    }

    // ==================== updateAndReturnKeys ====================

    @Test
    @DisplayName("updateAndReturnKeys：命名参数插入返回主键")
    void testUpdateAndReturnKeys() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        RowMapper<Long> rowMapper = (rs, rowNum) -> rs.getLong(1);

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("name", "keyUser");
        params.put("email", "key@test.com");

        List<Long> keys = template.updateAndReturnKeys(
                "INSERT INTO users(username, email) VALUES(#{name}, #{email})",
                params,
                rowMapper);

        assertEquals(1, keys.size());
        assertTrue(keys.get(0) > 0);
    }

    // ==================== 参数编码 ====================

    @Test
    @DisplayName("命名参数，处理 ParamBuilder 的编码逻辑（如 LocalDate）")
    void testNamedParamWithLocalDate() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Map<String, Object> params = new HashMap<String, Object>();
        params.put("date", java.time.LocalDate.of(2000, 1, 1));
        params.put("name", "alice");

        int rows = template.update(
                "UPDATE users SET birth_date = #{date} WHERE username = #{name}",
                params);

        assertEquals(1, rows);
    }

    // ==================== PreparedSql 重载：更新 ====================

    @Test
    @DisplayName("PreparedSql 重载：update")
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
    @DisplayName("PreparedSql 重载：updateAndReturnKeys")
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
