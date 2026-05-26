package xyz.zhouxy.jdbc.test;

import static org.junit.jupiter.api.Assertions.*;
import static xyz.zhouxy.jdbc.ParamBuilder.buildParams;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.zhouxy.jdbc.RowMapper;
import xyz.zhouxy.jdbc.SimpleJdbcTemplate;

/**
 * 更新 API 测试：update、updateAndReturnKeys。
 */
@DisplayName("SimpleJdbcTemplate 更新操作")
class UpdateTest extends BaseH2Test {

    private static final Logger logger = LoggerFactory.getLogger(UpdateTest.class);

    @BeforeEach
    void setUp() throws Exception {
        resetDatabase();
    }

    // ==================== update ====================

    @Test
    @DisplayName("update：INSERT 操作返回影响行数 1")
    void testUpdateInsert() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        User user = new User(
            "frank",
            "frank@example.com",
            25, 10000L,
            true,
            LocalDateTime.now(),
            LocalDate.now(),
            LocalTime.now());
        String sql = "INSERT INTO users (" //
                + "    username," //
                + "    email," //
                + "    age," //
                + "    balance," //
                + "    active," //
                + "    created_at," //
                + "    birth_date," //
                + "    work_start_time" //
                + ")" //
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        int rows = template.update(
                sql,
                buildParams(
                    user.getUsername(),
                    user.getEmail(),
                    user.getAge(),
                    user.getBalance(),
                    user.getActive(),
                    user.getCreatedAt(),
                    user.getBirthDate(),
                    user.getWorkStartTime()
                ));

        logger.info("INSERT 影响行数: {}", rows);
        assertEquals(1, rows);
    }

    @Test
    @DisplayName("update：UPDATE 操作返回影响行数")
    void testUpdateModify() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        int rows = template.update(
                "UPDATE users SET email = ? WHERE username = ?",
                buildParams("newalice@example.com", "alice"));

        logger.info("UPDATE 影响行数: {}", rows);
        assertEquals(1, rows);

        // 验证数据确实被更新
        int count = template.update(
                "UPDATE users SET email = ? WHERE username = ? AND email = ?",
                buildParams("alice@example.com", "alice", "newalice@example.com"));
        assertEquals(1, count);
    }

    @Test
    @DisplayName("update：DELETE 操作返回影响行数")
    void testUpdateDelete() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        int rows = template.update(
                "DELETE FROM users WHERE username = ?",
                buildParams("charlie"));

        logger.info("DELETE 影响行数: {}", rows);
        assertEquals(1, rows);
    }

    @Test
    @DisplayName("update：影响 0 行")
    void testUpdateZeroRows() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        int rows = template.update(
                "UPDATE users SET email = ? WHERE username = ?",
                buildParams("x@x.com", "nobody"));

        logger.info("影响 0 行: {}", rows);
        assertEquals(0, rows);
    }

    @Test
    @DisplayName("update：DELETE 全表")
    void testUpdateDeleteAll() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        int rows = template.update("DELETE FROM users");

        logger.info("DELETE 全表影响行数: {}", rows);
        assertEquals(5, rows);

        // 验证表为空
        int count = template.query("SELECT COUNT(*) FROM users",
                rs -> { rs.next(); return rs.getInt(1); });
        assertEquals(0, count);
    }

    @Test
    @DisplayName("update：null 参数数组")
    void testUpdateWithNullParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        int rows = template.update(
                "DELETE FROM users WHERE username = ?",
                new Object[]{ null });

        // 因为 DELETE ? 中参数 null 不会匹配任何行
        assertEquals(0, rows);
    }

    @Test
    @DisplayName("update：无参数重载")
    void testUpdateNoParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        int rows = template.update(
                "UPDATE users SET balance = 9999 WHERE username = 'alice'");

        assertEquals(1, rows);
    }

    @Test
    @DisplayName("update：语法错误抛出 SQLException")
    void testUpdateInvalidSql() {
        SimpleJdbcTemplate template = createTemplate();

        assertThrows(SQLException.class, () ->
                template.update("UPDAT users SET x = 1"));
    }

    // ==================== updateAndReturnKeys ====================

    @Test
    @DisplayName("updateAndReturnKeys：INSERT 返回自增主键")
    void testUpdateAndReturnKeys() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        List<Long> keys = template.updateAndReturnKeys(
                "INSERT INTO users (username, email, age, balance, active) VALUES (?, ?, ?, ?, ?)",
                new Object[]{"grace", "grace@example.com", 29, 12000L, true},
                (rs, rowNumber) -> rs.getLong(1));

        logger.info("updateAndReturnKeys 返回: {}", keys);
        assertEquals(1, keys.size());
        assertTrue(keys.get(0) > 0);
    }

    @Test
    @DisplayName("updateAndReturnKeys：批量插入，返回所有自增主键")
    void testUpdateAndReturnKeysMultiple() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        // 使用多条 INSERT（H2 支持）
        List<Long> keys = template.updateAndReturnKeys(
                "INSERT INTO users (username, email, age, balance, active) VALUES " +
                "(?, ?, ?, ?, ?), (?, ?, ?, ?, ?)",
                new Object[]{
                        "henry", "henry@example.com", 33, 18000L, true,
                        "iris", "iris@example.com", 27, 9000L, false
                },
                (RowMapper<Long>) (rs, rowNumber) -> rs.getLong(1));

        logger.info("updateAndReturnKeys 返回 {} 个主键", keys.size());
        assertEquals(2, keys.size());
    }

    @Test
    @DisplayName("updateAndReturnKeys：无参数重载")
    void testUpdateAndReturnKeysNoParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        List<Long> keys = template.updateAndReturnKeys(
                "INSERT INTO users (username) VALUES ('jack')",
                (rs, rowNumber) -> rs.getLong(1));

        assertEquals(1, keys.size());
        assertTrue(keys.get(0) > 0);
    }
}
