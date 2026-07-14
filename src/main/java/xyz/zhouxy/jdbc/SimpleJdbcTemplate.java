/*
 * Copyright 2022-present ZhouXY
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

import xyz.zhouxy.jdbc.namedparam.NamedParamJdbcOperations;
import xyz.zhouxy.jdbc.util.AssertTools;

/**
 * JDBC 操作的模板类，对原生 JDBC 进行轻量封装，提供查询、更新、批量操作等便捷方法。
 *
 * <p>
 * 主要能力：
 * <ul>
 * <li>查询：支持 {@link ResultHandler} 自定义结果处理、{@link RowMapper} 行映射等多种方式</li>
 * <li>更新：执行 INSERT / UPDATE / DELETE，支持返回自增主键</li>
 * <li>批量操作：通过 {@link #batchUpdate} 分批执行 DML，支持静默模式（遇错继续）和
 *     非静默模式（遇错即中断）</li>
 * <li>事务：通过 {@link #transaction()} 获取 {@link TransactionTemplate} 执行</li>
 * </ul>
 *
 * <p>
 * 本类支持通过 {@link JdbcConfig} 自定义实例级的 Statement 参数和 ResultSet 类型。
 * {@link #transaction()} 返回的 {@link TransactionTemplate} 共享同一个 {@link JdbcConfig}。
 * </p>
 *
 * <p>
 * 线程安全：本类无内部可变状态，线程安全。所依赖的 {@link DataSource}
 * 需自行保证线程安全。
 * </p>
 *
 * @author ZhouXY
 * @since 1.0.0
 * @see JdbcConfig
 * @see JdbcOperations
 * @see TransactionTemplate
 * @see ParamBuilder
 */
@NullMarked
public class SimpleJdbcTemplate implements JdbcOperations, NamedParamJdbcOperations {

    private final DataSource dataSource;

    private final JdbcExecutor jdbcExecutor;

    private final TransactionTemplate transactionTemplate;

    /**
     * 使用默认配置构造一个 {@code SimpleJdbcTemplate} 实例。
     *
     * @param dataSource 数据源，用于获取数据库连接；不可为 {@code null}
     */
    public SimpleJdbcTemplate(DataSource dataSource) {
        this(dataSource, JdbcConfig.defaults());
    }

    /**
     * 使用指定配置构造一个 {@code SimpleJdbcTemplate} 实例。
     *
     * @param dataSource 数据源，用于获取数据库连接；不可为 {@code null}
     * @param config     JDBC 配置；不可为 {@code null}
     */
    public SimpleJdbcTemplate(DataSource dataSource, JdbcConfig config) {
        AssertTools.checkNotNull(dataSource);
        AssertTools.checkNotNull(config);
        this.dataSource = dataSource;
        this.jdbcExecutor = new JdbcExecutor(config);
        this.transactionTemplate = new TransactionTemplate(dataSource, config);
    }

    // #region - query

    /** {@inheritDoc} */
    @Override
    public <T extends @Nullable Object>
    T query(String sql, @Nullable Object @Nullable [] params, ResultHandler<T> resultHandler)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return this.jdbcExecutor.query(conn, sql, params, resultHandler);
        }
    }

    // #endregion

    // #region - queryList

    /** {@inheritDoc} */
    @Override
    public <T extends @Nullable Object>
    List<T> queryList(String sql, @Nullable Object @Nullable [] params, RowMapper<T> rowMapper)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return this.jdbcExecutor.queryList(conn, sql, params, rowMapper);
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T extends @Nullable Object>
    List<T> queryValues(String sql, @Nullable Object @Nullable [] params, Class<@NonNull T> clazz)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return this.jdbcExecutor.queryValues(conn, sql, params, clazz);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<@Nullable Map<String, @Nullable Object>> queryList(
            String sql, @Nullable Object @Nullable [] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return this.jdbcExecutor.queryList(conn, sql, params);
        }
    }

    // #endregion

    // #region - queryFirst

    /** {@inheritDoc} */
    @Override
    public <T> Optional<T> queryFirst(
            String sql, @Nullable Object @Nullable [] params,
            RowMapper<T> rowMapper)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return this.jdbcExecutor.queryFirst(conn, sql, params, rowMapper);
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T> Optional<T> queryValue(
            String sql, @Nullable Object @Nullable [] params, Class<T> clazz)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return this.jdbcExecutor.queryValue(conn, sql, params, clazz);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Optional<Map<String, @Nullable Object>> queryFirst(String sql, @Nullable Object @Nullable [] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return this.jdbcExecutor.queryFirst(conn, sql, params);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean queryBoolean(String sql, @Nullable Object @Nullable [] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return this.jdbcExecutor.queryBoolean(conn, sql, params);
        }
    }

    // #endregion

    // #region - update & batchUpdate

    /** {@inheritDoc} */
    @Override
    public int update(String sql, @Nullable Object @Nullable [] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return this.jdbcExecutor.update(conn, sql, params);
        }
    }

    /** {@inheritDoc} */
    @Override
    public <T extends @Nullable Object> List<T> updateAndReturnKeys(String sql, @Nullable Object @Nullable [] params, RowMapper<T> rowMapper)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return this.jdbcExecutor.updateAndReturnKeys(conn, sql, params, rowMapper);
        }
    }

    /** {@inheritDoc} */
    @Override
    public BatchUpdateResult batchUpdate(String sql, @Nullable Collection<@Nullable Object @Nullable []> params, int batchSize)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return this.jdbcExecutor.batchUpdate(conn, sql, params, batchSize);
        }
    }

    /** {@inheritDoc} */
    @Override
    public BatchUpdateResult batchUpdate(String sql, @Nullable Collection<@Nullable Object @Nullable []> params,
            int batchSize, boolean quietly)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return this.jdbcExecutor.batchUpdate(conn, sql, params, batchSize, quietly);
        }
    }

    // #endregion

    // #region - NamedParamJdbcOperations

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

    // #endregion

    // #region - transaction

    /**
     * 获取事务模板。
     *
     * <p>
     * 返回的 {@link TransactionTemplate} 与当前模板共享同一个 {@link DataSource}
     * 和 {@link JdbcConfig}。
     *
     * @return 事务模板
     */
    public TransactionTemplate transaction() {
        return this.transactionTemplate;
    }

    // #endregion

}
