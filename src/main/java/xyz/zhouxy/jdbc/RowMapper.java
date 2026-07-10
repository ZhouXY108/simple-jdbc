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
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
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
     * 每一行数据转换为 {@link HashMap}
     *
     * <p>
     * <b>注：如果两个属性映射到同一列名，后者静默覆盖前者。</b>
     */
    RowMapper<Map<String, @Nullable Object>> HASH_MAP_MAPPER = (rs, rowNumber) -> {
        Map<String, @Nullable Object> result = new HashMap<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String colName = metaData.getColumnLabel(i);
            result.put(colName, rs.getObject(colName));
        }
        return result;
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
