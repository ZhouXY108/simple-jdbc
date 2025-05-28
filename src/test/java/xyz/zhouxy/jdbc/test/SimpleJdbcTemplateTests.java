/*
 * Copyright 2023-2025 the original author or authors.
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

import static org.junit.jupiter.api.Assertions.*;
import static xyz.zhouxy.jdbc.ParamBuilder.buildParams;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;

import xyz.zhouxy.jdbc.RowMapper;
import xyz.zhouxy.jdbc.SimpleJdbcTemplate;
import xyz.zhouxy.jdbc.SimpleJdbcTemplate.JdbcExecutor;
import xyz.zhouxy.plusone.commons.util.IdGenerator;
import xyz.zhouxy.plusone.commons.util.IdWorker;

class SimpleJdbcTemplateTests {

    private static final Logger log = LoggerFactory.getLogger(SimpleJdbcTemplateTests.class);

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
    void initData() throws IOException, SQLException {
        // 初始化数据
        executeSqlFile("data.sql");
    }

    static void executeSqlFile(String filePath) throws IOException, SQLException {
        String[] sqls = Resources
                .toString(Resources.getResource(filePath), StandardCharsets.UTF_8)
                .split(";");
        for (String sql : sqls) {
            jdbcTemplate.update(sql);
        }
    }

    @Test
    void testQuery() throws SQLException {
        Object[] ids = buildParams(5, 9, 13, 14, 17, 20, 108);
        String sql = "SELECT id, username, account_status FROM sys_account WHERE id IN (?, ?, ?, ?, ?, ?, ?)";
        log.info(sql);
        List<Map<String, Object>> rs = jdbcTemplate.queryList(sql, ids);
        for (Map<String, Object> dbRecord : rs) {
            log.info("{}", dbRecord);
        }
        List<ImmutableMap<String, Object>> expected = ImmutableList.of(
            ImmutableMap.of("id", 5L, "account_status", "0", "username", "zhouxy5"),
            ImmutableMap.of("id", 9L, "account_status", "0", "username", "zhouxy9"),
            ImmutableMap.of("id", 13L, "account_status", "1", "username", "zhouxy13"),
            ImmutableMap.of("id", 14L, "account_status", "1", "username", "zhouxy14"),
            ImmutableMap.of("id", 17L, "account_status", "1", "username", "zhouxy17"),
            ImmutableMap.of("id", 20L, "account_status", "2", "username", "zhouxy20")
        );
        assertEquals(expected, rs);
    }

    @Test
    void testQueryExists() throws SQLException {
        boolean isExists = jdbcTemplate.queryAsBoolean(
                "SELECT EXISTS(SELECT 1 FROM sys_account WHERE id = ? LIMIT 1)",
                buildParams(998));
        assertFalse(isExists);
    }

    @Test
    void testInsert() throws SQLException {
        List<Map<String, Object>> keys = jdbcTemplate.update(
                "INSERT INTO sys_account(username, account_status, created_by) VALUES (?, ?, ?), (?, ?, ?)",
                buildParams("zhouxy21", "2", 123L, "code22", '2', 456L),
                RowMapper.HASH_MAP_MAPPER);
        log.info("keys: {}", keys);
        assertEquals(2, keys.size());
        for (Map<String,Object> key : keys) {
            assertTrue(key.containsKey("id"));
            assertInstanceOf(Long.class, key.get("id"));
            assertTrue(key.containsKey("create_time"));
            assertInstanceOf(Date.class, key.get("create_time"));
        }
        List<Long> ids = jdbcTemplate.update(
                "INSERT INTO sys_account(username, account_status, created_by) VALUES (?, ?, ?), (?, ?, ?)",
                buildParams("zhouxy21", "2", 123L, "code22", '2', 456L),
                (rs, rowNumber) -> rs.getObject("id", Long.class));
        log.info("ids: {}", ids);
        assertEquals(2, ids.size());
    }

    @Test
    void testUpdate() throws SQLException {
        List<Map<String, Object>> keys = jdbcTemplate.update(
                "UPDATE sys_account SET account_status = ?, version = version + 1, update_time = now(), updated_by = ? WHERE id = ? AND version = ?",
                buildParams("7", 886L, 20L, 88L),
                RowMapper.HASH_MAP_MAPPER);
        assertEquals(1, keys.size());
        log.info("keys: {}", keys);
        keys = jdbcTemplate.update(
                "UPDATE sys_account SET account_status = ?, version = version + 1, update_time = now(), updated_by = ? WHERE id = ? AND version = ?",
                buildParams("-1", 886L, 20L, 88L),
                RowMapper.HASH_MAP_MAPPER);
        assertEquals(0, keys.size());
    }

    final IdWorker idGenerator = IdGenerator.getSnowflakeIdGenerator(0);

    @Test
    void testTransaction() throws SQLException {
        // 抛异常，回滚
        {
            long id = this.idGenerator.nextId();
            try {
                jdbcTemplate.executeTransaction((JdbcExecutor jdbc) -> {
                    jdbc.update("INSERT INTO sys_account (id, username, created_by, create_time, account_status) VALUES (?, ?, ?, ?, ?)",
                            buildParams(id, "testTransaction1", 100, LocalDateTime.now(), "55"));
                    throw new NullPointerException();
                });
            }
            catch (NullPointerException e) {
                // ignore
            }
            Optional<Map<String, Object>> first = jdbcTemplate
                    .queryFirst("SELECT * FROM sys_account WHERE id = ?", buildParams(id));
            log.info("first: {}", first);
            assertFalse(first.isPresent());
        }

        // 没有异常，提交事务
        {
            long id = this.idGenerator.nextId();
            jdbcTemplate.executeTransaction(jdbc -> {
                jdbc.update("INSERT INTO sys_account (id, username, created_by, create_time, account_status) VALUES (?, ?, ?, ?, ?)",
                        buildParams(id, "testTransaction2", 101, LocalDateTime.now(), "55"));
            });

            Optional<Map<String, Object>> first = jdbcTemplate
                    .queryFirst("SELECT * FROM sys_account WHERE id = ?", buildParams(id));
            log.info("first: {}", first);
            assertTrue(first.isPresent());
        }

        // 抛异常，回滚
        {
            long id = this.idGenerator.nextId();
            try {
                jdbcTemplate.commitIfTrue(jdbc -> {
                    jdbc.update("INSERT INTO sys_account (id, username, created_by, create_time, account_status) VALUES (?, ?, ?, ?, ?)",
                            buildParams(id, "testTransaction3", 102, LocalDateTime.now(), "55"));
                    throw new NullPointerException();
                });
            }
            catch (NullPointerException e) {
                // ignore
            }
            Optional<Map<String, Object>> first = jdbcTemplate
                    .queryFirst("SELECT * FROM sys_account WHERE id = ?", buildParams(id));
            log.info("first: {}", first);
            assertFalse(first.isPresent());
        }

        // 返回 false，回滚
        {
            long id = this.idGenerator.nextId();
            jdbcTemplate.commitIfTrue(jdbc -> {
                jdbc.update("INSERT INTO sys_account (id, username, created_by, create_time, account_status) VALUES (?, ?, ?, ?, ?)",
                        buildParams(id, "testTransaction4", 103, LocalDateTime.now(), "55"));
                return false;
            });

            Optional<Map<String, Object>> first = jdbcTemplate
                    .queryFirst("SELECT * FROM sys_account WHERE id = ?", buildParams(id));
            log.info("first: {}", first);
            assertFalse(first.isPresent());
        }

        // 返回 true，提交事务
        {
            long id = this.idGenerator.nextId();
            jdbcTemplate.commitIfTrue(jdbc -> {
                jdbc.update("INSERT INTO sys_account (id, username, created_by, create_time, account_status) VALUES (?, ?, ?, ?, ?)",
                        buildParams(id, "testTransaction5", 104, LocalDateTime.now(), "55"));
                return true;
            });

            Optional<Map<String, Object>> first = jdbcTemplate
                    .queryFirst("SELECT * FROM sys_account WHERE id = ?", buildParams(id));
            log.info("first: {}", first);
            assertTrue(first.isPresent());
        }
    }

    @Test
    void testBean() throws Exception {
        Optional<AccountPO> t = jdbcTemplate.queryFirst(
                "SELECT * FROM sys_account WHERE id = ?",
                buildParams(18L),
                RowMapper.beanRowMapper(AccountPO.class));
        assertEquals(
                new AccountPO(18L, "zhouxy18", "1",
                    LocalDateTime.of(2000, 1, 1, 0, 0), 118L,
                    LocalDateTime.of(2000, 1, 29, 0, 0), null, 7L),
                t.get());
        log.info("{}", t);
    }
}
