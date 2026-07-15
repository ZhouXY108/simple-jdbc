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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import javax.sql.DataSource;

import xyz.zhouxy.jdbc.function.ThrowingBiConsumer;
import xyz.zhouxy.jdbc.function.ThrowingBiPredicate;
import xyz.zhouxy.jdbc.function.ThrowingConsumer;
import xyz.zhouxy.jdbc.function.ThrowingPredicate;
import xyz.zhouxy.jdbc.namedparam.NamedParamJdbcOperations;
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
 * <p>
 * 本类支持通过 {@link JdbcConfig} 自定义事务内操作的 Statement 参数和 ResultSet 类型。
 * 事务内的 {@link JdbcOperations} 实例与 {@link TransactionTemplate} 共享同一个 {@link JdbcConfig}。
 * </p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * TransactionTemplate tx = new TransactionTemplate(dataSource);
 *
 * // 纯位置参数
 * tx.execute(ops -> {
 *     ops.update("INSERT INTO ...", buildParams(...));
 *     ops.update("UPDATE ...", buildParams(...));
 * });
 *
 * // 纯命名参数
 * tx.executeNamed(nops -> {
 *     nops.update("INSERT INTO users(name, age) VALUES(#{name}, #{age})",
 *             Map.of("name", "Alice", "age", 25));
 * });
 *
 * // 混用两种参数风格
 * tx.execute((ops, nops) -> {
 *     ops.update("UPDATE accounts SET balance = ? WHERE id = ?",
 *             new Object[]{100, 1});
 *     nops.update("INSERT INTO logs(msg, user) VALUES(#{msg}, #{user})",
 *             Map.of("msg", "transfer", "user", "Alice"));
 * });
 *
 * // 谓词模式：返回 true 提交，false 回滚
 * tx.commitIfTrue(ops -> {
 *     ops.update("UPDATE ...", buildParams(...));
 *     return ops.queryBoolean("SELECT ...", buildParams(...));
 * });
 * // commitIfTrue 同样提供命名参数和混用两种重载：
 * //   commitIfTrueNamed(nops -> {...})       — 纯命名参数谓词
 * //   commitIfTrue((ops, nops) -> {...})     — 混用位置与命名参数谓词
 * }</pre>
 *
 * @author ZhouXY
 * @since 1.1.0
 * @see JdbcConfig
 * @see JdbcOperations
 * @see NamedParamJdbcOperations
 */
@NullMarked
public class TransactionTemplate {

    private final DataSource dataSource;

    private final JdbcConfig config;

    /**
     * 使用默认配置构造一个 {@code TransactionTemplate} 实例。
     *
     * @param dataSource 数据源，用于获取数据库连接；不可为 {@code null}
     */
    public TransactionTemplate(DataSource dataSource) {
        this(dataSource, JdbcConfig.defaults());
    }

    /**
     * 使用指定配置构造一个 {@code TransactionTemplate} 实例。
     *
     * @param dataSource 数据源，用于获取数据库连接；不可为 {@code null}
     * @param config     JDBC 配置；不可为 {@code null}
     */
    public TransactionTemplate(DataSource dataSource, JdbcConfig config) {
        AssertTools.checkNotNull(dataSource);
        AssertTools.checkNotNull(config);
        this.dataSource = dataSource;
        this.config = config;
    }

    // #region - execute

    /**
     * 执行事务。如果未发生异常，则提交事务；当有异常发生时，回滚事务
     *
     * <p>
     * operations 中使用 JdbcOperations 实参进行 JDBC 操作，这些操作在一个连接中
     * </p>
     *
     * @param <E>                   异常类型
     * @param operations            事务操作
     * @throws SQLException         SQL 异常
     * @throws TransactionException 事务异常。事务中的异常会包装在该异常中。
     */
    public <E extends @Nullable Exception> void execute(
            final ThrowingConsumer<JdbcOperations, E> operations)
            throws TransactionException, SQLException {
        execute(null, operations);
    }

    /**
     * 执行事务。如果未发生异常，则提交事务；当有异常发生时，回滚事务
     *
     * <p>
     * operations 中使用 JdbcOperations 实参进行 JDBC 操作，这些操作在一个连接中
     * </p>
     *
     * @param <E>                   异常类型
     * @param isolationLevel        事务隔离级别
     * @param operations            事务操作
     * @throws SQLException         SQL 异常
     * @throws TransactionException 事务异常。事务中的异常会包装在该异常中。
     */
    public <E extends @Nullable Exception> void execute(
            @Nullable final TransactionIsolationLevel isolationLevel,
            final ThrowingConsumer<JdbcOperations, E> operations)
            throws TransactionException, SQLException {
        AssertTools.checkNotNull(operations, "Operations can not be null.");
        try (Connection conn = this.dataSource.getConnection()) {
            final boolean srcAutoCommit = conn.getAutoCommit();
            final int srcIsolationLevel = conn.getTransactionIsolation();
            Exception caught = null;
            boolean needRollback = false;
            try {
                if (isolationLevel != null) {
                    //noinspection MagicConstant
                    conn.setTransactionIsolation(isolationLevel.getLevel());
                }
                conn.setAutoCommit(false);
                needRollback = true;
                operations.accept(new TransactionJdbcExecutor(conn, config));
                conn.commit();
                needRollback = false;
            }
            catch (Exception e) {
                caught = e;
                if (needRollback) {
                    rollbackSilently(conn, e);
                }
                throw new TransactionException(e);
            }
            finally {
                restoreConnectionState(conn, srcAutoCommit, srcIsolationLevel, caught);
            }
        }
    }

    /**
     * 执行事务（纯命名参数）。如果未发生异常，则提交事务；当有异常发生时，回滚事务
     *
     * <p>
     * 适用于事务中所有 SQL 都使用命名参数（{@code #{paramName}}）的场景。
     * 如需混用位置参数和命名参数，请使用 {@link #execute(ThrowingBiConsumer)}。
     * </p>
     *
     * @param <E>                   异常类型
     * @param operations            事务操作
     * @throws SQLException         SQL 异常
     * @throws TransactionException 事务异常。事务中的异常会包装在该异常中。
     * @since 1.1.0
     */
    public <E extends @Nullable Exception> void executeNamed(
            final ThrowingConsumer<NamedParamJdbcOperations, E> operations)
            throws TransactionException, SQLException {
        executeNamed(null, operations);
    }

    /**
     * 执行事务（纯命名参数）。如果未发生异常，则提交事务；当有异常发生时，回滚事务
     *
     * <p>
     * 适用于事务中所有 SQL 都使用命名参数（{@code #{paramName}}）的场景。
     * 如需混用位置参数和命名参数，请使用 {@link #execute(ThrowingBiConsumer)}。
     * </p>
     *
     * @param <E>                   异常类型
     * @param isolationLevel        事务隔离级别
     * @param operations            事务操作
     * @throws SQLException         SQL 异常
     * @throws TransactionException 事务异常。事务中的异常会包装在该异常中。
     * @since 1.1.0
     */
    public <E extends @Nullable Exception> void executeNamed(
            @Nullable final TransactionIsolationLevel isolationLevel,
            final ThrowingConsumer<NamedParamJdbcOperations, E> operations)
            throws TransactionException, SQLException {
        AssertTools.checkNotNull(operations, "Operations can not be null.");
        execute(isolationLevel, ops -> operations.accept(ops.getNamedParamJdbcOperations()));
    }

    /**
     * 执行事务（混用位置参数与命名参数）。如果未发生异常，则提交事务；当有异常发生时，回滚事务
     *
     * <p>
     * 回调同时提供 {@link JdbcOperations} 和 {@link NamedParamJdbcOperations}，
     * 可在同一事务中按需选择位置参数或命名参数风格。
     * </p>
     *
     * @param <E>                   异常类型
     * @param operations            事务操作
     * @throws SQLException         SQL 异常
     * @throws TransactionException 事务异常。事务中的异常会包装在该异常中。
     * @since 1.1.0
     */
    public <E extends @Nullable Exception> void execute(
            final ThrowingBiConsumer<JdbcOperations, NamedParamJdbcOperations, E> operations)
            throws TransactionException, SQLException {
        execute(null, operations);
    }

    /**
     * 执行事务（混用位置参数与命名参数）。如果未发生异常，则提交事务；当有异常发生时，回滚事务
     *
     * <p>
     * 回调同时提供 {@link JdbcOperations} 和 {@link NamedParamJdbcOperations}，
     * 可在同一事务中按需选择位置参数或命名参数风格。
     * </p>
     *
     * @param <E>                   异常类型
     * @param isolationLevel        事务隔离级别
     * @param operations            事务操作
     * @throws SQLException         SQL 异常
     * @throws TransactionException 事务异常。事务中的异常会包装在该异常中。
     * @since 1.1.0
     */
    public <E extends @Nullable Exception> void execute(
            @Nullable final TransactionIsolationLevel isolationLevel,
            final ThrowingBiConsumer<JdbcOperations, NamedParamJdbcOperations, E> operations)
            throws TransactionException, SQLException {
        AssertTools.checkNotNull(operations, "Operations can not be null.");
        execute(isolationLevel, ops -> operations.accept(ops, ops.getNamedParamJdbcOperations()));
    }

    // #endregion

    // #region - commitIfTrue

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
    public <E extends @Nullable Exception> void commitIfTrue(
            final ThrowingPredicate<JdbcOperations, E> operations)
            throws SQLException, TransactionException {
        commitIfTrue(null, operations);
    }

    /**
     * 执行事务。
     * 如果 {@code operations} 返回 {@code true}，则提交事务；
     * 如果抛出异常，或返回 {@code false}，则回滚事务
     *
     * @param <E>                   事务中的异常
     * @param isolationLevel        事务隔离级别
     * @param operations            事务操作
     * @throws SQLException         数据库异常
     * @throws TransactionException 事务异常。事务中的异常会包装在该异常中。
     */
    public <E extends @Nullable Exception> void commitIfTrue(
            @Nullable final TransactionIsolationLevel isolationLevel,
            final ThrowingPredicate<JdbcOperations, E> operations)
            throws SQLException, TransactionException {
        AssertTools.checkNotNull(operations, "Operations can not be null.");
        try (Connection conn = this.dataSource.getConnection()) {
            final boolean srcAutoCommit = conn.getAutoCommit();
            final int srcIsolationLevel = conn.getTransactionIsolation();
            Exception caught = null;
            boolean needRollback = false;
            try {
                if (isolationLevel != null) {
                    //noinspection MagicConstant
                    conn.setTransactionIsolation(isolationLevel.getLevel());
                }
                conn.setAutoCommit(false);
                needRollback = true;
                if (operations.test(new TransactionJdbcExecutor(conn, config))) {
                    conn.commit();
                }
                else {
                    conn.rollback();
                }
                needRollback = false;
            }
            catch (Exception e) {
                caught = e;
                if (needRollback) {
                    rollbackSilently(conn, e);
                }
                throw new TransactionException(e);
            }
            finally {
                restoreConnectionState(conn, srcAutoCommit, srcIsolationLevel, caught);
            }
        }
    }

    /**
     * 执行事务（纯命名参数）。
     * 如果 {@code operations} 返回 {@code true}，则提交事务；
     * 如果抛出异常，或返回 {@code false}，则回滚事务
     *
     * @param <E>                   事务中的异常
     * @param operations            事务操作
     * @throws SQLException         数据库异常
     * @throws TransactionException 事务异常。事务中的异常会包装在该异常中。
     * @since 1.1.0
     */
    public <E extends @Nullable Exception> void commitIfTrueNamed(
            final ThrowingPredicate<NamedParamJdbcOperations, E> operations)
            throws SQLException, TransactionException {
        commitIfTrueNamed(null, operations);
    }

    /**
     * 执行事务（纯命名参数）。
     * 如果 {@code operations} 返回 {@code true}，则提交事务；
     * 如果抛出异常，或返回 {@code false}，则回滚事务
     *
     * @param <E>                   事务中的异常
     * @param isolationLevel        事务隔离级别
     * @param operations            事务操作
     * @throws SQLException         数据库异常
     * @throws TransactionException 事务异常。事务中的异常会包装在该异常中。
     * @since 1.1.0
     */
    public <E extends @Nullable Exception> void commitIfTrueNamed(
            @Nullable final TransactionIsolationLevel isolationLevel,
            final ThrowingPredicate<NamedParamJdbcOperations, E> operations)
            throws SQLException, TransactionException {
        AssertTools.checkNotNull(operations, "Operations can not be null.");
        commitIfTrue(isolationLevel, ops -> operations.test(ops.getNamedParamJdbcOperations()));
    }

    /**
     * 执行事务（混用位置参数与命名参数）。
     * 如果 {@code operations} 返回 {@code true}，则提交事务；
     * 如果抛出异常，或返回 {@code false}，则回滚事务
     *
     * @param <E>                   事务中的异常
     * @param operations            事务操作
     * @throws SQLException         数据库异常
     * @throws TransactionException 事务异常。事务中的异常会包装在该异常中。
     * @since 1.1.0
     */
    public <E extends @Nullable Exception> void commitIfTrue(
            final ThrowingBiPredicate<JdbcOperations, NamedParamJdbcOperations, E> operations)
            throws SQLException, TransactionException {
        commitIfTrue(null, operations);
    }

    /**
     * 执行事务（混用位置参数与命名参数）。
     * 如果 {@code operations} 返回 {@code true}，则提交事务；
     * 如果抛出异常，或返回 {@code false}，则回滚事务
     *
     * @param <E>                   事务中的异常
     * @param isolationLevel        事务隔离级别
     * @param operations            事务操作
     * @throws SQLException         数据库异常
     * @throws TransactionException 事务异常。事务中的异常会包装在该异常中。
     * @since 1.1.0
     */
    public <E extends @Nullable Exception> void commitIfTrue(
            @Nullable final TransactionIsolationLevel isolationLevel,
            final ThrowingBiPredicate<JdbcOperations, NamedParamJdbcOperations, E> operations)
            throws SQLException, TransactionException {
        AssertTools.checkNotNull(operations, "Operations can not be null.");
        commitIfTrue(isolationLevel, ops -> operations.test(ops, ops.getNamedParamJdbcOperations()));
    }

    // #endregion

    // #region - transaction helpers

    private void rollbackSilently(Connection conn, Exception e) {
        try {
            conn.rollback();
        }
        catch (SQLException ex) {
            e.addSuppressed(ex);
        }
    }

    /**
     * 统一恢复连接的原始状态。
     * 确保所有恢复步骤均被执行，并妥善处理多重异常的聚合（Suppressed Exceptions）。
     *
     * @param conn               当前数据库连接
     * @param srcAutoCommit      事务开始前记录的原始 autoCommit 状态
     * @param srcIsolationLevel  事务开始前记录的原始隔离级别
     * @param primaryException   try 块中业务代码抛出的主异常（可能为 null）
     */
    private void restoreConnectionState(
            @Nullable Connection conn,
            boolean srcAutoCommit,
            int srcIsolationLevel,
            @Nullable Exception primaryException) throws SQLException {

        if (conn == null) {
            return;
        }

        SQLException firstRecoveryException = null;

        // 1. 恢复 autoCommit (即使失败也继续执行后续步骤)
        try {
            conn.setAutoCommit(srcAutoCommit);
        } catch (SQLException ex) {
            firstRecoveryException = ex;
        }

        // 2. 恢复隔离级别 (即使失败也继续，并聚合异常)
        try {
            conn.setTransactionIsolation(srcIsolationLevel);
        } catch (SQLException ex) {
            if (firstRecoveryException == null) {
                firstRecoveryException = ex;
            } else {
                firstRecoveryException.addSuppressed(ex);
            }
        }

        // 3. 统一处理最终异常
        if (firstRecoveryException != null) {
            if (primaryException != null) {
                // 如果业务执行过程中已经产生了异常，将恢复阶段的异常作为 Suppressed 附加
                primaryException.addSuppressed(firstRecoveryException);
            } else {
                // 如果业务执行正常，则抛出恢复阶段产生的第一个异常
                throw firstRecoveryException;
            }
        }
    }

    // #endregion

    // #region - TransactionJdbcExecutor

    @SuppressWarnings("java:S6665")
    private static final class TransactionJdbcExecutor
            implements JdbcOperations, NamedParamJdbcOperations {

        private final Connection conn;
        private final JdbcExecutor jdbcExecutor;

        private TransactionJdbcExecutor(Connection conn, JdbcConfig config) {
            this.conn = conn;
            this.jdbcExecutor = new JdbcExecutor(config);
        }

        /** {@inheritDoc} */
        @Override
        public JdbcOperations getJdbcOperations() {
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public NamedParamJdbcOperations getNamedParamJdbcOperations() {
            return this;
        }

        // #region - query

        /** {@inheritDoc} */
        @Override
        public <T extends @Nullable Object> T query(String sql, @Nullable Object @Nullable [] params,
                ResultHandler<T> resultHandler)
                throws SQLException {
            return this.jdbcExecutor.query(this.conn, sql, params, resultHandler);
        }

        // #endregion

        // #region - queryList

        /** {@inheritDoc} */
        @Override
        public <T extends @Nullable Object> List<T> queryList(
                String sql, @Nullable Object @Nullable [] params,
                RowMapper<T> rowMapper)
                throws SQLException {
            return this.jdbcExecutor.queryList(this.conn, sql, params, rowMapper);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends @Nullable Object> List<T> queryValues(
                String sql, @Nullable Object @Nullable [] params,
                Class<@NonNull T> clazz)
                throws SQLException {
            return this.jdbcExecutor.queryValues(this.conn, sql, params, clazz);
        }

        /** {@inheritDoc} */
        @Override
        public List<@Nullable Map<String, @Nullable Object>> queryList(
                String sql, @Nullable Object @Nullable [] params)
                throws SQLException {
            return this.jdbcExecutor.queryList(this.conn, sql, params);
        }

        // #endregion

        // #region - queryFirst

        /** {@inheritDoc} */
        @Override
        public <T> Optional<T> queryFirst(
                String sql, @Nullable Object @Nullable [] params,
                RowMapper<T> rowMapper)
                throws SQLException {
            return this.jdbcExecutor.queryFirst(this.conn, sql, params, rowMapper);
        }

        /** {@inheritDoc} */
        @Override
        public <T> Optional<T> queryValue(
                String sql, @Nullable Object @Nullable [] params,
                Class<T> clazz)
                throws SQLException {
            return this.jdbcExecutor.queryValue(this.conn, sql, params, clazz);
        }

        /** {@inheritDoc} */
        @Override
        public Optional<Map<String, @Nullable Object>> queryFirst(
                String sql, @Nullable Object @Nullable [] params)
                throws SQLException {
            return this.jdbcExecutor.queryFirst(this.conn, sql, params);
        }

        /** {@inheritDoc} */
        @Override
        public boolean queryBoolean(String sql, @Nullable Object @Nullable [] params)
                throws SQLException {
            return this.jdbcExecutor.queryBoolean(this.conn, sql, params);
        }

        // #endregion

        // #region - update & batchUpdate

        /** {@inheritDoc} */
        @Override
        public int update(String sql, @Nullable Object @Nullable [] params)
                throws SQLException {
            return this.jdbcExecutor.update(this.conn, sql, params);
        }

        /** {@inheritDoc} */
        @Override
        public <T extends @Nullable Object> List<T> updateAndReturnKeys(
                String sql, @Nullable Object @Nullable [] params,
                RowMapper<T> rowMapper)
                throws SQLException {
            return this.jdbcExecutor.updateAndReturnKeys(this.conn, sql, params, rowMapper);
        }

        /** {@inheritDoc} */
        @Override
        public BatchUpdateResult batchUpdate(
                String sql, @Nullable Collection<@Nullable Object @Nullable []> params,
                int batchSize)
                throws SQLException {
            return this.jdbcExecutor.batchUpdate(this.conn, sql, params, batchSize);
        }

        /** {@inheritDoc} */
        @Override
        public BatchUpdateResult batchUpdate(
                String sql, @Nullable Collection<@Nullable Object @Nullable []> params,
                int batchSize,
                boolean quietly) throws SQLException {
            return this.jdbcExecutor.batchUpdate(this.conn, sql, params, batchSize, quietly);
        }

        // #endregion

    }

    // #endregion
}
