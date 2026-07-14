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

/**
 * 位置参数 JDBC 执行器，面向“外部已经持有 {@link Connection}”的场景。
 *
 * <p>
 * 本类的所有方法均为静态方法，每个方法均显式接收一个 {@link Connection} 形参，
 * 在该连接上执行 SQL 并立即返回结果，方法体委托 {@link JdbcOperationSupport} 完成。
 * </p>
 *
 * <p>
 * <strong>本类不做连接生命周期与事务管理</strong>，使用时务必遵守以下契约：
 * </p>
 * <ul>
 *   <li>本类不获取、不持有、不关闭 {@link Connection}；连接的获取与关闭完全由调用方负责。</li>
 *   <li>本类不读取或修改 {@code autoCommit}，不发起 {@code commit} 或 {@code rollback}；
 *       事务边界由调用方自行管理（例如 JTA 的 {@code UserTransaction}，或直接操作 {@code Connection}）。</li>
 *   <li>传入的 {@code connection} 不可为 {@code null}，由内部断言保证。</li>
 * </ul>
 *
 * <p>
 * 典型场景：JTA / 容器托管事务中，连接由 {@code TransactionManager} 协调，
 * 应用层仅负责在连接上执行 SQL，不参与事务的两阶段提交。
 * </p>
 *
 * <p>
 * 与 {@link SimpleJdbcTemplate} / {@link TransactionTemplate} 的分工：
 * </p>
 * <ul>
 *   <li>{@link SimpleJdbcTemplate}：每次内部取连接、用完即关，关注单语句。</li>
 *   <li>{@link TransactionTemplate}：在独占连接上自行 {@code setAutoCommit(false)}/
 *       {@code commit}/{@code rollback}，管理本地显式事务。</li>
 *   <li>{@code JdbcExecutor}：纯执行，连接与事务均外部管理（JTA / 已持有连接等）。</li>
 * </ul>
 *
 * <p>
 * 方法表面与 {@link JdbcOperations} 一致，区别仅在首增加 {@code Connection} 形参；
 * 每个含 {@code params} 的方法都有一个等价的便捷重载，等价于以空参数数组调用含参版本。
 * 命名参数风格（{@code #{paramName}}）请使用
 * {@link xyz.zhouxy.jdbc.namedparam.NamedParamJdbcExecutor}。
 * </p>
 *
 * @author ZhouXY
 * @since 1.1.0
 * @see JdbcOperations
 * @see xyz.zhouxy.jdbc.namedparam.NamedParamJdbcExecutor
 * @see JdbcOperationSupport
 */
@NullMarked
public final class JdbcExecutor {

    private JdbcExecutor() {
    }

    // #region - query

    /**
     * 在指定连接上执行查询，通过 {@link ResultHandler} 自定义处理结果。
     *
     * @param <T>           返回结果类型
     * @param conn          数据库连接，不可为 {@code null}
     * @param sql           SQL 语句
     * @param params        参数数组
     * @param resultHandler 结果处理器
     * @return 查询结果
     * @throws SQLException SQL 异常
     */
    public static <T extends @Nullable Object> T query(
            Connection conn,
            String sql, @Nullable Object @Nullable [] params,
            ResultHandler<T> resultHandler)
            throws SQLException {
        return JdbcOperationSupport.query(conn, sql, params, resultHandler);
    }

    /**
     * 在指定连接上执行查询，通过 {@link ResultHandler} 自定义处理结果。
     *
     * @param <T>           返回结果类型
     * @param conn          数据库连接，不可为 {@code null}
     * @param sql           SQL 语句
     * @param resultHandler 结果处理器
     * @return 查询结果
     * @throws SQLException SQL 异常
     */
    public static <T extends @Nullable Object> T query(
            Connection conn,
            String sql, ResultHandler<T> resultHandler)
            throws SQLException {
        return query(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, resultHandler);
    }

    // #endregion

    // #region - queryList

    /**
     * 在指定连接上执行查询，通过 {@link RowMapper} 映射每一行，返回结果列表。
     *
     * @param <T>       结果元素类型
     * @param conn      数据库连接，不可为 {@code null}
     * @param sql       SQL 语句
     * @param params    参数数组
     * @param rowMapper 行映射器
     * @return 结果列表
     * @throws SQLException SQL 异常
     */
    public static <T extends @Nullable Object> List<T> queryList(
            Connection conn,
            String sql, @Nullable Object @Nullable [] params,
            RowMapper<T> rowMapper)
            throws SQLException {
        return JdbcOperationSupport.queryList(conn, sql, params, rowMapper);
    }

    /**
     * 在指定连接上执行查询，提取每行第一列的值，返回结果列表。
     *
     * @param <T>    目标类型
     * @param conn   数据库连接，不可为 {@code null}
     * @param sql    SQL 语句
     * @param params 参数数组
     * @param clazz  目标类型
     * @return 结果列表
     * @throws SQLException SQL 异常
     */
    public static <T extends @Nullable Object> List<T> queryValues(
            Connection conn,
            String sql, @Nullable Object @Nullable [] params,
            Class<@NonNull T> clazz)
            throws SQLException {
        return JdbcOperationSupport.queryValues(conn, sql, params, clazz);
    }

    /**
     * 在指定连接上执行查询，每行转为 {@code Map<String, Object>}，返回结果列表。
     *
     * <p>默认使用 {@link RowMapper#LINKED_HASH_MAP_MAPPER}，保持列的查询顺序。</p>
     *
     * @param conn   数据库连接，不可为 {@code null}
     * @param sql    SQL 语句
     * @param params 参数数组
     * @return 结果列表
     * @throws SQLException SQL 异常
     */
    public static List<@Nullable Map<String, @Nullable Object>> queryList(
            Connection conn,
            String sql, @Nullable Object @Nullable [] params)
            throws SQLException {
        return JdbcOperationSupport.queryList(conn, sql, params, RowMapper.LINKED_HASH_MAP_MAPPER);
    }

    /**
     * 在指定连接上执行查询，提取每行第一列的值，返回结果列表。
     *
     * @param <T>   目标类型
     * @param conn  数据库连接，不可为 {@code null}
     * @param sql   SQL 语句
     * @param clazz 目标类型
     * @return 结果列表
     * @throws SQLException SQL 异常
     */
    public static <T extends @Nullable Object> List<T> queryValues(
            Connection conn,
            String sql, Class<@NonNull T> clazz)
            throws SQLException {
        return queryValues(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, clazz);
    }

    /**
     * 在指定连接上执行查询，通过 {@link RowMapper} 映射每一行，返回结果列表。
     *
     * @param <T>       结果元素类型
     * @param conn      数据库连接，不可为 {@code null}
     * @param sql       SQL 语句
     * @param rowMapper 行映射器
     * @return 结果列表
     * @throws SQLException SQL 异常
     */
    public static <T extends @Nullable Object> List<T> queryList(
            Connection conn,
            String sql, RowMapper<T> rowMapper)
            throws SQLException {
        return queryList(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
    }

    /**
     * 在指定连接上执行查询，每行转为 {@code Map<String, Object>}，返回结果列表。
     *
     * <p>默认使用 {@link RowMapper#LINKED_HASH_MAP_MAPPER}，保持列的查询顺序。</p>
     *
     * @param conn 数据库连接，不可为 {@code null}
     * @param sql  SQL 语句
     * @return 结果列表
     * @throws SQLException SQL 异常
     */
    public static List<@Nullable Map<String, @Nullable Object>> queryList(
            Connection conn,
            String sql)
            throws SQLException {
        return queryList(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
    }

    // #endregion

    // #region - queryFirst

    /**
     * 在指定连接上执行查询，通过 {@link RowMapper} 映射结果，返回第一行。
     *
     * @param <T>       结果类型
     * @param conn      数据库连接，不可为 {@code null}
     * @param sql       SQL 语句
     * @param params    参数数组
     * @param rowMapper 行映射器
     * @return 第一行结果，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    public static <T> Optional<T> queryFirst(
            Connection conn,
            String sql, @Nullable Object @Nullable [] params,
            RowMapper<T> rowMapper)
            throws SQLException {
        return Optional.ofNullable(JdbcOperationSupport.queryFirst(conn, sql, params, rowMapper));
    }

    /**
     * 在指定连接上执行查询，返回第一行第一列的值。
     * 适用于 {@code SELECT single_column FROM ... WHERE ...} 单列单行查询场景。
     *
     * @param <T>    目标类型
     * @param conn   数据库连接，不可为 {@code null}
     * @param sql    SQL 语句
     * @param params 参数数组
     * @param clazz  目标类型
     * @return 第一行第一列的值，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    public static <T> Optional<T> queryValue(
            Connection conn,
            String sql, @Nullable Object @Nullable [] params,
            Class<T> clazz)
            throws SQLException {
        return Optional.ofNullable(JdbcOperationSupport.queryValue(conn, sql, params, clazz));
    }

    /**
     * 在指定连接上执行查询，将第一行转为 {@code Map<String, Object>}，返回结果。
     *
     * <p>默认使用 {@link RowMapper#LINKED_HASH_MAP_MAPPER}，保持列的查询顺序。</p>
     *
     * @param conn   数据库连接，不可为 {@code null}
     * @param sql    SQL 语句
     * @param params 参数数组
     * @return 第一行结果，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    public static Optional<Map<String, @Nullable Object>> queryFirst(
            Connection conn,
            String sql, @Nullable Object @Nullable [] params)
            throws SQLException {
        return Optional.ofNullable(
                JdbcOperationSupport.queryFirst(conn, sql, params, RowMapper.LINKED_HASH_MAP_MAPPER));
    }

    /**
     * 在指定连接上执行查询，通过 {@link RowMapper} 映射结果，返回第一行。
     *
     * @param <T>       结果类型
     * @param conn      数据库连接，不可为 {@code null}
     * @param sql       SQL 语句
     * @param rowMapper 行映射器
     * @return 第一行结果，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    public static <T> Optional<T> queryFirst(
            Connection conn,
            String sql, RowMapper<T> rowMapper)
            throws SQLException {
        return queryFirst(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
    }

    /**
     * 在指定连接上执行查询，返回第一行第一列的值。
     *
     * @param <T>   目标类型
     * @param conn  数据库连接，不可为 {@code null}
     * @param sql   SQL 语句
     * @param clazz 目标类型
     * @return 第一行第一列的值，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    public static <T> Optional<T> queryValue(
            Connection conn,
            String sql, Class<T> clazz)
            throws SQLException {
        return queryValue(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, clazz);
    }

    /**
     * 在指定连接上执行查询，将第一行转为 {@code Map<String, Object>}，返回结果。
     *
     * <p>默认使用 {@link RowMapper#LINKED_HASH_MAP_MAPPER}，保持列的查询顺序。</p>
     *
     * @param conn 数据库连接，不可为 {@code null}
     * @param sql  SQL 语句
     * @return 第一行结果，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    public static Optional<Map<String, @Nullable Object>> queryFirst(
            Connection conn,
            String sql)
            throws SQLException {
        return queryFirst(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
    }

    /**
     * 在指定连接上执行查询，返回第一行第一列的值；结果为空时返回 {@code defaultValue}。适用于聚合查询。
     *
     * @param <T>          目标类型
     * @param conn         数据库连接，不可为 {@code null}
     * @param sql          SQL 语句
     * @param params       参数数组
     * @param clazz        目标类型
     * @param defaultValue 查询结果为空时返回的默认值
     * @return 第一行第一列的值，如果查询结果为空则返回 {@code defaultValue}
     * @throws SQLException SQL 异常
     */
    public static <T extends @Nullable Object> T queryValueOrDefault(
            Connection conn,
            String sql,
            @Nullable Object @Nullable [] params,
            Class<@NonNull T> clazz,
            T defaultValue)
            throws SQLException {
        return queryValue(conn, sql, params, clazz).orElse(defaultValue);
    }

    /**
     * 在指定连接上执行查询，返回第一行第一列的值；结果为空时返回 {@code defaultValue}。适用于聚合查询。
     *
     * @param <T>          目标类型
     * @param conn         数据库连接，不可为 {@code null}
     * @param sql          SQL 语句
     * @param clazz        目标类型
     * @param defaultValue 查询结果为空时返回的默认值
     * @return 第一行第一列的值，如果查询结果为空则返回 {@code defaultValue}
     * @throws SQLException SQL 异常
     */
    public static <T extends @Nullable Object> T queryValueOrDefault(
            Connection conn,
            String sql, Class<@NonNull T> clazz, T defaultValue)
            throws SQLException {
        return queryValueOrDefault(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, clazz, defaultValue);
    }

    /**
     * 在指定连接上执行布尔查询，结果为空时返回 {@code false}。
     *
     * @param conn   数据库连接，不可为 {@code null}
     * @param sql    SQL 语句
     * @param params 参数数组
     * @return 查询结果。如果查询结果为空，则返回 {@code false}。
     * @throws SQLException SQL 异常
     */
    public static boolean queryBoolean(
            Connection conn,
            String sql, @Nullable Object @Nullable [] params)
            throws SQLException {
        return Boolean.TRUE.equals(JdbcOperationSupport.queryValue(conn, sql, params, Boolean.class));
    }

    /**
     * 在指定连接上执行布尔查询，结果为空时返回 {@code false}。
     *
     * @param conn 数据库连接，不可为 {@code null}
     * @param sql  SQL 语句
     * @return 查询结果。如果查询结果为空，则返回 {@code false}。
     * @throws SQLException SQL 异常
     */
    public static boolean queryBoolean(
            Connection conn,
            String sql)
            throws SQLException {
        return queryBoolean(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
    }

    // #endregion

    // #region - update & batchUpdate

    /**
     * 在指定连接上执行更新操作，返回影响行数。
     *
     * @param conn   数据库连接，不可为 {@code null}
     * @param sql    SQL 语句
     * @param params 参数数组
     * @return 更新记录数
     * @throws SQLException SQL 异常
     */
    public static int update(
            Connection conn,
            String sql, @Nullable Object @Nullable [] params)
            throws SQLException {
        return JdbcOperationSupport.update(conn, sql, params);
    }

    /**
     * 在指定连接上执行更新操作，返回影响行数。
     *
     * @param conn 数据库连接，不可为 {@code null}
     * @param sql  SQL 语句
     * @return 更新记录数
     * @throws SQLException SQL 异常
     */
    public static int update(
            Connection conn,
            String sql)
            throws SQLException {
        return update(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY);
    }

    /**
     * 在指定连接上执行更新操作，通过 {@link RowMapper} 映射并返回生成的主键列表。
     *
     * @param <T>       主键类型
     * @param conn      数据库连接，不可为 {@code null}
     * @param sql       SQL 语句
     * @param params    参数数组
     * @param rowMapper 主键行映射器
     * @return 生成的主键列表
     * @throws SQLException SQL 异常
     */
    public static <T extends @Nullable Object> List<T> updateAndReturnKeys(
            Connection conn,
            String sql, @Nullable Object @Nullable [] params,
            RowMapper<T> rowMapper)
            throws SQLException {
        return JdbcOperationSupport.updateAndReturnKeys(conn, sql, params, rowMapper);
    }

    /**
     * 在指定连接上执行更新操作，通过 {@link RowMapper} 映射并返回生成的主键列表。
     *
     * @param <T>       主键类型
     * @param conn      数据库连接，不可为 {@code null}
     * @param sql       SQL 语句
     * @param rowMapper 主键行映射器
     * @return 生成的主键列表
     * @throws SQLException SQL 异常
     */
    public static <T extends @Nullable Object> List<T> updateAndReturnKeys(
            Connection conn,
            String sql, RowMapper<T> rowMapper)
            throws SQLException {
        return updateAndReturnKeys(conn, sql, ParamBuilder.EMPTY_OBJECT_ARRAY, rowMapper);
    }

    /**
     * 在指定连接上执行批量更新，遇错中断。
     *
     * <p>
     * 当无法获取所更新的行数时，对应位置的更新行数将被设置为
     * {@link JdbcOperationSupport#UNKNOWN_COUNT}。
     * </p>
     *
     * @param conn      数据库连接，不可为 {@code null}
     * @param sql       SQL 语句
     * @param params    参数列表
     * @param batchSize 每批数量
     * @return 批量更新结果
     * @throws SQLException SQL 异常
     */
    public static BatchUpdateResult batchUpdate(
            Connection conn,
            String sql, @Nullable Collection<@Nullable Object @Nullable []> params, int batchSize)
            throws SQLException {
        return JdbcOperationSupport.batchUpdate(conn, sql, params, batchSize, false);
    }

    /**
     * 在指定连接上执行批量更新。
     *
     * <p>
     * 当无法获取所更新的行数时，对应位置的更新行数将被设置为
     * {@link JdbcOperationSupport#UNKNOWN_COUNT}。
     * </p>
     *
     * @param conn      数据库连接，不可为 {@code null}
     * @param sql       SQL 语句
     * @param params    参数列表
     * @param batchSize 每批数量
     * @param quietly   静默分批更新。若为 {@code true}，分批更新过程中发生异常不中断；
     *                  若为 {@code false}，分批更新过程中发生异常即中断并返回结果。
     * @return 批量更新结果
     * @throws SQLException SQL 异常
     */
    public static BatchUpdateResult batchUpdate(
            Connection conn,
            String sql, @Nullable Collection<@Nullable Object @Nullable []> params,
            int batchSize, boolean quietly)
            throws SQLException {
        return JdbcOperationSupport.batchUpdate(conn, sql, params, batchSize, quietly);
    }

    // #endregion
}
