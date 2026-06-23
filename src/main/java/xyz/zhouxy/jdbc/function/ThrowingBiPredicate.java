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

/**
 * 可抛出受检异常的双参数谓词函数式接口。
 *
 * <p>
 * 类似于 {@link java.util.function.BiPredicate}，但 {@code test} 方法允许抛出受检异常。
 * </p>
 *
 * @param <T> 第一个输入类型
 * @param <U> 第二个输入类型
 * @param <E> 允许抛出的异常类型
 * @author ZhouXY
 * @since 1.1.0
 */
@FunctionalInterface
public interface ThrowingBiPredicate<T, U, E extends Exception> {

    /**
     * 对给定参数执行此谓词判断。
     *
     * @param t 第一个输入参数
     * @param u 第二个输入参数
     * @return 谓词判断结果
     * @throws E 异常
     */
    boolean test(T t, U u) throws E;
}
