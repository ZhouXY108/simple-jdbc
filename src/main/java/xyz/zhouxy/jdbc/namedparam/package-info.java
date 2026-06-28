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
 * 命名参数（{@code #{paramName}}）解析与操作支持。
 *
 * <p>
 * 核心组件：
 * <ul>
 *   <li>{@link xyz.zhouxy.jdbc.namedparam.NamedParamSql} —— 命名参数 SQL 模板，
 *       解析 {@code #{paramName}} 为 JDBC {@code ?} 占位符，支持延迟绑值</li>
 *   <li>{@link xyz.zhouxy.jdbc.namedparam.PreparedSql} —— 预编译的命名参数 SQL，
 *       同时持有转换后的 SQL 和参数值数组，构建后不可变</li>
 *   <li>{@link xyz.zhouxy.jdbc.namedparam.NamedParamJdbcOperations} ——
 *       命名参数 JDBC 操作接口，提供与 {@link xyz.zhouxy.jdbc.JdbcOperations}
 *       对等的查询、更新、批量操作方法</li>
 * </ul>
 *
 * @author ZhouXY
 * @since 1.1.0
 * @see xyz.zhouxy.jdbc.namedparam.NamedParamSql
 * @see xyz.zhouxy.jdbc.namedparam.NamedParamJdbcOperations
 */
@org.jspecify.annotations.NullMarked
package xyz.zhouxy.jdbc.namedparam;
