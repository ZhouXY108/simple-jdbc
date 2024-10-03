/*
 * Copyright 2022-2024 the original author or authors.
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

@FunctionalInterface
public interface ResultMap<T> {
    T map(ResultSet rs, int rowNumber) throws SQLException;

    public static final ResultMap<Map<String, Object>> mapResultMap = (rs, rowNumber) -> {
        Map<String, Object> result = new HashMap<>();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String colName = metaData.getColumnName(i);
            result.put(colName, rs.getObject(colName));
        }
        return result;
    };


    public static final ResultMap<DbRecord> recordResultMap = (rs, rowNumber) -> {
        DbRecord result = new DbRecord();
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String colName = metaData.getColumnName(i);
            result.put(colName, rs.getObject(colName));
        }
        return result;
    };
}
