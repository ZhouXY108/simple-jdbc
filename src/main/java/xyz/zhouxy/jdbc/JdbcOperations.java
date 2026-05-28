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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nullable;

/**
 * JdbcOperations
 *
 * <p>
 * 定义 JdbcTemplate 的 API
 * </p>
 *
 * @author ZhouXY
 * @since 1.0.0
 */
public interface JdbcOperations {

    // #region - query

    /**
     * 执行查询，并按照自定义处理逻辑对结果进行处理，将结果转换为指定类型并返回
     *
     * @param <T>           返回结果类型
     * @param sql           SQL
     * @param params        参数
     * @param resultHandler 结果处理器，用于处理 {@link ResultSet}
     *
     * @return 查询结果
     * @throws SQLException SQL异常
     */
    <T> T query(String sql, Object[] params, ResultHandler<T> resultHandler)
            throws SQLException;

    /**
     * 执行查询，并按照自定义处理逻辑对结果进行处理，将结果转换为指定类型并返回
     *
     * @param <T>           返回结果类型
     * @param sql           SQL
     * @param resultHandler 结果处理器，用于处理 {@link ResultSet}
     *
     * @return 查询结果
     * @throws SQLException SQL异常
     */
    default <T> T query(String sql, ResultHandler<T> resultHandler)
            throws SQLException {
        return query(sql, ParamBuilder.EMPTY_OBJECT_ARRAY, resultHandler);
    }

    // #endregion

    // #region - queryList

    /**
     * 执行查询，将查询结果的每一行数据按照指定逻辑进行处理，返回结果列表
     *
     * @param <T>       返回结果列表中每一行数据的类型
     * @param sql       SQL
     * @param params    参数
     * @param rowMapper {@link ResultSet} 中每一行的数据的处理逻辑
     *
     * @return 查询结果列表
     * @throws SQLException SQL 异常
     */
    <T> List<T> queryList(String sql, Object[] params, RowMapper<T> rowMapper)
            throws SQLException;

    /**
     * 执行查询，返回结果映射为指定的类型。当结果为单列时使用
     *
     * @param <T>    目标类型
     * @param sql    SQL
     * @param params 参数
     * @param clazz  目标类型
     *
     * @return 映射结果。如果查询结果为空，则返回空列表
     * @throws SQLException SQL异常
     */
    <T> List<T> queryList(String sql, Object[] params, Class<T> clazz)
            throws SQLException;

    /**
     * 执行查询，每一行数据映射为 {@code Map<String, Object>}，返回结果列表
     *
     * @param sql    SQL
     * @param params 参数列表
     *
     * @return 结果列表
     * @throws SQLException SQL 异常
     */
    List<Map<String, Object>> queryList(String sql, Object[] params)
            throws SQLException;

    /**
     * 执行查询，将查询结果的每一行数据按照指定逻辑进行处理，返回结果列表
     *
     * @param <T>       目标类型
     * @param sql       SQL
     * @param rowMapper {@link ResultSet} 中每一行的数据的处理逻辑
     *
     * @return 结果列表
     * @throws SQLException SQL 异常
     */
    default <T> List<T> queryList(String sql, RowMapper<T> rowMapper)
            throws SQLException {
        return queryList(sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
    }

    /**
     * 执行查询，返回结果映射为指定的类型。当结果为单列时使用
     *
     * @param <T>   目标类型
     * @param sql   SQL
     * @param clazz 将结果映射为指定的类型
     *
     * @return 查询结果
     * @throws SQLException SQL 异常
     */
    default <T> List<T> queryList(String sql, Class<T> clazz)
            throws SQLException {
        return queryList(sql, ParamBuilder.EMPTY_OBJECT_ARRAY, clazz);
    }

    /**
     * 执行查询，每一行数据映射为 {@code Map<String, Object>}，返回结果列表
     *
     * @param sql SQL
     *
     * @return 结果列表
     * @throws SQLException SQL 异常
     */
    default List<Map<String, Object>> queryList(String sql)
            throws SQLException {
        return queryList(sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
    }

    // #endregion

    // #region - queryFirst

    /**
     * 执行查询，将查询结果的第一行数据按照指定逻辑进行处理，返回 {@link Optional}
     *
     * @param <T>       目标类型
     * @param sql       SQL
     * @param params    参数
     * @param rowMapper {@link ResultSet} 中每一行的数据的处理逻辑
     *
     * @return 查询结果
     * @throws SQLException SQL 异常
     */
    <T> Optional<T> queryFirst(String sql, Object[] params, RowMapper<T> rowMapper)
            throws SQLException;

    /**
     * 查询第一行第一列，并转换为指定类型
     *
     * @param <T>    目标类型
     * @param sql    SQL
     * @param params 参数
     * @param clazz  目标类型
     *
     * @return 查询结果
     * @throws SQLException SQL 异常
     */
    <T> Optional<T> queryFirst(String sql, Object[] params, Class<T> clazz)
            throws SQLException;

    /**
     * 执行查询，将第一行数据转为 Map&lt;String, Object&gt;
     *
     * @param sql    SQL
     * @param params 参数
     *
     * @return 查询结果
     * @throws SQLException SQL 异常
     */
    Optional<Map<String, Object>> queryFirst(String sql, Object[] params)
            throws SQLException;

    /**
     * 执行查询，将查询结果的第一行数据按照指定逻辑进行处理，返回 {@link Optional}
     *
     * @param <T>       目标类型
     * @param sql       SQL
     * @param rowMapper {@link ResultSet} 中每一行的数据的处理逻辑
     *
     * @return 查询结果
     * @throws SQLException SQL 异常
     */
    default <T> Optional<T> queryFirst(String sql, RowMapper<T> rowMapper)
            throws SQLException {
        return queryFirst(sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
    }

    /**
     * 查询第一行第一列，并转换为指定类型
     *
     * @param <T>   目标类型
     * @param sql   SQL
     * @param clazz 目标类型
     *
     * @return 第一行第一列的值，如果查询结果为空，则返回 {@code Optional#empty()}
     * @throws SQLException SQL 异常
     */
    default <T> Optional<T> queryFirst(String sql, Class<T> clazz)
            throws SQLException {
        return queryFirst(sql, ParamBuilder.EMPTY_OBJECT_ARRAY, clazz);
    }

    /**
     * 执行查询，将第一行数据转为 Map&lt;String, Object&gt;
     *
     * @param sql SQL
     *
     * @return 查询结果
     * @throws SQLException SQL 异常
     */
    default Optional<Map<String, Object>> queryFirst(String sql)
            throws SQLException {
        return queryFirst(sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
    }

    /**
     * 查询第一行第一列并转换为 boolean
     *
     * @param sql SQL
     *
     * @return 查询结果。如果查询结果为空，则返回 {@code false}。
     * @throws SQLException SQL 异常
     */
    default boolean queryBoolean(String sql)
            throws SQLException {
        return queryBoolean(sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
    }

    /**
     * 查询第一行第一列并转换为 boolean
     *
     * @param sql SQL
     * @param params 参数
     *
     * @return 查询结果。如果查询结果为空，则返回 {@code false}
     *
     * @throws SQLException SQL 异常
     */
    boolean queryBoolean(String sql, Object[] params)
            throws SQLException;

    // #endregion

    // #region - update & batchUpdate

    /**
     * 执行更新操作
     *
     * @param sql    要执行的 SQL
     * @param params 参数
     * @return 更新记录数
     * @throws SQLException SQL 异常
     */
    int update(String sql, Object[] params)
            throws SQLException;

    /**
     * 执行更新操作
     *
     * @param sql 要执行的 SQL
     * @return 更新记录数
     *
     * @throws SQLException 数据库执行异常
     */
    default int update(String sql)
            throws SQLException {
        return update(sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
    }

    /**
     * 执行 SQL 并返回生成的 keys
     *
     * @param sql       要执行的 SQL
     * @param params    参数
     * @param rowMapper 行数据映射逻辑
     *
     * @param <T>       映射结果类型
     *
     * @return generated keys
     * @throws SQLException 数据库执行异常
     */
    <T> List<T> updateAndReturnKeys(String sql, Object[] params, RowMapper<T> rowMapper)
            throws SQLException;

    /**
     * 执行 SQL 并返回生成的 keys
     *
     * @param sql       要执行的 SQL
     * @param rowMapper 行数据映射逻辑
     *
     * @param <T>       映射结果类型
     *
     * @return generated keys
     * @throws SQLException 数据库执行异常
     */
    default <T> List<T> updateAndReturnKeys(String sql, RowMapper<T> rowMapper)
            throws SQLException {
        return updateAndReturnKeys(sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
    }

    /**
     * 批量更新
     *
     * <p>
     * 跑批过程中发生异常即中断操作，并返回结果。
     *
     * <p>
     * 当无法获取所更新的行数时，对应位置的更新行数将被设置为 {@link JdbcOperationSupport#UNKNOWN_COUNT}。
     *
     * @param sql       SQL 语句
     * @param params    参数列表
     * @param batchSize 每次批量更新的数据量
     *
     * @return 批量更新的结果
     * @throws SQLException SQL 异常
     */
    BatchUpdateResult batchUpdate(String sql, @Nullable Collection<Object[]> params, int batchSize)
            throws SQLException;

    /**
     * 批量更新
     *
     * <p>
     * 当无法获取所更新的行数时，对应位置的更新行数将被设置为 {@link JdbcOperationSupport#UNKNOWN_COUNT}。
     *
     * @param sql        sql语句
     * @param params     参数列表
     * @param batchSize  每次批量更新的数据量
     * @param quietly    静默分批更新。
     *                   如果 {@code quietly} 为 {@code true}，分批更新过程中发生异常不中断操作；
     *                   如果 {@code quietly} 为 {@code false}，分批更新过程中发生异常即中断操作，并返回结果。
     *
     * @return 批次更新结果
     * @throws SQLException 数据库执行异常
     */
    BatchUpdateResult batchUpdate(String sql, @Nullable Collection<Object[]> params,
                            int batchSize, boolean quietly)
            throws SQLException;

    // #endregion
}
