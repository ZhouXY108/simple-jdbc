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
import java.util.Map;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 将 {@link ResultSet} 中每一行数据转换为 {@link Map} 的 {@link RowMapper} 基础实现。
 *
 * <p>
 * 该类定义了标准的列映射逻辑，子类只需重写 {@link #createMap()} 方法即可指定所需的 Map 实现。
 * 例如，重写 {@code createMap()} 返回 {@code new TreeMap<>()} 或
 * {@code new LinkedHashMap<>()} 即可获得按自然顺序排序或保持列查询顺序的 Map 映射器。
 * </p>
 *
 * @author ZhouXY
 * @since 1.1.0
 * @see RowMapper#HASH_MAP_MAPPER
 * @see RowMapper#LINKED_HASH_MAP_MAPPER
 */
@NullMarked
public abstract class MapRowMapper<T extends Map<String, @Nullable Object>> implements RowMapper<T> {

    /** {@inheritDoc} */
    @Override
    public T mapRow(ResultSet rs, @SuppressWarnings("unused") int rowNumber) throws SQLException {
        T result = createMap();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String colName = metaData.getColumnLabel(i);
            result.put(colName, rs.getObject(i));
        }
        return result;
    }

    /**
     * 创建一个 {@link Map} 实例，用于承载当前行的列数据。
     *
     * <p>
     * 该方法在每一行数据映射时被调用一次，返回的 Map 随后会通过
     * {@link Map#put(Object, Object)} 填充列数据，因此返回的 Map <b>必须是可变的</b>。
     * </p>
     * <p>
     * 一般情况下应返回一个新的、空的 Map 实例；如有需要（如预填充默认值），
     * 也可以返回包含初始数据的 Map。
     * </p>
     *
     * @return 一个可变 Map 实例（不可为 {@code null}）
     */
    protected abstract T createMap();

}
