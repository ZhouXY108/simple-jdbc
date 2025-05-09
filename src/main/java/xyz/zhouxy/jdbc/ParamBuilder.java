/*
 * Copyright 2022-2025 the original author or authors.
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
import java.util.stream.Stream;

import xyz.zhouxy.plusone.commons.collection.CollectionTools;
import xyz.zhouxy.plusone.commons.util.ArrayTools;
import xyz.zhouxy.plusone.commons.util.AssertTools;
import xyz.zhouxy.plusone.commons.util.OptionalTools;

/**
 * ParamBuilder
 *
 * <p>
 * JDBC 参数构造器，将数据转换为 {@code Object[]} 类型，以传给 {@link PreparedStatement}
 * </p>
 *
 * @author <a href="http://zhouxy.xyz:3000/ZhouXY108">ZhouXY</a>
 * @since 1.0.0
 */
public class ParamBuilder {
    public static final Object[] EMPTY_OBJECT_ARRAY = {};

    public static Object[] buildParams(final Object... params) {
        if (ArrayTools.isEmpty(params)) {
            return EMPTY_OBJECT_ARRAY;
        }
        return buildParamsFromStream(Arrays.stream(params));
    }

    public static Object[] buildParams(final Collection<?> params) {
        if (CollectionTools.isEmpty(params)) {
            return EMPTY_OBJECT_ARRAY;
        }
        return buildParamsFromStream(params.stream());
    }

    private static Object[] buildParamsFromStream(Stream<?> stream) {
        return stream
                .map(param -> {
                    if (param instanceof Optional) {
                        return OptionalTools.orElseNull((Optional<?>) param);
                    }
                    if (param instanceof OptionalInt) {
                        return OptionalTools.toInteger((OptionalInt) param);
                    }
                    if (param instanceof OptionalLong) {
                        return OptionalTools.toLong((OptionalLong) param);
                    }
                    if (param instanceof OptionalDouble) {
                        return OptionalTools.toDouble((OptionalDouble) param);
                    }
                    return param;
                })
                .toArray();
    }

    public static <T> List<Object[]> buildBatchParams(final Collection<T> c, final Function<T, Object[]> func) {
        AssertTools.checkNotNull(c, "The collection can not be null.");
        AssertTools.checkNotNull(func, "The func can not be null.");
        if (CollectionTools.isEmpty(c)) {
            return Collections.emptyList();
        }
        return c.stream().map(func).collect(Collectors.toList());
    }

    private ParamBuilder() {
        throw new IllegalStateException("Utility class");
    }
}
