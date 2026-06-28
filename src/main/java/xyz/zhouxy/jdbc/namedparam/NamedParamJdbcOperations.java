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
package xyz.zhouxy.jdbc.namedparam;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import xyz.zhouxy.jdbc.BatchUpdateResult;
import xyz.zhouxy.jdbc.JdbcOperations;
import xyz.zhouxy.jdbc.ResultHandler;
import xyz.zhouxy.jdbc.RowMapper;

/**
 * 命名参数 JDBC 操作接口，提供对 {@link JdbcOperations} 的命名参数扩展。
 *
 * <p>
 * 本接口不继承 {@link JdbcOperations}，而是通过 {@link #getJdbcOperations()}
 * 获取底层的 {@code JdbcOperations} 实例，default 方法委托给该实例执行。
 * 这样可以保持两个接口职责分离、互不污染。
 * </p>
 *
 * <p>
 * 每个方法提供两种重载形式：
 * </p>
 * <ol>
 *   <li><strong>便捷重载</strong>：接受 {@code (String sql, Map<String, ?> params, ...)}，
 *       内部解析 SQL 模板并提取参数值后委托给 {@link JdbcOperations}。</li>
 *   <li><strong>预构建重载</strong>：接受 {@link PreparedSql}，直接提取 SQL 与参数数组后委托。</li>
 * </ol>
 *
 * <p>
 * {@link SimpleJdbcTemplate} 同时实现了两个接口，直接使用即可：
 * </p>
 * <pre>{@code
 * SimpleJdbcTemplate tmpl = new SimpleJdbcTemplate(dataSource);
 * // 命名参数（Map 直传）
 * tmpl.update("UPDATE t SET x = #{val}", Map.of("val", 1));
 * // 命名参数（PreparedSql 预构建）
 * PreparedSql ps = NamedParamSql.of("UPDATE t SET x = #{val}")
 *     .prepare().param("val", 1).build();
 * tmpl.update(ps);
 * // 位置参数（继承自 JdbcOperations）
 * tmpl.update("UPDATE t SET x = ?", buildParams(1));
 * }</pre>
 *
 * <p>
 * 多态方式获取不同视图：
 * </p>
 * <pre>{@code
 * SimpleJdbcTemplate tmpl = new SimpleJdbcTemplate(dataSource);
 * JdbcOperations ops = tmpl;                       // 位置参数视图
 * NamedParamJdbcOperations nops = tmpl;            // 命名参数视图
 * }</pre>
 *
 * <p>
 * 事务中通过 {@link TransactionTemplate} 的回调重载直接获取：
 * </p>
 * <pre>{@code
 * // 纯命名参数
 * tx.executeNamed(nops -> {
 *     nops.update("UPDATE t SET name = #{name}", Map.of("name", "Alice"));
 * });
 *
 * // 混用两种参数风格
 * tx.execute((ops, nops) -> {
 *     ops.update("UPDATE t SET x = ?", new Object[]{1});
 *     nops.update("INSERT INTO t VALUES(#{a}, #{b})", Map.of("a", 1, "b", 2));
 * });
 * }</pre>
 *
 * @author ZhouXY
 * @since 1.1.0
 * @see JdbcOperations
 * @see NamedParamSql
 * @see PreparedSql
 */
public interface NamedParamJdbcOperations {

    /**
     * 获取底层的 {@link JdbcOperations} 实例。
     *
     * <p>
     * 实现类应返回 {@code this}（当自身同时实现了 {@code JdbcOperations} 时），
     * 或返回持有的 {@code JdbcOperations} 委托对象。
     * </p>
     *
     * @return JdbcOperations 实例
     */
    JdbcOperations getJdbcOperations();

    // #region - query

    /**
     * 使用命名参数执行查询，通过 {@link ResultHandler} 自定义结果处理。
     *
     * @param <T>           返回结果类型
     * @param sql           包含命名参数（格式：{@code #{paramName}}）的 SQL
     * @param params        命名参数映射
     * @param resultHandler 结果处理器
     * @return 查询结果
     * @throws SQLException SQL 异常
     */
    default <T extends @Nullable Object> T query(
            String sql,
            Map<String, ?> params,
            ResultHandler<T> resultHandler) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return getJdbcOperations().query(tmpl.getSql(), tmpl.toArgs(params), resultHandler);
    }

    /**
     * 使用预构建的 {@link PreparedSql} 执行查询。
     *
     * @param <T>           返回结果类型
     * @param ps            预构建的命名参数 SQL
     * @param resultHandler 结果处理器
     * @return 查询结果
     * @throws SQLException SQL 异常
     */
    default <T extends @Nullable Object> T query(
            PreparedSql ps,
            ResultHandler<T> resultHandler) throws SQLException {
        return getJdbcOperations().query(ps.getSql(), ps.getArgs(), resultHandler);
    }

    // #endregion

    // #region - queryList

    /**
     * 使用命名参数执行查询，通过 {@link RowMapper} 映射每一行。
     *
     * @param <T>       结果元素类型
     * @param sql       包含命名参数的 SQL
     * @param params    命名参数映射
     * @param rowMapper 行映射器
     * @return 结果列表
     * @throws SQLException SQL 异常
     */
    default <T extends @Nullable Object> List<T> queryList(
            String sql,
            Map<String, ?> params,
            RowMapper<T> rowMapper) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return getJdbcOperations().queryList(tmpl.getSql(), tmpl.toArgs(params), rowMapper);
    }

    /**
     * 使用预构建的 {@link PreparedSql} 执行查询。
     *
     * @param <T>       结果元素类型
     * @param ps        预构建的命名参数 SQL
     * @param rowMapper 行映射器
     * @return 结果列表
     * @throws SQLException SQL 异常
     */
    default <T extends @Nullable Object> List<T> queryList(
            PreparedSql ps,
            RowMapper<T> rowMapper) throws SQLException {
        return getJdbcOperations().queryList(ps.getSql(), ps.getArgs(), rowMapper);
    }

    /**
     * 使用命名参数执行查询，提取每行第一列的值。
     *
     * @param <T>    目标类型
     * @param sql    包含命名参数的 SQL
     * @param params 命名参数映射
     * @param clazz  目标类型
     * @return 每一行第一列的值列表
     * @throws SQLException SQL 异常
     */
    default <T extends @Nullable Object> List<T> queryValues(
            String sql,
            Map<String, ?> params,
            Class<T> clazz) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return getJdbcOperations().queryValues(tmpl.getSql(), tmpl.toArgs(params), clazz);
    }

    /**
     * 使用预构建的 {@link PreparedSql} 执行单列查询。
     *
     * @param <T>   目标类型
     * @param ps    预构建的命名参数 SQL
     * @param clazz 目标类型
     * @return 每一行第一列的值列表
     * @throws SQLException SQL 异常
     */
    default <T extends @Nullable Object> List<T> queryValues(
            PreparedSql ps,
            Class<T> clazz) throws SQLException {
        return getJdbcOperations().queryValues(ps.getSql(), ps.getArgs(), clazz);
    }

    /**
     * 使用命名参数执行查询，每行转为 {@code Map<String, Object>}。
     *
     * @param sql    包含命名参数的 SQL
     * @param params 命名参数映射
     * @return 结果列表
     * @throws SQLException SQL 异常
     */
    default List<@Nullable Map<String, @Nullable Object>> queryList(
            String sql,
            Map<String, ?> params) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return getJdbcOperations().queryList(tmpl.getSql(), tmpl.toArgs(params));
    }

    /**
     * 使用预构建的 {@link PreparedSql} 执行查询，每行转为 {@code Map<String, Object>}。
     *
     * @param ps 预构建的命名参数 SQL
     * @return 结果列表
     * @throws SQLException SQL 异常
     */
    default List<@Nullable Map<String, @Nullable Object>> queryList(
            PreparedSql ps) throws SQLException {
        return getJdbcOperations().queryList(ps.getSql(), ps.getArgs());
    }

    // #endregion

    // #region - queryFirst

    /**
     * 使用命名参数执行查询，返回第一行。
     *
     * @param <T>       结果类型
     * @param sql       包含命名参数的 SQL
     * @param params    命名参数映射
     * @param rowMapper 行映射器
     * @return 第一行结果，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    default <T extends @Nullable Object> Optional<T> queryFirst(
            String sql,
            Map<String, ?> params,
            RowMapper<T> rowMapper) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return getJdbcOperations().queryFirst(tmpl.getSql(), tmpl.toArgs(params), rowMapper);
    }

    /**
     * 使用预构建的 {@link PreparedSql} 查询第一行。
     *
     * @param <T>       结果类型
     * @param ps        预构建的命名参数 SQL
     * @param rowMapper 行映射器
     * @return 第一行结果，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    default <T extends @Nullable Object> Optional<T> queryFirst(
            PreparedSql ps,
            RowMapper<T> rowMapper) throws SQLException {
        return getJdbcOperations().queryFirst(ps.getSql(), ps.getArgs(), rowMapper);
    }

    /**
     * 使用命名参数执行查询，返回第一行第一列的值。
     *
     * @param <T>    目标类型
     * @param sql    包含命名参数的 SQL
     * @param params 命名参数映射
     * @param clazz  目标类型
     * @return 第一行第一列的值，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    default <T extends @Nullable Object> Optional<T> queryValue(
            String sql,
            Map<String, ?> params,
            Class<T> clazz) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return getJdbcOperations().queryValue(tmpl.getSql(), tmpl.toArgs(params), clazz);
    }

    /**
     * 使用预构建的 {@link PreparedSql} 查询第一行第一列的值。
     *
     * @param <T>   目标类型
     * @param ps    预构建的命名参数 SQL
     * @param clazz 目标类型
     * @return 第一行第一列的值，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    default <T extends @Nullable Object> Optional<T> queryValue(
            PreparedSql ps,
            Class<T> clazz) throws SQLException {
        return getJdbcOperations().queryValue(ps.getSql(), ps.getArgs(), clazz);
    }

    /**
     * 使用命名参数执行查询，返回第一行第一列的值；结果为空时返回默认值。
     * 适用于 {@code SELECT COUNT(*)}、{@code SELECT MAX(...)} 等聚合查询场景。
     *
     * @param <T>          目标类型
     * @param sql          包含命名参数的 SQL
     * @param params       命名参数映射
     * @param clazz        目标类型
     * @param defaultValue 查询结果为空时返回的默认值
     * @return 第一行第一列的值，如果查询结果为空则返回 {@code defaultValue}
     * @throws SQLException SQL 异常
     */
    default <T extends @Nullable Object> T queryValueOrDefault(
            String sql,
            Map<String, ?> params,
            Class<T> clazz,
            T defaultValue) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return getJdbcOperations().queryValueOrDefault(
                tmpl.getSql(), tmpl.toArgs(params), clazz, defaultValue);
    }

    /**
     * 使用预构建的 {@link PreparedSql} 查询第一行第一列的值，结果为空时返回默认值。
     *
     * @param <T>          目标类型
     * @param ps           预构建的命名参数 SQL
     * @param clazz        目标类型
     * @param defaultValue 查询结果为空时返回的默认值
     * @return 第一行第一列的值，如果查询结果为空则返回 {@code defaultValue}
     * @throws SQLException SQL 异常
     */
    default <T extends @Nullable Object> T queryValueOrDefault(
            PreparedSql ps,
            Class<T> clazz,
            T defaultValue) throws SQLException {
        return getJdbcOperations().queryValueOrDefault(ps.getSql(), ps.getArgs(), clazz, defaultValue);
    }

    /**
     * 使用命名参数执行查询，返回第一行转为 {@code Map<String, Object>}。
     *
     * @param sql    包含命名参数的 SQL
     * @param params 命名参数映射
     * @return 第一行结果，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    default Optional<@Nullable Map<String, @Nullable Object>> queryFirst(
            String sql,
            Map<String, ?> params) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return getJdbcOperations().queryFirst(tmpl.getSql(), tmpl.toArgs(params));
    }

    /**
     * 使用预构建的 {@link PreparedSql} 查询第一行，返回 {@code Map<String, Object>}。
     *
     * @param ps 预构建的命名参数 SQL
     * @return 第一行结果，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    default Optional<@Nullable Map<String, @Nullable Object>> queryFirst(
            PreparedSql ps) throws SQLException {
        return getJdbcOperations().queryFirst(ps.getSql(), ps.getArgs());
    }

    /**
     * 使用命名参数执行布尔查询。
     *
     * @param sql    包含命名参数的 SQL
     * @param params 命名参数映射
     * @return 查询结果。如果查询结果为空，则返回 {@code false}
     * @throws SQLException SQL 异常
     */
    default boolean queryBoolean(
            String sql,
            Map<String, ?> params) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return getJdbcOperations().queryBoolean(tmpl.getSql(), tmpl.toArgs(params));
    }

    /**
     * 使用预构建的 {@link PreparedSql} 执行布尔查询。
     *
     * @param ps 预构建的命名参数 SQL
     * @return 查询结果。如果查询结果为空，则返回 {@code false}
     * @throws SQLException SQL 异常
     */
    default boolean queryBoolean(
            PreparedSql ps) throws SQLException {
        return getJdbcOperations().queryBoolean(ps.getSql(), ps.getArgs());
    }

    // #endregion

    // #region - update

    /**
     * 使用命名参数执行更新操作。
     *
     * @param sql    包含命名参数的 SQL
     * @param params 命名参数映射
     * @return 更新记录数
     * @throws SQLException SQL 异常
     */
    default int update(
            String sql,
            Map<String, ?> params) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return getJdbcOperations().update(tmpl.getSql(), tmpl.toArgs(params));
    }

    /**
     * 使用预构建的 {@link PreparedSql} 执行更新操作。
     *
     * @param ps 预构建的命名参数 SQL
     * @return 更新记录数
     * @throws SQLException SQL 异常
     */
    default int update(
            PreparedSql ps) throws SQLException {
        return getJdbcOperations().update(ps.getSql(), ps.getArgs());
    }

    /**
     * 使用命名参数执行更新操作，并返回生成的主键。
     *
     * @param <T>       主键类型
     * @param sql       包含命名参数的 SQL
     * @param params    命名参数映射
     * @param rowMapper 主键行映射器
     * @return 生成的主键列表
     * @throws SQLException SQL 异常
     */
    default <T extends @Nullable Object> List<T> updateAndReturnKeys(
            String sql,
            Map<String, ?> params,
            RowMapper<T> rowMapper) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return getJdbcOperations().updateAndReturnKeys(tmpl.getSql(), tmpl.toArgs(params), rowMapper);
    }

    /**
     * 使用预构建的 {@link PreparedSql} 执行更新操作，并返回生成的主键。
     *
     * @param <T>       主键类型
     * @param ps        预构建的命名参数 SQL
     * @param rowMapper 主键行映射器
     * @return 生成的主键列表
     * @throws SQLException SQL 异常
     */
    default <T extends @Nullable Object> List<T> updateAndReturnKeys(
            PreparedSql ps,
            RowMapper<T> rowMapper) throws SQLException {
        return getJdbcOperations().updateAndReturnKeys(ps.getSql(), ps.getArgs(), rowMapper);
    }

    // #endregion

    // #region - batchUpdate

    /**
     * 使用命名参数执行批量更新（遇错中断）。
     *
     * @param sql         包含命名参数的 SQL
     * @param batchParams 批量参数映射列表，每个 Map 代表一行参数
     * @param batchSize   每批数量
     * @return 批量更新结果
     * @throws SQLException SQL 异常
     */
    default BatchUpdateResult batchUpdate(
            String sql,
            List<Map<String, ?>> batchParams,
            int batchSize) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return getJdbcOperations().batchUpdate(tmpl.getSql(), tmpl.toBatchArgs(batchParams), batchSize);
    }

    /**
     * 使用命名参数执行批量更新。
     *
     * @param sql         包含命名参数的 SQL
     * @param batchParams 批量参数映射列表
     * @param batchSize   每批数量
     * @param quietly     如果为 {@code true}，遇错不中断继续执行；
     *                    如果为 {@code false}，遇错立即中断
     * @return 批量更新结果
     * @throws SQLException SQL 异常
     */
    default BatchUpdateResult batchUpdate(
            String sql,
            List<Map<String, ?>> batchParams,
            int batchSize,
            boolean quietly) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return getJdbcOperations().batchUpdate(
                tmpl.getSql(), tmpl.toBatchArgs(batchParams), batchSize, quietly);
    }

    // #endregion
}
