/*
 * Copyright 2026 the original author or authors.
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
package xyz.zhouxy.jdbc.test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static xyz.zhouxy.jdbc.ParamBuilder.buildBatchParams;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.List;

import java.util.Optional;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import xyz.zhouxy.jdbc.BatchUpdateResult;
import xyz.zhouxy.jdbc.BatchUpdateStatus;
import xyz.zhouxy.jdbc.SimpleJdbcTemplate;

public class BatchUpdateTests {

    private static SimpleJdbcTemplate jdbcTemplate;

    @BeforeAll
    static void initH2() throws IOException, SQLException {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE;MODE=MySQL");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        jdbcTemplate = new SimpleJdbcTemplate(dataSource);

        // 建表
        executeSqlFile("schema.sql");
    }

    @BeforeEach
    void initData() throws SQLException {
        // 初始化数据
        jdbcTemplate.update("truncate table sys_account");
    }

    static void executeSqlFile(String filePath) throws IOException, SQLException {
        String[] sqls = Resources
                .toString(Resources.getResource(filePath), StandardCharsets.UTF_8)
                .split(";");
        for (String sql : sqls) {
            jdbcTemplate.update(sql);
        }
    }

    final List<AccountPO> accountPOs = Lists.newArrayList(
            // batch 0
            new AccountPO(10001L, "test_0001", "1", 1L, 1L),
            new AccountPO(10002L, "test_0002", "1", 1L, 1L),
            new AccountPO(10003L, "test_0003", "1", 1L, 1L),
            // batch 1
            new AccountPO(10004L, "test_0004", "1", 1L, 1L),
            new AccountPO(10005L, "test_0005", "1", 1L, 1L),
            new AccountPO(10006L, "test_0006", "1", 1L, 1L),
            // batch 2
            new AccountPO(10007L, "test_0007", "1", 1L, 1L),
            new AccountPO(10007L, "test_*0007", "1", 1L, 1L),
            // new AccountPO(10008L, "test_0008", "1", 1L, 1L),
            new AccountPO(10009L, "test_0009", "1", 1L, 1L),
            // batch 3
            new AccountPO(10009L, "test_*0009", "1", 1L, 1L),
            // new AccountPO(10010L, "test_0010", "1", 1L, 1L),
            new AccountPO(10011L, "test_0011", "1", 1L, 1L),
            new AccountPO(10012L, "test_0012", "1", 1L, 1L),
            // batch 4
            new AccountPO(10013L, "test_0013", "1", 1L, 1L)
    );

    @Test
    void testBatchUpdate() throws SQLException {

        Optional<Integer> count0 = jdbcTemplate.queryFirst("SELECT COUNT(*) FROM sys_account", (rs, i) -> rs.getInt(1));
        assertEquals(0, count0.get().intValue());

        BatchUpdateResult result = jdbcTemplate.batchUpdate(
                "INSERT INTO sys_account (id, username, account_status, created_by, updated_by) VALUES (?, ?, ?, ?, ?)",
                buildBatchParams(accountPOs,
                        a -> new Object[] { a.getId(), a.getUsername(), a.getAccountStatus(), a.getCreatedBy(), a.getUpdatedBy() }),
                3);
        assertEquals(BatchUpdateStatus.INTERRUPTED, result.getStatus());
        assertEquals(13, result.getTotal());
        assertEquals(5, result.getBatchCount());
        assertEquals(3, result.getCompleteBatchCount());
        assertEquals(2, result.getSuccessBatchCount());
        assertEquals(1, result.getErrorBatchCount());
        assertEquals(2, result.getRemainingBatchCount());
        assertArrayEquals(new int[] { 1, 1, 1 }, result.getUpdateCounts(0));
        assertArrayEquals(new int[] { 1, 1, 1 }, result.getUpdateCounts(1));
        assertArrayEquals(new int[] { 1, -3, 1 }, result.getUpdateCounts(2));
        assertNull(result.getUpdateCounts(3));
        assertNull(result.getUpdateCounts(4));

        Optional<Integer> count8 = jdbcTemplate.queryFirst("SELECT COUNT(*) FROM sys_account", (rs, i) -> rs.getInt(1));
        assertEquals(8, count8.get().intValue());
    }

    @Test
    void testBatchUpdateQuietly() throws SQLException {
        Optional<Integer> count0 = jdbcTemplate.queryFirst("SELECT COUNT(*) FROM sys_account", (rs, i) -> rs.getInt(1));
        assertEquals(0, count0.get().intValue());

        BatchUpdateResult result = jdbcTemplate.batchUpdate(
                "INSERT INTO sys_account (id, username, account_status, created_by, updated_by) VALUES (?, ?, ?, ?, ?)",
                buildBatchParams(accountPOs,
                        a -> new Object[] { a.getId(), a.getUsername(), a.getAccountStatus(), a.getCreatedBy(), a.getUpdatedBy() }),
                3,
                true);
        assertEquals(BatchUpdateStatus.COMPLETED_WITH_ERRORS, result.getStatus());
        assertEquals(13, result.getTotal());
        assertEquals(5, result.getBatchCount());
        assertEquals(5, result.getCompleteBatchCount());
        assertEquals(3, result.getSuccessBatchCount());
        assertEquals(2, result.getErrorBatchCount());
        assertEquals(0, result.getRemainingBatchCount());
        assertArrayEquals(new int[] { 1, 1, 1 }, result.getUpdateCounts(0));
        assertArrayEquals(new int[] { 1, 1, 1 }, result.getUpdateCounts(1));
        assertArrayEquals(new int[] { 1, -3, 1 }, result.getUpdateCounts(2));
        assertArrayEquals(new int[] { -3, 1, 1 }, result.getUpdateCounts(3));
        assertArrayEquals(new int[] { 1 }, result.getUpdateCounts(4));

        Optional<Integer> count11 = jdbcTemplate.queryFirst("SELECT COUNT(*) FROM sys_account", (rs, i) -> rs.getInt(1));
        assertEquals(11, count11.get().intValue());
    }
}
