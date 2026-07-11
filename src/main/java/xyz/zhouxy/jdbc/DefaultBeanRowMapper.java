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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * DefaultBeanRowMapper
 *
 * <p>
 * 自 1.1.0 起，本类已废弃，并作为 {@link SimpleBeanRowMapper} 的兼容别名存在。
 * 内部实现完全委托给 {@link SimpleBeanRowMapper}，行为与其保持一致。
 *
 * @author ZhouXY
 * @since 1.0.0
 * @deprecated 自 1.1.0 起，请使用 {@link SimpleBeanRowMapper}。本类将在 1.2.0 中移除。
 */
@Deprecated
@NullMarked
public class DefaultBeanRowMapper<T extends @Nullable Object> implements RowMapper<T> {

    private final SimpleBeanRowMapper<T> delegate;

    private DefaultBeanRowMapper(SimpleBeanRowMapper<T> delegate) {
        this.delegate = delegate;
    }

    /**
     * 创建一个 {@code DefaultBeanRowMapper}
     *
     * @param <T>      Bean 类型
     * @param beanType Bean 类型
     * @return DefaultBeanRowMapper 对象
     * @throws IllegalStateException 创建 {@code DefaultBeanRowMapper} 出现错误的异常时抛出
     * @deprecated 自 1.1.0 起，请使用 {@link SimpleBeanRowMapper#of(Class)}。
     */
    @Deprecated
    public static <T extends @Nullable Object> DefaultBeanRowMapper<T> of(Class<@NonNull T> beanType) {
        return of(beanType, null);
    }

    /**
     * 创建一个 {@code DefaultBeanRowMapper}
     *
     * @param <T>            Bean 类型
     * @param beanType       Bean 类型
     * @param propertyColMap Bean 字段与列名的映射关系。key 是字段，value 是列名。
     * @return {@code DefaultBeanRowMapper} 对象
     * @throws IllegalStateException 创建 {@code DefaultBeanRowMapper} 出现错误的异常时抛出
     * @deprecated 自 1.1.0 起，请使用 {@link SimpleBeanRowMapper#of(Class, Map)}。
     */
    @Deprecated
    public static <T extends @Nullable Object> DefaultBeanRowMapper<T> of(
            Class<@NonNull T> beanType,
            @Nullable Map<String, String> propertyColMap) {
        return new DefaultBeanRowMapper<>(SimpleBeanRowMapper.of(beanType, propertyColMap));
    }

    /** {@inheritDoc} */
    @Override
    public T mapRow(ResultSet rs, int rowNumber) throws SQLException {
        return delegate.mapRow(rs, rowNumber);
    }
}
