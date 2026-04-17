/*
 * Copyright 2022-2025 the original author or authors.
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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;

import xyz.zhouxy.plusone.commons.function.ThrowingConsumer;
import xyz.zhouxy.plusone.commons.function.ThrowingPredicate;
import xyz.zhouxy.plusone.commons.util.AssertTools;

/**
 * SimpleJdbcTemplate
 *
 * <p>
 * 对 JDBC 的简单封装，方便数据库操作，支持事务，支持批量操作，支持自定义结果集映射
 * </p>
 *
 * @author ZhouXY108 <luquanlion@outlook.com>
 * @since 1.0.0
 */
public class SimpleJdbcTemplate implements JdbcOperations {

    @Nonnull
    private final DataSource dataSource;

    public SimpleJdbcTemplate(@Nonnull DataSource dataSource) {
        AssertTools.checkNotNull(dataSource);
        this.dataSource = dataSource;
    }

    // #region - query

    /** {@inheritDoc} */
    @Override
    public <T> T query(String sql, Object[] params, ResultHandler<T> resultHandler)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcOperationSupport.query(conn, sql, params, resultHandler);
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T> T query(String sql, ResultHandler<T> resultHandler)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcOperationSupport.query(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, resultHandler);
        }
    }

    // #endregion

    // #region - queryList

    /** {@inheritDoc} */
    @Override
    public <T> List<T> queryList(String sql, Object[] params, RowMapper<T> rowMapper)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcOperationSupport.queryList(conn, sql, params, rowMapper);
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T> List<T> queryList(String sql, Object[] params, Class<T> clazz)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcOperationSupport.queryList(conn, sql, params, clazz);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<Map<String, Object>> queryList(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcOperationSupport.queryList(conn, sql, params, RowMapper.HASH_MAP_MAPPER);
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T> List<T> queryList(String sql, RowMapper<T> rowMapper)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcOperationSupport.queryList(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T> List<T> queryList(String sql, Class<T> clazz)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcOperationSupport.queryList(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, clazz);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<Map<String, Object>> queryList(String sql)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcOperationSupport
                    .queryList(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, RowMapper.HASH_MAP_MAPPER);
        }
    }

    // #endregion

    // #region - queryFirst

    /** {@inheritDoc} */
    @Override
    public <T> Optional<T> queryFirst(String sql, Object[] params, RowMapper<T> rowMapper)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            final T result = JdbcOperationSupport.queryFirst(conn, sql, params, rowMapper);
            return Optional.ofNullable(result);
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T> Optional<T> queryFirst(String sql, Object[] params, Class<T> clazz)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            final T result = JdbcOperationSupport.queryFirst(conn, sql, params, clazz);
            return Optional.ofNullable(result);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Map<String, Object>> queryFirst(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            final Map<String, Object> result = JdbcOperationSupport
                    .queryFirst(conn, sql, params, RowMapper.HASH_MAP_MAPPER);
            return Optional.ofNullable(result);
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T> Optional<T> queryFirst(String sql, RowMapper<T> rowMapper)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            final T result = JdbcOperationSupport
                    .queryFirst(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
            return Optional.ofNullable(result);
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T> Optional<T> queryFirst(String sql, Class<T> clazz)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            final T result = JdbcOperationSupport
                    .queryFirst(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, clazz);
            return Optional.ofNullable(result);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Map<String, Object>> queryFirst(String sql)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            final Map<String, Object> result = JdbcOperationSupport
                    .queryFirst(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, RowMapper.HASH_MAP_MAPPER);
            return Optional.ofNullable(result);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean queryAsBoolean(String sql) // TODO 单元测试
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            final Boolean result = JdbcOperationSupport
                    .queryFirstBoolean(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
            return Boolean.TRUE.equals(result);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean queryAsBoolean(String sql, Object[] params) // TODO 单元测试
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            final Boolean result = JdbcOperationSupport
                    .queryFirstBoolean(conn, sql, params);
            return Boolean.TRUE.equals(result);
        }
    }

    // #endregion

    // #region - update & batchUpdate

    /** {@inheritDoc} */
    @Override
    public int update(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcOperationSupport.update(conn, sql, params);
        }
    }

    /** {@inheritDoc} */
    @Override
    public int update(String sql)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcOperationSupport.update(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T> List<T> update(String sql, Object[] params, RowMapper<T> rowMapper)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcOperationSupport.update(conn, sql, params, rowMapper);
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T> List<T> update(String sql, RowMapper<T> rowMapper)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcOperationSupport.update(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<int[]> batchUpdate(String sql, @Nullable Collection<Object[]> params, int batchSize)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcOperationSupport.batchUpdate(conn, sql, params, batchSize, null, false);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<int[]> batchUpdate(String sql, @Nullable Collection<Object[]> params,
                                   int batchSize, List<Exception> exceptions, boolean quietly)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcOperationSupport
                    .batchUpdate(conn, sql, params, batchSize, exceptions, quietly);
        }
    }

    // #endregion

    // #region - transaction

    /**
     * 执行事务。如果未发生异常，则提交事务；当有异常发生时，回滚事务
     *
     * <p>
     * operations 中使用 JdbcExecutor 实参进行 JDBC 操作，这些操作在一个连接中
     * </p>
     *
     * @param <E>           异常类型
     * @param operations    事务操作
     * @throws SQLException SQL 异常
     * @throws E            事务中的异常
     */
    public <E extends Exception> void executeTransaction(
            @Nonnull final ThrowingConsumer<JdbcOperations, E> operations)
            throws SQLException, E {
        AssertTools.checkNotNull(operations, "Operations can not be null.");
        try (Connection conn = this.dataSource.getConnection()) {
            final boolean autoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                operations.accept(new JdbcExecutor(conn));
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

    /**
     * 执行事务。
     * 如果 {@code operations} 返回 {@code true}，则提交事务；
     * 如果抛出异常，或返回 {@code false}，则回滚事务
     *
     * @param <E>        事务中的异常
     * @param operations 事务操作
     * @throws SQLException 数据库异常
     * @throws E            事务中的异常类型
     */
    public <E extends Exception> void commitIfTrue(
            @Nonnull final ThrowingPredicate<JdbcOperations, E> operations)
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

    // #endregion

    private static final class JdbcExecutor implements JdbcOperations {

        private final Connection conn;

        private JdbcExecutor(Connection conn) {
            this.conn = conn;
        }

        // #region - query

        /** {@inheritDoc} */
        @Override
        public <T> T query(String sql, Object[] params, ResultHandler<T> resultHandler)
                throws SQLException {
            return JdbcOperationSupport.query(this.conn, sql, params, resultHandler);
        }

        /** {@inheritDoc} */
        @Override
        public <T> T query(String sql, ResultHandler<T> resultHandler)
                throws SQLException {
            return JdbcOperationSupport
                    .query(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, resultHandler);
        }

        // #endregion

        // #region - queryList

        /** {@inheritDoc} */
        @Override
        public <T> List<T> queryList(String sql, Object[] params, RowMapper<T> rowMapper)
                throws SQLException {
            return JdbcOperationSupport.queryList(this.conn, sql, params, rowMapper);
        }

        /** {@inheritDoc} */
        @Override
        public <T> List<T> queryList(String sql, Object[] params, Class<T> clazz)
                throws SQLException {
            return JdbcOperationSupport.queryList(this.conn, sql, params, clazz);
        }

        /** {@inheritDoc} */
        @Override
        public List<Map<String, Object>> queryList(String sql, Object[] params)
                throws SQLException {
            return JdbcOperationSupport.queryList(this.conn, sql, params, RowMapper.HASH_MAP_MAPPER);
        }

        /** {@inheritDoc} */
        @Override
        public <T> List<T> queryList(String sql, RowMapper<T> rowMapper)
                throws SQLException {
            return JdbcOperationSupport
                    .queryList(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
        }

        /** {@inheritDoc} */
        @Override
        public <T> List<T> queryList(String sql, Class<T> clazz)
                throws SQLException {
            return JdbcOperationSupport.queryList(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, clazz);
        }

        /** {@inheritDoc} */
        @Override
        public List<Map<String, Object>> queryList(String sql)
                throws SQLException {
            return JdbcOperationSupport
                    .queryList(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, RowMapper.HASH_MAP_MAPPER);
        }

        // #endregion

        // #region - queryFirst

        /** {@inheritDoc} */
        @Override
        public <T> Optional<T> queryFirst(String sql, Object[] params, RowMapper<T> rowMapper)
                throws SQLException {
            final T result = JdbcOperationSupport.queryFirst(this.conn, sql, params, rowMapper);
            return Optional.ofNullable(result);
        }

        /** {@inheritDoc} */
        @Override
        public <T> Optional<T> queryFirst(String sql, Object[] params, Class<T> clazz)
                throws SQLException {
            final T result = JdbcOperationSupport.queryFirst(this.conn, sql, params, clazz);
            return Optional.ofNullable(result);
        }

        /** {@inheritDoc} */
        @Override
        public Optional<Map<String, Object>> queryFirst(String sql, Object[] params)
                throws SQLException {
            final Map<String, Object> result = JdbcOperationSupport
                    .queryFirst(this.conn, sql, params, RowMapper.HASH_MAP_MAPPER);
            return Optional.ofNullable(result);
        }

        /** {@inheritDoc} */
        @Override
        public <T> Optional<T> queryFirst(String sql, RowMapper<T> rowMapper)
                throws SQLException {
            final T result = JdbcOperationSupport
                    .queryFirst(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
            return Optional.ofNullable(result);
        }

        /** {@inheritDoc} */
        @Override
        public <T> Optional<T> queryFirst(String sql, Class<T> clazz)
                throws SQLException {
            final T result = JdbcOperationSupport
                    .queryFirst(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, clazz);
            return Optional.ofNullable(result);
        }

        /** {@inheritDoc} */
        @Override
        public Optional<Map<String, Object>> queryFirst(String sql)
                throws SQLException {
            final Map<String, Object> result = JdbcOperationSupport
                    .queryFirst(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, RowMapper.HASH_MAP_MAPPER);
            return Optional.ofNullable(result);
        }

        /** {@inheritDoc} */
        @Override
        public boolean queryAsBoolean(String sql)
                throws SQLException {
            final Boolean result = JdbcOperationSupport
                    .queryFirstBoolean(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
            return Boolean.TRUE.equals(result);
        }

        /** {@inheritDoc} */
        @Override
        public boolean queryAsBoolean(String sql, Object[] params)
                throws SQLException {
            final Boolean result = JdbcOperationSupport.queryFirstBoolean(this.conn, sql, params);
            return Boolean.TRUE.equals(result);
        }

        // #endregion

        // #region - update & batchUpdate

        /** {@inheritDoc} */
        @Override
        public int update(String sql, Object[] params)
                throws SQLException {
            return JdbcOperationSupport.update(this.conn, sql, params);
        }

        /** {@inheritDoc} */
        @Override
        public int update(String sql)
                throws SQLException {
            return JdbcOperationSupport.update(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }

        /** {@inheritDoc} */
        @Override
        public <T> List<T> update(String sql, Object[] params, RowMapper<T> rowMapper)
                throws SQLException {
            return JdbcOperationSupport.update(this.conn, sql, params, rowMapper);
        }

        /** {@inheritDoc} */
        @Override
        public <T> List<T> update(String sql, RowMapper<T> rowMapper)
                throws SQLException {
            return JdbcOperationSupport.update(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
        }

        /** {@inheritDoc} */
        @Override
        public List<int[]> batchUpdate(String sql, @Nullable Collection<Object[]> params, int batchSize)
                throws SQLException {
            return JdbcOperationSupport.batchUpdate(this.conn, sql, params, batchSize, null, false);
        }

        /** {@inheritDoc} */
        @Override
        public List<int[]> batchUpdate(String sql,
                                       @Nullable Collection<Object[]> params,
                                       int batchSize,
                                       List<Exception> exceptions,
                                       boolean quietly) throws SQLException {
            return JdbcOperationSupport
                    .batchUpdate(this.conn, sql, params, batchSize, exceptions, quietly);
        }

        // #endregion

    }

}
