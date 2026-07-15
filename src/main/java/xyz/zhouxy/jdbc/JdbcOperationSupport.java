/*
 * Copyright 2026-present ZhouXY
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.zhouxy.jdbc;

import static xyz.zhouxy.jdbc.util.AssertTools.checkArgument;
import static xyz.zhouxy.jdbc.util.AssertTools.checkArgumentNotNull;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * JdbcOperationSupport
 *
 * <p>
 * 提供静态方法，封装 JDBC 基础操作
 * </p>
 *
 * @author ZhouXY
 * @since 1.0.0
 */
@NullMarked
class JdbcOperationSupport {

    // #region - query

    /**
     * 表示无法获取所更新的行数
     */
    public static final int UNKNOWN_COUNT = -999;

    /**
     * 执行查询，并按照自定义处理逻辑对结果进行处理，将结果转换为指定类型并返回
     *
     * @param conn          数据库连接
     * @param sql           SQL
     * @param params        参数
     * @param resultHandler 结果处理器，用于处理 {@link ResultSet}
     * @param config        JDBC 配置
     */
    static <T extends @Nullable Object> T query(
            Connection conn,
            String sql, @Nullable Object @Nullable [] params,
            ResultHandler<T> resultHandler,
            JdbcConfig config)
            throws SQLException {
        assertConnectionNotNull(conn);
        assertSqlNotNull(sql);
        assertResultHandlerNotNull(resultHandler);
        assertConfigNotNull(config);
        return queryInternal(conn, sql, params, resultHandler, config);
    }

    // #endregion

    // #region - queryList

    /**
     * 执行查询，将查询结果的每一行数据按照指定逻辑进行处理，返回结果列表
     *
     * @param conn      数据库连接
     * @param sql       SQL
     * @param params    参数
     * @param rowMapper {@link ResultSet} 中每一行的数据的处理逻辑
     * @param config    JDBC 配置
     */
    static <T extends @Nullable Object> List<T> queryList(
            Connection conn,
            String sql, @Nullable Object @Nullable [] params,
            RowMapper<T> rowMapper,
            JdbcConfig config)
            throws SQLException {
        assertConnectionNotNull(conn);
        assertSqlNotNull(sql);
        assertRowMapperNotNull(rowMapper);
        assertConfigNotNull(config);
        return queryListInternal(conn, sql, params, rowMapper, config);
    }

    /**
     * 执行查询，只取结果集每行第一列的值，映射为指定类型并返回列表
     *
     * @param conn   数据库连接
     * @param sql    SQL
     * @param params 参数
     * @param clazz  将结果映射为指定的类型
     * @param config JDBC 配置
     */
    static <T extends @Nullable Object> List<T> queryValues(
            Connection conn,
            String sql, @Nullable Object @Nullable [] params,
            Class<@NonNull T> clazz,
            JdbcConfig config)
            throws SQLException {
        assertConnectionNotNull(conn);
        assertSqlNotNull(sql);
        assertClazzNotNull(clazz);
        assertConfigNotNull(config);
        return queryListInternal(conn, sql, params, (rs, rowNumber) -> rs.getObject(1, clazz), config);
    }

    // #endregion

    // #region - queryFirst

    /**
     * 执行查询，将查询结果的第一行数据按照指定逻辑进行映射
     *
     * @param conn      数据库连接
     * @param sql       SQL
     * @param params    参数
     * @param rowMapper {@link ResultSet} 中每一行的数据的处理逻辑
     * @param config    JDBC 配置
     */
    static <T> @Nullable T queryFirst(
            Connection conn,
            String sql, @Nullable Object @Nullable [] params,
            RowMapper<T> rowMapper,
            JdbcConfig config)
            throws SQLException {
        assertConnectionNotNull(conn);
        assertSqlNotNull(sql);
        assertRowMapperNotNull(rowMapper);
        assertConfigNotNull(config);
        return queryFirstInternal(conn, sql, params, rowMapper, config);
    }

    /**
     * 执行查询，只取结果集第一行第一列的值，映射为指定类型并返回
     *
     * @param conn   数据库连接
     * @param <T>    目标类型
     * @param sql    SQL
     * @param params 参数
     * @param clazz  目标类型
     * @param config JDBC 配置
     */
    static <T> @Nullable T queryValue(
            Connection conn,
            String sql, @Nullable Object @Nullable [] params,
            Class<T> clazz,
            JdbcConfig config)
            throws SQLException {
        assertConnectionNotNull(conn);
        assertSqlNotNull(sql);
        assertClazzNotNull(clazz);
        assertConfigNotNull(config);
        return queryFirstInternal(conn, sql, params,
                (rs, rowNumber) -> rs.getObject(1, clazz), config);
    }

    // #endregion

    // #region - update & batchUpdate

    /**
     * 执行更新操作
     *
     * @param conn   数据库连接
     * @param sql    要执行的 SQL
     * @param params 参数
     * @param config JDBC 配置
     * @return 更新记录数
     */
    static int update(Connection conn, String sql, @Nullable Object @Nullable [] params,
            JdbcConfig config)
            throws SQLException {
        assertConnectionNotNull(conn);
        assertSqlNotNull(sql);
        assertConfigNotNull(config);
        if (params != null && params.length > 0) {
            try (PreparedStatement stmt = createPreparedStatement(conn, sql, params, config)) {
                return stmt.executeUpdate();
            }
        }
        else {
            try (Statement stmt = conn.createStatement()) {
                applyConfig(stmt, config);
                return stmt.executeUpdate(sql);
            }
        }
    }

    /**
     * 执行 SQL 并返回生成的 keys
     *
     * @param conn      数据库连接
     * @param sql       要执行的 SQL
     * @param params    参数
     * @param rowMapper 行数据映射逻辑
     * @param config    JDBC 配置
     * @return generated keys
     * @throws SQLException 数据库执行异常
     */
    static <T extends @Nullable Object> List<T> updateAndReturnKeys(
            Connection conn,
            String sql, @Nullable Object @Nullable [] params,
            RowMapper<T> rowMapper,
            JdbcConfig config)
            throws SQLException {
        assertConnectionNotNull(conn);
        assertSqlNotNull(sql);
        assertRowMapperNotNull(rowMapper);
        assertConfigNotNull(config);
        if (params != null && params.length > 0) {
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                applyConfig(stmt, config);
                fillStatement(stmt, params);
                stmt.executeUpdate();
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    final ResultHandler<List<T>> resultHandler = ResultHandler.mapToList(rowMapper);
                    return resultHandler.handle(generatedKeys);
                }
            }
        }
        else {
            try (Statement stmt = conn.createStatement()) {
                applyConfig(stmt, config);
                stmt.executeUpdate(sql, Statement.RETURN_GENERATED_KEYS);
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    final ResultHandler<List<T>> resultHandler = ResultHandler.mapToList(rowMapper);
                    return resultHandler.handle(generatedKeys);
                }
            }
        }
    }

    /**
     * 批量更新
     *
     * <p>
     * 当无法获取所更新的行数时，对应位置的更新行数将被设置为 {@link #UNKNOWN_COUNT}。
     *
     * @param conn       数据库连接
     * @param sql        sql语句
     * @param params     参数列表
     * @param batchSize  每次批量更新的数据量
     * @param quietly    静默分批更新。
     *                   如果 {@code quietly} 为 {@code true}，分批更新过程中发生异常不中断操作；
     *                   如果 {@code quietly} 为 {@code false}，分批更新过程中发生异常即中断操作，并返回结果。
     * @param config     JDBC 配置
     */
    static BatchUpdateResult batchUpdate(
            Connection conn,
            String sql, @Nullable Collection<@Nullable Object @Nullable []> params,
            int batchSize, boolean quietly,
            JdbcConfig config)
            throws SQLException {
        assertConnectionNotNull(conn);
        assertSqlNotNull(sql);
        assertConfigNotNull(config);
        checkArgument(batchSize > 0, "The batch size must be greater than 0.");
        if (params == null || params.isEmpty()) {
            return BatchUpdateResult.empty(batchSize, quietly);
        }

        final int paramsSize = params.size();
        final int batchCount = (paramsSize + batchSize - 1) / batchSize;

        final BatchUpdateResult.Builder builder =
                BatchUpdateResult.builder(paramsSize, batchCount, batchSize, quietly);

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            applyConfig(stmt, config);

            // 表示第几条数据，1, 2, 3, ..., paramsSize
            int itemIndex = 0;
            // 表示第几个批次，0, 1, ..., batchCount-1
            int batchIndex = 0;

            for (Object[] ps : params) {
                itemIndex++;
                fillStatement(stmt, ps);
                stmt.addBatch();

                // 表示当前数据在批次中的索引，1, 2, 3, ..., batchSize-1, batchSize
                final int indexInBatch = (itemIndex - 1) % batchSize + 1;

                if (indexInBatch == batchSize || itemIndex == paramsSize) {
                    try {
                        int[] updateCounts = stmt.executeBatch();
                        builder.recordSuccessBatch(batchIndex, updateCounts);
                    }
                    catch (Exception e) {
                        final int[] updateCounts = getUpdateCountsOnError(indexInBatch, e);
                        builder.recordErrorBatch(batchIndex, updateCounts, e);
                        if (!quietly) {
                            break;
                        }
                    }
                    finally {
                        stmt.clearBatch();
                        batchIndex++;
                    }
                }
            }
            return builder.build();
        }
    }

    private static int[] getUpdateCountsOnError(final int indexInBatch, final Exception e) {
        final int[] updateCounts;
        if (e instanceof BatchUpdateException) {
            updateCounts = ((BatchUpdateException) e).getUpdateCounts();
        }
        else {
            updateCounts = new int[indexInBatch];
            Arrays.fill(updateCounts, UNKNOWN_COUNT);
        }
        return updateCounts;
    }

    // #endregion

    // #region - internal

    /**
     * 执行查询，将查询结果按照指定逻辑进行处理并返回
     *
     * @param conn          数据库连接
     * @param sql           SQL
     * @param params        参数
     * @param resultHandler 结果处理器，用于处理 {@link ResultSet}
     * @param config        JDBC 配置
     * @return 查询结果
     */
    private static <T extends @Nullable Object> T queryInternal(
            Connection conn,
            String sql, @Nullable Object @Nullable [] params,
            ResultHandler<T> resultHandler,
            JdbcConfig config)
            throws SQLException {
        if (params != null && params.length > 0) {
            try (PreparedStatement stmt = createPreparedStatement(conn, sql, params, config);
                 ResultSet rs = stmt.executeQuery()) {
                return resultHandler.handle(rs);
            }
        }
        else {
            try (Statement stmt = createStatement(conn, config);
                 ResultSet rs = stmt.executeQuery(sql)) {
                return resultHandler.handle(rs);
            }
        }
    }

    /**
     * 执行查询，将查询结果的每一行数据按照指定逻辑进行处理，返回结果列表
     *
     * @param conn      数据库连接
     * @param sql       SQL
     * @param params    参数
     * @param rowMapper {@link ResultSet} 中每一行的数据的处理逻辑
     * @param config    JDBC 配置
     * @return 查询结果列表
     */
    private static <T extends @Nullable Object> List<T> queryListInternal(
            Connection conn,
            String sql, @Nullable Object @Nullable [] params,
            RowMapper<T> rowMapper,
            JdbcConfig config)
            throws SQLException {
        return queryInternal(conn, sql, params, ResultHandler.mapToList(rowMapper), config);
    }

    /**
     * 执行查询，将查询结果的第一行数据按照指定逻辑进行处理，返回映射结果
     *
     * @param conn      数据库连接
     * @param sql       SQL
     * @param params    参数
     * @param rowMapper 行数据映射逻辑
     * @param config    JDBC 配置
     * @return 映射结果。如果查询结果为空，则返回 null
     */
    private static <T> @Nullable T queryFirstInternal(
            Connection conn,
            String sql, @Nullable Object @Nullable [] params,
            RowMapper<T> rowMapper,
            JdbcConfig config)
            throws SQLException {
        return queryInternal(conn, sql, params, rs ->
                rs.next() ? rowMapper.mapRow(rs, 0) : null, config);
    }

    // #endregion

    // #region - statement helpers

    /**
     * 创建 PreparedStatement 并设置参数
     */
    private static PreparedStatement createPreparedStatement(
            Connection conn,
            String sql,
            @Nullable Object @Nullable [] params,
            JdbcConfig config)
            throws SQLException {
        @SuppressWarnings("MagicConstant")
        final PreparedStatement stmt = conn.prepareStatement(sql, config.getResultSetType(), config.getResultSetConcurrency());
        applyConfig(stmt, config);
        fillStatement(stmt, params);
        return stmt;
    }

    /**
     * 创建 Statement（用于无参数查询）
     */
    private static Statement createStatement(Connection conn, JdbcConfig config)
            throws SQLException {
        @SuppressWarnings("MagicConstant")
        Statement stmt = conn.createStatement(config.getResultSetType(), config.getResultSetConcurrency());
        applyConfig(stmt, config);
        return stmt;
    }

    /**
     * 对 Statement 应用配置参数
     */
    private static void applyConfig(Statement stmt, JdbcConfig config) throws SQLException {
        final Integer fetchSize = config.getFetchSize();
        if (fetchSize != null) {
            stmt.setFetchSize(fetchSize);
        }
        final Integer maxRows = config.getMaxRows();
        if (maxRows != null) {
            stmt.setMaxRows(maxRows);
        }
        final Integer queryTimeout = config.getQueryTimeout();
        if (queryTimeout != null) {
            stmt.setQueryTimeout(queryTimeout);
        }
    }

    // #endregion

    /**
     * 填充参数
     */
    private static void fillStatement(PreparedStatement stmt, @Nullable Object @Nullable [] params)
            throws SQLException {
        if (params != null && params.length > 0) {
            Object param;
            for (int i = 0; i < params.length; i++) {
                param = params[i];
                if (param == null) {
                    stmt.setObject(i + 1, null, Types.NULL);
                }
                else if (param instanceof LocalDate) {
                    stmt.setDate(i + 1, java.sql.Date.valueOf((LocalDate) param));
                }
                else if (param instanceof LocalTime) {
                    stmt.setTime(i + 1, java.sql.Time.valueOf((LocalTime) param));
                }
                else if (param instanceof LocalDateTime) {
                    stmt.setTimestamp(i + 1, java.sql.Timestamp.valueOf((LocalDateTime) param));
                }
                else if (param instanceof Instant) {
                    stmt.setTimestamp(i + 1, java.sql.Timestamp.from((Instant) param));
                }
                else {
                    stmt.setObject(i + 1, param);
                }
            }
        }
    }

    // #region - 参数校验

    private static void assertConnectionNotNull(@Nullable Connection conn) {
        checkArgumentNotNull(conn, "The argument \"conn\" could not be null.");
    }

    private static void assertSqlNotNull(@Nullable String sql) {
        checkArgumentNotNull(sql, "The argument \"sql\" could not be null.");
    }

    private static void assertRowMapperNotNull(@Nullable RowMapper<?> rowMapper) {
        checkArgumentNotNull(rowMapper, "The argument \"rowMapper\" could not be null.");
    }

    private static void assertResultHandlerNotNull(@Nullable ResultHandler<?> resultHandler) {
        checkArgumentNotNull(resultHandler, "The argument \"resultHandler\" could not be null.");
    }

    private static void assertClazzNotNull(@Nullable Class<?> clazz) {
        checkArgumentNotNull(clazz, "The argument \"clazz\" could not be null.");
    }

    private static void assertConfigNotNull(@Nullable JdbcConfig config) {
        checkArgumentNotNull(config, "The argument \"config\" could not be null.");
    }

    // #endregion

    private JdbcOperationSupport() {
        throw new IllegalStateException("Utility class");
    }
}
