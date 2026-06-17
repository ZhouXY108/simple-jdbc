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

import java.sql.PreparedStatement;
import java.time.temporal.Temporal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import xyz.zhouxy.jdbc.util.AssertTools;

/**
 * ParamBuilder
 *
 * <p>
 * JDBC 参数构造器，将数据转换为 {@code Object[]} 类型，以传给 {@link PreparedStatement}
 * </p>
 *
 * @author ZhouXY
 * @since 1.0.0
 */
public class ParamBuilder {
    /**
     * 空参数数组常量
     *
     * <p>
     * 用于表示无参数的 SQL 操作
     */
    public static final Object[] EMPTY_OBJECT_ARRAY = {};

    /**
     * 构建 SQL 参数数组
     *
     * <p>
     * 将传入的参数转换为 {@code Object[]}，用于 {@link PreparedStatement} 的参数填充。
     * 支持自动拆箱 {@link Optional}、{@link OptionalInt}、{@link OptionalLong}、{@link OptionalDouble}。
     * 对于 {@link CharSequence}、{@link Number}、{@link Boolean}、{@link Temporal} 类型不做转换直接透传。
     * 如果传入的 {@code params} 为 {@code null} 或空，则返回 {@link #EMPTY_OBJECT_ARRAY}。
     *
     * @param params SQL 参数列表（可变参数）
     * @return 参数数组
     */
    public static Object[] buildParams(final Object... params) {
        if (params == null || params.length == 0) {
            return EMPTY_OBJECT_ARRAY;
        }
        return Arrays.stream(params)
                .map(ParamBuilder::handleItem)
                .toArray();
    }

    private static Object handleItem(Object param) {
        if (param == null) {
            return null;
        }
        if (param instanceof CharSequence) {
            return param.toString();
        }
        if (param instanceof Number) {
            return param;
        }
        if (param instanceof Boolean) {
            return param;
        }
        if (param instanceof Temporal) {
            return param;
        }
        if (param instanceof Optional) {
            return ((Optional<?>) param).orElse(null);
        }
        if (param instanceof OptionalInt) {
            return ((OptionalInt) param).isPresent() ? ((OptionalInt) param).getAsInt() : null;
        }
        if (param instanceof OptionalLong) {
            return ((OptionalLong) param).isPresent() ? ((OptionalLong) param).getAsLong() : null;
        }
        if (param instanceof OptionalDouble) {
            return ((OptionalDouble) param).isPresent() ? ((OptionalDouble) param).getAsDouble() : null;
        }
        return param;
    }

    /**
     * 批量构建参数列表
     *
     * <p>
     * 将集合中的每个元素通过 {@code func} 映射为 {@code Object[]}，
     * 最终返回 {@code List<Object[]>}，用于 {@link #batchUpdate} 批量操作。
     *
     * @param <T>  集合元素类型
     * @param c    待转换的集合
     * @param func 转换函数，将集合元素转换为参数数组
     * @return 参数数组列表
     * @throws NullPointerException 如果 {@code c} 或 {@code func} 为 {@code null}
     */
    public static <T> List<Object[]> buildBatchParams(final Collection<T> c, final Function<T, Object[]> func) {
        AssertTools.checkNotNull(c, "The collection can not be null.");
        AssertTools.checkNotNull(func, "The func can not be null.");
        if (c.isEmpty()) {
            return Collections.emptyList();
        }
        return c.stream().map(func).collect(Collectors.toList());
    }

    private ParamBuilder() {
        throw new IllegalStateException("Utility class");
    }
}
