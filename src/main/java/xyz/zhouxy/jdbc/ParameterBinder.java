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
import java.sql.SQLException;

import org.jspecify.annotations.NullMarked;

/**
 * 通用参数绑定器。
 * <p>
 * 允许用户拦截特定的参数并自定义其绑定逻辑（如处理注解驱动的类型、组合条件等）。
 * <p>
 * <b>注意：</b>框架保证传入的 {@code value} 始终非空。
 * {@code null} 值由 {@link NullBindingStrategy} 单独处理，不会进入此绑定器。
 *
 * @author ZhouXY
 * @since 1.1.0
 * @see JdbcConfig.Builder#addParameterBinder(ParameterBinder)
 * @see JdbcConfig.Builder#addParameterBinder(Class, TypeBinder)
 */
@FunctionalInterface
@NullMarked
public interface ParameterBinder {

    /**
     * 尝试绑定参数。
     *
     * @param ps         PreparedStatement
     * @param paramIndex 参数索引 (1-based)
     * @param value      参数值，保证非空
     * @return {@code true} 表示已处理（框架不再调用后续规则或 {@code setObject}），
     *         {@code false} 表示未处理（交由后续规则或默认逻辑）
     * @throws SQLException 如果发生数据库访问错误
     */
    boolean bind(PreparedStatement ps, int paramIndex, Object value) throws SQLException;
}
