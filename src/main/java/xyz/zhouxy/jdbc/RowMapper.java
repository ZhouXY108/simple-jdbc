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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * RowMapper
 *
 * <p>
 * {@link ResultSet} 中每一行数据的处理逻辑。
 * </p>
 *
 * @author ZhouXY
 * @since 1.0.0
 */
@FunctionalInterface
@NullMarked
public interface RowMapper<T extends @Nullable Object> {
    T mapRow(ResultSet rs, @SuppressWarnings("unused") int rowNumber) throws SQLException;

    /**
     * 每一行数据转换为 {@link HashMap}，不保证键的迭代顺序。
     *
     * <p>
     * 如需保持列的查询顺序，请使用 {@link #LINKED_HASH_MAP_MAPPER}。
     * </p>
     * <p>
     * <b>如果两列映射到同一列名，后者静默覆盖前者。</b>
     * 自 1.1.0 起，重复列名按列索引取值并依次覆盖，最终 Map 中保留的是最后一列的值。
     * </p>
     */
    RowMapper<Map<String, @Nullable Object>> HASH_MAP_MAPPER = new MapRowMapper<Map<String, @Nullable Object>>() {
        @Override
        protected @NonNull Map<String, @Nullable Object> createMap() {
            return new HashMap<>();
        }
    };

    /**
     * 每一行数据转换为 {@link LinkedHashMap}，保持列的查询顺序。
     *
     * <p>
     * 自 1.1.0 起作为 {@code queryList(sql, params)} 和 {@code queryFirst(sql, params)}
     * 方法的默认 Map 映射器。如需无序的低开销实现，可使用 {@link #HASH_MAP_MAPPER}。
     * </p>
     * <p>
     * <b>注：如果两列映射到同一列名，后者静默覆盖前者。</b>
     * </p>
     *
     * @since 1.1.0
     */
    RowMapper<Map<String, @Nullable Object>> LINKED_HASH_MAP_MAPPER = new MapRowMapper<Map<String, @Nullable Object>>() {
        @Override
        protected @NonNull Map<String, @Nullable Object> createMap() {
            return new LinkedHashMap<>();
        }
    };

    /**
     * 默认实现的将 {@link ResultSet} 转换为 Java Bean 的 {@link RowMapper}。
     *
     * @param beanType Java Bean 的类型
     * @param <T> Java Bean 的类型
     *
     * @return {@link DefaultBeanRowMapper}
     * @throws IllegalStateException 如果创建 {@link DefaultBeanRowMapper} 失败
     */
    static <T extends @Nullable Object> RowMapper<T> beanRowMapper(Class<@NonNull T> beanType) {
        return DefaultBeanRowMapper.of(beanType);
    }

    /**
     * 默认实现的将 {@link ResultSet} 转换为 Java Bean 的 {@link RowMapper}。
     *
     * @param beanType Java Bean 的类型
     * @param propertyColMap Java Bean 属性名与数据库列名的映射关系
     * @param <T> Java Bean 的类型
     *
     * @return {@link DefaultBeanRowMapper}
     * @throws IllegalStateException 如果创建 {@link DefaultBeanRowMapper} 失败
     */
    static <T extends @Nullable Object> RowMapper<T> beanRowMapper(Class<@NonNull T> beanType,
            Map<String, String> propertyColMap) {
        return DefaultBeanRowMapper.of(beanType, propertyColMap);
    }
}
