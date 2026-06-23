package xyz.zhouxy.jdbc.test;

import static org.junit.jupiter.api.Assertions.*;
import static xyz.zhouxy.jdbc.ParamBuilder.buildParams;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.zhouxy.jdbc.JdbcOperations;
import xyz.zhouxy.jdbc.SimpleJdbcTemplate;
import xyz.zhouxy.jdbc.TransactionException;
import xyz.zhouxy.jdbc.function.ThrowingConsumer;
import xyz.zhouxy.jdbc.namedparam.NamedParamJdbcOperations;

/**
 * 事务 API 测试：通过 {@link xyz.zhouxy.jdbc.TransactionTemplate#execute} 和
 * {@link xyz.zhouxy.jdbc.TransactionTemplate#commitIfTrue} 测试事务提交与回滚。
 */
@DisplayName("TransactionTemplate 事务操作")
class TransactionTest extends BaseH2Test {

    private static final Logger logger = LoggerFactory.getLogger(TransactionTest.class);

    @BeforeEach
    void setUp() throws Exception {
        resetDatabase();
    }

    // ==================== execute 正常提交 ====================

    @Test
    @DisplayName("execute：正常提交，数据持久化")
    void testExecuteTransactionCommit() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        template.transaction().execute((JdbcOperations ops) -> {
            ops.update("INSERT INTO users (username, email, age, balance, active) VALUES (?, ?, ?, ?, ?)",
                    buildParams("txUser1", "tx1@test.com", 25, 1000L, true));
            ops.update("UPDATE users SET balance = ? WHERE username = ?",
                    buildParams(99999L, "alice"));
        });

        // 验证事务已提交
        Optional<String> newUser = template.queryValue(
                "SELECT username FROM users WHERE username = ?",
                buildParams("txUser1"), String.class);
        assertTrue(newUser.isPresent());

        Optional<Long> balance = template.queryValue(
                "SELECT balance FROM users WHERE username = ?",
                buildParams("alice"), Long.class);
        assertEquals(Long.valueOf(99999L), balance.orElse(null));

        logger.info("事务提交验证通过");
    }

    // ==================== execute 异常回滚 ====================

    @Test
    @DisplayName("execute：异常回滚，数据恢复原状")
    void testExecuteTransactionRollback() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        // 记录原始 balance
        Optional<Long> originalBalance = template.queryValue(
                "SELECT balance FROM users WHERE username = ?",
                buildParams("alice"), Long.class);

        TransactionException ex = assertThrows(TransactionException.class, () ->
                template.transaction().execute((JdbcOperations ops) -> {
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
        Optional<Long> currentBalance = template.queryValue(
                "SELECT balance FROM users WHERE username = ?",
                buildParams("alice"), Long.class);
        assertEquals(originalBalance.orElse(null), currentBalance.orElse(null));

        // 验证插入已回滚
        Optional<String> rolledBackUser = template.queryValue(
                "SELECT username FROM users WHERE username = ?",
                buildParams("txUser2"), String.class);
        assertFalse(rolledBackUser.isPresent());

        logger.info("事务回滚验证通过");
    }

    @Test
    @DisplayName("execute：SQL 异常触发回滚")
    void testExecuteTransactionSqlExceptionRollback() {
        SimpleJdbcTemplate template = createTemplate();

        assertThrows(TransactionException.class, () ->
                template.transaction().execute((JdbcOperations ops) -> {
                    ops.update("INSERT INTO users (username) VALUES (?)",
                            buildParams("validUser"));
                    // 错误的 SQL
                    ops.update("INVALID SQL STATEMENT");
                }));

        // 验证插入已回滚
        assertDoesNotThrow(() -> {
            Optional<String> user = template.queryValue(
                    "SELECT username FROM users WHERE username = ?",
                    buildParams("validUser"), String.class);
            assertFalse(user.isPresent());
        });
    }

    // ==================== commitIfTrue：返回 true 提交 ====================

    @Test
    @DisplayName("commitIfTrue：返回 true 提交事务")
    void testCommitIfTrueCommit() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        template.transaction().commitIfTrue((JdbcOperations ops) -> {
            ops.update("INSERT INTO users (username, email) VALUES (?, ?)",
                    buildParams("cftUser", "cft@test.com"));
            return true;
        });

        // 验证数据已持久化
        Optional<String> user = template.queryValue(
                "SELECT username FROM users WHERE username = ?",
                buildParams("cftUser"), String.class);
        assertTrue(user.isPresent());

        logger.info("commitIfTrue(true) 提交验证通过");
    }

    @Test
    @DisplayName("commitIfTrue：返回 false 回滚事务")
    void testCommitIfFalseRollback() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        template.transaction().commitIfTrue((JdbcOperations ops) -> {
            ops.update("INSERT INTO users (username, email) VALUES (?, ?)",
                    buildParams("cffUser", "cff@test.com"));
            return false;
        });

        // 验证数据已回滚
        Optional<String> user = template.queryValue(
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
                template.transaction().commitIfTrue((JdbcOperations ops) -> {
                    ops.update("INSERT INTO users (username) VALUES (?)",
                            buildParams("exUser"));
                    throw new IllegalStateException("条件不满足");
                }));

        // 验证回滚
        assertDoesNotThrow(() -> {
            Optional<String> user = template.queryValue(
                    "SELECT username FROM users WHERE username = ?",
                    buildParams("exUser"), String.class);
            assertFalse(user.isPresent());
        });
    }

    // ==================== 事务内查询可见性 ====================

    @Test
    @DisplayName("execute：事务内可查询到未提交的数据")
    void testTransactionVisibility() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        template.transaction().execute((JdbcOperations ops) -> {
            ops.update("INSERT INTO users (username, email) VALUES (?, ?)",
                    buildParams("visible", "visible@test.com"));

            // 在同一事务内可以查询到刚插入的数据
            Optional<String> user = ops.queryValue(
                    "SELECT username FROM users WHERE username = ?",
                    buildParams("visible"), String.class);
            assertTrue(user.isPresent());

            logger.info("事务内查询可见性验证通过");
        });
    }

    // ==================== 边界情况 ====================

    @Test
    @DisplayName("execute：空操作（无异常）正常提交")
    void testExecuteTransactionEmpty() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        // 空操作不应抛异常
        assertDoesNotThrow(() ->
                template.transaction().execute(ops -> { /* no-op */ }));

        // 数据应保持不变
        int count = template.query("SELECT COUNT(*) FROM users",
                rs -> { rs.next(); return rs.getInt(1); });
        assertEquals(5, count);
    }

    @Test
    @DisplayName("execute：null 操作抛异常")
    @SuppressWarnings("null")
    void testExecuteTransactionNullOps() {
        SimpleJdbcTemplate template = createTemplate();

        assertThrows(Exception.class, () ->
                template.transaction().execute((ThrowingConsumer<JdbcOperations, Exception>) null));
    }

    // ==================== executeNamed 命名参数事务 ====================

    @Test
    @DisplayName("executeNamed：纯命名参数提交，数据持久化")
    void testExecuteNamedCommit() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        template.transaction().executeNamed(nops -> {
            Map<String, Object> params = new HashMap<>();
            params.put("name", "txNpUser");
            params.put("email", "txnp@test.com");
            nops.update("INSERT INTO users (username, email) VALUES(#{name}, #{email})",
                    params);
        });

        Optional<String> user = template.queryValue(
                "SELECT username FROM users WHERE username = ?",
                buildParams("txNpUser"), String.class);
        assertTrue(user.isPresent());
    }

    @Test
    @DisplayName("executeNamed：异常触发回滚，数据恢复原状")
    void testExecuteNamedRollback() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        Optional<Long> originalBalance = template.queryValue(
                "SELECT balance FROM users WHERE username = ?",
                buildParams("alice"), Long.class);

        TransactionException ex = assertThrows(TransactionException.class, () ->
                template.transaction().executeNamed(nops -> {
                    Map<String, Object> params = new HashMap<>();
                    params.put("bal", 0L);
                    params.put("name", "alice");
                    nops.update("UPDATE users SET balance = #{bal} WHERE username = #{name}",
                            params);
                    nops.update("INSERT INTO users (username) VALUES(#{name})",
                            Collections.singletonMap("name", "txNpRbUser"));
                    throw new RuntimeException("模拟业务异常");
                }));

        assertEquals(RuntimeException.class, ex.getCause().getClass());

        Optional<Long> currentBalance = template.queryValue(
                "SELECT balance FROM users WHERE username = ?",
                buildParams("alice"), Long.class);
        assertEquals(originalBalance.orElse(null), currentBalance.orElse(null));

        Optional<String> rolledBackUser = template.queryValue(
                "SELECT username FROM users WHERE username = ?",
                buildParams("txNpRbUser"), String.class);
        assertFalse(rolledBackUser.isPresent());
    }

    @Test
    @DisplayName("executeNamed：SQL 异常触发回滚")
    void testExecuteNamedSqlExceptionRollback() {
        SimpleJdbcTemplate template = createTemplate();

        assertThrows(TransactionException.class, () ->
                template.transaction().executeNamed(nops -> {
                    nops.update("INSERT INTO users (username) VALUES(#{name})",
                            Collections.singletonMap("name", "validUser"));
                    nops.update("INVALID SQL STATEMENT", Collections.emptyMap());
                }));

        assertDoesNotThrow(() -> {
            Optional<String> user = template.queryValue(
                    "SELECT username FROM users WHERE username = ?",
                    buildParams("validUser"), String.class);
            assertFalse(user.isPresent());
        });
    }

    // ==================== execute(BiConsumer) 混用参数事务 ====================

    @Test
    @DisplayName("execute(BiConsumer)：混用位置参数与命名参数提交")
    void testExecuteMixedCommit() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        template.transaction().execute((JdbcOperations ops, NamedParamJdbcOperations nops) -> {
            ops.update("UPDATE users SET email = ? WHERE username = ?",
                    buildParams("mixed@test.com", "bob"));
            Map<String, Object> params = new HashMap<>();
            params.put("name", "mixedUser");
            params.put("email", "mixed@test.com");
            nops.update("INSERT INTO users (username, email) VALUES(#{name}, #{email})",
                    params);
        });

        Optional<String> email = template.queryValue(
                "SELECT email FROM users WHERE username = ?",
                buildParams("bob"), String.class);
        assertEquals("mixed@test.com", email.orElse(null));

        Optional<String> newUser = template.queryValue(
                "SELECT username FROM users WHERE username = ?",
                buildParams("mixedUser"), String.class);
        assertTrue(newUser.isPresent());
    }

    @Test
    @DisplayName("execute(BiConsumer)：混用模式异常触发回滚")
    void testExecuteMixedRollback() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        TransactionException ex = assertThrows(TransactionException.class, () ->
                template.transaction().execute((JdbcOperations ops, NamedParamJdbcOperations nops) -> {
                    ops.update("UPDATE users SET email = ? WHERE username = ?",
                            buildParams("mixed_rb@test.com", "alice"));
                    Map<String, Object> params = new HashMap<>();
                    params.put("name", "mixedRbUser");
                    params.put("email", "mrb@test.com");
                    nops.update("INSERT INTO users (username, email) VALUES(#{name}, #{email})",
                            params);
                    throw new RuntimeException("模拟混用回滚");
                }));

        assertEquals(RuntimeException.class, ex.getCause().getClass());

        Optional<String> email = template.queryValue(
                "SELECT email FROM users WHERE username = ?",
                buildParams("alice"), String.class);
        assertEquals("alice@example.com", email.orElse(null));

        Optional<String> user = template.queryValue(
                "SELECT username FROM users WHERE username = ?",
                buildParams("mixedRbUser"), String.class);
        assertFalse(user.isPresent());
    }

    // ==================== commitIfTrueNamed 命名参数谓词事务 ====================

    @Test
    @DisplayName("commitIfTrueNamed：返回 true 提交事务")
    void testCommitIfTrueNamedCommit() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        template.transaction().commitIfTrueNamed(nops -> {
            Map<String, Object> params = new HashMap<>();
            params.put("name", "cftNpUser");
            params.put("email", "cftnp@test.com");
            nops.update("INSERT INTO users (username, email) VALUES(#{name}, #{email})",
                    params);
            return true;
        });

        Optional<String> user = template.queryValue(
                "SELECT username FROM users WHERE username = ?",
                buildParams("cftNpUser"), String.class);
        assertTrue(user.isPresent());
    }

    @Test
    @DisplayName("commitIfTrueNamed：返回 false 回滚事务")
    void testCommitIfTrueNamedFalseRollback() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        template.transaction().commitIfTrueNamed(nops -> {
            Map<String, Object> params = new HashMap<>();
            params.put("name", "cftNpFalse");
            params.put("email", "cftnpf@test.com");
            nops.update("INSERT INTO users (username, email) VALUES(#{name}, #{email})",
                    params);
            return false;
        });

        Optional<String> user = template.queryValue(
                "SELECT username FROM users WHERE username = ?",
                buildParams("cftNpFalse"), String.class);
        assertFalse(user.isPresent());
    }

    @Test
    @DisplayName("commitIfTrueNamed：异常触发回滚")
    void testCommitIfTrueNamedExceptionRollback() {
        SimpleJdbcTemplate template = createTemplate();

        assertThrows(TransactionException.class, () ->
                template.transaction().commitIfTrueNamed(nops -> {
                    nops.update("INSERT INTO users (username) VALUES(#{name})",
                            Collections.singletonMap("name", "cftNpEx"));
                    throw new IllegalStateException("条件不满足");
                }));

        assertDoesNotThrow(() -> {
            Optional<String> user = template.queryValue(
                    "SELECT username FROM users WHERE username = ?",
                    buildParams("cftNpEx"), String.class);
            assertFalse(user.isPresent());
        });
    }

    // ==================== commitIfTrue(BiPredicate) 混用谓词事务 ====================

    @Test
    @DisplayName("commitIfTrue(BiPredicate)：混用模式返回 true 提交")
    void testCommitIfTrueMixedCommit() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        template.transaction().commitIfTrue((JdbcOperations ops, NamedParamJdbcOperations nops) -> {
            ops.update("UPDATE users SET email = ? WHERE username = ?",
                    buildParams("bipred@test.com", "bob"));
            return nops.queryBoolean(
                    "SELECT active FROM users WHERE username = #{name}",
                    Collections.singletonMap("name", "bob"));
        });

        Optional<String> email = template.queryValue(
                "SELECT email FROM users WHERE username = ?",
                buildParams("bob"), String.class);
        assertEquals("bipred@test.com", email.orElse(null));
    }

    @Test
    @DisplayName("commitIfTrue(BiPredicate)：返回 false 触发回滚")
    void testCommitIfTrueMixedFalseRollback() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        template.transaction().commitIfTrue((JdbcOperations ops, NamedParamJdbcOperations nops) -> {
            ops.update("INSERT INTO users (username, email) VALUES(?, ?)",
                    buildParams("bipredFalse", "bf@test.com"));
            return nops.queryBoolean(
                    "SELECT active FROM users WHERE username = #{name}",
                    Collections.singletonMap("name", "nobody"));
        });

        Optional<String> user = template.queryValue(
                "SELECT username FROM users WHERE username = ?",
                buildParams("bipredFalse"), String.class);
        assertFalse(user.isPresent());
    }

    @Test
    @DisplayName("commitIfTrue(BiPredicate)：混用模式异常触发回滚")
    void testCommitIfTrueMixedExceptionRollback() {
        SimpleJdbcTemplate template = createTemplate();

        assertThrows(TransactionException.class, () ->
                template.transaction().commitIfTrue(
                        (JdbcOperations ops, NamedParamJdbcOperations nops) -> {
                    ops.update("INSERT INTO users (username) VALUES(?)",
                            buildParams("bipredEx"));
                    throw new IllegalStateException("混用异常");
                }));

        assertDoesNotThrow(() -> {
            Optional<String> user = template.queryValue(
                    "SELECT username FROM users WHERE username = ?",
                    buildParams("bipredEx"), String.class);
            assertFalse(user.isPresent());
        });
    }

    // ==================== 命名参数事务边界 ====================

    @Test
    @DisplayName("executeNamed：空操作正常提交")
    void testExecuteNamedEmpty() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        assertDoesNotThrow(() ->
                template.transaction().executeNamed(nops -> { /* no-op */ }));

        int count = template.query("SELECT COUNT(*) FROM users",
                rs -> { rs.next(); return rs.getInt(1); });
        assertEquals(5, count);
    }

    @Test
    @DisplayName("executeNamed：null 操作抛异常")
    @SuppressWarnings("null")
    void testExecuteNamedNullOps() {
        SimpleJdbcTemplate template = createTemplate();

        assertThrows(Exception.class, () ->
                template.transaction().executeNamed(
                        (ThrowingConsumer<NamedParamJdbcOperations, Exception>) null));
    }

    @Test
    @DisplayName("commitIfTrueNamed：null 操作抛异常")
    @SuppressWarnings("null")
    void testCommitIfTrueNamedNullOps() {
        SimpleJdbcTemplate template = createTemplate();

        assertThrows(Exception.class, () ->
                template.transaction().commitIfTrueNamed(null));
    }

    // ==================== TransactionException ====================

    @Test
    @DisplayName("TransactionException：单参构造器（仅 cause）")
    void testTransactionExceptionSingleArg() {
        RuntimeException cause = new RuntimeException("原始异常");
        TransactionException ex = new TransactionException(cause);
        assertEquals("Transaction failed during execution", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    @DisplayName("TransactionException：双参构造器")
    void testTransactionExceptionWithMessage() {
        RuntimeException cause = new RuntimeException("原始异常");
        TransactionException ex = new TransactionException("自定义消息", cause);
        assertEquals("自定义消息", ex.getMessage());
        assertSame(cause, ex.getCause());
    }

    @Test
    @DisplayName("TransactionException：null cause")
    void testTransactionExceptionNullCause() {
        TransactionException ex = new TransactionException(null);
        assertEquals("Transaction failed during execution", ex.getMessage());
        assertNull(ex.getCause());
    }
}
