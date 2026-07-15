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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import xyz.zhouxy.jdbc.util.AssertTools;

/**
 * JDBC 实例级配置，用于控制常见的 {@link java.sql.Statement} 参数和
 * {@link ResultSet} 类型。
 *
 * <p>
 * 本类为不可变对象，通过 {@link Builder} 创建。
 * 可作用于 {@link SimpleJdbcTemplate}、{@link TransactionTemplate}、
 * {@link JdbcExecutor}、{@link xyz.zhouxy.jdbc.namedparam.NamedParamJdbcExecutor}
 * 等实例，影响其后续所有 JDBC 操作。
 * </p>
 *
 * <p>
 * 未显式设置的字段将使用 JDBC 驱动默认行为。
 * </p>
 *
 * @author ZhouXY
 * @since 1.1.0
 */
@NullMarked
public final class JdbcConfig {

    private final @Nullable Integer fetchSize;
    private final @Nullable Integer maxRows;
    private final @Nullable Integer queryTimeout;
    private final int resultSetType;
    private final int resultSetConcurrency;

    private final NullBindingStrategy nullBindingStrategy;
    private final List<ParameterBinder> parameterBinders;

    private JdbcConfig(Builder builder) {
        this.fetchSize = builder.fetchSize;
        this.maxRows = builder.maxRows;
        this.queryTimeout = builder.queryTimeout;
        this.resultSetType = builder.resultSetType;
        this.resultSetConcurrency = builder.resultSetConcurrency;
        this.nullBindingStrategy = builder.nullBindingStrategy != null
                ? builder.nullBindingStrategy
                : NullBindingStrategy.STANDARD;
        this.parameterBinders = builder.parameterBinders != null
                ? Collections.unmodifiableList(new ArrayList<>(builder.parameterBinders))
                : Collections.emptyList();
    }

    /**
     * 返回默认配置实例。
     *
     * <p>默认值：{@code resultSetType = TYPE_FORWARD_ONLY}，
     * {@code resultSetConcurrency = CONCUR_READ_ONLY}，
     * 其余字段为 {@code null}（由 JDBC 驱动自行决定）。</p>
     *
     * @return 默认配置
     */
    public static JdbcConfig defaults() {
        return new Builder().build();
    }

    /**
     * 创建一个新的 {@link Builder} 实例。
     *
     * @return Builder
     */
    public static Builder builder() {
        return new Builder();
    }

    // #region - getters

    /**
     * 获取 fetchSize。
     *
     * @return fetchSize，可能为 {@code null}
     */
    public @Nullable Integer getFetchSize() {
        return fetchSize;
    }

    /**
     * 获取 maxRows。
     *
     * @return maxRows，可能为 {@code null}
     */
    public @Nullable Integer getMaxRows() {
        return maxRows;
    }

    /**
     * 获取 queryTimeout（秒）。
     *
     * @return queryTimeout，可能为 {@code null}
     */
    public @Nullable Integer getQueryTimeout() {
        return queryTimeout;
    }

    /**
     * 获取结果集类型。
     *
     * @return 结果集类型
     * @see ResultSet#TYPE_FORWARD_ONLY
     * @see ResultSet#TYPE_SCROLL_INSENSITIVE
     * @see ResultSet#TYPE_SCROLL_SENSITIVE
     */
    public int getResultSetType() {
        return resultSetType;
    }

    /**
     * 获取结果集并发模式。
     *
     * @return 并发模式
     * @see ResultSet#CONCUR_READ_ONLY
     * @see ResultSet#CONCUR_UPDATABLE
     */
    public int getResultSetConcurrency() {
        return resultSetConcurrency;
    }

    /**
     * 获取 null 绑定策略。
     *
     * @return null 绑定策略
     */
    public NullBindingStrategy getNullBindingStrategy() {
        return nullBindingStrategy;
    }

    /**
     * 获取自定义参数绑定器列表。
     *
     * @return 自定义参数绑定器列表
     */
    public List<ParameterBinder> getParameterBinders() {
        return parameterBinders;
    }

    // #endregion

    // #region - equals, hashCode, toString

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof JdbcConfig)) return false;
        JdbcConfig that = (JdbcConfig) o;
        return resultSetType == that.resultSetType
                && resultSetConcurrency == that.resultSetConcurrency
                && nullBindingStrategy == that.nullBindingStrategy
                && Objects.equals(fetchSize, that.fetchSize)
                && Objects.equals(maxRows, that.maxRows)
                && Objects.equals(queryTimeout, that.queryTimeout)
                && Objects.equals(parameterBinders, that.parameterBinders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                fetchSize, maxRows, queryTimeout,
                resultSetType, resultSetConcurrency,
                nullBindingStrategy, parameterBinders);
    }

    @Override
    public String toString() {
        return "JdbcConfig{"
                + "fetchSize=" + fetchSize
                + ", maxRows=" + maxRows
                + ", queryTimeout=" + queryTimeout
                + ", resultSetType=" + resultSetType
                + ", resultSetConcurrency=" + resultSetConcurrency
                + ", nullBindingStrategy=" + nullBindingStrategy
                + ", parameterBinders=" + parameterBinders
                + '}';
    }

    // #endregion

    /**
     * {@link JdbcConfig} 的构建器。
     */
    public static final class Builder {

        private @Nullable Integer fetchSize;
        private @Nullable Integer maxRows;
        private @Nullable Integer queryTimeout;
        private int resultSetType = ResultSet.TYPE_FORWARD_ONLY;
        private int resultSetConcurrency = ResultSet.CONCUR_READ_ONLY;

        private NullBindingStrategy nullBindingStrategy = NullBindingStrategy.STANDARD;
        private List<ParameterBinder> parameterBinders = Collections.emptyList();

        private Builder() {
        }

        /**
         * 设置 fetchSize。
         *
         * @param fetchSize 每次获取的记录行数；必须 >= 0
         * @return this
         */
        public Builder fetchSize(int fetchSize) {
            AssertTools.checkArgument(fetchSize >= 0, "fetchSize must be >= 0");
            this.fetchSize = fetchSize;
            return this;
        }

        /**
         * 设置 maxRows。
         *
         * @param maxRows 最大行数；必须 >= 0，0 表示不限制
         * @return this
         */
        public Builder maxRows(int maxRows) {
            AssertTools.checkArgument(maxRows >= 0, "maxRows must be >= 0");
            this.maxRows = maxRows;
            return this;
        }

        /**
         * 设置 queryTimeout（秒）。
         *
         * @param queryTimeout 查询超时秒数；必须 >= 0，0 表示不限制
         * @return this
         */
        public Builder queryTimeout(int queryTimeout) {
            AssertTools.checkArgument(queryTimeout >= 0, "queryTimeout must be >= 0");
            this.queryTimeout = queryTimeout;
            return this;
        }

        /**
         * 设置结果集类型。
         *
         * @param resultSetType 结果集类型；必须是
         *        {@link ResultSet#TYPE_FORWARD_ONLY}、
         *        {@link ResultSet#TYPE_SCROLL_INSENSITIVE} 或
         *        {@link ResultSet#TYPE_SCROLL_SENSITIVE} 之一
         * @return this
         */
        public Builder resultSetType(int resultSetType) {
            AssertTools.checkArgument(
                    resultSetType == ResultSet.TYPE_FORWARD_ONLY
                            || resultSetType == ResultSet.TYPE_SCROLL_INSENSITIVE
                            || resultSetType == ResultSet.TYPE_SCROLL_SENSITIVE,
                    "Invalid resultSetType: " + resultSetType);
            this.resultSetType = resultSetType;
            return this;
        }

        /**
         * 设置结果集并发模式。
         *
         * @param resultSetConcurrency 并发模式；必须是
         *        {@link ResultSet#CONCUR_READ_ONLY} 或
         *        {@link ResultSet#CONCUR_UPDATABLE} 之一
         * @return this
         */
        public Builder resultSetConcurrency(int resultSetConcurrency) {
            AssertTools.checkArgument(
                    resultSetConcurrency == ResultSet.CONCUR_READ_ONLY
                            || resultSetConcurrency == ResultSet.CONCUR_UPDATABLE,
                    "Invalid resultSetConcurrency: " + resultSetConcurrency);
            this.resultSetConcurrency = resultSetConcurrency;
            return this;
        }

        /**
         * 设置 Null 值绑定策略。默认为 {@link NullBindingStrategy#STANDARD}。
         *
         * @param strategy null 值绑定策略
         * @return this
         * @see NullBindingStrategy
         */
        public Builder nullBindingStrategy(NullBindingStrategy strategy) {
            this.nullBindingStrategy = AssertTools.checkArgumentNotNull(
                    strategy, "nullBindingStrategy could not be null.");
            return this;
        }

        /**
         * 注册通用的自定义参数绑定器。
         * <p>
         * 适用于需要复杂条件判断的场景（如基于注解、接口组合等）。
         * 框架保证传入的 value 非空。
         *
         * @param binder 自定义参数绑定器
         * @return this
         */
        public Builder addParameterBinder(ParameterBinder binder) {
            AssertTools.checkArgumentNotNull(binder, "parameterBinder could not be null.");
            if (this.parameterBinders.isEmpty()) {
                this.parameterBinders = new ArrayList<>();
            }
            this.parameterBinders.add(binder);
            return this;
        }

        /**
         * 注册针对特定类型的参数绑定器（推荐方式）。
         * <p>
         * 框架自动处理 {@code instanceof} 判断（支持多态），用户只需关注绑定逻辑。
         * <p>
         * 示例：
         * <pre>{@code
         * .addParameterBinder(Enum.class, (ps, i, v) -> ps.setString(i, v.name()))
         * }</pre>
         *
         * @param type   要拦截的参数类型
         * @param binder 绑定逻辑
         * @return this
         */
        public <T> Builder addParameterBinder(Class<T> type, TypeBinder<T> binder) {
            AssertTools.checkArgumentNotNull(type, "type could not be null.");
            AssertTools.checkArgumentNotNull(binder, "typeBinder could not be null.");
            return addParameterBinder((ps, index, value) -> {
                if (type.isInstance(value)) {
                    binder.bind(ps, index, type.cast(value));
                    return true;
                }
                return false;
            });
        }

        /**
         * 构建 {@link JdbcConfig} 实例。
         *
         * @return 新的 JdbcConfig 实例
         */
        public JdbcConfig build() {
            return new JdbcConfig(this);
        }
    }
}
