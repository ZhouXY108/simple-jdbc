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


/**
 * JDBC Null 值绑定策略。
 * <p>
 * 不同的数据库驱动对 null 值的类型推断机制差异巨大，此枚举用于配置框架在绑定
 * {@code null} 参数时的行为。
 *
 * @author ZhouXY
 * @since 1.1.0
 * @see JdbcConfig.Builder#nullBindingStrategy(NullBindingStrategy)
 */
public enum NullBindingStrategy {

    /**
     * 默认策略：使用 {@code ps.setNull(paramIndex, Types.NULL)}。
     * <p>
     * 适用于绝大多数现代数据库驱动（MySQL 8.x+、PostgreSQL、H2、SQL Server 等）。
     * 性能最优，无语义歧义。
     */
    STANDARD,

    /**
     * 精确策略：通过 {@code ParameterMetaData.getParameterType(paramIndex)} 获取
     * 目标列的精确 SQL 类型后调用 {@code ps.setNull(paramIndex, sqlType)}。
     * <p>
     * <b>⚠️ 警告：</b>在 {@code batchUpdate} 等高频场景中可能引发严重的性能下降
     * （每次绑定都可能触发元数据查询），除非底层连接池已开启 PreparedStatement 缓存。
     * <p>
     * 若 {@code getParameterMetaData()} 调用失败，将自动降级为 {@link #VARCHAR_FALLBACK}。
     */
    PARAMETER_METADATA,

    /**
     * 终极兜底策略：使用 {@code ps.setNull(paramIndex, Types.VARCHAR)}。
     * <p>
     * 兼容性最强。对于 NULL 值，几乎所有 JDBC 驱动都能接受 VARCHAR 类型。
     * 适用于旧版 Oracle、DB2 或任何对 {@code Types.NULL} 支持不佳的数据库。
     */
    VARCHAR_FALLBACK
}
