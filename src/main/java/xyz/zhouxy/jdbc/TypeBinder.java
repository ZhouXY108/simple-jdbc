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
 * 针对特定类型的参数绑定器。
 * <p>
 * 与 {@link ParameterBinder} 不同，此接口无需判断类型，
 * 框架会在调用前确保类型匹配（基于 {@code instanceof}，支持多态）。
 *
 * @param <T> 要绑定的参数类型
 * @author ZhouXY
 * @since 1.1.0
 * @see JdbcConfig.Builder#addParameterBinder(Class, TypeBinder)
 */
@FunctionalInterface
@NullMarked
public interface TypeBinder<T> {

    /**
     * 绑定参数。
     *
     * @param ps         PreparedStatement
     * @param paramIndex 参数索引 (1-based)
     * @param value      参数值，保证非空且类型匹配
     * @throws SQLException 如果发生数据库访问错误
     */
    void bind(PreparedStatement ps, int paramIndex, T value) throws SQLException;
}
