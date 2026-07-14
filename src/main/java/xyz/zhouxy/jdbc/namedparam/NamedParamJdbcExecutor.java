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

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import xyz.zhouxy.jdbc.BatchUpdateResult;
import xyz.zhouxy.jdbc.JdbcExecutor;
import xyz.zhouxy.jdbc.ResultHandler;
import xyz.zhouxy.jdbc.RowMapper;

/**
 * 命名参数 JDBC 执行器，面向“外部已经持有 {@link Connection}”的场景。
 *
 * <p>
 * 本类的所有方法接收一个 {@link Connection} 形参，在该连接上执行 SQL 并立即返回结果。
 * 内部通过 {@link NamedParamSql} / {@link PreparedSql} 解析命名参数后委托给
 * {@link JdbcExecutor} 完成实际执行。
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
 * 与位置参数版 {@link JdbcExecutor} 的分工相同，二者可共享同一个外部 {@link Connection}：
 * </p>
 * <pre>{@code
 * try (Connection conn = xaDataSource.getConnection()) {
 *     userTx.begin();
 *     try {
 *         jdbcExec.update(conn, "UPDATE accounts SET balance = ? WHERE id = ?", buildParams(100, 1));
 *         namedExec.update(conn,
 *                 "INSERT INTO logs(msg, user) VALUES(#{msg}, #{user})",
 *                 Map.of("msg", "transfer", "user", "Alice"));
 *         userTx.commit();
 *     } catch (Exception e) {
 *         userTx.rollback();
 *     }
 * }
 * }</pre>
 *
 * <p>
 * 每个方法提供三种重载形式，与 {@link NamedParamJdbcOperations} 保持一致：
 * </p>
 * <ol>
 *   <li><strong>直接传参</strong>：接受 {@code (Connection, String sql, Map<String, ?> params, ...)}，
 *       内部解析 SQL 模板并提取参数值。适用于一次性执行。</li>
 *   <li><strong>模板传参</strong>：接受 {@code (Connection, NamedParamSql template, Map<String, ?> params, ...)}，
 *       适用于同一 SQL 多次执行、仅参数变化的场景。</li>
 *   <li><strong>预构建传参</strong>：接受 {@code (Connection, PreparedSql ps, ...)}，
 *       SQL 与参数数组均已预构建，直接委托 {@link JdbcExecutor}。</li>
 * </ol>
 *
 * @author ZhouXY
 * @since 1.1.0
 * @see JdbcExecutor
 * @see NamedParamJdbcOperations
 * @see NamedParamSql
 * @see PreparedSql
 */
public final class NamedParamJdbcExecutor {

    private final JdbcExecutor jdbcExecutor = new JdbcExecutor();

    // #region - query

    /**
     * 在指定连接上执行查询，通过 {@link ResultHandler} 自定义处理结果。
     *
     * @param <T>           返回结果类型
     * @param conn          数据库连接，不可为 {@code null}
     * @param sql           包含命名参数（格式：{@code #{paramName}}）的 SQL
     * @param params        命名参数映射
     * @param resultHandler 结果处理器
     * @return 查询结果
     * @throws SQLException SQL 异常
     */
    public <T extends @Nullable Object> T query(
            Connection conn,
            String sql,
            Map<String, ?> params,
            ResultHandler<T> resultHandler) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return jdbcExecutor.query(conn, tmpl.getSql(), tmpl.toArgs(params), resultHandler);
    }

    /**
     * 在指定连接上执行查询，通过 {@link ResultHandler} 自定义处理结果。
     *
     * @param <T>           返回结果类型
     * @param conn          数据库连接，不可为 {@code null}
     * @param template      预解析的命名参数 SQL 模板
     * @param params        命名参数映射
     * @param resultHandler 结果处理器
     * @return 查询结果
     * @throws SQLException SQL 异常
     */
    public <T extends @Nullable Object> T query(
            Connection conn,
            NamedParamSql template,
            Map<String, ?> params,
            ResultHandler<T> resultHandler) throws SQLException {
        return jdbcExecutor.query(conn, template.getSql(), template.toArgs(params), resultHandler);
    }

    /**
     * 在指定连接上执行查询，通过 {@link ResultHandler} 自定义处理结果。
     *
     * @param <T>           返回结果类型
     * @param conn          数据库连接，不可为 {@code null}
     * @param ps            预构建的命名参数 SQL
     * @param resultHandler 结果处理器
     * @return 查询结果
     * @throws SQLException SQL 异常
     */
    public <T extends @Nullable Object> T query(
            Connection conn,
            PreparedSql ps,
            ResultHandler<T> resultHandler) throws SQLException {
        return jdbcExecutor.query(conn, ps.getSql(), ps.getArgs(), resultHandler);
    }

    // #endregion

    // #region - queryList

    /**
     * 在指定连接上执行查询，通过 {@link RowMapper} 映射每一行，返回结果列表。
     *
     * @param <T>       结果元素类型
     * @param conn      数据库连接，不可为 {@code null}
     * @param sql       包含命名参数的 SQL
     * @param params    命名参数映射
     * @param rowMapper 行映射器
     * @return 结果列表
     * @throws SQLException SQL 异常
     */
    public <T extends @Nullable Object> List<T> queryList(
            Connection conn,
            String sql,
            Map<String, ?> params,
            RowMapper<T> rowMapper) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return jdbcExecutor.queryList(conn, tmpl.getSql(), tmpl.toArgs(params), rowMapper);
    }

    /**
     * 在指定连接上执行查询，通过 {@link RowMapper} 映射每一行，返回结果列表。
     *
     * @param <T>       结果元素类型
     * @param conn      数据库连接，不可为 {@code null}
     * @param template  预解析的命名参数 SQL 模板
     * @param params    命名参数映射
     * @param rowMapper 行映射器
     * @return 结果列表
     * @throws SQLException SQL 异常
     */
    public <T extends @Nullable Object> List<T> queryList(
            Connection conn,
            NamedParamSql template,
            Map<String, ?> params,
            RowMapper<T> rowMapper) throws SQLException {
        return jdbcExecutor.queryList(conn, template.getSql(), template.toArgs(params), rowMapper);
    }

    /**
     * 在指定连接上执行查询，通过 {@link RowMapper} 映射每一行，返回结果列表。
     *
     * @param <T>       结果元素类型
     * @param conn      数据库连接，不可为 {@code null}
     * @param ps        预构建的命名参数 SQL
     * @param rowMapper 行映射器
     * @return 结果列表
     * @throws SQLException SQL 异常
     */
    public <T extends @Nullable Object> List<T> queryList(
            Connection conn,
            PreparedSql ps,
            RowMapper<T> rowMapper) throws SQLException {
        return jdbcExecutor.queryList(conn, ps.getSql(), ps.getArgs(), rowMapper);
    }

    /**
     * 在指定连接上执行查询，提取每行第一列的值，返回结果列表。
     *
     * @param <T>    目标类型
     * @param conn   数据库连接，不可为 {@code null}
     * @param sql    包含命名参数的 SQL
     * @param params 命名参数映射
     * @param clazz  目标类型
     * @return 每一行第一列的值列表
     * @throws SQLException SQL 异常
     */
    public <T extends @Nullable Object> List<T> queryValues(
            Connection conn,
            String sql,
            Map<String, ?> params,
            Class<@NonNull T> clazz) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return jdbcExecutor.queryValues(conn, tmpl.getSql(), tmpl.toArgs(params), clazz);
    }

    /**
     * 在指定连接上执行查询，提取每行第一列的值，返回结果列表。
     *
     * @param <T>      目标类型
     * @param conn     数据库连接，不可为 {@code null}
     * @param template 预解析的命名参数 SQL 模板
     * @param params   命名参数映射
     * @param clazz    目标类型
     * @return 每一行第一列的值列表
     * @throws SQLException SQL 异常
     */
    public <T extends @Nullable Object> List<T> queryValues(
            Connection conn,
            NamedParamSql template,
            Map<String, ?> params,
            Class<@NonNull T> clazz) throws SQLException {
        return jdbcExecutor.queryValues(conn, template.getSql(), template.toArgs(params), clazz);
    }

    /**
     * 在指定连接上执行查询，提取每行第一列的值，返回结果列表。
     *
     * @param <T>   目标类型
     * @param conn  数据库连接，不可为 {@code null}
     * @param ps    预构建的命名参数 SQL
     * @param clazz 目标类型
     * @return 每一行第一列的值列表
     * @throws SQLException SQL 异常
     */
    public <T extends @Nullable Object> List<T> queryValues(
            Connection conn,
            PreparedSql ps,
            Class<@NonNull T> clazz) throws SQLException {
        return jdbcExecutor.queryValues(conn, ps.getSql(), ps.getArgs(), clazz);
    }

    /**
     * 在指定连接上执行查询，每行转为 {@code Map<String, Object>}，返回结果列表。
     *
     * <p>默认使用 {@link RowMapper#LINKED_HASH_MAP_MAPPER}，保持列的查询顺序。</p>
     *
     * @param conn   数据库连接，不可为 {@code null}
     * @param sql    包含命名参数的 SQL
     * @param params 命名参数映射
     * @return 结果列表
     * @throws SQLException SQL 异常
     */
    public List<@Nullable Map<String, @Nullable Object>> queryList(
            Connection conn,
            String sql,
            Map<String, ?> params) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return jdbcExecutor.queryList(conn, tmpl.getSql(), tmpl.toArgs(params));
    }

    /**
     * 在指定连接上执行查询，每行转为 {@code Map<String, Object>}，返回结果列表。
     *
     * <p>默认使用 {@link RowMapper#LINKED_HASH_MAP_MAPPER}，保持列的查询顺序。</p>
     *
     * @param conn     数据库连接，不可为 {@code null}
     * @param template 预解析的命名参数 SQL 模板
     * @param params   命名参数映射
     * @return 结果列表
     * @throws SQLException SQL 异常
     */
    public List<@Nullable Map<String, @Nullable Object>> queryList(
            Connection conn,
            NamedParamSql template,
            Map<String, ?> params) throws SQLException {
        return jdbcExecutor.queryList(conn, template.getSql(), template.toArgs(params));
    }

    /**
     * 在指定连接上执行查询，每行转为 {@code Map<String, Object>}，返回结果列表。
     *
     * <p>默认使用 {@link RowMapper#LINKED_HASH_MAP_MAPPER}，保持列的查询顺序。</p>
     *
     * @param conn 数据库连接，不可为 {@code null}
     * @param ps   预构建的命名参数 SQL
     * @return 结果列表
     * @throws SQLException SQL 异常
     */
    public List<@Nullable Map<String, @Nullable Object>> queryList(
            Connection conn,
            PreparedSql ps) throws SQLException {
        return jdbcExecutor.queryList(conn, ps.getSql(), ps.getArgs());
    }

    // #endregion

    // #region - queryFirst

    /**
     * 在指定连接上执行查询，通过 {@link RowMapper} 映射结果，返回第一行。
     *
     * @param <T>       结果类型
     * @param conn      数据库连接，不可为 {@code null}
     * @param sql       包含命名参数的 SQL
     * @param params    命名参数映射
     * @param rowMapper 行映射器
     * @return 第一行结果，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    public <T> Optional<T> queryFirst(
            Connection conn,
            String sql,
            Map<String, ?> params,
            RowMapper<T> rowMapper) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return jdbcExecutor.queryFirst(conn, tmpl.getSql(), tmpl.toArgs(params), rowMapper);
    }

    /**
     * 在指定连接上执行查询，通过 {@link RowMapper} 映射结果，返回第一行。
     *
     * @param <T>       结果类型
     * @param conn      数据库连接，不可为 {@code null}
     * @param template  预解析的命名参数 SQL 模板
     * @param params    命名参数映射
     * @param rowMapper 行映射器
     * @return 第一行结果，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    public <T> Optional<T> queryFirst(
            Connection conn,
            NamedParamSql template,
            Map<String, ?> params,
            RowMapper<T> rowMapper) throws SQLException {
        return jdbcExecutor.queryFirst(conn, template.getSql(), template.toArgs(params), rowMapper);
    }

    /**
     * 在指定连接上执行查询，通过 {@link RowMapper} 映射结果，返回第一行。
     *
     * @param <T>       结果类型
     * @param conn      数据库连接，不可为 {@code null}
     * @param ps        预构建的命名参数 SQL
     * @param rowMapper 行映射器
     * @return 第一行结果，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    public <T> Optional<T> queryFirst(
            Connection conn,
            PreparedSql ps,
            RowMapper<T> rowMapper) throws SQLException {
        return jdbcExecutor.queryFirst(conn, ps.getSql(), ps.getArgs(), rowMapper);
    }

    /**
     * 在指定连接上执行查询，返回第一行第一列的值。
     *
     * @param <T>    目标类型
     * @param conn   数据库连接，不可为 {@code null}
     * @param sql    包含命名参数的 SQL
     * @param params 命名参数映射
     * @param clazz  目标类型
     * @return 第一行第一列的值，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    public <T> Optional<T> queryValue(
            Connection conn,
            String sql,
            Map<String, ?> params,
            Class<T> clazz) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return jdbcExecutor.queryValue(conn, tmpl.getSql(), tmpl.toArgs(params), clazz);
    }

    /**
     * 在指定连接上执行查询，返回第一行第一列的值。
     *
     * @param <T>      目标类型
     * @param conn     数据库连接，不可为 {@code null}
     * @param template 预解析的命名参数 SQL 模板
     * @param params   命名参数映射
     * @param clazz    目标类型
     * @return 第一行第一列的值，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    public <T> Optional<T> queryValue(
            Connection conn,
            NamedParamSql template,
            Map<String, ?> params,
            Class<T> clazz) throws SQLException {
        return jdbcExecutor.queryValue(conn, template.getSql(), template.toArgs(params), clazz);
    }

    /**
     * 在指定连接上执行查询，返回第一行第一列的值。
     *
     * @param <T>   目标类型
     * @param conn  数据库连接，不可为 {@code null}
     * @param ps    预构建的命名参数 SQL
     * @param clazz 目标类型
     * @return 第一行第一列的值，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    public <T> Optional<T> queryValue(
            Connection conn,
            PreparedSql ps,
            Class<T> clazz) throws SQLException {
        return jdbcExecutor.queryValue(conn, ps.getSql(), ps.getArgs(), clazz);
    }

    /**
     * 在指定连接上执行查询，返回第一行第一列的值；结果为空时返回 {@code defaultValue}。适用于聚合查询。
     *
     * @param <T>          目标类型
     * @param conn         数据库连接，不可为 {@code null}
     * @param sql          包含命名参数的 SQL
     * @param params       命名参数映射
     * @param clazz        目标类型
     * @param defaultValue 查询结果为空时返回的默认值
     * @return 第一行第一列的值，如果查询结果为空则返回 {@code defaultValue}
     * @throws SQLException SQL 异常
     */
    public <T extends @Nullable Object> T queryValueOrDefault(
            Connection conn,
            String sql,
            Map<String, ?> params,
            Class<@NonNull T> clazz,
            T defaultValue) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return jdbcExecutor.queryValueOrDefault(conn, tmpl.getSql(), tmpl.toArgs(params), clazz, defaultValue);
    }

    /**
     * 在指定连接上执行查询，返回第一行第一列的值；结果为空时返回 {@code defaultValue}。适用于聚合查询。
     *
     * @param <T>          目标类型
     * @param conn         数据库连接，不可为 {@code null}
     * @param template     预解析的命名参数 SQL 模板
     * @param params       命名参数映射
     * @param clazz        目标类型
     * @param defaultValue 查询结果为空时返回的默认值
     * @return 第一行第一列的值，如果查询结果为空则返回 {@code defaultValue}
     * @throws SQLException SQL 异常
     */
    public <T extends @Nullable Object> T queryValueOrDefault(
            Connection conn,
            NamedParamSql template,
            Map<String, ?> params,
            Class<@NonNull T> clazz,
            T defaultValue) throws SQLException {
        return jdbcExecutor.queryValueOrDefault(conn, template.getSql(), template.toArgs(params), clazz, defaultValue);
    }

    /**
     * 在指定连接上执行查询，返回第一行第一列的值；结果为空时返回 {@code defaultValue}。适用于聚合查询。
     *
     * @param <T>          目标类型
     * @param conn         数据库连接，不可为 {@code null}
     * @param ps           预构建的命名参数 SQL
     * @param clazz        目标类型
     * @param defaultValue 查询结果为空时返回的默认值
     * @return 第一行第一列的值，如果查询结果为空则返回 {@code defaultValue}
     * @throws SQLException SQL 异常
     */
    public <T extends @Nullable Object> T queryValueOrDefault(
            Connection conn,
            PreparedSql ps,
            Class<@NonNull T> clazz,
            T defaultValue) throws SQLException {
        return jdbcExecutor.queryValueOrDefault(conn, ps.getSql(), ps.getArgs(), clazz, defaultValue);
    }

    /**
     * 在指定连接上执行查询，将第一行转为 {@code Map<String, Object>}，返回结果。
     *
     * <p>默认使用 {@link RowMapper#LINKED_HASH_MAP_MAPPER}，保持列的查询顺序。</p>
     *
     * @param conn   数据库连接，不可为 {@code null}
     * @param sql    包含命名参数的 SQL
     * @param params 命名参数映射
     * @return 第一行结果，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    public Optional<Map<String, @Nullable Object>> queryFirst(
            Connection conn,
            String sql,
            Map<String, ?> params) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return jdbcExecutor.queryFirst(conn, tmpl.getSql(), tmpl.toArgs(params));
    }

    /**
     * 在指定连接上执行查询，将第一行转为 {@code Map<String, Object>}，返回结果。
     *
     * <p>默认使用 {@link RowMapper#LINKED_HASH_MAP_MAPPER}，保持列的查询顺序。</p>
     *
     * @param conn     数据库连接，不可为 {@code null}
     * @param template 预解析的命名参数 SQL 模板
     * @param params   命名参数映射
     * @return 第一行结果，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    public Optional<Map<String, @Nullable Object>> queryFirst(
            Connection conn,
            NamedParamSql template,
            Map<String, ?> params) throws SQLException {
        return jdbcExecutor.queryFirst(conn, template.getSql(), template.toArgs(params));
    }

    /**
     * 在指定连接上执行查询，将第一行转为 {@code Map<String, Object>}，返回结果。
     *
     * <p>默认使用 {@link RowMapper#LINKED_HASH_MAP_MAPPER}，保持列的查询顺序。</p>
     *
     * @param conn 数据库连接，不可为 {@code null}
     * @param ps   预构建的命名参数 SQL
     * @return 第一行结果，可能为 {@code Optional.empty()}
     * @throws SQLException SQL 异常
     */
    public Optional<Map<String, @Nullable Object>> queryFirst(
            Connection conn,
            PreparedSql ps) throws SQLException {
        return jdbcExecutor.queryFirst(conn, ps.getSql(), ps.getArgs());
    }

    /**
     * 在指定连接上执行布尔查询，结果为空时返回 {@code false}。
     *
     * @param conn   数据库连接，不可为 {@code null}
     * @param sql    包含命名参数的 SQL
     * @param params 命名参数映射
     * @return 查询结果。如果查询结果为空，则返回 {@code false}。
     * @throws SQLException SQL 异常
     */
    public boolean queryBoolean(
            Connection conn,
            String sql,
            Map<String, ?> params) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return jdbcExecutor.queryBoolean(conn, tmpl.getSql(), tmpl.toArgs(params));
    }

    /**
     * 在指定连接上执行布尔查询，结果为空时返回 {@code false}。
     *
     * @param conn     数据库连接，不可为 {@code null}
     * @param template 预解析的命名参数 SQL 模板
     * @param params   命名参数映射
     * @return 查询结果。如果查询结果为空，则返回 {@code false}。
     * @throws SQLException SQL 异常
     */
    public boolean queryBoolean(
            Connection conn,
            NamedParamSql template,
            Map<String, ?> params) throws SQLException {
        return jdbcExecutor.queryBoolean(conn, template.getSql(), template.toArgs(params));
    }

    /**
     * 在指定连接上执行布尔查询，结果为空时返回 {@code false}。
     *
     * @param conn 数据库连接，不可为 {@code null}
     * @param ps   预构建的命名参数 SQL
     * @return 查询结果。如果查询结果为空，则返回 {@code false}。
     * @throws SQLException SQL 异常
     */
    public boolean queryBoolean(
            Connection conn,
            PreparedSql ps) throws SQLException {
        return jdbcExecutor.queryBoolean(conn, ps.getSql(), ps.getArgs());
    }

    // #endregion

    // #region - update & batchUpdate

    /**
     * 在指定连接上执行更新操作，返回影响行数。
     *
     * @param conn   数据库连接，不可为 {@code null}
     * @param sql    包含命名参数的 SQL
     * @param params 命名参数映射
     * @return 更新记录数
     * @throws SQLException SQL 异常
     */
    public int update(
            Connection conn,
            String sql,
            Map<String, ?> params) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return jdbcExecutor.update(conn, tmpl.getSql(), tmpl.toArgs(params));
    }

    /**
     * 在指定连接上执行更新操作，返回影响行数。
     *
     * @param conn     数据库连接，不可为 {@code null}
     * @param template 预解析的命名参数 SQL 模板
     * @param params   命名参数映射
     * @return 更新记录数
     * @throws SQLException SQL 异常
     */
    public int update(
            Connection conn,
            NamedParamSql template,
            Map<String, ?> params) throws SQLException {
        return jdbcExecutor.update(conn, template.getSql(), template.toArgs(params));
    }

    /**
     * 在指定连接上执行更新操作，返回影响行数。
     *
     * @param conn 数据库连接，不可为 {@code null}
     * @param ps   预构建的命名参数 SQL
     * @return 更新记录数
     * @throws SQLException SQL 异常
     */
    public int update(
            Connection conn,
            PreparedSql ps) throws SQLException {
        return jdbcExecutor.update(conn, ps.getSql(), ps.getArgs());
    }

    /**
     * 在指定连接上执行更新操作，通过 {@link RowMapper} 映射并返回生成的主键列表。
     *
     * @param <T>       主键类型
     * @param conn      数据库连接，不可为 {@code null}
     * @param sql       包含命名参数的 SQL
     * @param params    命名参数映射
     * @param rowMapper 主键行映射器
     * @return 生成的主键列表
     * @throws SQLException SQL 异常
     */
    public <T extends @Nullable Object> List<T> updateAndReturnKeys(
            Connection conn,
            String sql,
            Map<String, ?> params,
            RowMapper<T> rowMapper) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return jdbcExecutor.updateAndReturnKeys(conn, tmpl.getSql(), tmpl.toArgs(params), rowMapper);
    }

    /**
     * 在指定连接上执行更新操作，通过 {@link RowMapper} 映射并返回生成的主键列表。
     *
     * @param <T>       主键类型
     * @param conn      数据库连接，不可为 {@code null}
     * @param template  预解析的命名参数 SQL 模板
     * @param params    命名参数映射
     * @param rowMapper 主键行映射器
     * @return 生成的主键列表
     * @throws SQLException SQL 异常
     */
    public <T extends @Nullable Object> List<T> updateAndReturnKeys(
            Connection conn,
            NamedParamSql template,
            Map<String, ?> params,
            RowMapper<T> rowMapper) throws SQLException {
        return jdbcExecutor.updateAndReturnKeys(conn, template.getSql(), template.toArgs(params), rowMapper);
    }

    /**
     * 在指定连接上执行更新操作，通过 {@link RowMapper} 映射并返回生成的主键列表。
     *
     * @param <T>       主键类型
     * @param conn      数据库连接，不可为 {@code null}
     * @param ps        预构建的命名参数 SQL
     * @param rowMapper 主键行映射器
     * @return 生成的主键列表
     * @throws SQLException SQL 异常
     */
    public <T extends @Nullable Object> List<T> updateAndReturnKeys(
            Connection conn,
            PreparedSql ps,
            RowMapper<T> rowMapper) throws SQLException {
        return jdbcExecutor.updateAndReturnKeys(conn, ps.getSql(), ps.getArgs(), rowMapper);
    }

    /**
     * 在指定连接上执行批量更新，遇错中断。
     *
     * @param conn        数据库连接，不可为 {@code null}
     * @param sql         包含命名参数的 SQL
     * @param batchParams 批量参数映射列表，每个 Map 代表一行参数
     * @param batchSize   每批数量
     * @return 批量更新结果
     * @throws SQLException SQL 异常
     */
    public BatchUpdateResult batchUpdate(
            Connection conn,
            String sql,
            List<Map<String, ?>> batchParams,
            int batchSize) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return jdbcExecutor.batchUpdate(conn, tmpl.getSql(), tmpl.toBatchArgs(batchParams), batchSize);
    }

    /**
     * 在指定连接上执行批量更新，遇错中断。
     *
     * @param conn        数据库连接，不可为 {@code null}
     * @param template    预解析的命名参数 SQL 模板
     * @param batchParams 批量参数映射列表，每个 Map 代表一行参数
     * @param batchSize   每批数量
     * @return 批量更新结果
     * @throws SQLException SQL 异常
     */
    public BatchUpdateResult batchUpdate(
            Connection conn,
            NamedParamSql template,
            List<Map<String, ?>> batchParams,
            int batchSize) throws SQLException {
        return jdbcExecutor.batchUpdate(conn, template.getSql(), template.toBatchArgs(batchParams), batchSize);
    }

    /**
     * 在指定连接上执行批量更新。
     *
     * @param conn        数据库连接，不可为 {@code null}
     * @param sql         包含命名参数的 SQL
     * @param batchParams 批量参数映射列表，每个 Map 代表一行参数
     * @param batchSize   每批数量
     * @param quietly     静默分批更新。若为 {@code true}，分批更新过程中发生异常不中断；
     *                    若为 {@code false}，分批更新过程中发生异常即中断并返回结果。
     * @return 批量更新结果
     * @throws SQLException SQL 异常
     */
    public BatchUpdateResult batchUpdate(
            Connection conn,
            String sql,
            List<Map<String, ?>> batchParams,
            int batchSize, boolean quietly) throws SQLException {
        final NamedParamSql tmpl = NamedParamSql.of(sql);
        return jdbcExecutor.batchUpdate(conn, tmpl.getSql(), tmpl.toBatchArgs(batchParams), batchSize, quietly);
    }

    /**
     * 在指定连接上执行批量更新。
     *
     * @param conn        数据库连接，不可为 {@code null}
     * @param template    预解析的命名参数 SQL 模板
     * @param batchParams 批量参数映射列表，每个 Map 代表一行参数
     * @param batchSize   每批数量
     * @param quietly     静默分批更新。若为 {@code true}，分批更新过程中发生异常不中断；
     *                    若为 {@code false}，分批更新过程中发生异常即中断并返回结果。
     * @return 批量更新结果
     * @throws SQLException SQL 异常
     */
    public BatchUpdateResult batchUpdate(
            Connection conn,
            NamedParamSql template,
            List<Map<String, ?>> batchParams,
            int batchSize, boolean quietly) throws SQLException {
        return jdbcExecutor.batchUpdate(conn, template.getSql(), template.toBatchArgs(batchParams), batchSize, quietly);
    }

    // #endregion
}
