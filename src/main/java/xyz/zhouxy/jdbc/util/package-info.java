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
 * 工具包，提供断言校验、命名转换、标记解析等基础工具。
 *
 * <p>
 * 核心组件：
 * <ul>
 *   <li>{@link xyz.zhouxy.jdbc.util.AssertTools} —— 断言工具，提供参数校验、
 *       状态检查和条件断言方法</li>
 *   <li>{@link xyz.zhouxy.jdbc.util.NamingTools} —— 命名转换工具，支持
 *       小驼峰命名与 snake_case 之间的转换</li>
 *   <li>{@link xyz.zhouxy.jdbc.util.GenericTokenParser} —— 通用标记解析器，
 *       用于解析 {@code #{paramName}} 等占位符语法</li>
 *   <li>{@link xyz.zhouxy.jdbc.util.TokenHandler} —— 标记处理器接口</li>
 * </ul>
 *
 * @author ZhouXY
 * @since 1.0.0
 */
package xyz.zhouxy.jdbc.util;
