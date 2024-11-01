/*
 * Copyright 2022-2024 the original author or authors.
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
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;

import com.google.common.collect.Lists;

import xyz.zhouxy.plusone.commons.collection.CollectionTools;
import xyz.zhouxy.plusone.commons.util.AssertTools;
import xyz.zhouxy.plusone.commons.util.OptionalTools;

public class SimpleJdbcTemplate {

    @Nonnull
    private final DataSource dataSource;

    public SimpleJdbcTemplate(@Nonnull DataSource dataSource) {
        AssertTools.checkNotNull(dataSource);
        this.dataSource = dataSource;
    }

    // #region - query

    public <T> T query(String sql, Object[] params, ResultHandler<T> resultHandler)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.query(conn, sql, params, resultHandler);
        }
    }

    public <T> T query(String sql, ResultHandler<T> resultHandler)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.query(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, resultHandler);
        }
    }

    // #endregion

    // #region - queryList

    public <T> List<T> queryList(String sql, Object[] params, RowMapper<T> rowMapper)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryList(conn, sql, params, rowMapper);
        }
    }

    public <T> List<T> queryList(String sql, Object[] params, Class<T> clazz)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryList(conn, sql, params, clazz);
        }
    }

    public List<Map<String, Object>> queryList(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryList(conn, sql, params, RowMapper.HASH_MAP_MAPPER);
        }
    }

    public List<DbRecord> queryRecordList(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryList(conn, sql, params, RowMapper.RECORD_MAPPER);
        }
    }

    public <T> List<T> queryList(String sql, RowMapper<T> rowMapper)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryList(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
        }
    }

    public <T> List<T> queryList(String sql, Class<T> clazz)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryList(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, clazz);
        }
    }

    public List<Map<String, Object>> queryList(String sql)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryList(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, RowMapper.HASH_MAP_MAPPER);
        }
    }

    public List<DbRecord> queryRecordList(String sql)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryList(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, RowMapper.RECORD_MAPPER);
        }
    }

    // #endregion

    // #region - queryFirst

    public <T> Optional<T> queryFirst(String sql, Object[] params, RowMapper<T> rowMapper)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirst(conn, sql, params, rowMapper);
        }
    }

    public <T> Optional<T> queryFirst(String sql, Object[] params, Class<T> clazz)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirst(conn, sql, params, clazz);
        }
    }

    public Optional<Map<String, Object>> queryFirst(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirst(conn, sql, params, RowMapper.HASH_MAP_MAPPER);
        }
    }

    public Optional<DbRecord> queryFirstRecord(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirst(conn, sql, params, RowMapper.RECORD_MAPPER);
        }
    }

    public Optional<String> queryFirstString(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirstString(conn, sql, params);
        }
    }

    public OptionalInt queryFirstInt(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirstInt(conn, sql, params);
        }
    }

    public OptionalLong queryFirstLong(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirstLong(conn, sql, params);
        }
    }

    public OptionalDouble queryFirstDouble(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirstDouble(conn, sql, params);
        }
    }

    public Optional<BigDecimal> queryFirstBigDecimal(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirstBigDecimal(conn, sql, params);
        }
    }

    public <T> Optional<T> queryFirst(String sql, RowMapper<T> rowMapper)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirst(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
        }
    }

    public <T> Optional<T> queryFirst(String sql, Class<T> clazz)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirst(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, clazz);
        }
    }

    public Optional<Map<String, Object>> queryFirst(String sql)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirst(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, RowMapper.HASH_MAP_MAPPER);
        }
    }

    public Optional<DbRecord> queryFirstRecord(String sql)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirst(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, RowMapper.RECORD_MAPPER);
        }
    }

    public Optional<String> queryFirstString(String sql)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirstString(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }
    }

    public OptionalInt queryFirstInt(String sql)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirstInt(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }
    }

    public OptionalLong queryFirstLong(String sql)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirstLong(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }
    }

    public OptionalDouble queryFirstDouble(String sql)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirstDouble(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }
    }

    public Optional<BigDecimal> queryFirstBigDecimal(String sql)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirstBigDecimal(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }
    }

    // #endregion

    // #region - update & batchUpdate

    public int update(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.update(conn, sql, params);
        }
    }

    public int update(String sql)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.update(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }
    }

    /**
     * 执行 SQL 并更新后的数据
     * 
     * @param sql       要执行的 SQL 语句
     * @param params    参数
     * @param rowMapper 结果映射规则
     * 
     * @return 更新的数据
     * @throws SQLException 执行 SQL 遇到异常情况将抛出
     */
    public <T> List<T> update(String sql, Object[] params, RowMapper<T> rowMapper)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.update(conn, sql, params, rowMapper);
        }
    }

    /**
     * 执行 SQL 并更新后的数据
     * 
     * @param sql       要执行的 SQL 语句
     * @param params    参数
     * @param rowMapper 结果映射规则
     * 
     * @return 更新的数据
     * @throws SQLException 执行 SQL 遇到异常情况将抛出
     */
    public <T> List<T> update(String sql, RowMapper<T> rowMapper)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.update(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
        }
    }

    public List<int[]> batchUpdate(String sql, @Nullable Collection<Object[]> params, int batchSize)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.batchUpdate(conn, sql, params, batchSize);
        }
    }

    public List<int[]> batchUpdateAndIgnoreException(String sql, @Nullable Collection<Object[]> params,
            int batchSize, List<Exception> exceptions)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.batchUpdateAndIgnoreException(conn, sql, params, batchSize, exceptions);
        }
    }

    // #endregion

    // #region - transaction

    public <E extends Exception> void executeTransaction(@Nonnull final DbOperations<E> operations)
            throws SQLException, E {
        AssertTools.checkNotNull(operations, "Operations can not be null.");
        try (Connection conn = this.dataSource.getConnection()) {
            final boolean autoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                operations.execute(new JdbcExecutor(conn));
                conn.commit();
            }
            catch (Exception e) {
                conn.rollback();
                throw e;
            }
            finally {
                conn.setAutoCommit(autoCommit);
            }
        }
    }

    public <E extends Exception> void commitIfTrue(@Nonnull final PredicateWithThrowable<E> operations)
            throws SQLException, E {
        AssertTools.checkNotNull(operations, "Operations can not be null.");
        try (Connection conn = this.dataSource.getConnection()) {
            final boolean autoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                if (operations.test(new JdbcExecutor(conn))) {
                    conn.commit();
                }
                else {
                    conn.rollback();
                }
            }
            catch (Exception e) {
                conn.rollback();
                throw e;
            }
            finally {
                conn.setAutoCommit(autoCommit);
            }
        }
    }

    @FunctionalInterface
    public interface DbOperations<E extends Exception> {
        void execute(JdbcExecutor jdbcExecutor) throws E;
    }

    @FunctionalInterface
    public interface PredicateWithThrowable<E extends Throwable> {
        boolean test(JdbcExecutor jdbcExecutor) throws E;
    }

    // #endregion

    public static final class JdbcExecutor {

        private final Connection conn;

        private JdbcExecutor(Connection conn) {
            this.conn = conn;
        }

        // #region - query

        public <T> T query(String sql, Object[] params, ResultHandler<T> resulthHandler)
                throws SQLException {
            return JdbcExecutor.query(this.conn, sql, params, resulthHandler);
        }

        public <T> T query(String sql, ResultHandler<T> resulthHandler)
                throws SQLException {
            return JdbcExecutor.query(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, resulthHandler);
        }

        // #endregion

        // #region - queryList

        public <T> List<T> queryList(String sql, Object[] params, RowMapper<T> rowMapper)
                throws SQLException {
            return JdbcExecutor.queryList(this.conn, sql, params, rowMapper);
        }

        public <T> List<T> queryList(String sql, Object[] params, Class<T> clazz)
                throws SQLException {
            return JdbcExecutor.queryList(this.conn, sql, params, clazz);
        }

        public List<Map<String, Object>> queryList(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryList(this.conn, sql, params, RowMapper.HASH_MAP_MAPPER);
        }

        public List<DbRecord> queryRecordList(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryList(this.conn, sql, params, RowMapper.RECORD_MAPPER);
        }

        public <T> List<T> queryList(String sql, RowMapper<T> rowMapper)
                throws SQLException {
            return JdbcExecutor.queryList(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
        }

        public <T> List<T> queryList(String sql, Class<T> clazz)
                throws SQLException {
            return JdbcExecutor.queryList(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, clazz);
        }

        public List<Map<String, Object>> queryList(String sql)
                throws SQLException {
            return JdbcExecutor.queryList(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, RowMapper.HASH_MAP_MAPPER);
        }

        public List<DbRecord> queryRecordList(String sql)
                throws SQLException {
            return JdbcExecutor.queryList(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, RowMapper.RECORD_MAPPER);
        }

        // #endregion

        // #region - queryFirst

        public <T> Optional<T> queryFirst(String sql, Object[] params, RowMapper<T> rowMapper)
                throws SQLException {
            return JdbcExecutor.queryFirst(this.conn, sql, params, rowMapper);
        }

        public <T> Optional<T> queryFirst(String sql, Object[] params, Class<T> clazz)
                throws SQLException {
            return JdbcExecutor.queryFirst(this.conn, sql, params, clazz);
        }

        public Optional<Map<String, Object>> queryFirst(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryFirst(this.conn, sql, params, RowMapper.HASH_MAP_MAPPER);
        }

        public Optional<DbRecord> queryFirstRecord(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryFirst(this.conn, sql, params, RowMapper.RECORD_MAPPER);
        }

        public Optional<String> queryFirstString(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryFirstString(this.conn, sql, params);
        }

        public OptionalInt queryFirstInt(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryFirstInt(this.conn, sql, params);
        }

        public OptionalLong queryFirstLong(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryFirstLong(this.conn, sql, params);
        }

        public OptionalDouble queryFirstDouble(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryFirstDouble(this.conn, sql, params);
        }

        public Optional<BigDecimal> queryFirstBigDecimal(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryFirstBigDecimal(this.conn, sql, params);
        }

        public <T> Optional<T> queryFirst(String sql, RowMapper<T> rowMapper)
                throws SQLException {
            return JdbcExecutor.queryFirst(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
        }

        public <T> Optional<T> queryFirst(String sql, Class<T> clazz)
                throws SQLException {
            return JdbcExecutor.queryFirst(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, clazz);
        }

        public Optional<Map<String, Object>> queryFirst(String sql)
                throws SQLException {
            return JdbcExecutor.queryFirst(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, RowMapper.HASH_MAP_MAPPER);
        }

        public Optional<DbRecord> queryFirstRecord(String sql)
                throws SQLException {
            return JdbcExecutor.queryFirst(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, RowMapper.RECORD_MAPPER);
        }

        public Optional<String> queryFirstString(String sql)
                throws SQLException {
            return JdbcExecutor.queryFirstString(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }

        public OptionalInt queryFirstInt(String sql)
                throws SQLException {
            return JdbcExecutor.queryFirstInt(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }

        public OptionalLong queryFirstLong(String sql)
                throws SQLException {
            return JdbcExecutor.queryFirstLong(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }

        public OptionalDouble queryFirstDouble(String sql)
                throws SQLException {
            return JdbcExecutor.queryFirstDouble(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }

        public Optional<BigDecimal> queryFirstBigDecimal(String sql)
                throws SQLException {
            return JdbcExecutor.queryFirstBigDecimal(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }

        // #endregion

        // #region - update & batchUpdate

        public int update(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.update(this.conn, sql, params);
        }

        public int update(String sql)
                throws SQLException {
            return JdbcExecutor.update(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }

        /**
         * 执行 SQL 并更新后的数据
         * 
         * @param sql       要执行的 SQL 语句
         * @param params    参数
         * @param rowMapper 结果映射规则
         * 
         * @return 更新的数据
         * @throws SQLException 执行 SQL 遇到异常情况将抛出
         */
        public <T> List<T> update(String sql, Object[] params, RowMapper<T> rowMapper)
                throws SQLException {
            return JdbcExecutor.update(this.conn, sql, params, rowMapper);
        }

        /**
         * 执行 SQL 并更新后的数据
         * 
         * @param sql       要执行的 SQL 语句
         * @param params    参数
         * @param rowMapper 结果映射规则
         * 
         * @return 更新的数据
         * @throws SQLException 执行 SQL 遇到异常情况将抛出
         */
        public <T> List<T> update(String sql, RowMapper<T> rowMapper)
                throws SQLException {
            return JdbcExecutor.update(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
        }

        public List<int[]> batchUpdate(String sql, @Nullable Collection<Object[]> params, int batchSize)
                throws SQLException {
            return JdbcExecutor.batchUpdate(this.conn, sql, params, batchSize);
        }

        public List<int[]> batchUpdateAndIgnoreException(String sql, @Nullable Collection<Object[]> params,
                int batchSize, List<Exception> exceptions)
                throws SQLException {
            return JdbcExecutor.batchUpdateAndIgnoreException(this.conn, sql, params, batchSize, exceptions);
        }

        // #endregion

        // #region - internal

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

        private static <T> Optional<T> queryFirstInternal(@Nonnull Connection conn,
                                               @Nonnull String sql,
                                               @Nullable Object[] params,
                                               @Nonnull RowMapper<T> rowMapper)
                throws SQLException {
            return queryInternal(conn, sql, params, rs -> {
                if (rs.next()) {
                    return Optional.ofNullable(rowMapper.mapRow(rs, 0));
                }
                return Optional.empty();
            });
        }

        // #endregion

        // #region - query

        private static <T> T query(Connection conn, String sql, Object[] params, ResultHandler<T> resultHandler)
                throws SQLException {
            assertConnectionNotNull(conn);
            assertSqlNotNull(sql);
            assertResultHandlerNotNull(resultHandler);
            return queryInternal(conn, sql, params, resultHandler);
        }

        // #endregion

        // #region - queryList

        private static <T> List<T> queryList(Connection conn, String sql, Object[] params, RowMapper<T> rowMapper)
                throws SQLException {
            assertConnectionNotNull(conn);
            assertSqlNotNull(sql);
            assertRowMapperNotNull(rowMapper);
            return queryListInternal(conn, sql, params, rowMapper);
        }

        private static <T> List<T> queryList(Connection conn, String sql, Object[] params, Class<T> clazz)
                throws SQLException {
            assertConnectionNotNull(conn);
            assertSqlNotNull(sql);
            assertClazzNotNull(clazz);
            return queryListInternal(conn, sql, params, (rs, rowNumber) -> rs.getObject(1, clazz));
        }

        // #endregion

        // #region - queryFirst

        private static <T> Optional<T> queryFirst(Connection conn, String sql, Object[] params, RowMapper<T> rowMapper)
                throws SQLException {
            assertConnectionNotNull(conn);
            assertSqlNotNull(sql);
            assertRowMapperNotNull(rowMapper);
            return queryFirstInternal(conn, sql, params, rowMapper);
        }

        private static <T> Optional<T> queryFirst(Connection conn, String sql, Object[] params, Class<T> clazz)
                throws SQLException {
            assertConnectionNotNull(conn);
            assertSqlNotNull(sql);
            assertClazzNotNull(clazz);
            return queryFirstInternal(conn, sql, params, (rs, rowNumber) -> rs.getObject(1, clazz));
        }

        private static Optional<String> queryFirstString(Connection conn, String sql, Object[] params)
                throws SQLException {
            return queryFirst(conn, sql, params, (rs, rowNumber) -> rs.getString(1));
        }

        private static OptionalInt queryFirstInt(Connection conn, String sql, Object[] params)
                throws SQLException {
            Optional<Integer> result = queryFirst(conn, sql, params, (rs, rowNumber) -> rs.getInt(1));
            return OptionalTools.toOptionalInt(result);
        }

        private static OptionalLong queryFirstLong(Connection conn, String sql, Object[] params)
                throws SQLException {
            Optional<Long> result = queryFirst(conn, sql, params, (rs, rowNumber) -> rs.getLong(1));
            return OptionalTools.toOptionalLong(result);
        }

        private static OptionalDouble queryFirstDouble(Connection conn, String sql, Object[] params)
                throws SQLException {
            Optional<Double> result = queryFirst(conn, sql, params, (rs, rowNumber) -> rs.getDouble(1));
            return OptionalTools.toOptionalDouble(result);
        }

        private static Optional<BigDecimal> queryFirstBigDecimal(Connection conn, String sql, Object[] params)
                throws SQLException {
            return queryFirst(conn, sql, params, (rs, rowNumber) -> rs.getBigDecimal(1));
        }

        // #endregion

        // #region - update & batchUpdate

        private static int update(Connection conn, String sql, Object[] params)
                throws SQLException {
            assertConnectionNotNull(conn);
            assertSqlNotNull(sql);
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                fillStatement(stmt, params);
                return stmt.executeUpdate();
            }
        }

        /**
         * 执行 SQL 并更新后的数据
         * 
         * @param sql       要执行的 SQL 语句
         * @param params    参数
         * @param rowMapper 结果映射规则
         * 
         * @return 更新的数据
         * @throws SQLException 执行 SQL 遇到异常情况将抛出
         */
        private static <T> List<T> update(Connection conn, String sql, Object[] params, RowMapper<T> rowMapper)
                throws SQLException {
            assertConnectionNotNull(conn);
            assertSqlNotNull(sql);
            assertRowMapperNotNull(rowMapper);
            final List<T> result = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                fillStatement(stmt, params);
                stmt.executeUpdate();
                try (ResultSet generatedKeys = stmt.getGeneratedKeys();) {
                    int rowNumber = 0;
                    while (generatedKeys.next()) {
                        T e = rowMapper.mapRow(generatedKeys, rowNumber++);
                        result.add(e);
                    }
                }
                return result;
            }
        }

        private static List<int[]> batchUpdate(Connection conn, String sql, Collection<Object[]> params, int batchSize)
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

        private static List<int[]> batchUpdateAndIgnoreException(Connection conn,
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

        // #region - Asserts

        private static void assertConnectionNotNull(Connection conn) {
            AssertTools.checkArgumentNotNull(conn, "The argument \"conn\" could not be null.");
        }

        private static void assertSqlNotNull(String sql) {
            AssertTools.checkArgumentNotNull(sql, "The argument \"sql\" could not be null.");
        }

        private static void assertRowMapperNotNull(RowMapper<?> rowMapper) {
            AssertTools.checkArgumentNotNull(rowMapper, "The argument \"rowMapper\" could not be null.");
        }

        private static void assertResultHandlerNotNull(ResultHandler<?> resultHandler) {
            AssertTools.checkArgumentNotNull(resultHandler, "The argument \"resultHandler\" could not be null.");
        }

        private static void assertClazzNotNull(Class<?> clazz) {
            AssertTools.checkArgumentNotNull(clazz, "The argument \"clazz\" could not be null.");
        }

        // #endregion
    }
}
