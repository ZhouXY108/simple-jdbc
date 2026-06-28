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

import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import xyz.zhouxy.jdbc.ParamBuilder;
import xyz.zhouxy.jdbc.util.AssertTools;

/**
 * 预编译的命名参数 SQL，同时持有转换后的 JDBC SQL 和参数值数组。
 *
 * <p>
 * 由 {@link NamedParamSql#prepare()} 或 {@link #sql(String)} / {@link #sql(NamedParamSql)}
 * 进入 Builder 链式构建。构建后不可变，线程安全。
 * </p>
 *
 * <p><strong>使用示例：</strong></p>
 * <pre>{@code
 * // 从 NamedParamSql 模板构建
 * PreparedSql ps = NamedParamSql.of("SELECT * FROM t WHERE a = #{a} AND b = #{b}")
 *     .prepare()
 *     .param("a", 1)
 *     .param("b", 2)
 *     .build();
 *
 * // 直接从 SQL 字符串构建
 * PreparedSql ps2 = PreparedSql
 *     .sql("INSERT INTO t VALUES(#{x}, #{y})")
 *     .param("x", 100)
 *     .param("y", 200)
 *     .build();
 *
 * // 从已有 NamedParamSql 模板构建（复用解析结果）
 * NamedParamSql tmpl = NamedParamSql.of("SELECT * FROM t WHERE id = #{id}");
 * PreparedSql ps3 = PreparedSql
 *     .sql(tmpl)
 *     .param("id", 1)
 *     .build();
 *
 * String jdbcSql = ps.getSql();   // SELECT * FROM t WHERE a = ? AND b = ?
 * Object[] args = ps.getArgs();   // [1, 2]
 * }</pre>
 *
 * @author ZhouXY
 * @since 1.1.0
 * @see NamedParamSql
 */
public final class PreparedSql {

    private final String sql;
    private final @Nullable Object[] args;

    private PreparedSql(String sql, @Nullable Object[] args) {
        this.sql = sql;
        this.args = args.clone();
    }

    /**
     * 获取转换后的 JDBC SQL，命名参数已被替换为 {@code ?}
     *
     * @return JDBC SQL
     */
    public String getSql() {
        return sql;
    }

    /**
     * 获取参数值数组，顺序对应 SQL 中 {@code ?} 的位置
     *
     * @return 参数值数组（防御性拷贝）
     */
    public @Nullable Object[] getArgs() {
        return args.clone();
    }

    // #region - 静态入口

    /**
     * 从 SQL 字符串启动 Builder 链式构建。
     *
     * <p>
     * 内部会先解析 SQL 中的 {@code #{paramName}} 为 {@code ?}，
     * 再通过 {@link Builder#param(String, Object)} 链式绑定参数值。
     * </p>
     *
     * @param sql 包含命名参数的 SQL
     * @return Builder 实例
     */
    public static Builder sql(String sql) {
        AssertTools.checkNotNull(sql, "sql must not be null");
        return new Builder(NamedParamSql.of(sql));
    }

    /**
     * 从已有 {@link NamedParamSql} 模板启动 Builder 链式构建。
     *
     * <p>
     * 复用模板的解析结果，仅绑定参数值。
     * </p>
     *
     * @param template 命名参数 SQL 模板
     * @return Builder 实例
     */
    public static Builder sql(NamedParamSql template) {
        AssertTools.checkNotNull(template, "template must not be null");
        return new Builder(template);
    }

    // #endregion

    // #region - Builder

    /**
     * {@link PreparedSql} 的构建器。
     */
    public static final class Builder {
        private final NamedParamSql template;
        private final Map<String, @Nullable Object> params = new LinkedHashMap<>();

        private Builder(NamedParamSql template) {
            this.template = template;
        }

        /**
         * 添加命名参数值。
         *
         * <p>
         * 可多次调用；同名参数后设的覆盖前设的。
         * SQL 中未引用的多余参数将被静默忽略（宽松策略）。
         * </p>
         *
         * @param name  参数名
         * @param value 参数值
         * @return 当前 Builder 实例
         */
        public Builder param(String name, @Nullable Object value) {
            AssertTools.checkNotNull(name, "name must not be null");
            this.params.put(name, value);
            return this;
        }

        /**
         * 构建 {@link PreparedSql} 实例。
         *
         * <p>
         * 校验 SQL 中引用的所有参数名均已提供；多余参数静默忽略。
         * 参数值会经 {@link ParamBuilder#handleItem(Object)} 处理。
         * </p>
         *
         * @return 构建完成的 {@link PreparedSql} 实例
         * @throws IllegalArgumentException 如果缺少 SQL 中引用的参数名
         */
        public PreparedSql build() {
            return new PreparedSql(template.getSql(), template.toArgs(params));
        }
    }

    // #endregion
}
