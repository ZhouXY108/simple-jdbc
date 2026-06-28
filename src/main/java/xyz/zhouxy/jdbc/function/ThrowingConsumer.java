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

package xyz.zhouxy.jdbc.function;

import org.jspecify.annotations.Nullable;

/**
 * 可抛出受检异常的函数式接口。
 *
 * <p>
 * 类似于 {@link java.util.function.Consumer}，但 {@code accept} 方法允许抛出受检异常。
 * </p>
 *
 * @param <T> 输入类型
 * @param <E> 允许抛出的异常类型
 * @author ZhouXY
 * @since 1.1.0
 */
@FunctionalInterface
public interface ThrowingConsumer<T extends @Nullable Object,
                                  E extends @Nullable Exception> {

    /**
     * 对给定参数执行此操作。
     *
     * @param t 输入参数
     * @throws E 异常
     */
    void accept(T t) throws E;
}
