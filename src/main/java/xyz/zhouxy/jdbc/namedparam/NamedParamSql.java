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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import xyz.zhouxy.jdbc.ParamBuilder;
import xyz.zhouxy.jdbc.util.AssertTools;
import xyz.zhouxy.jdbc.util.GenericTokenParser;
import xyz.zhouxy.jdbc.util.TokenHandler;

/**
 * 命名参数 SQL 模板。
 *
 * <p>
 * 解析包含 {@code #{paramName}} 格式命名参数的 SQL，将其转换为 JDBC 标准的 {@code ?} 占位符 SQL，
 * 并记录参数名的出现顺序。本类<strong>不绑定参数值</strong>，参数值通过
 * {@link #toArgs(Map)} 或 {@link #toBatchArgs(List)} 延迟绑定。
 * </p>
 *
 * <p>
 * 构建后不可变，线程安全。
 * </p>
 *
 * <p><strong>使用示例：</strong></p>
 * <pre>{@code
 * // 纯模板
 * NamedParamSql tmpl = NamedParamSql.of("SELECT * FROM user WHERE id = #{id} AND name = #{name}");
 * String jdbcSql = tmpl.getSql();                         // SELECT * FROM user WHERE id = ? AND name = ?
 * List<String> names = tmpl.getParamNames();              // [id, name]
 *
 * // 单次绑值
 * Object[] args = tmpl.toArgs(Map.of("id", 1, "name", "Alice"));  // [1, "Alice"]
 *
 * // 批量绑值
 * List<Object[]> batch = tmpl.toBatchArgs(List.of(
 *     Map.of("id", 1, "name", "Alice"),
 *     Map.of("id", 2, "name", "Bob")));
 *
 * // 转为 PreparedSql（链式 .param()）
 * PreparedSql ps = tmpl.prepare()
 *     .param("id", 1)
 *     .param("name", "Alice")
 *     .build();
 * }</pre>
 *
 * @author ZhouXY
 * @since 1.1.0
 * @see PreparedSql
 */
public final class NamedParamSql {

    private final String sql;
    private final List<String> paramNames;

    private NamedParamSql(String sql, List<String> paramNames) {
        this.sql = sql;
        this.paramNames = Collections.unmodifiableList(paramNames);
    }

    /**
     * 获取转换后的 JDBC SQL，其中的命名参数已被替换为 {@code ?}
     *
     * @return 转换后的 SQL
     */
    public String getSql() {
        return sql;
    }

    /**
     * 获取 SQL 中命名参数的出现顺序列表。
     *
     * <p>
     * 用于自省或调试；{@link #toArgs(Map)} 和 {@link #toBatchArgs(List)}
     * 内部使用此顺序从 Map 中提取值。
     * </p>
     *
     * @return 不可变的参数名列表，顺序与 SQL 中 {@code #{name}} 出现顺序一致
     */
    public List<String> getParamNames() {
        return paramNames;
    }

    /**
     * 创建 {@link Builder} 实例。
     *
     * <p>
     * 等价于 {@link #of(String)}，提供 builder 风格的构建方式。
     * </p>
     *
     * @return Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 解析命名参数 SQL，创建纯模板实例。
     *
     * <p>
     * 仅解析 SQL 中的 {@code #{paramName}} 为 {@code ?} 并记录参数名顺序，
     * 不绑定参数值。参数值后续通过 {@link #toArgs(Map)} 绑定。
     * </p>
     *
     * @param sql 包含命名参数（格式：{@code #{paramName}}）的 SQL，不可为 {@code null}
     * @return 构建完成的 {@link NamedParamSql} 模板实例
     */
    public static NamedParamSql of(String sql) {
        return builder().sql(sql).build();
    }

    /**
     * 启动参数绑定链，返回 {@link PreparedSql} 的 Builder。
     *
     * <pre>{@code
     * PreparedSql ps = NamedParamSql.of("... WHERE a = #{a}")
     *     .prepare()
     *     .param("a", 1)
     *     .build();
     * }</pre>
     *
     * @return PreparedSql 构建器
     */
    public PreparedSql.Builder prepare() {
        return PreparedSql.sql(this);
    }

    // #region - 参数值绑定

    /**
     * 按 SQL 中参数名出现顺序，从 Map 中提取参数值，返回参数数组。
     *
     * <p>
     * 每个值会经 {@link ParamBuilder#handleItem(Object)} 处理
     * （如 {@code Optional} 拆箱等）。
     * </p>
     *
     * @param params 命名参数映射，key 为参数名，value 为参数值
     * @return 参数值数组，顺序与 {@link #getParamNames()} 一致
     * @throws IllegalArgumentException 如果缺少某个 SQL 中引用的参数名
     */
    public Object[] toArgs(Map<String, ?> params) {
        AssertTools.checkNotNull(params, "params must not be null");
        return extractArgs(params);
    }

    /**
     * 按 SQL 中参数名出现顺序，从多个 Map 中逐行提取参数值，返回批量参数数组列表。
     *
     * <p>
     * SQL 仅在此方法首次调用时解析一次；每个 Map 按固定的参数名顺序提取值。
     * 值会经 {@link ParamBuilder#handleItem(Object)} 处理。
     * </p>
     *
     * @param batchParams 批量参数映射列表，每个 Map 代表一行参数
     * @return 批量参数数组列表，与 {@code batchParams} 顺序一致
     * @throws IllegalArgumentException 如果某个 Map 缺少 SQL 中引用的参数名
     */
    public List<Object[]> toBatchArgs(List<Map<String, ?>> batchParams) {
        AssertTools.checkNotNull(batchParams, "batchParams must not be null");
        final List<Object[]> result = new ArrayList<>(batchParams.size());
        for (final Map<String, ?> params : batchParams) {
            AssertTools.checkNotNull(params, "batchParams element must not be null");
            result.add(extractArgs(params));
        }
        return result;
    }

    private Object[] extractArgs(Map<String, ?> params) {
        final Object[] args = new Object[paramNames.size()];
        for (int i = 0; i < paramNames.size(); i++) {
            final String name = paramNames.get(i);
            AssertTools.checkArgument(params.containsKey(name),
                    () -> "No parameter found for '" + name + "'");
            args[i] = ParamBuilder.handleItem(params.get(name));
        }
        return args;
    }

    // #endregion

    // #region - Builder

    /**
     * {@link NamedParamSql} 的构建器。
     *
     * <p>
     * 仅用于解析 SQL 并记录参数名顺序，不绑定参数值。
     * 如需链式绑定参数值，请使用 {@link #prepare()} 获取 {@link PreparedSql.Builder}。
     * </p>
     */
    public static final class Builder {
        private String sql;

        private Builder() {
        }

        /**
         * 设置 SQL 语句。
         *
         * @param sql SQL 语句，其中命名参数以 {@code #{}} 包含，不可为 {@code null}
         * @return 当前 Builder 实例
         */
        public Builder sql(String sql) {
            AssertTools.checkNotNull(sql, "sql must not be null");
            this.sql = sql;
            return this;
        }

        /**
         * 构建 {@link NamedParamSql} 模板实例。
         *
         * <p>
         * 解析 SQL 中的 {@code #{paramName}} 为 {@code ?}，并记录参数名出现顺序。
         * 不绑定参数值。
         * </p>
         *
         * @return 构建完成的 {@link NamedParamSql} 模板实例
         */
        public NamedParamSql build() {
            AssertTools.checkNotNull(sql, "sql must be set before build");

            final List<String> names = new ArrayList<>();

            final TokenHandler handler = content -> {
                names.add(content);
                return "?";
            };

            final GenericTokenParser parser = new GenericTokenParser("#{", "}", handler);
            final String parsedSql = parser.parse(sql);

            return new NamedParamSql(parsedSql, names);
        }
    }

    // #endregion
}
