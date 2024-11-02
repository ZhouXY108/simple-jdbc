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
import xyz.zhouxy.plusone.commons.function.ThrowingConsumer;
import xyz.zhouxy.plusone.commons.function.ThrowingPredicate;
import xyz.zhouxy.plusone.commons.util.AssertTools;
import xyz.zhouxy.plusone.commons.util.OptionalTools;

/**
 * SimpleJdbcTemplate
 *
 * <p>
 * 对 JDBC 的简单封装，方便数据库操作，支持事务，支持批量操作，支持自定义结果集映射
 * </p>
 *
 * @author <a href="http://zhouxy.xyz:3000/ZhouXY108">ZhouXY</a>
 * @since 1.0.0
 */
public class SimpleJdbcTemplate {

    @Nonnull
    private final DataSource dataSource;

    public SimpleJdbcTemplate(@Nonnull DataSource dataSource) {
        AssertTools.checkNotNull(dataSource);
        this.dataSource = dataSource;
    }

    // #region - query

    /**
     * 执行查询，并按照自定义处理逻辑对结果进行处理，将结果转换为指定类型并返回
     *
     * @param sql           SQL
     * @param params        参数
     * @param resultHandler 结果处理器，用于处理 {@link ResultSet}
     */
    public <T> T query(String sql, Object[] params, ResultHandler<T> resultHandler)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.query(conn, sql, params, resultHandler);
        }
    }

    /**
     * 执行查询，并按照自定义处理逻辑对结果进行处理，将结果转换为指定类型并返回
     *
     * @param sql           SQL
     * @param resultHandler 结果处理器，用于处理 {@link ResultSet}
     */
    public <T> T query(String sql, ResultHandler<T> resultHandler)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.query(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, resultHandler);
        }
    }

    // #endregion

    // #region - queryList

    /**
     * 执行查询，将查询结果的每一行数据按照指定逻辑进行处理，返回结果列表
     *
     * @param sql       SQL
     * @param params    参数
     * @param rowMapper {@link ResultSet} 中每一行的数据的处理逻辑
     */
    public <T> List<T> queryList(String sql, Object[] params, RowMapper<T> rowMapper)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryList(conn, sql, params, rowMapper);
        }
    }

    /**
     * 执行查询，返回结果映射为指定的类型。当结果为单列时使用
     *
     * @param sql    SQL
     * @param params 参数
     * @param clazz  将结果映射为指定的类型
     */
    public <T> List<T> queryList(String sql, Object[] params, Class<T> clazz)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryList(conn, sql, params, clazz);
        }
    }

    /**
     * 执行查询，每一行数据映射为 {@code Map<String, Object>}，返回结果列表
     *
     * @param sql    SQL
     * @param params 参数列表
     */
    public List<Map<String, Object>> queryList(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryList(conn, sql, params, RowMapper.HASH_MAP_MAPPER);
        }
    }

    /**
     * 执行查询，每一行数据映射为 {@link DbRecord}，返回结果列表
     *
     * @param sql    SQL
     * @param params 参数列表
     */
    public List<DbRecord> queryRecordList(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryList(conn, sql, params, RowMapper.RECORD_MAPPER);
        }
    }

    /**
     * 执行查询，将查询结果的每一行数据按照指定逻辑进行处理，返回结果列表
     *
     * @param sql       SQL
     * @param rowMapper {@link ResultSet} 中每一行的数据的处理逻辑
     */
    public <T> List<T> queryList(String sql, RowMapper<T> rowMapper)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryList(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
        }
    }

    /**
     * 执行查询，返回结果映射为指定的类型。当结果为单列时使用
     *
     * @param sql   SQL
     * @param clazz 将结果映射为指定的类型
     */
    public <T> List<T> queryList(String sql, Class<T> clazz)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryList(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, clazz);
        }
    }

    /**
     * 执行查询，每一行数据映射为 {@code Map<String, Object>}，返回结果列表
     *
     * @param sql SQL
     */
    public List<Map<String, Object>> queryList(String sql)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryList(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, RowMapper.HASH_MAP_MAPPER);
        }
    }

    /**
     * 执行查询，每一行数据映射为 {@link DbRecord}，返回结果列表
     *
     * @param sql SQL
     */
    public List<DbRecord> queryRecordList(String sql)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryList(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, RowMapper.RECORD_MAPPER);
        }
    }

    // #endregion

    // #region - queryFirst

    /**
     * 执行查询，将查询结果的第一行数据按照指定逻辑进行处理，返回 {@link Optional}
     *
     * @param sql       SQL
     * @param params    参数
     * @param rowMapper {@link ResultSet} 中每一行的数据的处理逻辑
     */
    public <T> Optional<T> queryFirst(String sql, Object[] params, RowMapper<T> rowMapper)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirst(conn, sql, params, rowMapper);
        }
    }

    /**
     * 查询第一行第一列，并转换为指定类型
     *
     * @param <T>    目标类型
     * @param sql    SQL
     * @param params 参数
     * @param clazz  目标类型
     */
    public <T> Optional<T> queryFirst(String sql, Object[] params, Class<T> clazz)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirst(conn, sql, params, clazz);
        }
    }

    /**
     * 执行查询，将第一行数据转为 Map<String, Object>
     *
     * @param sql    SQL
     * @param params 参数
     */
    public Optional<Map<String, Object>> queryFirst(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirst(conn, sql, params, RowMapper.HASH_MAP_MAPPER);
        }
    }

    /**
     * 执行查询，将第一行数据转为 DbRecord
     *
     * @param sql    SQL
     * @param params 参数
     */
    public Optional<DbRecord> queryFirstRecord(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirst(conn, sql, params, RowMapper.RECORD_MAPPER);
        }
    }

    /**
     * 查询第一行第一列，并转换为字符串
     *
     * @param sql    SQL
     * @param params 参数
     */
    public Optional<String> queryFirstString(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirstString(conn, sql, params);
        }
    }

    /**
     * 查询第一行第一列，并转换为整数值
     *
     * @param sql    SQL
     * @param params 参数
     */
    public OptionalInt queryFirstInt(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirstInt(conn, sql, params);
        }
    }

    /**
     * 查询第一行第一列，并转换为长整型
     *
     * @param sql    SQL
     * @param params 参数
     */
    public OptionalLong queryFirstLong(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirstLong(conn, sql, params);
        }
    }

    /**
     * 查询第一行第一列，并转换为双精度浮点型
     *
     * @param sql    SQL
     * @param params 参数
     */
    public OptionalDouble queryFirstDouble(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirstDouble(conn, sql, params);
        }
    }

    /**
     * 查询第一行第一列，并转换为 {@link BigDecimal}
     *
     * @param sql    SQL
     * @param params 参数
     */
    public Optional<BigDecimal> queryFirstBigDecimal(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirstBigDecimal(conn, sql, params);
        }
    }

    /**
     * 执行查询，将查询结果的第一行数据按照指定逻辑进行处理，返回 {@link Optional}
     *
     * @param sql SQL
     * @param rowMapper {@link ResultSet} 中每一行的数据的处理逻辑
     */
    public <T> Optional<T> queryFirst(String sql, RowMapper<T> rowMapper)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirst(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
        }
    }

    /**
     * 查询第一行第一列，并转换为指定类型
     *
     * @param <T>    目标类型
     * @param sql    SQL
     * @param clazz  目标类型
     */
    public <T> Optional<T> queryFirst(String sql, Class<T> clazz)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirst(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, clazz);
        }
    }

    /**
     * 执行查询，将第一行数据转为 Map<String, Object>
     *
     * @param sql    SQL
     */
    public Optional<Map<String, Object>> queryFirst(String sql)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirst(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, RowMapper.HASH_MAP_MAPPER);
        }
    }

    /**
     * 执行查询，将第一行数据转为 DbRecord
     *
     * @param sql    SQL
     */
    public Optional<DbRecord> queryFirstRecord(String sql)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirst(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, RowMapper.RECORD_MAPPER);
        }
    }

    /**
     * 查询第一行第一列，并转换为字符串
     *
     * @param sql    SQL
     */
    public Optional<String> queryFirstString(String sql)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirstString(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }
    }

    /**
     * 查询第一行第一列，并转换为整数值
     *
     * @param sql    SQL
     */
    public OptionalInt queryFirstInt(String sql)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirstInt(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }
    }

    /**
     * 查询第一行第一列，并转换为长整型
     *
     * @param sql    SQL
     */
    public OptionalLong queryFirstLong(String sql)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirstLong(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }
    }

    /**
     * 查询第一行第一列，并转换为双精度浮点型
     *
     * @param sql    SQL
     */
    public OptionalDouble queryFirstDouble(String sql)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirstDouble(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }
    }

    /**
     * 查询第一行第一列，并转换为 {@link BigDecimal}
     *
     * @param sql    SQL
     */
    public Optional<BigDecimal> queryFirstBigDecimal(String sql)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirstBigDecimal(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }
    }

    // #endregion

    // #region - update & batchUpdate

    /**
     * 执行更新操作
     *
     * @param sql    要执行的 SQL
     * @param params 参数
     * @return 更新记录数
     */
    public int update(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.update(conn, sql, params);
        }
    }

    /**
     * 执行更新操作
     *
     * @param sql 要执行的 SQL
     * @return 更新记录数
     */
    public int update(String sql)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.update(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }
    }

    /**
     * 执行 SQL 并返回生成的 keys
     *
     * @param sql       要执行的 SQL
     * @param params    参数
     * @param rowMapper 行数据映射逻辑
     *
     * @return generated keys
     * @throws SQLException 执行 SQL 遇到异常情况将抛出
     */
    public <T> List<T> update(String sql, Object[] params, RowMapper<T> rowMapper)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.update(conn, sql, params, rowMapper);
        }
    }

    /**
     * 执行 SQL 并返回生成的 keys
     *
     * @param sql       要执行的 SQL
     * @param rowMapper 行数据映射逻辑
     *
     * @return generated keys
     * @throws SQLException 执行 SQL 遇到异常情况将抛出
     */
    public <T> List<T> update(String sql, RowMapper<T> rowMapper)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.update(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
        }
    }

    /**
     * 执行批量更新，批量更新数据，返回每条记录更新的行数
     *
     * @param sql       SQL 语句
     * @param params    参数列表
     * @param batchSize 每次批量更新的数据量
     */
    public List<int[]> batchUpdate(String sql, @Nullable Collection<Object[]> params, int batchSize)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.batchUpdate(conn, sql, params, batchSize);
        }
    }

    /**
     * 批量更新，返回更新成功的记录行数。发生异常时不中断操作，将异常存入 {@code exceptions} 中
     *
     * @param sql        sql语句
     * @param params     参数列表
     * @param batchSize  每次批量更新的数据量
     * @param exceptions 异常列表，用于记录异常信息
     */
    public List<int[]> batchUpdateAndIgnoreException(String sql, @Nullable Collection<Object[]> params,
            int batchSize, List<Exception> exceptions)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.batchUpdateAndIgnoreException(conn, sql, params, batchSize, exceptions);
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
    public <E extends Exception> void executeTransaction(@Nonnull final ThrowingConsumer<JdbcExecutor, E> operations)
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
    public <E extends Exception> void commitIfTrue(@Nonnull final ThrowingPredicate<JdbcExecutor, E> operations)
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

    public static final class JdbcExecutor {

        private final Connection conn;

        private JdbcExecutor(Connection conn) {
            this.conn = conn;
        }

        // #region - query

        /**
         * 执行查询，并按照自定义处理逻辑对结果进行处理，将结果转换为指定类型并返回
         *
         * @param sql           SQL
         * @param params        参数
         * @param resultHandler 结果处理器，用于处理 {@link ResultSet}
         */
        public <T> T query(String sql, Object[] params, ResultHandler<T> resultHandler)
                throws SQLException {
            return JdbcExecutor.query(this.conn, sql, params, resultHandler);
        }

        /**
         * 执行查询，并按照自定义处理逻辑对结果进行处理，将结果转换为指定类型并返回
         *
         * @param sql           SQL
         * @param resultHandler 结果处理器，用于处理 {@link ResultSet}
         */
        public <T> T query(String sql, ResultHandler<T> resultHandler)
                throws SQLException {
            return JdbcExecutor.query(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, resultHandler);
        }

        // #endregion

        // #region - queryList

        /**
         * 执行查询，将查询结果的每一行数据按照指定逻辑进行处理，返回结果列表
         *
         * @param sql       SQL
         * @param params    参数
         * @param rowMapper {@link ResultSet} 中每一行的数据的处理逻辑
         */
        public <T> List<T> queryList(String sql, Object[] params, RowMapper<T> rowMapper)
                throws SQLException {
            return JdbcExecutor.queryList(this.conn, sql, params, rowMapper);
        }

        /**
         * 执行查询，返回结果映射为指定的类型。当结果为单列时使用
         *
         * @param sql    SQL
         * @param params 参数
         * @param clazz  将结果映射为指定的类型
         */
        public <T> List<T> queryList(String sql, Object[] params, Class<T> clazz)
                throws SQLException {
            return JdbcExecutor.queryList(this.conn, sql, params, clazz);
        }

        /**
         * 执行查询，每一行数据映射为 {@code Map<String, Object>}，返回结果列表
         *
         * @param sql    SQL
         * @param params 参数列表
         */
        public List<Map<String, Object>> queryList(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryList(this.conn, sql, params, RowMapper.HASH_MAP_MAPPER);
        }

        /**
         * 执行查询，每一行数据映射为 {@link DbRecord}，返回结果列表
         *
         * @param sql    SQL
         * @param params 参数列表
         */
        public List<DbRecord> queryRecordList(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryList(this.conn, sql, params, RowMapper.RECORD_MAPPER);
        }

        /**
         * 执行查询，将查询结果的每一行数据按照指定逻辑进行处理，返回结果列表
         *
         * @param sql       SQL
         * @param rowMapper {@link ResultSet} 中每一行的数据的处理逻辑
         */
        public <T> List<T> queryList(String sql, RowMapper<T> rowMapper)
                throws SQLException {
            return JdbcExecutor.queryList(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
        }

        /**
         * 执行查询，返回结果映射为指定的类型。当结果为单列时使用
         *
         * @param sql   SQL
         * @param clazz 将结果映射为指定的类型
         */
        public <T> List<T> queryList(String sql, Class<T> clazz)
                throws SQLException {
            return JdbcExecutor.queryList(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, clazz);
        }

        /**
         * 执行查询，每一行数据映射为 {@code Map<String, Object>}，返回结果列表
         *
         * @param sql SQL
         */
        public List<Map<String, Object>> queryList(String sql)
                throws SQLException {
            return JdbcExecutor.queryList(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, RowMapper.HASH_MAP_MAPPER);
        }

        /**
         * 执行查询，每一行数据映射为 {@link DbRecord}，返回结果列表
         *
         * @param sql SQL
         */
        public List<DbRecord> queryRecordList(String sql)
                throws SQLException {
            return JdbcExecutor.queryList(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, RowMapper.RECORD_MAPPER);
        }

        // #endregion

        // #region - queryFirst

        /**
         * 执行查询，将查询结果的第一行数据按照指定逻辑进行处理，返回 {@link Optional}
         *
         * @param sql SQL
         * @param params 参数
         * @param rowMapper {@link ResultSet} 中每一行的数据的处理逻辑
         */
        public <T> Optional<T> queryFirst(String sql, Object[] params, RowMapper<T> rowMapper)
                throws SQLException {
            return JdbcExecutor.queryFirst(this.conn, sql, params, rowMapper);
        }

        /**
         * 查询第一行第一列，并转换为指定类型
         *
         * @param <T>    目标类型
         * @param sql    SQL
         * @param params 参数
         * @param clazz  目标类型
         */
        public <T> Optional<T> queryFirst(String sql, Object[] params, Class<T> clazz)
                throws SQLException {
            return JdbcExecutor.queryFirst(this.conn, sql, params, clazz);
        }

        /**
         * 执行查询，将第一行数据转为 Map<String, Object>
         *
         * @param sql    SQL
         * @param params 参数
         */
        public Optional<Map<String, Object>> queryFirst(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryFirst(this.conn, sql, params, RowMapper.HASH_MAP_MAPPER);
        }

        /**
         * 执行查询，将第一行数据转为 DbRecord
         *
         * @param sql    SQL
         * @param params 参数
         */
        public Optional<DbRecord> queryFirstRecord(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryFirst(this.conn, sql, params, RowMapper.RECORD_MAPPER);
        }

        /**
         * 查询第一行第一列，并转换为字符串
         *
         * @param sql    SQL
         * @param params 参数
         */
        public Optional<String> queryFirstString(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryFirstString(this.conn, sql, params);
        }

        /**
         * 查询第一行第一列，并转换为整数值
         *
         * @param sql    SQL
         * @param params 参数
         */
        public OptionalInt queryFirstInt(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryFirstInt(this.conn, sql, params);
        }

        /**
         * 查询第一行第一列，并转换为长整型
         *
         * @param sql    SQL
         * @param params 参数
         */
        public OptionalLong queryFirstLong(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryFirstLong(this.conn, sql, params);
        }

        /**
         * 查询第一行第一列，并转换为双精度浮点型
         *
         * @param sql    SQL
         * @param params 参数
         */
        public OptionalDouble queryFirstDouble(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryFirstDouble(this.conn, sql, params);
        }

        /**
         * 查询第一行第一列，并转换为 {@link BigDecimal}
         *
         * @param sql    SQL
         * @param params 参数
         */
        public Optional<BigDecimal> queryFirstBigDecimal(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryFirstBigDecimal(this.conn, sql, params);
        }

        /**
         * 执行查询，将查询结果的第一行数据按照指定逻辑进行处理，返回 {@link Optional}
         *
         * @param sql SQL
         * @param rowMapper {@link ResultSet} 中每一行的数据的处理逻辑
         */
        public <T> Optional<T> queryFirst(String sql, RowMapper<T> rowMapper)
                throws SQLException {
            return JdbcExecutor.queryFirst(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
        }

        /**
         * 查询第一行第一列，并转换为指定类型
         *
         * @param <T>    目标类型
         * @param sql    SQL
         * @param clazz  目标类型
         */
        public <T> Optional<T> queryFirst(String sql, Class<T> clazz)
                throws SQLException {
            return JdbcExecutor.queryFirst(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, clazz);
        }

        /**
         * 执行查询，将第一行数据转为 Map<String, Object>
         *
         * @param sql    SQL
         */
        public Optional<Map<String, Object>> queryFirst(String sql)
                throws SQLException {
            return JdbcExecutor.queryFirst(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, RowMapper.HASH_MAP_MAPPER);
        }

        /**
         * 执行查询，将第一行数据转为 DbRecord
         *
         * @param sql    SQL
         */
        public Optional<DbRecord> queryFirstRecord(String sql)
                throws SQLException {
            return JdbcExecutor.queryFirst(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, RowMapper.RECORD_MAPPER);
        }

        /**
         * 查询第一行第一列，并转换为字符串
         *
         * @param sql    SQL
         */
        public Optional<String> queryFirstString(String sql)
                throws SQLException {
            return JdbcExecutor.queryFirstString(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }

        /**
         * 查询第一行第一列，并转换为整数值
         *
         * @param sql    SQL
         */
        public OptionalInt queryFirstInt(String sql)
                throws SQLException {
            return JdbcExecutor.queryFirstInt(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }

        /**
         * 查询第一行第一列，并转换为长整型
         *
         * @param sql    SQL
         */
        public OptionalLong queryFirstLong(String sql)
                throws SQLException {
            return JdbcExecutor.queryFirstLong(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }

        /**
         * 查询第一行第一列，并转换为双精度浮点型
         *
         * @param sql    SQL
         */
        public OptionalDouble queryFirstDouble(String sql)
                throws SQLException {
            return JdbcExecutor.queryFirstDouble(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }

        /**
         * 查询第一行第一列，并转换为 {@link BigDecimal}
         *
         * @param sql    SQL
         */
        public Optional<BigDecimal> queryFirstBigDecimal(String sql)
                throws SQLException {
            return JdbcExecutor.queryFirstBigDecimal(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }

        // #endregion

        // #region - update & batchUpdate

        /**
         * 执行更新操作
         *
         * @param sql    要执行的 SQL
         * @param params 参数
         * @return 更新记录数
         */
        public int update(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.update(this.conn, sql, params);
        }

        /**
         * 执行更新操作
         *
         * @param sql 要执行的 SQL
         * @return 更新记录数
         */
        public int update(String sql)
                throws SQLException {
            return JdbcExecutor.update(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
        }

        /**
         * 执行 SQL 并返回生成的 keys
         *
         * @param sql       要执行的 SQL
         * @param params    参数
         * @param rowMapper 行数据映射逻辑
         *
         * @return generated keys
         * @throws SQLException 执行 SQL 遇到异常情况将抛出
         */
        public <T> List<T> update(String sql, Object[] params, RowMapper<T> rowMapper)
                throws SQLException {
            return JdbcExecutor.update(this.conn, sql, params, rowMapper);
        }

        /**
         * 执行 SQL 并返回生成的 keys
         *
         * @param sql       要执行的 SQL
         * @param rowMapper 行数据映射逻辑
         *
         * @return generated keys
         * @throws SQLException 执行 SQL 遇到异常情况将抛出
         */
        public <T> List<T> update(String sql, RowMapper<T> rowMapper)
                throws SQLException {
            return JdbcExecutor.update(this.conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
        }

        /**
         * 执行批量更新，批量更新数据，返回每条记录更新的行数
         *
         * @param sql       SQL 语句
         * @param params    参数列表
         * @param batchSize 每次批量更新的数据量
         */
        public List<int[]> batchUpdate(String sql, @Nullable Collection<Object[]> params, int batchSize)
                throws SQLException {
            return JdbcExecutor.batchUpdate(this.conn, sql, params, batchSize);
        }

        /**
         * 批量更新，返回更新成功的记录行数。发生异常时不中断操作，将异常存入 {@code exceptions} 中
         *
         * @param sql        sql语句
         * @param params     参数列表
         * @param batchSize  每次批量更新的数据量
         * @param exceptions 异常列表，用于记录异常信息
         */
        public List<int[]> batchUpdateAndIgnoreException(String sql, @Nullable Collection<Object[]> params,
                int batchSize, List<Exception> exceptions)
                throws SQLException {
            return JdbcExecutor.batchUpdateAndIgnoreException(this.conn, sql, params, batchSize, exceptions);
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
         * 执行查询，将查询结果的第一行数据按照指定逻辑进行处理，返回 {@link Optional}
         *
         * @param conn      数据库连接
         * @param sql       SQL
         * @param params    参数
         * @param rowMapper 行数据映射逻辑
         */
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

        /**
         * 执行查询，并按照自定义处理逻辑对结果进行处理，将结果转换为指定类型并返回
         *
         * @param conn          数据库连接
         * @param sql           SQL
         * @param params        参数
         * @param resultHandler 结果处理器，用于处理 {@link ResultSet}
         */
        private static <T> T query(Connection conn, String sql, Object[] params, ResultHandler<T> resultHandler)
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
        private static <T> List<T> queryList(Connection conn, String sql, Object[] params, RowMapper<T> rowMapper)
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
        private static <T> List<T> queryList(Connection conn, String sql, Object[] params, Class<T> clazz)
                throws SQLException {
            assertConnectionNotNull(conn);
            assertSqlNotNull(sql);
            assertClazzNotNull(clazz);
            return queryListInternal(conn, sql, params, (rs, rowNumber) -> rs.getObject(1, clazz));
        }

        // #endregion

        // #region - queryFirst

        /**
         * 执行查询，将查询结果的第一行数据按照指定逻辑进行处理，返回 {@link Optional}
         *
         * @param conn      数据库连接
         * @param sql       SQL
         * @param params    参数
         * @param rowMapper {@link ResultSet} 中每一行的数据的处理逻辑
         */
        private static <T> Optional<T> queryFirst(Connection conn, String sql, Object[] params, RowMapper<T> rowMapper)
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
        private static <T> Optional<T> queryFirst(Connection conn, String sql, Object[] params, Class<T> clazz)
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
        private static Optional<String> queryFirstString(Connection conn, String sql, Object[] params)
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
        private static OptionalInt queryFirstInt(Connection conn, String sql, Object[] params)
                throws SQLException {
            Optional<Integer> result = queryFirst(conn, sql, params, (rs, rowNumber) -> rs.getInt(1));
            return OptionalTools.toOptionalInt(result);
        }

        /**
         * 查询第一行第一列，并转换为长整型
         *
         * @param conn   数据库连接
         * @param sql    SQL
         * @param params 参数
         */
        private static OptionalLong queryFirstLong(Connection conn, String sql, Object[] params)
                throws SQLException {
            Optional<Long> result = queryFirst(conn, sql, params, (rs, rowNumber) -> rs.getLong(1));
            return OptionalTools.toOptionalLong(result);
        }

        /**
         * 查询第一行第一列，并转换为双精度浮点型
         *
         * @param conn   数据库连接
         * @param sql    SQL
         * @param params 参数
         */
        private static OptionalDouble queryFirstDouble(Connection conn, String sql, Object[] params)
                throws SQLException {
            Optional<Double> result = queryFirst(conn, sql, params, (rs, rowNumber) -> rs.getDouble(1));
            return OptionalTools.toOptionalDouble(result);
        }

        /**
         * 查询第一行第一列，并转换为 {@link BigDecimal}
         *
         * @param conn   数据库连接
         * @param sql    SQL
         * @param params 参数
         */
        private static Optional<BigDecimal> queryFirstBigDecimal(Connection conn, String sql, Object[] params)
                throws SQLException {
            return queryFirst(conn, sql, params, (rs, rowNumber) -> rs.getBigDecimal(1));
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

        /**
         * 执行批量更新，批量更新数据，返回每条记录更新的行数
         *
         * @param conn      数据库连接
         * @param sql       SQL 语句
         * @param params    参数列表
         * @param batchSize 每次批量更新的数据量
         */
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

        /**
         * 批量更新，返回更新成功的记录行数。发生异常时不中断操作，将异常存入 {@code exceptions} 中
         *
         * @param conn       数据库连接
         * @param sql        sql语句
         * @param params     参数列表
         * @param batchSize  每次批量更新的数据量
         * @param exceptions 异常列表，用于记录异常信息
         */
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
