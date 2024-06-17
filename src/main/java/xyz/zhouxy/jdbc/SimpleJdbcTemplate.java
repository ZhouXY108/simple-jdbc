/*
 * Copyright 2022-2023 the original author or authors.
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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;

import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import xyz.zhouxy.plusone.commons.collection.CollectionTools;
import xyz.zhouxy.plusone.commons.util.ArrayTools;
import xyz.zhouxy.plusone.commons.util.OptionalUtil;

@Beta
public class SimpleJdbcTemplate {

    public static JdbcExecutor connect(final Connection conn) {
        return new JdbcExecutor(conn);
    }

    public static String paramsToString(Object[] params) {
        return Arrays.toString(params);
    }

    public static String paramsToString(final Collection<Object[]> params) {
        if (params == null) {
            return "null";
        }
        if (params.isEmpty()) {
            return "[]";
        }
        int iMax = params.size() - 1;
        StringBuilder b = new StringBuilder();
        b.append('[');
        int i = 0;
        for (Object[] p : params) {
            b.append(Arrays.toString(p));
            if (i == iMax) {
                return b.append(']').toString();
            }
            b.append(',');
            i++;
        }
        return b.append(']').toString();
    }

    private SimpleJdbcTemplate() {
        throw new IllegalStateException("Utility class");
    }

    public static class JdbcExecutor {

        private final Connection conn;

        public JdbcExecutor(Connection conn) {
            this.conn = conn;
        }

        public <T> List<T> query(String sql, Object[] params, ResultMap<T> resultMap) throws SQLException {
            try (PreparedStatement stmt = this.conn.prepareStatement(sql)) {
                fillStatement(stmt, params);
                try (ResultSet rs = stmt.executeQuery()) {
                    List<T> result = new ArrayList<>();
                    int rowNumber = 0;
                    while (rs.next()) {
                        T e = resultMap.map(rs, rowNumber++);
                        result.add(e);
                    }
                    return result;
                }
            }
        }

        public <T> Optional<T> queryFirst(String sql, Object[] params, ResultMap<T> resultMap) throws SQLException {
            return query(sql, params, resultMap).stream().findFirst();
        }

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

        public List<Map<String, Object>> query(String sql, Object... params) throws SQLException {
            return query(sql, params, mapResultMap);
        }

        public Optional<Map<String, Object>> queryFirst(String sql, Object... params) throws SQLException {
            return queryFirst(sql, params, mapResultMap);
        }

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

        public List<DbRecord> queryToRecordList(String sql, Object... params) throws SQLException {
            return query(sql, params, recordResultMap);
        }

        public Optional<DbRecord> queryFirstRecord(String sql, Object... params) throws SQLException {
            return queryFirst(sql, params, recordResultMap);
        }

        public Optional<String> queryToString(String sql, Object... params) throws SQLException {
            return queryFirst(sql, params, (rs, rowNumber) -> rs.getString(1));
        }

        public OptionalInt queryToInt(String sql, Object... params) throws SQLException {
            Optional<Integer> result = queryFirst(sql, params, (rs, rowNumber) -> rs.getInt(1));
            return OptionalUtil.toOptionalInt(result);
        }

        public OptionalLong queryToLong(String sql, Object... params) throws SQLException {
            Optional<Long> result = queryFirst(sql, params, (rs, rowNumber) -> rs.getLong(1));
            return OptionalUtil.toOptionalLong(result);
        }

        public OptionalDouble queryToDouble(String sql, Object... params) throws SQLException {
            Optional<Double> result = queryFirst(sql, params, (rs, rowNumber) -> rs.getDouble(1));
            return OptionalUtil.toOptionalDouble(result);
        }

        public Optional<BigDecimal> queryToBigDecimal(String sql, Object... params) throws SQLException {
            return queryFirst(sql, params, (rs, rowNumber) -> rs.getBigDecimal(1));
        }

        public int update(String sql, Object... params) throws SQLException {
            try (PreparedStatement stmt = this.conn.prepareStatement(sql)) {
                fillStatement(stmt, params);
                return stmt.executeUpdate();
            }
        }

        public int[] batchUpdate(String sql, Collection<Object[]> params, int batchSize) throws SQLException {
            int executeCount = params.size() / batchSize;
            executeCount = (params.size() % batchSize == 0) ? executeCount : (executeCount + 1);
            List<int[]> result = Lists.newArrayListWithCapacity(executeCount);

            try (PreparedStatement stmt = this.conn.prepareStatement(sql)) {
                int i = 0;
                for (Object[] ps : params) {
                    i++;
                    for (int j = 0; j < ps.length; j++) {
                        stmt.setObject(j + 1, ps[j]);
                    }
                    stmt.addBatch();
                    if (i % batchSize == 0 || i >= params.size()) {
                        int[] n = stmt.executeBatch();
                        result.add(n);
                        stmt.clearBatch();
                    }
                }
                return ArrayTools.concatIntArray(result);
            }
        }

        public <E extends Exception> void tx(final IAtom<E> atom) throws SQLException, E {
            Preconditions.checkNotNull(atom, "Atom can not be null.");
            final boolean autoCommit = this.conn.getAutoCommit();
            try {
                this.conn.setAutoCommit(false);
                atom.execute(this);
                this.conn.commit();
            }
            catch (Exception e) {
                this.conn.rollback();
                throw e;
            }
            finally {
                this.conn.setAutoCommit(autoCommit);
            }
        }

        @FunctionalInterface
        public interface IAtom<E extends Exception> {
            void execute(JdbcExecutor jdbcExecutor) throws SQLException, E;
        }

        private static void fillStatement(PreparedStatement stmt, Object[] params) throws SQLException {
            if (params != null && params.length > 0) {
                Object param;
                for (int i = 0; i < params.length; i++) {
                    param = params[i];
                    if (param instanceof java.sql.Date) {
                        stmt.setDate(i + 1, (java.sql.Date) param);
                    }
                    else if (param instanceof java.sql.Time) {
                        stmt.setTime(i + 1, (java.sql.Time) param);
                    }
                    else if (param instanceof java.sql.Timestamp) {
                        stmt.setTimestamp(i + 1, (java.sql.Timestamp) param);
                    }
                    else {
                        stmt.setObject(i + 1, param);
                    }
                }
            }
        }
    }

    public static class ParamBuilder {

        public static Object[] buildParams(final Object... params) {
            if (ArrayUtils.isEmpty(params)) {
                return ArrayUtils.EMPTY_OBJECT_ARRAY;
            }
            return Arrays.stream(params)
                    .map(param -> {
                        if (param instanceof Optional) {
                            return OptionalUtil.orElseNull((Optional<?>) param);
                        }
                        if (param instanceof OptionalInt) {
                            return OptionalUtil.toInteger(((OptionalInt) param));
                        }
                        if (param instanceof OptionalLong) {
                            return OptionalUtil.toLong(((OptionalLong) param));
                        }
                        if (param instanceof OptionalDouble) {
                            return OptionalUtil.toDouble(((OptionalDouble) param));
                        }
                        return param;
                    })
                    .toArray();
        }

        public static <T> List<Object[]> buildBatchParams(final Collection<T> c, final Function<T, Object[]> func) {
            Preconditions.checkNotNull(c, "The collection can not be null.");
            Preconditions.checkNotNull(func, "The func can not be null.");
            if (CollectionTools.isEmpty(c)) {
                return Collections.emptyList();
            }
            return c.stream().map(func).collect(Collectors.toList());
        }

        private ParamBuilder() {
            throw new IllegalStateException("Utility class");
        }
    }
}
