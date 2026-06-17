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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.sql.DataSource;

import xyz.zhouxy.jdbc.util.AssertTools;

/**
 * 事务模板，提供事务执行能力。
 *
 * <p>
 * 负责管理事务的生命周期：开启、提交、回滚、恢复自动提交。
 * 事务内的 JDBC 操作通过 {@link JdbcOperations} 接口进行，
 * 所有操作共享同一个数据库连接。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * TransactionTemplate tx = new TransactionTemplate(dataSource);
 *
 * // 消费者模式：无异常自动提交
 * tx.execute(ops -> {
 *     ops.update("INSERT INTO ...", buildParams(...));
 *     ops.update("UPDATE ...", buildParams(...));
 * });
 *
 * // 谓词模式：返回 true 提交，false 回滚
 * tx.commitIfTrue(ops -> {
 *     ops.update("UPDATE ...", buildParams(...));
 *     return ops.queryBoolean("SELECT ...", buildParams(...));
 * });
 * }</pre>
 *
 * @author ZhouXY
 * @since 1.1.0
 */
public class TransactionTemplate {

    @Nonnull
    private final DataSource dataSource;

    /**
     * 构造一个 {@code TransactionTemplate} 实例
     *
     * @param dataSource 数据源，用于获取数据库连接；不可为 {@code null}
     */
    public TransactionTemplate(DataSource dataSource) {
        AssertTools.checkNotNull(dataSource);
        this.dataSource = dataSource;
    }

    /**
     * 执行事务。如果未发生异常，则提交事务；当有异常发生时，回滚事务
     *
     * <p>
     * operations 中使用 JdbcExecutor 实参进行 JDBC 操作，这些操作在一个连接中
     * </p>
     *
     * @param <E>                   异常类型
     * @param operations            事务操作
     * @throws SQLException         SQL 异常
     * @throws TransactionException 事务异常。事务中的异常会包装在该异常中。
     */
    public <E extends Exception> void execute(
            @Nonnull final ThrowingConsumer<JdbcOperations, E> operations)
            throws TransactionException, SQLException {
        AssertTools.checkNotNull(operations, "Operations can not be null.");
        try (Connection conn = this.dataSource.getConnection()) {
            final boolean autoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                operations.accept(new TransactionJdbcExecutor(conn));
                conn.commit();
            }
            catch (Exception e) {
                rollbackSilently(conn, e);
                throw new TransactionException(e);
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
     * @param <E>                   事务中的异常
     * @param operations            事务操作
     * @throws SQLException         数据库异常
     * @throws TransactionException 事务异常。事务中的异常会包装在该异常中。
     */
    public <E extends Exception> void commitIfTrue(
            @Nonnull final ThrowingPredicate<JdbcOperations, E> operations)
            throws SQLException, TransactionException {
        AssertTools.checkNotNull(operations, "Operations can not be null.");
        try (Connection conn = this.dataSource.getConnection()) {
            final boolean autoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                if (operations.test(new TransactionJdbcExecutor(conn))) {
                    conn.commit();
                }
                else {
                    conn.rollback();
                }
            }
            catch (Exception e) {
                rollbackSilently(conn, e);
                throw new TransactionException(e);
            }
            finally {
                conn.setAutoCommit(autoCommit);
            }
        }
    }

    private void rollbackSilently(Connection conn, Exception e) {
        try {
            conn.rollback();
        }
        catch (SQLException ex) {
            e.addSuppressed(ex);
        }
    }

    // #region - TransactionJdbcExecutor

    private static final class TransactionJdbcExecutor implements JdbcOperations {

        private final Connection conn;

        private TransactionJdbcExecutor(Connection conn) {
            this.conn = conn;
        }

        // #region - query

        /** {@inheritDoc} */
        @Override
        public <T> T query(String sql, Object[] params, ResultHandler<T> resultHandler)
                throws SQLException {
            return JdbcOperationSupport.query(this.conn, sql, params, resultHandler);
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
        public <T> List<T> queryValues(String sql, Object[] params, Class<T> clazz)
                throws SQLException {
            return JdbcOperationSupport.queryValues(this.conn, sql, params, clazz);
        }

        /** {@inheritDoc} */
        @Override
        public List<Map<String, Object>> queryList(String sql, Object[] params)
                throws SQLException {
            return JdbcOperationSupport.queryList(this.conn, sql, params, RowMapper.HASH_MAP_MAPPER);
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
        public <T> Optional<T> queryValue(String sql, Object[] params, Class<T> clazz)
                throws SQLException {
            final T result = JdbcOperationSupport.queryValue(this.conn, sql, params, clazz);
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
        public boolean queryBoolean(String sql, Object[] params)
                throws SQLException {
            final Boolean result = JdbcOperationSupport
                    .queryValue(this.conn, sql, params, Boolean.class);
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
        public <T> List<T> updateAndReturnKeys(String sql, Object[] params, RowMapper<T> rowMapper)
                throws SQLException {
            return JdbcOperationSupport.updateAndReturnKeys(this.conn, sql, params, rowMapper);
        }

        /** {@inheritDoc} */
        @Override
        public BatchUpdateResult batchUpdate(String sql, @Nullable Collection<Object[]> params, int batchSize)
                throws SQLException {
            return JdbcOperationSupport.batchUpdate(this.conn, sql, params, batchSize, false);
        }

        /** {@inheritDoc} */
        @Override
        public BatchUpdateResult batchUpdate(String sql,
                @Nullable Collection<Object[]> params,
                int batchSize,
                boolean quietly) throws SQLException {
            return JdbcOperationSupport
                    .batchUpdate(this.conn, sql, params, batchSize, quietly);
        }

        // #endregion

    }

    // #endregion
}
