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
 * Simple JDBC —— 基于原生 JDBC 的轻量级封装库，无运行时第三方依赖。
 *
 * <p>
 * 核心组件：
 * <ul>
 *   <li>操作模板与接口：
 *     <ul>
 *       <li>{@link xyz.zhouxy.jdbc.SimpleJdbcTemplate} —— JDBC 操作模板类，提供查询、更新、
 *           批量操作及事务入口</li>
 *       <li>{@link xyz.zhouxy.jdbc.JdbcOperations} —— 统一操作接口，定义查询、更新、批量操作的 API</li>
 *       <li>{@link xyz.zhouxy.jdbc.TransactionTemplate} —— 事务模板，负责事务生命周期管理</li>
 *       <li>{@link xyz.zhouxy.jdbc.ParamBuilder} —— 参数构建工具</li>
 *     </ul>
 *   </li>
 *   <li>底层执行器：
 *     <ul>
 *       <li>{@link xyz.zhouxy.jdbc.JdbcOperationSupport} —— JDBC 底层操作支持类</li>
 *       <li>{@link xyz.zhouxy.jdbc.JdbcExecutor} —— 位置参数执行器，面向外部已持有 Connection 的场景</li>
 *       <li>{@link xyz.zhouxy.jdbc.namedparam.NamedParamJdbcExecutor} —— 命名参数执行器，面向外部已持有 Connection 的场景</li>
 *     </ul>
 *   </li>
 *   <li>自定义配置：
 *     <ul>
 *       <li>{@link xyz.zhouxy.jdbc.JdbcConfig} —— JDBC 实例级配置，控制 Statement 参数与 ResultSet 类型</li>
 *       <li>{@link xyz.zhouxy.jdbc.NullBindingStrategy} —— JDBC Null 值绑定策略</li>
 *       <li>{@link xyz.zhouxy.jdbc.ParameterBinder} —— 通用参数绑定器</li>
 *       <li>{@link xyz.zhouxy.jdbc.TypeBinder} —— 针对特定类型的参数绑定器</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <p>
 * 主要子包：
 * <ul>
 *   <li>{@link xyz.zhouxy.jdbc.util} —— 工具包，包含断言、命名转换等基础工具</li>
 *   <li>{@link xyz.zhouxy.jdbc.function} —— 可抛出受检异常的函数式接口</li>
 *   <li>{@link xyz.zhouxy.jdbc.namedparam} —— 命名参数（{@code #{paramName}}）解析与操作支持</li>
 * </ul>
 *
 * @author ZhouXY
 * @since 1.0.0
 */
package xyz.zhouxy.jdbc;
