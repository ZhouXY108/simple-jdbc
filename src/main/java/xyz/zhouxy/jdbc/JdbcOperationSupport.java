/*
 * Copyright 2024 the original author or authors.
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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.collect.Lists;

import xyz.zhouxy.plusone.commons.collection.CollectionTools;
import xyz.zhouxy.plusone.commons.util.AssertTools;

/**
 * JdbcOperationSupport
 *
 * <p>
 * 提供静态方法，封装 JDBC 基础操作
 * </p>
 *
 * @author <a href="http://zhouxy.xyz:3000/ZhouXY108">ZhouXY</a>
 * @since 1.0.0
 */
class JdbcOperationSupport {

    // #region - query

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

    /**
     * 查询第一行第一列，并转换为字符串
     *
     * @param conn   数据库连接
     * @param sql    SQL
     * @param params 参数
     */
    static String queryFirstString(Connection conn, String sql, Object[] params)
            throws SQLException {
        return queryFirst(conn, sql, params, (rs, rowNumber) -> rs.getString(1));
    }

    /**
     * 查询第一行第一列，并转换为整数值
     *
     * @param conn   数据库连接
     * @param sql    SQL
     * @param params 参数
     */
    static Integer queryFirstInt(Connection conn, String sql, Object[] params)
            throws SQLException {
        return queryFirst(conn, sql, params, (rs, rowNumber) -> rs.getInt(1));
    }

    /**
     * 查询第一行第一列，并转换为长整型
     *
     * @param conn   数据库连接
     * @param sql    SQL
     * @param params 参数
     */
    static Long queryFirstLong(Connection conn, String sql, Object[] params)
            throws SQLException {
        return queryFirst(conn, sql, params, (rs, rowNumber) -> rs.getLong(1));
    }

    /**
     * 查询第一行第一列，并转换为双精度浮点型
     *
     * @param conn   数据库连接
     * @param sql    SQL
     * @param params 参数
     */
    static Double queryFirstDouble(Connection conn, String sql, Object[] params)
            throws SQLException {
        return queryFirst(conn, sql, params, (rs, rowNumber) -> rs.getDouble(1));
    }

    /**
     * 查询第一行第一列，并转换为 {@link BigDecimal}
     *
     * @param conn   数据库连接
     * @param sql    SQL
     * @param params 参数
     */
    static BigDecimal queryFirstBigDecimal(Connection conn, String sql, Object[] params)
            throws SQLException {
        return queryFirst(conn, sql, params, (rs, rowNumber) -> rs.getBigDecimal(1));
    }

    /**
     * 查询结果，并转换为 bool 值
     *
     * @param conn   数据库连接
     * @param sql    SQL
     * @param params 参数
     */
    static Boolean queryFirstBoolean(Connection conn, String sql, Object[] params)
            throws SQLException {
        return queryFirst(conn, sql, params, (rs, rowNumber) -> rs.getBoolean(1));
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
    static <T> List<T> update(Connection conn, String sql, Object[] params, RowMapper<T> rowMapper)
            throws SQLException {
        assertConnectionNotNull(conn);
        assertSqlNotNull(sql);
        assertRowMapperNotNull(rowMapper);
        final List<T> result = new ArrayList<>();
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
     * 执行批量更新，批量更新数据，返回每条记录更新的行数
     *
     * @param conn      数据库连接
     * @param sql       SQL 语句
     * @param params    参数列表
     * @param batchSize 每次批量更新的数据量
     */
    static List<int[]> batchUpdate(Connection conn, String sql, Collection<Object[]> params, int batchSize)
            throws SQLException {
        assertConnectionNotNull(conn);
        assertSqlNotNull(sql);

        if (params == null || params.isEmpty()) {
            return Collections.emptyList();
        }
        int executeCount = params.size() / batchSize;
        executeCount = (params.size() % batchSize == 0) ? executeCount : (executeCount + 1);
        List<int[]> result = Lists.newArrayListWithCapacity(executeCount);

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int i = 0;
            for (Object[] ps : params) {
                i++;
                fillStatement(stmt, ps);
                stmt.addBatch();
                if (i % batchSize == 0 || i >= params.size()) {
                    int[] n = stmt.executeBatch();
                    result.add(n);
                    stmt.clearBatch();
                }
            }
            return result;
        }
    }

    /**
     * 批量更新，返回更新成功的记录行数。发生异常时不中断操作，将异常存入 {@code exceptions} 中
     *
     * @param conn       数据库连接
     * @param sql        sql语句
     * @param params     参数列表
     * @param batchSize  每次批量更新的数据量
     * @param exceptions 异常列表，用于记录异常信息
     */
    static List<int[]> batchUpdateAndIgnoreException(Connection conn,
            String sql, @Nullable Collection<Object[]> params, int batchSize,
            List<Exception> exceptions)
            throws SQLException {
        assertConnectionNotNull(conn);
        assertSqlNotNull(sql);
        AssertTools.checkArgument(CollectionTools.isNotEmpty(exceptions),
                "The list used to store exceptions should be non-null and empty.");
        if (params == null || params.isEmpty()) {
            return Collections.emptyList();
        }
        int executeCount = params.size() / batchSize;
        executeCount = (params.size() % batchSize == 0) ? executeCount : (executeCount + 1);
        List<int[]> result = Lists.newArrayListWithCapacity(executeCount);

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int i = 0;
            for (Object[] ps : params) {
                i++;
                fillStatement(stmt, ps);
                stmt.addBatch();
                final int batchIndex = i % batchSize;
                if (batchIndex == 0 || i >= params.size()) {
                    try {
                        int[] n = stmt.executeBatch();
                        result.add(n);
                        stmt.clearBatch();
                    }
                    catch (Exception e) {
                        int n = (i >= params.size() && batchIndex != 0) ? batchIndex : batchSize;
                        result.add(new int[n]);
                        stmt.clearBatch();
                        // 收集异常信息
                        exceptions.add(e);
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
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            fillStatement(stmt, params);
            try (ResultSet rs = stmt.executeQuery()) {
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
     */
    private static <T> List<T> queryListInternal(@Nonnull Connection conn,
                                                 @Nonnull String sql,
                                                 @Nullable Object[] params,
                                                 @Nonnull RowMapper<T> rowMapper)
            throws SQLException {
        return queryInternal(conn, sql, params, rs -> {
            List<T> result = new ArrayList<>();
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
                if (param instanceof java.sql.Date) {
                    stmt.setDate(i + 1, (java.sql.Date) param);
                }
                else if (param instanceof java.sql.Time) {
                    stmt.setTime(i + 1, (java.sql.Time) param);
                }
                else if (param instanceof java.sql.Timestamp) {
                    stmt.setTimestamp(i + 1, (java.sql.Timestamp) param);
                }
                else {
                    stmt.setObject(i + 1, param);
                }
            }
        }
    }

    // #region - 参数校验

    private static void assertConnectionNotNull(Connection conn) {
        AssertTools.checkArgument(Objects.nonNull(conn),
                "The argument \"conn\" could not be null.");
    }

    private static void assertSqlNotNull(String sql) {
        AssertTools.checkArgument(Objects.nonNull(sql),
                "The argument \"sql\" could not be null.");
    }

    private static void assertRowMapperNotNull(RowMapper<?> rowMapper) {
        AssertTools.checkArgument(Objects.nonNull(rowMapper),
                "The argument \"rowMapper\" could not be null.");
    }

    private static void assertResultHandlerNotNull(ResultHandler<?> resultHandler) {
        AssertTools.checkArgument(Objects.nonNull(resultHandler),
                "The argument \"resultHandler\" could not be null.");
    }

    private static void assertClazzNotNull(Class<?> clazz) {
        AssertTools.checkArgument(Objects.nonNull(clazz),
                "The argument \"clazz\" could not be null.");
    }

    // #endregion

    private JdbcOperationSupport() {
        throw new IllegalStateException("Utility class");
    }
}
