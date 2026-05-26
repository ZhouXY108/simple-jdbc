/*
 * Copyright 2026-present the original author or authors.
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

import static xyz.zhouxy.plusone.commons.util.AssertTools.checkArgument;
import static xyz.zhouxy.plusone.commons.util.AssertTools.checkArgumentNotNull;

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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Lists;

/**
 * JdbcOperationSupport
 *
 * <p>
 * 提供静态方法，封装 JDBC 基础操作
 * </p>
 *
 * @author ZhouXY108 <luquanlion@outlook.com>
 * @since 1.0.0
 */
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
     */
    static <T> T query(Connection conn, String sql, Object[] params, ResultHandler<T> resultHandler)
            throws SQLException {
        assertConnectionNotNull(conn);
        assertSqlNotNull(sql);
        assertResultHandlerNotNull(resultHandler);
        return queryInternal(conn, sql, params, resultHandler);
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
     */
    static <T> List<T> queryList(Connection conn, String sql, Object[] params, RowMapper<T> rowMapper)
            throws SQLException {
        assertConnectionNotNull(conn);
        assertSqlNotNull(sql);
        assertRowMapperNotNull(rowMapper);
        return queryListInternal(conn, sql, params, rowMapper);
    }

    /**
     * 执行查询，返回结果映射为指定的类型。当结果为单列时使用
     *
     * @param conn   数据库连接
     * @param sql    SQL
     * @param params 参数
     * @param clazz  将结果映射为指定的类型
     */
    static <T> List<T> queryList(Connection conn, String sql, Object[] params, Class<T> clazz)
            throws SQLException {
        assertConnectionNotNull(conn);
        assertSqlNotNull(sql);
        assertClazzNotNull(clazz);
        return queryListInternal(conn, sql, params, (rs, rowNumber) -> rs.getObject(1, clazz));
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
     */
    static <T> T queryFirst(Connection conn, String sql, Object[] params, RowMapper<T> rowMapper)
            throws SQLException {
        assertConnectionNotNull(conn);
        assertSqlNotNull(sql);
        assertRowMapperNotNull(rowMapper);
        return queryFirstInternal(conn, sql, params, rowMapper);
    }

    /**
     * 查询第一行第一列，并转换为指定类型
     *
     * @param <T>    目标类型
     * @param sql    SQL
     * @param params 参数
     * @param clazz  目标类型
     */
    static <T> T queryFirst(Connection conn, String sql, Object[] params, Class<T> clazz)
            throws SQLException {
        assertConnectionNotNull(conn);
        assertSqlNotNull(sql);
        assertClazzNotNull(clazz);
        return queryFirstInternal(conn, sql, params, (rs, rowNumber) -> rs.getObject(1, clazz));
    }

    // #endregion

    // #region - update & batchUpdate

    /**
     * 执行更新操作
     *
     * @param conn   数据库连接
     * @param sql    要执行的 SQL
     * @param params 参数
     * @return 更新记录数
     */
    static int update(Connection conn, String sql, Object[] params)
            throws SQLException {
        assertConnectionNotNull(conn);
        assertSqlNotNull(sql);
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            fillStatement(stmt, params);
            return stmt.executeUpdate();
        }
    }

    /**
     * 执行 SQL 并返回生成的 keys
     *
     * @param conn      数据库连接
     * @param sql       要执行的 SQL
     * @param params    参数
     * @param rowMapper 行数据映射逻辑
     *
     * @return generated keys
     * @throws SQLException 执行 SQL 遇到异常情况将抛出
     */
    static <T> List<T> updateAndReturnKeys(Connection conn, String sql, Object[] params, RowMapper<T> rowMapper)
            throws SQLException {
        assertConnectionNotNull(conn);
        assertSqlNotNull(sql);
        assertRowMapperNotNull(rowMapper);
        final List<T> result = Lists.newArrayListWithCapacity(4);
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            fillStatement(stmt, params);
            stmt.executeUpdate();
            try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                int rowNumber = 0;
                while (generatedKeys.next()) {
                    T e = rowMapper.mapRow(generatedKeys, rowNumber++);
                    result.add(e);
                }
            }
            return result;
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
     */
    static BatchUpdateResult batchUpdate(Connection conn,
                                   String sql, @Nullable Collection<Object[]> params, int batchSize,
                                   boolean quietly)
            throws SQLException {
        assertConnectionNotNull(conn);
        assertSqlNotNull(sql);
        checkArgument(batchSize > 0, "The batch size must be greater than 0.");
        if (params == null || params.isEmpty()) {
            return new BatchUpdateResult(0, 0, batchSize);
        }

        final int paramsSize = params.size();
        final int batchCount = (paramsSize + batchSize - 1) / batchSize;

        final BatchUpdateResult result = new BatchUpdateResult(paramsSize, batchCount, batchSize);

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int itemIndex = 0;
            int batchIndex = 0;
            for (Object[] ps : params) {
                itemIndex++;
                fillStatement(stmt, ps);
                stmt.addBatch();
                final int indexInBatch = itemIndex % batchSize;
                if (indexInBatch == 0 || itemIndex >= paramsSize) {
                    try {
                        int[] updateCounts = stmt.executeBatch();
                        result.recordSuccessBatch(batchIndex, updateCounts);
                    }
                    catch (Exception e) {
                        final int[] updateCounts;
                        if (e instanceof BatchUpdateException) {
                            updateCounts = ((BatchUpdateException) e).getUpdateCounts();
                        }
                        else {
                            int n = (itemIndex >= paramsSize && indexInBatch != 0) ? indexInBatch : batchSize;
                            updateCounts = new int[n];
                            Arrays.fill(updateCounts, UNKNOWN_COUNT);
                        }
                        result.recordErrorBatch(batchIndex, updateCounts, e);
                        if (!quietly) {
                            result.interrupt();
                            break;
                        }
                    }
                    finally {
                        stmt.clearBatch();
                        batchIndex++;
                    }
                }
            }
            return result;
        }
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
     */
    private static <T> T queryInternal(@Nonnull Connection conn,
                                       @Nonnull String sql,
                                       @Nullable Object[] params,
                                       @Nonnull ResultHandler<T> resultHandler)
            throws SQLException {
        try (PreparedStatement stmt = createPreparedStatementInternal(conn, sql, params);
             ResultSet rs = stmt.executeQuery()) {
            return resultHandler.handle(rs);
        }
    }

    private static PreparedStatement createPreparedStatementInternal(
            @Nonnull Connection conn,
            @Nonnull String sql,
            @Nullable Object[] params)
            throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(sql);
        fillStatement(stmt, params);
        return stmt;
    }

    /**
     * 执行查询，将查询结果的每一行数据按照指定逻辑进行处理，返回结果列表
     *
     * @param conn      数据库连接
     * @param sql       SQL
     * @param params    参数
     * @param rowMapper {@link ResultSet} 中每一行的数据的处理逻辑
     */
    private static <T> List<T> queryListInternal(@Nonnull Connection conn,
                                                 @Nonnull String sql,
                                                 @Nullable Object[] params,
                                                 @Nonnull RowMapper<T> rowMapper)
            throws SQLException {
        return queryInternal(conn, sql, params, rs -> {
            List<T> result = Lists.newArrayList();
            int rowNumber = 0;
            while (rs.next()) {
                T e = rowMapper.mapRow(rs, rowNumber++);
                result.add(e);
            }
            return result;
        });
    }

    /**
     * 执行查询，将查询结果的第一行数据按照指定逻辑进行处理，返回映射结果
     *
     * @param conn      数据库连接
     * @param sql       SQL
     * @param params    参数
     * @param rowMapper 行数据映射逻辑
     * @return 映射结果。如果查询结果为空，则返回 null
     */
    private static <T> T queryFirstInternal(@Nonnull Connection conn,
                                            @Nonnull String sql,
                                            @Nullable Object[] params,
                                            @Nonnull RowMapper<T> rowMapper)
            throws SQLException {
        return queryInternal(conn, sql, params, rs ->
                rs.next() ? rowMapper.mapRow(rs, 0) : null);
    }

    // #endregion

    /**
     * 填充参数
     */
    private static void fillStatement(@Nonnull PreparedStatement stmt, @Nullable Object[] params)
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

    private static void assertConnectionNotNull(Connection conn) {
        checkArgumentNotNull(conn, "The argument \"conn\" could not be null.");
    }

    private static void assertSqlNotNull(String sql) {
        checkArgumentNotNull(sql, "The argument \"sql\" could not be null.");
    }

    private static void assertRowMapperNotNull(RowMapper<?> rowMapper) {
        checkArgumentNotNull(rowMapper, "The argument \"rowMapper\" could not be null.");
    }

    private static void assertResultHandlerNotNull(ResultHandler<?> resultHandler) {
        checkArgumentNotNull(resultHandler, "The argument \"resultHandler\" could not be null.");
    }

    private static void assertClazzNotNull(Class<?> clazz) {
        checkArgumentNotNull(clazz, "The argument \"clazz\" could not be null.");
    }

    // #endregion

    private JdbcOperationSupport() {
        throw new IllegalStateException("Utility class");
    }
}
