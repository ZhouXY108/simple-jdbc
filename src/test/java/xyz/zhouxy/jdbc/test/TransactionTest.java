package xyz.zhouxy.jdbc.test;

import static org.junit.jupiter.api.Assertions.*;
import static xyz.zhouxy.jdbc.ParamBuilder.buildParams;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.zhouxy.jdbc.JdbcOperations;
import xyz.zhouxy.jdbc.SimpleJdbcTemplate;
import xyz.zhouxy.jdbc.TransactionException;

/**
 * 事务 API 测试：executeTransaction、commitIfTrue。
 */
@DisplayName("SimpleJdbcTemplate 事务操作")
class TransactionTest extends BaseH2Test {

    private static final Logger logger = LoggerFactory.getLogger(TransactionTest.class);

    @BeforeEach
    void setUp() throws Exception {
        resetDatabase();
    }

    // ==================== executeTransaction 正常提交 ====================

    @Test
    @DisplayName("executeTransaction：正常提交，数据持久化")
    void testExecuteTransactionCommit() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        template.executeTransaction((JdbcOperations ops) -> {
            ops.update("INSERT INTO users (username, email, age, balance, active) VALUES (?, ?, ?, ?, ?)",
                    buildParams("txUser1", "tx1@test.com", 25, 1000L, true));
            ops.update("UPDATE users SET balance = ? WHERE username = ?",
                    buildParams(99999L, "alice"));
        });

        // 验证事务已提交
        Optional<String> newUser = template.queryFirst(
                "SELECT username FROM users WHERE username = ?",
                buildParams("txUser1"), String.class);
        assertTrue(newUser.isPresent());

        Optional<Long> balance = template.queryFirst(
                "SELECT balance FROM users WHERE username = ?",
                buildParams("alice"), Long.class);
        assertEquals(Long.valueOf(99999L), balance.orElse(null));

        logger.info("事务提交验证通过");
    }

    // ==================== executeTransaction 异常回滚 ====================

    @Test
    @DisplayName("executeTransaction：异常回滚，数据恢复原状")
    void testExecuteTransactionRollback() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        // 记录原始 balance
        Optional<Long> originalBalance = template.queryFirst(
                "SELECT balance FROM users WHERE username = ?",
                buildParams("alice"), Long.class);

        TransactionException ex = assertThrows(TransactionException.class, () ->
                template.executeTransaction((JdbcOperations ops) -> {
                    ops.update("UPDATE users SET balance = ? WHERE username = ?",
                            buildParams(0L, "alice"));
                    ops.update("INSERT INTO users (username, email) VALUES (?, ?)",
                            buildParams("txUser2", "tx2@test.com"));
                    // 故意抛出异常触发回滚
                    throw new RuntimeException("模拟业务异常");
                }));

        logger.info("捕获到 TransactionException: {}", ex.getMessage());
        assertNotNull(ex.getCause());
        assertEquals(RuntimeException.class, ex.getCause().getClass());
        assertEquals("模拟业务异常", ex.getCause().getMessage());

        // 验证更新已回滚
        Optional<Long> currentBalance = template.queryFirst(
                "SELECT balance FROM users WHERE username = ?",
                buildParams("alice"), Long.class);
        assertEquals(originalBalance.orElse(null), currentBalance.orElse(null));

        // 验证插入已回滚
        Optional<String> rolledBackUser = template.queryFirst(
                "SELECT username FROM users WHERE username = ?",
                buildParams("txUser2"), String.class);
        assertFalse(rolledBackUser.isPresent());

        logger.info("事务回滚验证通过");
    }

    @Test
    @DisplayName("executeTransaction：SQL 异常触发回滚")
    void testExecuteTransactionSqlExceptionRollback() {
        SimpleJdbcTemplate template = createTemplate();

        assertThrows(TransactionException.class, () ->
                template.executeTransaction((JdbcOperations ops) -> {
                    ops.update("INSERT INTO users (username) VALUES (?)",
                            buildParams("validUser"));
                    // 错误的 SQL
                    ops.update("INVALID SQL STATEMENT");
                }));

        // 验证插入已回滚
        assertDoesNotThrow(() -> {
            Optional<String> user = template.queryFirst(
                    "SELECT username FROM users WHERE username = ?",
                    buildParams("validUser"), String.class);
            assertFalse(user.isPresent());
        });
    }

    // ==================== commitIfTrue 返回 true 提交 ====================

    @Test
    @DisplayName("commitIfTrue：返回 true 提交事务")
    void testCommitIfTrueCommit() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        template.commitIfTrue((JdbcOperations ops) -> {
            ops.update("INSERT INTO users (username, email) VALUES (?, ?)",
                    buildParams("cftUser", "cft@test.com"));
            return true;
        });

        // 验证数据已持久化
        Optional<String> user = template.queryFirst(
                "SELECT username FROM users WHERE username = ?",
                buildParams("cftUser"), String.class);
        assertTrue(user.isPresent());

        logger.info("commitIfTrue(true) 提交验证通过");
    }

    @Test
    @DisplayName("commitIfTrue：返回 false 回滚事务")
    void testCommitIfFalseRollback() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        template.commitIfTrue((JdbcOperations ops) -> {
            ops.update("INSERT INTO users (username, email) VALUES (?, ?)",
                    buildParams("cffUser", "cff@test.com"));
            return false;
        });

        // 验证数据已回滚
        Optional<String> user = template.queryFirst(
                "SELECT username FROM users WHERE username = ?",
                buildParams("cffUser"), String.class);
        assertFalse(user.isPresent());

        logger.info("commitIfTrue(false) 回滚验证通过");
    }

    @Test
    @DisplayName("commitIfTrue：异常触发回滚")
    void testCommitIfTrueExceptionRollback() {
        SimpleJdbcTemplate template = createTemplate();

        assertThrows(TransactionException.class, () ->
                template.commitIfTrue((JdbcOperations ops) -> {
                    ops.update("INSERT INTO users (username) VALUES (?)",
                            buildParams("exUser"));
                    throw new IllegalStateException("条件不满足");
                }));

        // 验证回滚
        assertDoesNotThrow(() -> {
            Optional<String> user = template.queryFirst(
                    "SELECT username FROM users WHERE username = ?",
                    buildParams("exUser"), String.class);
            assertFalse(user.isPresent());
        });
    }

    // ==================== 事务内查询可见性 ====================

    @Test
    @DisplayName("executeTransaction：事务内可查询到未提交的数据")
    void testTransactionVisibility() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        template.executeTransaction((JdbcOperations ops) -> {
            ops.update("INSERT INTO users (username, email) VALUES (?, ?)",
                    buildParams("visible", "visible@test.com"));

            // 在同一事务内可以查询到刚插入的数据
            Optional<String> user = ops.queryFirst(
                    "SELECT username FROM users WHERE username = ?",
                    buildParams("visible"), String.class);
            assertTrue(user.isPresent());

            logger.info("事务内查询可见性验证通过");
        });
    }

    // ==================== 边界情况 ====================

    @Test
    @DisplayName("executeTransaction：空操作（无异常）正常提交")
    void testExecuteTransactionEmpty() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        // 空操作不应抛异常
        assertDoesNotThrow(() ->
                template.executeTransaction(ops -> { /* no-op */ }));

        // 数据应保持不变
        int count = template.query("SELECT COUNT(*) FROM users",
                rs -> { rs.next(); return rs.getInt(1); });
        assertEquals(5, count);
    }

    @Test
    @DisplayName("executeTransaction：null 操作抛异常")
    @SuppressWarnings("null")
    void testExecuteTransactionNullOps() {
        SimpleJdbcTemplate template = createTemplate();

        assertThrows(Exception.class, () ->
                template.executeTransaction(null));
    }
}
