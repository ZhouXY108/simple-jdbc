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
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import xyz.zhouxy.plusone.commons.util.OptionalTools;

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

        private JdbcExecutor(Connection conn) {
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

        public List<Map<String, Object>> query(String sql, Object[] params) throws SQLException {
            return query(sql, params, ResultMap.mapResultMap);
        }

        public Optional<Map<String, Object>> queryFirst(String sql, Object[] params) throws SQLException {
            return queryFirst(sql, params, ResultMap.mapResultMap);
        }

        public List<DbRecord> queryToRecordList(String sql, Object[] params) throws SQLException {
            return query(sql, params, ResultMap.recordResultMap);
        }

        public Optional<DbRecord> queryFirstRecord(String sql, Object[] params) throws SQLException {
            return queryFirst(sql, params, ResultMap.recordResultMap);
        }

        public Optional<String> queryToString(String sql, Object[] params) throws SQLException {
            return queryFirst(sql, params, (rs, rowNumber) -> rs.getString(1));
        }

        public OptionalInt queryToInt(String sql, Object[] params) throws SQLException {
            Optional<Integer> result = queryFirst(sql, params, (rs, rowNumber) -> rs.getInt(1));
            return OptionalTools.toOptionalInt(result);
        }

        public OptionalLong queryToLong(String sql, Object[] params) throws SQLException {
            Optional<Long> result = queryFirst(sql, params, (rs, rowNumber) -> rs.getLong(1));
            return OptionalTools.toOptionalLong(result);
        }

        public OptionalDouble queryToDouble(String sql, Object[] params) throws SQLException {
            Optional<Double> result = queryFirst(sql, params, (rs, rowNumber) -> rs.getDouble(1));
            return OptionalTools.toOptionalDouble(result);
        }

        public Optional<BigDecimal> queryToBigDecimal(String sql, Object[] params) throws SQLException {
            return queryFirst(sql, params, (rs, rowNumber) -> rs.getBigDecimal(1));
        }

        public int update(String sql, Object[] params) throws SQLException {
            try (PreparedStatement stmt = this.conn.prepareStatement(sql)) {
                fillStatement(stmt, params);
                return stmt.executeUpdate();
            }
        }

        /**
         * 执行 SQL 并更新后的数据
         * 
         * @param sql       要执行的 SQL 语句
         * @param params    参数
         * @param resultMap 结果映射规则
         * 
         * @return 更新的数据
         * @throws SQLException 执行 SQL 遇到异常情况将抛出
         */
        public <T> List<T> update(@Nonnull String sql, @Nonnull Object[] params, ResultMap<T> resultMap)
                throws SQLException {
            Preconditions.checkNotNull(sql, "The sql could not be null.");
            Preconditions.checkNotNull(params, "The params could not be null.");
            Preconditions.checkNotNull(resultMap, "The resultMap could not be null.");
            final List<T> result = new ArrayList<>();
            try (PreparedStatement stmt = this.conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                fillStatement(stmt, params);
                stmt.executeUpdate();
                try (ResultSet generatedKeys = stmt.getGeneratedKeys();) {
                    int rowNumber = 0;
                    while (generatedKeys.next()) {
                        T e = resultMap.map(generatedKeys, rowNumber++);
                        result.add(e);
                    }
                }
                return result;
            }
        }

        public List<int[]> batchUpdate(String sql, Collection<Object[]> params, int batchSize) throws SQLException {
            int executeCount = params.size() / batchSize;
            executeCount = (params.size() % batchSize == 0) ? executeCount : (executeCount + 1);
            List<int[]> result = Lists.newArrayListWithCapacity(executeCount);

            try (PreparedStatement stmt = this.conn.prepareStatement(sql)) {
                int i = 0;
                for (Object[] ps : params) {
                    i++;
                    fillStatement(stmt, ps);
                    stmt.addBatch();
                    if (i % batchSize == 0 || i >= params.size()) {
                        int[] n = stmt.executeBatch();
                        result.add(n);
                        stmt.clearBatch();
                    }
                }
                return result;
            }
        }

        public <E extends Exception> void executeTransaction(@Nonnull final DbOperations<E> operations)
                throws SQLException, E {
            Preconditions.checkNotNull(operations, "Operations can not be null.");
            final boolean autoCommit = this.conn.getAutoCommit();
            try {
                this.conn.setAutoCommit(false);
                operations.execute(this);
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

        public <E extends Exception> void commitIfTrue(@Nonnull final PredicateWithThrowable<E> operations)
                throws SQLException, E {
            Preconditions.checkNotNull(operations, "Operations can not be null.");
            final boolean autoCommit = this.conn.getAutoCommit();
            try {
                this.conn.setAutoCommit(false);
                if (operations.test(this)) {
                    this.conn.commit();
                }
                else {
                    this.conn.rollback();
                }
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
        public interface DbOperations<E extends Exception> {
            void execute(JdbcExecutor jdbcExecutor) throws E;
        }

        @FunctionalInterface
        public interface PredicateWithThrowable<E extends Throwable> {
            boolean test(JdbcExecutor jdbcExecutor) throws E;
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
}
