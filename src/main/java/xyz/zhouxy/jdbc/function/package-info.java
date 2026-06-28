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

/**
 * 可抛出受检异常的函数式接口。
 *
 * <p>
 * 提供与标准 {@link java.util.function} 包类似的函数式接口（{@code Consumer}、{@code BiConsumer}、
 * {@code Predicate}、{@code BiPredicate}），但其抽象方法允许抛出受检异常，
 * 用于简化事务回调等场景中的异常处理。
 * </p>
 *
 * <ul>
 *   <li>{@link xyz.zhouxy.jdbc.function.ThrowingConsumer} —— 单参数消费者</li>
 *   <li>{@link xyz.zhouxy.jdbc.function.ThrowingBiConsumer} —— 双参数消费者</li>
 *   <li>{@link xyz.zhouxy.jdbc.function.ThrowingPredicate} —— 单参数谓词</li>
 *   <li>{@link xyz.zhouxy.jdbc.function.ThrowingBiPredicate} —— 双参数谓词</li>
 * </ul>
 *
 * @author ZhouXY
 * @since 1.1.0
 */
@org.jspecify.annotations.NullMarked
package xyz.zhouxy.jdbc.function;
