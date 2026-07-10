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

import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * JDBC 位置参数操作接口，使用 {@code ?} 占位符的数据库操作方法集合。
 *
 * <p>方法按功能分为以下几组：</p>
 * <ul>
 *   <li><b>通用查询</b> — {@code query(sql, params, resultHandler)}：
 *       通过 {@link ResultHandler} 自定义结果处理</li>
 *   <li><b>列表查询</b> — {@code queryList / queryValues}：返回多行结果列表</li>
 *   <li><b>单结果查询</b> — {@code queryFirst / queryValue / queryBoolean / queryValueOrDefault}：
 *       返回单行结果或标量值，部分以 {@link Optional} 包装</li>
 *   <li><b>更新操作</b> — {@code update / updateAndReturnKeys / batchUpdate}：执行增删改操作</li>
 * </ul>
 *
 * <p>
 * 每个方法均提供含 {@code params} 和不含 {@code params} 两种重载，
 * 不含 {@code params} 的版本为便捷重载，等价于以空参数数组调用含参版本。
 * </p>
 *
 * <p>
 * 本接口使用位置参数（{@code ?}），如需命名参数（{@code #{paramName}}）支持，
 * 参见 {@link xyz.zhouxy.jdbc.namedparam.NamedParamJdbcOperations}。
 * </p>
 *
 * @author ZhouXY
 * @since 1.0.0
 * @see RowMapper
 * @see ResultHandler
 * @see xyz.zhouxy.jdbc.namedparam.NamedParamJdbcOperations
 */
@NullMarked
public interface JdbcOperations {

    // #region - query

    /**
     * 执行查询，通过 {@link ResultHandler} 自定义处理结果。
     *
     * @param <T>           返回结果类型
     * @param sql           SQL 语句
     * @param params        参数数组
     * @param resultHandler 结果处理器
     *
     * @return 查询结果
     * @throws SQLException SQL 异常
     */
    <T extends @Nullable Object> T query(String sql, @Nullable Object @Nullable [] params,
            ResultHandler<T> resultHandler)
            throws SQLException;

    /**
     * 执行查询，通过 {@link ResultHandler} 自定义处理结果。
     *
     * @param <T>           返回结果类型
     * @param sql           SQL 语句
     * @param resultHandler 结果处理器
     *
     * @return 查询结果
     * @throws SQLException SQL 异常
     */
    default <T extends @Nullable Object> T query(String sql, ResultHandler<T> resultHandler)
            throws SQLException {
        return query(sql, ParamBuilder.EMPTY_OBJECT_ARRAY, resultHandler);
    }

    // #endregion

    // #region - queryList

    /**
     * 执行查询，通过 {@link RowMapper} 映射每一行，返回结果列表。
     *
     * @param <T>       结果元素类型
     * @param sql       SQL 语句
     * @param params    参数数组
     * @param rowMapper 行映射器
     *
     * @return 结果列表
     * @throws SQLException SQL 异常
     */
    <T extends @Nullable Object> List<T> queryList(String sql, @Nullable Object @Nullable [] params,
            RowMapper<T> rowMapper)
            throws SQLException;

    /**
     * 执行查询，提取每行第一列的值，返回结果列表。
     *
     * @param <T>    目标类型
     * @param sql    SQL 语句
     * @param params 参数数组
     * @param clazz  目标类型
     *
     * @return 结果列表
     * @throws SQLException SQL 异常
     */
    <T extends @Nullable Object> List<T> queryValues(String sql, @Nullable Object @Nullable [] params,
            Class<@NonNull T> clazz)
            throws SQLException;

    /**
     * 执行查询，每行转为 {@code Map<String, Object>}，返回结果列表。
     *
     * <p>自 1.1.0 起，默认实现使用 {@link java.util.LinkedHashMap}，保持列的查询顺序。</p>
     *
     * @param sql    SQL 语句
     * @param params 参数数组
     *
     * @return 结果列表
     * @throws SQLException SQL 异常
     */
    List<@Nullable Map<String, @Nullable Object>> queryList(String sql, @Nullable Object @Nullable [] params)
            throws SQLException;

    /**
     * 执行查询，通过 {@link RowMapper} 映射每一行，返回结果列表。
     *
     * @param <T>       结果元素类型
     * @param sql       SQL 语句
     * @param rowMapper 行映射器
     *
     * @return 结果列表
     * @throws SQLException SQL 异常
     */
    default <T extends @Nullable Object> List<T> queryList(String sql, RowMapper<T> rowMapper)
            throws SQLException {
        return queryList(sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
    }

    /**
     * 执行查询，提取每行第一列的值，返回结果列表。
     *
     * @param <T>   目标类型
     * @param sql   SQL 语句
     * @param clazz 目标类型
     *
     * @return 结果列表
     * @throws SQLException SQL 异常
     */
    default <T extends @Nullable Object> List<T> queryValues(String sql, Class<@NonNull T> clazz)
            throws SQLException {
        return queryValues(sql, ParamBuilder.EMPTY_OBJECT_ARRAY, clazz);
    }

    /**
     * 执行查询，提取每行第一列的值，返回结果列表。
     *
     * @param <T>    目标类型
     * @param sql    SQL 语句
     * @param params 参数数组
     * @param clazz  目标类型
     *
     * @return 结果列表
     * @throws SQLException SQL 异常
     *
     * @deprecated 自 1.1.0 起，请使用 {@link #queryValues(String, Object[], Class)}。
     *             此方法将在后续版本中移除。
     */
    @Deprecated
    default <T extends @Nullable Object> List<T> queryList(String sql, @Nullable Object @Nullable [] params,
            Class<@NonNull T> clazz)
            throws SQLException {
        return queryValues(sql, params, clazz);
    }

    /**
     * 执行查询，提取每行第一列的值，返回结果列表。
     *
     * @param <T>   目标类型
     * @param sql   SQL 语句
     * @param clazz 目标类型
     *
     * @return 结果列表
     * @throws SQLException SQL 异常
     *
     * @deprecated 自 1.1.0 起，请使用 {@link #queryValues(String, Class)}。
     *             此方法将在后续版本中移除。
     */
    @Deprecated
    default <T extends @Nullable Object> List<T> queryList(String sql, Class<@NonNull T> clazz)
            throws SQLException {
        return queryValues(sql, clazz);
    }

    /**
     * 执行查询，每行转为 {@code Map<String, Object>}，返回结果列表。
     *
     * <p>自 1.1.0 起，默认实现使用 {@link java.util.LinkedHashMap}，保持列的查询顺序。</p>
     *
     * @param sql SQL 语句
     *
     * @return 结果列表
     * @throws SQLException SQL 异常
     */
    default List<@Nullable Map<String, @Nullable Object>> queryList(String sql)
            throws SQLException {
        return queryList(sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
    }

    // #endregion

    // #region - queryFirst

    /**
     * 执行查询，通过 {@link RowMapper} 映射结果，返回第一行。
     *
     * @param <T>       结果类型
     * @param sql       SQL 语句
     * @param params    参数数组
     * @param rowMapper 行映射器
     *
     * @return 第一行结果，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    <T> Optional<T> queryFirst(String sql, @Nullable Object @Nullable [] params,
            RowMapper<T> rowMapper)
            throws SQLException;

    /**
     * 执行查询，返回第一行第一列的值。
     * 适用于 {@code SELECT single_column FROM ... WHERE ...} 单列单行查询场景。
     *
     * @param <T>    目标类型
     * @param sql    SQL 语句
     * @param params 参数数组
     * @param clazz  目标类型
     *
     * @return 第一行第一列的值，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    <T> Optional<T> queryValue(String sql, @Nullable Object @Nullable [] params,
            Class<T> clazz)
            throws SQLException;

    /**
     * 执行查询，将第一行转为 {@code Map<String, Object>}，返回结果。
     *
     * <p>自 1.1.0 起，默认实现使用 {@link java.util.LinkedHashMap}，保持列的查询顺序。</p>
     *
     * @param sql    SQL 语句
     * @param params 参数数组
     *
     * @return 第一行结果，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    Optional<Map<String, @Nullable Object>> queryFirst(
            String sql, @Nullable Object @Nullable [] params)
            throws SQLException;

    /**
     * 执行查询，通过 {@link RowMapper} 映射结果，返回第一行。
     *
     * @param <T>       结果类型
     * @param sql       SQL 语句
     * @param rowMapper 行映射器
     *
     * @return 第一行结果，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    default <T> Optional<T> queryFirst(String sql, RowMapper<T> rowMapper)
            throws SQLException {
        return queryFirst(sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
    }

    /**
     * 执行查询，返回第一行第一列的值。
     * 适用于 {@code SELECT single_column FROM ... WHERE ...} 单列单行查询场景。
     *
     * @param <T>   目标类型
     * @param sql   SQL 语句
     * @param clazz 目标类型
     *
     * @return 第一行第一列的值，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    default <T> Optional<T> queryValue(String sql, Class<T> clazz)
            throws SQLException {
        return queryValue(sql, ParamBuilder.EMPTY_OBJECT_ARRAY, clazz);
    }

    /**
     * 执行查询，返回第一行第一列的值。
     *
     * @param <T>    目标类型
     * @param sql    SQL 语句
     * @param params 参数数组
     * @param clazz  目标类型
     *
     * @return 第一行第一列的值，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     *
     * @deprecated 自 1.1.0 起，请使用 {@link #queryValue(String, Object[], Class)}。
     *             此方法将在后续版本中移除。
     */
    @Deprecated
    default <T> Optional<T> queryFirst(
            String sql, @Nullable Object @Nullable [] params,
            Class<T> clazz)
            throws SQLException {
        return queryValue(sql, params, clazz);
    }

    /**
     * 执行查询，返回第一行第一列的值。
     *
     * @param <T>   目标类型
     * @param sql   SQL 语句
     * @param clazz 目标类型
     *
     * @return 第一行第一列的值，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     *
     * @deprecated 自 1.1.0 起，请使用 {@link #queryValue(String, Class)}。
     *             此方法将在后续版本中移除。
     */
    @Deprecated
    default <T> Optional<T> queryFirst(String sql, Class<T> clazz)
            throws SQLException {
        return queryValue(sql, clazz);
    }

    /**
     * 执行查询，将第一行转为 {@code Map<String, Object>}，返回结果。
     *
     * <p>自 1.1.0 起，默认实现使用 {@link java.util.LinkedHashMap}，保持列的查询顺序。</p>
     *
     * @param sql SQL 语句
     *
     * @return 第一行结果，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    default Optional<Map<String, @Nullable Object>> queryFirst(String sql)
            throws SQLException {
        return queryFirst(sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
    }

    /**
     * 执行查询，返回第一行第一列的值；结果为空时返回 {@code defaultValue}。适用于聚合查询。
     *
     * @param <T>          目标类型
     * @param sql          SQL 语句
     * @param params       参数数组
     * @param clazz        目标类型
     * @param defaultValue 查询结果为空时返回的默认值
     *
     * @return 第一行第一列的值，如果查询结果为空则返回 {@code defaultValue}
     * @throws SQLException SQL 异常
     */
    default <T extends @Nullable Object> T queryValueOrDefault(
            String sql,
            @Nullable Object @Nullable [] params,
            Class<@NonNull T> clazz,
            T defaultValue)
            throws SQLException {
        return queryValue(sql, params, clazz).orElse(defaultValue);
    }

    /**
     * 执行查询，返回第一行第一列的值；结果为空时返回 {@code defaultValue}。适用于聚合查询。
     *
     * @param <T>          目标类型
     * @param sql          SQL 语句
     * @param clazz        目标类型
     * @param defaultValue 查询结果为空时返回的默认值
     *
     * @return 第一行第一列的值，如果查询结果为空则返回 {@code defaultValue}
     * @throws SQLException SQL 异常
     */
    default <T extends @Nullable Object> T queryValueOrDefault(String sql, Class<@NonNull T> clazz, T defaultValue)
            throws SQLException {
        return queryValueOrDefault(sql, ParamBuilder.EMPTY_OBJECT_ARRAY, clazz, defaultValue);
    }

    /**
     * 执行布尔查询，结果为空时返回 {@code false}。
     *
     * @param sql SQL 语句
     *
     * @return 查询结果。如果查询结果为空，则返回 {@code false}。
     * @throws SQLException SQL 异常
     */
    default boolean queryBoolean(String sql)
            throws SQLException {
        return queryBoolean(sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
    }

    /**
     * 执行布尔查询，结果为空时返回 {@code false}。
     *
     * @param sql    SQL 语句
     * @param params 参数数组
     *
     * @return 查询结果。如果查询结果为空，则返回 {@code false}。
     * @throws SQLException SQL 异常
     */
    boolean queryBoolean(String sql, @Nullable Object @Nullable [] params)
            throws SQLException;

    // #endregion

    // #region - update & batchUpdate

    /**
     * 执行更新操作，返回影响行数。
     *
     * @param sql    SQL 语句
     * @param params 参数数组
     *
     * @return 更新记录数
     * @throws SQLException SQL 异常
     */
    int update(String sql, @Nullable Object @Nullable [] params)
            throws SQLException;

    /**
     * 执行更新操作，返回影响行数。
     *
     * @param sql SQL 语句
     *
     * @return 更新记录数
     * @throws SQLException SQL 异常
     */
    default int update(String sql)
            throws SQLException {
        return update(sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
    }

    /**
     * 执行更新操作，通过 {@link RowMapper} 映射并返回生成的主键列表。
     *
     * @param <T>       主键类型
     * @param sql       SQL 语句
     * @param params    参数数组
     * @param rowMapper 主键行映射器
     *
     * @return 生成的主键列表
     * @throws SQLException SQL 异常
     */
    <T extends @Nullable Object> List<T> updateAndReturnKeys(
            String sql, @Nullable Object @Nullable [] params,
            RowMapper<T> rowMapper)
            throws SQLException;

    /**
     * 执行更新操作，通过 {@link RowMapper} 映射并返回生成的主键列表。
     *
     * @param <T>       主键类型
     * @param sql       SQL 语句
     * @param rowMapper 主键行映射器
     *
     * @return 生成的主键列表
     * @throws SQLException SQL 异常
     */
    default <T extends @Nullable Object> List<T> updateAndReturnKeys(String sql, RowMapper<T> rowMapper)
            throws SQLException {
        return updateAndReturnKeys(sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
    }

    /**
     * 执行批量更新，遇错中断。
     *
     * <p>
     * 当无法获取所更新的行数时，对应位置的更新行数将被设置为
     * {@link JdbcOperationSupport#UNKNOWN_COUNT}。
     * </p>
     *
     * @param sql       SQL 语句
     * @param params    参数列表
     * @param batchSize 每批数量
     *
     * @return 批量更新结果
     * @throws SQLException SQL 异常
     */
    BatchUpdateResult batchUpdate(
            String sql, @Nullable Collection<@Nullable Object @Nullable []> params,
            int batchSize)
            throws SQLException;

    /**
     * 执行批量更新。{@code quietly} 为 {@code true} 时遇错继续执行。
     *
     * <p>
     * 当无法获取所更新的行数时，对应位置的更新行数将被设置为
     * {@link JdbcOperationSupport#UNKNOWN_COUNT}。
     * </p>
     *
     * @param sql       SQL 语句
     * @param params    参数列表
     * @param batchSize 每批数量
     * @param quietly   如果为 {@code true}，遇错不中断，继续执行；
     *                  如果为 {@code false}，遇错立即中断
     *
     * @return 批量更新结果
     * @throws SQLException SQL 异常
     */
    BatchUpdateResult batchUpdate(
        String sql, @Nullable Collection<@Nullable Object @Nullable []> params,
        int batchSize, boolean quietly)
            throws SQLException;

    // #endregion
}
