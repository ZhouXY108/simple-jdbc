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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import javax.annotation.Nonnull;
import javax.sql.DataSource;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import xyz.zhouxy.plusone.commons.util.OptionalTools;

public class SimpleJdbcTemplate {

    private final DataSource dataSource;

    public SimpleJdbcTemplate(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public <T> List<T> query(String sql, Object[] params, ResultMap<T> resultMap)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.query(conn, sql, params, resultMap);
        }
    }

    public <T> Optional<T> queryFirst(String sql, Object[] params, ResultMap<T> resultMap)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirst(conn, sql, params, resultMap);
        }
    }

    public List<Map<String, Object>> query(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.query(conn, sql, params);
        }
    }

    public Optional<Map<String, Object>> queryFirst(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirst(conn, sql, params);
        }
    }

    public List<DbRecord> queryToRecordList(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryToRecordList(conn, sql, params);
        }
    }

    public Optional<DbRecord> queryFirstRecord(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryFirstRecord(conn, sql, params);
        }
    }

    public Optional<String> queryToString(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryToString(conn, sql, params);
        }
    }

    public OptionalInt queryToInt(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryToInt(conn, sql, params);
        }
    }

    public OptionalLong queryToLong(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryToLong(conn, sql, params);
        }
    }

    public OptionalDouble queryToDouble(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryToDouble(conn, sql, params);
        }
    }

    public Optional<BigDecimal> queryToBigDecimal(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.queryToBigDecimal(conn, sql, params);
        }
    }

    public int update(String sql, Object[] params)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.update(conn, sql, params);
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
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.update(conn, sql, params, resultMap);
        }
    }

    public List<int[]> batchUpdate(String sql, Collection<Object[]> params, int batchSize)
            throws SQLException {
        try (Connection conn = this.dataSource.getConnection()) {
            return JdbcExecutor.batchUpdate(conn, sql, params, batchSize);
        }
    }

    public <E extends Exception> void executeTransaction(@Nonnull final DbOperations<E> operations)
            throws SQLException, E {
        Preconditions.checkNotNull(operations, "Operations can not be null.");
        try (Connection conn = this.dataSource.getConnection()) {
            final boolean autoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                operations.execute(new JdbcExecutor(conn));
                conn.commit();
            }
            catch (Exception e) {
                conn.rollback();
                throw e;
            }
            finally {
                conn.setAutoCommit(autoCommit);
            }
        }
    }

    public <E extends Exception> void commitIfTrue(@Nonnull final PredicateWithThrowable<E> operations)
            throws SQLException, E {
        Preconditions.checkNotNull(operations, "Operations can not be null.");
        try (Connection conn = this.dataSource.getConnection()) {
            final boolean autoCommit = conn.getAutoCommit();
            try {
                conn.setAutoCommit(false);
                if (operations.test(new JdbcExecutor(conn))) {
                    conn.commit();
                }
                else {
                    conn.rollback();
                }
            }
            catch (Exception e) {
                conn.rollback();
                throw e;
            }
            finally {
                conn.setAutoCommit(autoCommit);
            }
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

    public static final class JdbcExecutor {

        private final Connection conn;

        private JdbcExecutor(Connection conn) {
            this.conn = conn;
        }

        public <T> List<T> query(String sql, Object[] params, ResultMap<T> resultMap)
                throws SQLException {
            return JdbcExecutor.query(this.conn, sql, params, resultMap);
        }

        public <T> Optional<T> queryFirst(String sql, Object[] params, ResultMap<T> resultMap)
                throws SQLException {
            return JdbcExecutor.queryFirst(this.conn, sql, params, resultMap);
        }

        public List<Map<String, Object>> query(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.query(this.conn, sql, params);
        }

        public Optional<Map<String, Object>> queryFirst(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryFirst(this.conn, sql, params);
        }

        public List<DbRecord> queryToRecordList(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryToRecordList(this.conn, sql, params);
        }

        public Optional<DbRecord> queryFirstRecord(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryFirstRecord(this.conn, sql, params);
        }

        public Optional<String> queryToString(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryToString(this.conn, sql, params);
        }

        public OptionalInt queryToInt(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryToInt(this.conn, sql, params);
        }

        public OptionalLong queryToLong(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryToLong(this.conn, sql, params);
        }

        public OptionalDouble queryToDouble(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryToDouble(this.conn, sql, params);
        }

        public Optional<BigDecimal> queryToBigDecimal(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.queryToBigDecimal(this.conn, sql, params);
        }

        public int update(String sql, Object[] params)
                throws SQLException {
            return JdbcExecutor.update(this.conn, sql, params);
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
            return JdbcExecutor.update(this.conn, sql, params, resultMap);
        }

        public List<int[]> batchUpdate(String sql, Collection<Object[]> params, int batchSize)
                throws SQLException {
            return JdbcExecutor.batchUpdate(this.conn, sql, params, batchSize);
        }

        private static <T> List<T> query(Connection conn, String sql, Object[] params, ResultMap<T> resultMap)
                throws SQLException {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
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

        private static <T> Optional<T> queryFirst(Connection conn, String sql, Object[] params, ResultMap<T> resultMap)
                throws SQLException {
            return query(conn, sql, params, resultMap).stream().findFirst();
        }

        private static List<Map<String, Object>> query(Connection conn, String sql, Object[] params)
                throws SQLException {
            return query(conn, sql, params, ResultMap.mapResultMap);
        }

        private static Optional<Map<String, Object>> queryFirst(Connection conn, String sql, Object[] params)
                throws SQLException {
            return queryFirst(conn, sql, params, ResultMap.mapResultMap);
        }

        private static List<DbRecord> queryToRecordList(Connection conn, String sql, Object[] params)
                throws SQLException {
            return query(conn, sql, params, ResultMap.recordResultMap);
        }

        private static Optional<DbRecord> queryFirstRecord(Connection conn, String sql, Object[] params)
                throws SQLException {
            return queryFirst(conn, sql, params, ResultMap.recordResultMap);
        }

        private static Optional<String> queryToString(Connection conn, String sql, Object[] params)
                throws SQLException {
            return queryFirst(conn, sql, params, (rs, rowNumber) -> rs.getString(1));
        }

        private static OptionalInt queryToInt(Connection conn, String sql, Object[] params)
                throws SQLException {
            Optional<Integer> result = queryFirst(conn, sql, params, (rs, rowNumber) -> rs.getInt(1));
            return OptionalTools.toOptionalInt(result);
        }

        private static OptionalLong queryToLong(Connection conn, String sql, Object[] params)
                throws SQLException {
            Optional<Long> result = queryFirst(conn, sql, params, (rs, rowNumber) -> rs.getLong(1));
            return OptionalTools.toOptionalLong(result);
        }

        private static OptionalDouble queryToDouble(Connection conn, String sql, Object[] params)
                throws SQLException {
            Optional<Double> result = queryFirst(conn, sql, params, (rs, rowNumber) -> rs.getDouble(1));
            return OptionalTools.toOptionalDouble(result);
        }

        private static Optional<BigDecimal> queryToBigDecimal(Connection conn, String sql, Object[] params)
                throws SQLException {
            return queryFirst(conn, sql, params, (rs, rowNumber) -> rs.getBigDecimal(1));
        }

        private static int update(Connection conn, String sql, Object[] params)
                throws SQLException {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
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
        private static <T> List<T> update(Connection conn, String sql, Object[] params, ResultMap<T> resultMap)
                throws SQLException {
            Preconditions.checkNotNull(sql, "The sql could not be null.");
            Preconditions.checkNotNull(params, "The params could not be null.");
            Preconditions.checkNotNull(resultMap, "The resultMap could not be null.");
            final List<T> result = new ArrayList<>();
            try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
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

        private static List<int[]> batchUpdate(Connection conn, String sql, Collection<Object[]> params, int batchSize)
                throws SQLException {
            int executeCount = params.size() / batchSize;
            executeCount = (params.size() % batchSize == 0) ? executeCount : (executeCount + 1);
            List<int[]> result = Lists.newArrayListWithCapacity(executeCount);

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
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

        private static void fillStatement(PreparedStatement stmt, Object[] params)
                throws SQLException {
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
