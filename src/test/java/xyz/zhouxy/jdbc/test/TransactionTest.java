package xyz.zhouxy.jdbc.test;

import static org.junit.jupiter.api.Assertions.*;
import static xyz.zhouxy.jdbc.ParamBuilder.buildParams;
import static xyz.zhouxy.jdbc.test.JdbcTestAssertions.assertLinkedHashMapOrder;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.zhouxy.jdbc.JdbcConfig;
import xyz.zhouxy.jdbc.JdbcOperations;
import xyz.zhouxy.jdbc.SimpleJdbcTemplate;
import xyz.zhouxy.jdbc.TransactionException;
import xyz.zhouxy.jdbc.TransactionIsolationLevel;
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

    @Test
    @DisplayName("execute：事务内 queryList/queryFirst(Map) 默认返回 LinkedHashMap 并保持列顺序")
    void testTransactionMapQueryReturnsLinkedHashMap() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        template.transaction().execute((JdbcOperations ops) -> {
            List<Map<String, Object>> users = ops.queryList(
                    "SELECT id, username FROM users ORDER BY id");

            assertFalse(users.isEmpty());
            assertLinkedHashMapOrder(users.get(0), "id", "username");

            Optional<Map<String, Object>> first = ops.queryFirst(
                    "SELECT email, age, id, username FROM users WHERE username = ?",
                    buildParams("bob"));

            assertTrue(first.isPresent());
            assertLinkedHashMapOrder(first.get(), "email", "age", "id", "username");

            logger.info("事务内 Map 查询顺序验证通过");
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
        int count = template.queryValueOrDefault("SELECT COUNT(*) FROM users", Integer.class, 0);
        assertEquals(5, count);
    }

    @Test
    @DisplayName("execute：null 操作抛异常")
    @SuppressWarnings({"null", "DataFlowIssue"})
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

        int count = template.queryValueOrDefault("SELECT COUNT(*) FROM users", Integer.class, 0);
        assertEquals(5, count);
    }

    @Test
    @DisplayName("executeNamed：null 操作抛异常")
    @SuppressWarnings({"null", "DataFlowIssue"})
    void testExecuteNamedNullOps() {
        SimpleJdbcTemplate template = createTemplate();

        assertThrows(Exception.class, () ->
                template.transaction().executeNamed(null));
    }

    @Test
    @DisplayName("commitIfTrueNamed：null 操作抛异常")
    @SuppressWarnings({"null", "DataFlowIssue"})
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
    @SuppressWarnings({"null", "DataFlowIssue"})
    void testTransactionExceptionNullCause() {
        TransactionException ex = new TransactionException(null);
        assertEquals("Transaction failed during execution", ex.getMessage());
        assertNull(ex.getCause());
    }

    // ==================== TransactionIsolationLevel 枚举 ====================

    @Test
    @DisplayName("TransactionIsolationLevel：枚举值与 JDBC 常量一致")
    void testTransactionIsolationLevelMapping() {
        assertEquals(Connection.TRANSACTION_NONE, TransactionIsolationLevel.NONE.getLevel());
        assertEquals(Connection.TRANSACTION_READ_UNCOMMITTED, TransactionIsolationLevel.READ_UNCOMMITTED.getLevel());
        assertEquals(Connection.TRANSACTION_READ_COMMITTED, TransactionIsolationLevel.READ_COMMITTED.getLevel());
        assertEquals(Connection.TRANSACTION_REPEATABLE_READ, TransactionIsolationLevel.REPEATABLE_READ.getLevel());
        assertEquals(Connection.TRANSACTION_SERIALIZABLE, TransactionIsolationLevel.SERIALIZABLE.getLevel());
    }

    // ==================== execute(isolationLevel) 指定隔离级别 ====================

    @Test
    @DisplayName("execute(isolationLevel)：指定 READ_COMMITTED 正常提交")
    void testExecuteIsolationLevelCommit() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        template.transaction().execute(TransactionIsolationLevel.READ_COMMITTED, (JdbcOperations ops) -> {
            ops.update("INSERT INTO users (username, email, age, balance, active) VALUES (?, ?, ?, ?, ?)",
                    buildParams("isoUser1", "iso1@test.com", 30, 5000L, true));
            ops.update("UPDATE users SET balance = ? WHERE username = ?",
                    buildParams(88888L, "bob"));
        });

        Optional<String> user = template.queryValue(
                "SELECT username FROM users WHERE username = ?",
                buildParams("isoUser1"), String.class);
        assertTrue(user.isPresent());

        Optional<Long> balance = template.queryValue(
                "SELECT balance FROM users WHERE username = ?",
                buildParams("bob"), Long.class);
        assertEquals(Long.valueOf(88888L), balance.orElse(null));
    }

    @Test
    @DisplayName("execute(isolationLevel)：指定 SERIALIZABLE，异常回滚")
    void testExecuteIsolationLevelRollback() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        Optional<Long> originalBalance = template.queryValue(
                "SELECT balance FROM users WHERE username = ?",
                buildParams("charlie"), Long.class);

        TransactionException ex = assertThrows(TransactionException.class, () ->
                template.transaction().execute(TransactionIsolationLevel.SERIALIZABLE, (JdbcOperations ops) -> {
                    ops.update("UPDATE users SET balance = ? WHERE username = ?",
                            buildParams(0L, "charlie"));
                    ops.update("INSERT INTO users (username, email) VALUES (?, ?)",
                            buildParams("isoRbUser", "isor@test.com"));
                    throw new RuntimeException("模拟回滚");
                }));

        assertEquals(RuntimeException.class, ex.getCause().getClass());

        Optional<Long> currentBalance = template.queryValue(
                "SELECT balance FROM users WHERE username = ?",
                buildParams("charlie"), Long.class);
        assertEquals(originalBalance.orElse(null), currentBalance.orElse(null));

        Optional<String> rolledBackUser = template.queryValue(
                "SELECT username FROM users WHERE username = ?",
                buildParams("isoRbUser"), String.class);
        assertFalse(rolledBackUser.isPresent());
    }

    @Test
    @DisplayName("execute(isolationLevel)：传入 null 等同于无参重载")
    void testExecuteIsolationLevelNull() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        assertDoesNotThrow(() ->
                template.transaction().execute(null, ops -> { /* no-op */ }));

        int count = template.queryValueOrDefault("SELECT COUNT(*) FROM users", Integer.class, 0);
        assertEquals(5, count);
    }

    // ==================== commitIfTrue(isolationLevel) 指定隔离级别 ====================

    @Test
    @DisplayName("commitIfTrue(isolationLevel)：返回 true 提交事务")
    void testCommitIfTrueIsolationLevelCommit() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        template.transaction().commitIfTrue(TransactionIsolationLevel.READ_COMMITTED, (JdbcOperations ops) -> {
            ops.update("INSERT INTO users (username, email) VALUES (?, ?)",
                    buildParams("isoCftUser", "isocft@test.com"));
            return true;
        });

        Optional<String> user = template.queryValue(
                "SELECT username FROM users WHERE username = ?",
                buildParams("isoCftUser"), String.class);
        assertTrue(user.isPresent());
    }

    @Test
    @DisplayName("commitIfTrue(isolationLevel)：返回 false 回滚事务")
    void testCommitIfTrueIsolationLevelFalseRollback() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        template.transaction().commitIfTrue(TransactionIsolationLevel.SERIALIZABLE, (JdbcOperations ops) -> {
            ops.update("INSERT INTO users (username, email) VALUES (?, ?)",
                    buildParams("isoCffUser", "isocff@test.com"));
            return false;
        });

        Optional<String> user = template.queryValue(
                "SELECT username FROM users WHERE username = ?",
                buildParams("isoCffUser"), String.class);
        assertFalse(user.isPresent());
    }

    @Test
    @DisplayName("commitIfTrue(isolationLevel)：传入 null 等同于无参重载")
    void testCommitIfTrueIsolationLevelNull() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        assertDoesNotThrow(() ->
                template.transaction().commitIfTrue(null, ops -> true));

        int count = template.queryValueOrDefault("SELECT COUNT(*) FROM users", Integer.class, 0);
        assertEquals(5, count);
    }

    // ==================== executeNamed(isolationLevel) 命名参数 + 隔离级别 ====================

    @Test
    @DisplayName("executeNamed(isolationLevel)：指定 REPEATABLE_READ 正常提交")
    void testExecuteNamedIsolationLevelCommit() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        template.transaction().executeNamed(TransactionIsolationLevel.REPEATABLE_READ, nops -> {
            Map<String, Object> params = new HashMap<>();
            params.put("name", "isoNpUser");
            params.put("email", "isonp@test.com");
            nops.update("INSERT INTO users (username, email) VALUES(#{name}, #{email})", params);
        });

        Optional<String> user = template.queryValue(
                "SELECT username FROM users WHERE username = ?",
                buildParams("isoNpUser"), String.class);
        assertTrue(user.isPresent());
    }

    // ==================== 指定隔离级别后验证后续操作正常（连接状态恢复） ====================

    @Test
    @DisplayName("事务指定隔离级别后，后续非事务操作正常执行")
    void testAfterIsolationLevelTransactionNormalQueryWorks() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        // 第一次事务指定 SERIALIZABLE
        template.transaction().execute(TransactionIsolationLevel.SERIALIZABLE, (JdbcOperations ops) -> {
            ops.update("INSERT INTO users (username, email) VALUES (?, ?)",
                    buildParams("afterIsoUser", "afteriso@test.com"));
        });

        // 验证事务已提交
        Optional<String> user = template.queryValue(
                "SELECT username FROM users WHERE username = ?",
                buildParams("afterIsoUser"), String.class);
        assertTrue(user.isPresent());

        // 后续非事务查询正常执行
        int count = template.queryValueOrDefault("SELECT COUNT(*) FROM users", Integer.class, 0);
        assertEquals(6, count);
    }

    // ==================== JdbcConfig 事务内集成测试 ====================

    @Test
    @DisplayName("默认配置事务内 ResultSetType 为 TYPE_FORWARD_ONLY")
    void testTransactionDefaultConfigResultSetType() throws Exception {
        SimpleJdbcTemplate template = createTemplate();

        template.transaction().execute(ops -> {
            Integer type = ops.query("SELECT 1", rs -> {
                rs.next();
                return rs.getType();
            });
            assertEquals(ResultSet.TYPE_FORWARD_ONLY, type);
        });
    }

    @Test
    @DisplayName("自定义 JdbcConfig 事务内 ResultSetType 生效")
    void testTransactionCustomConfigResultSetType() throws Exception {
        JdbcConfig config = JdbcConfig.builder()
                .resultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE)
                .build();
        SimpleJdbcTemplate template = new SimpleJdbcTemplate(dataSource, config);

        template.transaction().execute(ops -> {
            Integer type = ops.query("SELECT 1", rs -> {
                rs.next();
                return rs.getType();
            });
            assertEquals(ResultSet.TYPE_SCROLL_INSENSITIVE, type);
        });
    }
}
