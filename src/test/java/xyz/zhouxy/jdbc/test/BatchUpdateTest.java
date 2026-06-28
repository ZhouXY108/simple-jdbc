package xyz.zhouxy.jdbc.test;

import static org.junit.jupiter.api.Assertions.*;
import static xyz.zhouxy.jdbc.ParamBuilder.*;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.zhouxy.jdbc.BatchUpdateErrorInfo;
import xyz.zhouxy.jdbc.BatchUpdateResult;
import xyz.zhouxy.jdbc.BatchUpdateStatus;
import xyz.zhouxy.jdbc.SimpleJdbcTemplate;

/**
 * 批量更新 API 测试：batchUpdate。
 */
@DisplayName("SimpleJdbcTemplate 批量更新操作")
class BatchUpdateTest extends BaseH2Test {

    private static final Logger logger = LoggerFactory.getLogger(BatchUpdateTest.class);

    private static final String INSERT_SQL =
            "INSERT INTO users (username, email, age, balance, active) VALUES (?, ?, ?, ?, ?)";

    @BeforeEach
    void setUp() throws Exception {
        resetDatabase();
    }

    // ================================
    // #region - 正常批量操作
    // ================================

    @Test
    @DisplayName("batchUpdate：正常批量插入，单批次")
    void testBatchUpdateSingleBatch() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        List<User> users = new ArrayList<>();
        users.add(new User("user01", "u01@test.com", 20, 1000L, true));
        users.add(new User("user02", "u02@test.com", 21, 2000L, false));
        users.add(new User("user03", "u03@test.com", 22, 3000L, true));

        List<Object[]> params = buildBatchParams(users, a -> new Object[] { a.getUsername(), a.getEmail(), a.getAge(), a.getBalance(), a.getActive() });
        BatchUpdateResult result = template.batchUpdate(INSERT_SQL, params, 10);

        logger.info("batchUpdate 结果: {}", result);
        assertEquals(BatchUpdateStatus.SUCCESS, result.getStatus());
        assertEquals(3, result.getTotal());
        assertEquals(1, result.getBatchCount());
        assertEquals(1, result.getCompleteBatchCount());
        assertEquals(1, result.getSuccessBatchCount());
        assertEquals(0, result.getErrorBatchCount());
        assertEquals(10, result.getBatchSize());
        assertEquals(0, result.getRemainingBatchCount());

        // 验证数据已插入
        int count = template.queryValueOrDefault("SELECT COUNT(*) FROM users", Integer.class, 0);
        assertEquals(8, count); // 5 初始 + 3
    }

    @Test
    @DisplayName("batchUpdate：多批次，batchSize 恰好整除")
    void testBatchUpdateMultipleBatchesExact() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        List<User> paramsList = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            paramsList.add(new User("user" + i, "u" + i + "@test.com", 20 + i, 1000L, true));
        }

        List<Object[]> params = buildBatchParams(paramsList, a -> new Object[] { a.getUsername(), a.getEmail(), a.getAge(), a.getBalance(), a.getActive() });
        BatchUpdateResult result = template.batchUpdate(INSERT_SQL, params, 5);

        logger.info("多批次 batchUpdate 结果: {}", result);
        assertEquals(BatchUpdateStatus.SUCCESS, result.getStatus());
        assertEquals(10, result.getTotal());
        assertEquals(2, result.getBatchCount());
        assertEquals(2, result.getSuccessBatchCount());
        assertEquals(0, result.getErrorBatchCount());
    }

    @Test
    @DisplayName("batchUpdate：batchSize 不整除")
    void testBatchUpdateBatchSizeNotDivisible() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        List<Object[]> paramsList = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            paramsList.add(new Object[]{"user" + i, "u" + i + "@test.com", 20, 1000L, true});
        }

        BatchUpdateResult result = template.batchUpdate(INSERT_SQL, paramsList, 3);

        logger.info("不整除 batchUpdate 结果: {}", result);
        assertEquals(7, result.getTotal());
        assertEquals(3, result.getBatchCount());
        assertEquals(BatchUpdateStatus.SUCCESS, result.getStatus());
    }

    // ================================
    // #endregion - 正常批量操作
    // ================================

    // ================================
    // #region - 边界情况
    // ================================

    @Test
    @DisplayName("batchUpdate：空 params 集合")
    void testBatchUpdateEmptyParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        List<Object[]> params = buildBatchParams(Collections.<User>emptyList(),
                a -> new Object[] { a.getUsername(), a.getEmail(), a.getAge(), a.getBalance(), a.getActive() });
        assertEquals(Collections.emptyList(), params);
        BatchUpdateResult result = template.batchUpdate(
                INSERT_SQL, params, 10);

        assertEquals(0, result.getTotal());
        assertEquals(0, result.getBatchCount());
        assertEquals(10, result.getBatchSize());
    }

    @Test
    @DisplayName("batchUpdate：null params")
    void testBatchUpdateNullParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        BatchUpdateResult result = template.batchUpdate(
                INSERT_SQL, (Collection<Object[]>) null, 10);

        assertEquals(0, result.getTotal());
        assertEquals(0, result.getBatchCount());
    }

    @Test
    @DisplayName("batchUpdate：单条数据")
    void testBatchUpdateSingleItem() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        List<Object[]> paramsList = Collections.singletonList(
                new Object[]{"single", "single@test.com", 30, 5000L, true});

        BatchUpdateResult result = template.batchUpdate(INSERT_SQL, paramsList, 5);

        assertEquals(BatchUpdateStatus.SUCCESS, result.getStatus());
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getBatchCount());
    }

    // ================================
    // #endregion - 边界情况
    // ================================

    // ================================
    // #region - 包含错误数据
    // ================================

    final List<User> userListContainingInvalidData = Arrays.asList(
            // batch 0
            new User("test_0001", "test_0001@example.com", 1, 1L, true),
            new User("test_0002", "test_0002@example.com", 1, 1L, true),
            new User("test_0003", "test_0003@example.com", 1, 1L, true),
            // batch 1
            new User("test_0004", "test_0004@example.com", 1, 1L, true),
            new User("test_0005", "test_0005@example.com", 1, 1L, true),
            new User("test_0006", "test_0006@example.com", 1, 1L, true),
            // batch 2
            new User("test_0007", "test_0007@example.com", 1, 1L, true),
            new User("test_0007", "test_*0007@example.com", 1, 1L, true),
            // new User("test_0008", "test_0008@example.com", 1, 1L, true),
            new User("test_0009", "test_0009@example.com", 1, 1L, true),
            // batch 3
            new User("test_0009", "test_*0009@example.com", 1, 1L, true),
            // new User("test_0010", "test_0010@example.com", 1, 1L, true),
            new User("test_0011", "test_0011@example.com", 1, 1L, true),
            new User("test_0012", "test_0012@example.com", 1, 1L, true),
            // batch 4
            new User("test_0013", "test_0013@example.com", 1, 1L, true)
    );

    // ==================== quietly=false 中断模式 ====================

    @Test
    @DisplayName("batchUpdate：quietly=false，中间出错中断返回 INTERRUPTED")
    void testBatchUpdateQuietlyFalseInterrupted() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        int count0 = template.queryValue("SELECT COUNT(*) FROM users", Integer.class)
                .orElse(0);

        List<Object[]> params = buildBatchParams(userListContainingInvalidData, a -> new Object[] { a.getUsername(), a.getEmail(), a.getAge(), a.getBalance(), a.getActive() });
        BatchUpdateResult result = template.batchUpdate(INSERT_SQL, params, 3);

        assertEquals(BatchUpdateStatus.INTERRUPTED, result.getStatus());
        assertEquals(13, result.getTotal());
        assertEquals(5, result.getBatchCount());
        assertEquals(3, result.getCompleteBatchCount());
        assertEquals(2, result.getSuccessBatchCount());
        assertEquals(1, result.getErrorBatchCount());
        assertEquals(2, result.getRemainingBatchCount());

        assertArrayEquals(new int[] { 2 }, result.getErrorBatchIndexes());

        assertEquals(1, result.getAllErrorsInfo().size());
        assertTrue(result.getAllErrorsInfo().containsKey(2));

        BatchUpdateErrorInfo batchUpdateErrorInfo = result.getBatchUpdateErrorInfo(2);
        assertNotNull(batchUpdateErrorInfo);
        assertEquals(2, batchUpdateErrorInfo.getBatchIndex());
        assertNotNull(batchUpdateErrorInfo.getCause());
        assertInstanceOf(SQLException.class, batchUpdateErrorInfo.getCause());
        assertTrue(SQLException.class.isAssignableFrom(batchUpdateErrorInfo.getErrorType()));

        assertArrayEquals(new int[] { 1, 1, 1 }, result.getUpdateCounts(0));
        assertArrayEquals(new int[] { 1, 1, 1 }, result.getUpdateCounts(1));
        assertArrayEquals(new int[] { 1, Statement.EXECUTE_FAILED, 1 }, result.getUpdateCounts(2));
        assertNull(result.getUpdateCounts(3));
        assertNull(result.getUpdateCounts(4));

        Optional<Integer> count8 = template.queryValue("SELECT COUNT(*) FROM users", Integer.class);
        assertEquals(count0 + 8, count8.get().intValue());
    }

    // ==================== quietly=true 静默模式 ====================

    @Test
    @DisplayName("batchUpdate：quietly=true，出错继续返回 COMPLETED_WITH_ERRORS")
    void testBatchUpdateQuietlyTrue() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        int count0 = template.queryValue("SELECT COUNT(*) FROM users", Integer.class)
                .orElse(0);

        List<Object[]> params = buildBatchParams(userListContainingInvalidData, a -> new Object[] { a.getUsername(), a.getEmail(), a.getAge(), a.getBalance(), a.getActive() });
        BatchUpdateResult result = template.batchUpdate(INSERT_SQL, params, 3, true);

        assertEquals(BatchUpdateStatus.COMPLETED_WITH_ERRORS, result.getStatus());
        assertEquals(13, result.getTotal());
        assertEquals(5, result.getBatchCount());
        assertEquals(5, result.getCompleteBatchCount());
        assertEquals(3, result.getSuccessBatchCount());
        assertEquals(2, result.getErrorBatchCount());
        assertEquals(0, result.getRemainingBatchCount());
        assertArrayEquals(new int[] { 1, 1, 1 }, result.getUpdateCounts(0));
        assertArrayEquals(new int[] { 1, 1, 1 }, result.getUpdateCounts(1));
        assertArrayEquals(new int[] { 1, Statement.EXECUTE_FAILED, 1 }, result.getUpdateCounts(2));
        assertArrayEquals(new int[] { Statement.EXECUTE_FAILED, 1, 1 }, result.getUpdateCounts(3));
        assertArrayEquals(new int[] { 1 }, result.getUpdateCounts(4));

        Optional<Integer> count11 = template.queryValue("SELECT COUNT(*) FROM users", Integer.class);
        assertEquals(count0 + 11, count11.get().intValue());
    }

    // ================================
    // #endregion - 包含错误数据
    // ================================

    // ================================
    // #region - wrong batchSize
    // ================================

    @Test
    @DisplayName("batchUpdate：batchSize < 0，参数校验不通过")
    void testBatchUpdateWithWrongBatchSize() {
        SimpleJdbcTemplate template = createTemplate();

        List<Object[]> params = buildBatchParams(userListContainingInvalidData, a -> new Object[] { a.getUsername(), a.getEmail(), a.getAge(), a.getBalance(), a.getActive() });
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> template.batchUpdate(INSERT_SQL, params, -1, true));
        assertEquals("The batch size must be greater than 0.", e.getMessage());
    }

    @Test
    @DisplayName("batchUpdate：batchSize == 0，参数校验不通过")
    void testBatchUpdateWithBatchSizeZero() {
        SimpleJdbcTemplate template = createTemplate();

        List<Object[]> params = buildBatchParams(userListContainingInvalidData, a -> new Object[] { a.getUsername(), a.getEmail(), a.getAge(), a.getBalance(), a.getActive() });
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> template.batchUpdate(INSERT_SQL, params, 0, true));
        assertEquals("The batch size must be greater than 0.", e.getMessage());
    }

    // ================================
    // #endregion - wrong batchSize
    // ================================

}
