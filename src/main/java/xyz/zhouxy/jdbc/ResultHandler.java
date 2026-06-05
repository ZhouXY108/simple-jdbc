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
import java.util.ArrayList;
import java.util.List;

/**
 * ResultHandler
 *
 * <p>
 * 处理 {@link ResultSet}
 * </p>
 *
 * @author ZhouXY
 * @since 1.0.0
 */
@FunctionalInterface
public interface ResultHandler<T> {

    /**
     * 将 {@link ResultSet} 转换为指定类型的对象
     *
     * @param resultSet {@link ResultSet}
     * @return 转换后的对象
     *
     * @throws SQLException 数据库执行异常
     */
    T handle(ResultSet resultSet) throws SQLException;

    /**
     * 创建一个返回 {@link List} 的 {@link ResultHandler}，将 {@link ResultSet} 中的每一行
     * 通过指定的 {@link RowMapper} 映射为对象，最终收集为一个 {@link List}。
     *
     * @param <T>        列表元素类型
     * @param rowMapper  行映射器，用于将 {@link ResultSet} 的单行转换为对象
     * @return 返回 {@code List<T>} 的 {@code ResultHandler}
     * @since 1.0.0
     * @see RowMapper
     */
    static <T> ResultHandler<List<T>> mapToList(RowMapper<T> rowMapper) {
        return resultSet -> {
            List<T> result = new ArrayList<>();
            int rowNumber = 0;
            while (resultSet.next()) {
                T e = rowMapper.mapRow(resultSet, rowNumber++);
                result.add(e);
            }
            return result;
        };
    }
}
