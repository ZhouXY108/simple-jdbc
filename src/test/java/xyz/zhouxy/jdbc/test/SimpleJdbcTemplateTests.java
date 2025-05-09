package xyz.zhouxy.jdbc.test;

import static org.junit.jupiter.api.Assertions.*;
import static xyz.zhouxy.jdbc.ParamBuilder.*;

import java.sql.SQLException;
import java.time.LocalDate;
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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import xyz.zhouxy.jdbc.RowMapper;
import xyz.zhouxy.jdbc.SimpleJdbcTemplate;
import xyz.zhouxy.jdbc.SimpleJdbcTemplate.JdbcExecutor;
import xyz.zhouxy.plusone.commons.util.IdGenerator;
import xyz.zhouxy.plusone.commons.util.IdWorker;

class SimpleJdbcTemplateTests {

    private static final Logger log = LoggerFactory.getLogger(SimpleJdbcTemplateTests.class);

    private static final SimpleJdbcTemplate jdbcTemplate;

    static {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE;MODE=MySQL");
        dataSource.setUser("sa");
        dataSource.setPassword("");
        jdbcTemplate = new SimpleJdbcTemplate(dataSource);
    }

    @BeforeAll
    static void createTable() throws SQLException {
        jdbcTemplate.update("CREATE TABLE sys_account ("
            + "\n" + "    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY"
            + "\n" + "    ,username VARCHAR(255) NOT NULL"
            + "\n" + "    ,account_status VARCHAR(2) NOT NULL"
            + "\n" + "    ,create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP"
            + "\n" + "    ,created_by BIGINT NOT NULL"
            + "\n" + "    ,update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP"
            + "\n" + "    ,updated_by BIGINT DEFAULT NULL"
            + "\n" + "    ,version BIGINT NOT NULL DEFAULT 0"
            + "\n" + ")");
    }

    @BeforeEach
    void initData() throws SQLException {
        jdbcTemplate.update("truncate table sys_account");
        jdbcTemplate.batchUpdate("INSERT INTO sys_account(id, username, account_status, created_by) VALUES (?, ?, ?, ?)", Lists.newArrayList(
            buildParams(2L, "zhouxy2", "0", 108L),
            buildParams(3L, "zhouxy3", "0", 108L),
            buildParams(4L, "zhouxy4", "0", 108L),
            buildParams(5L, "zhouxy5", "0", 108L),
            buildParams(6L, "zhouxy6", "0", 108L),
            buildParams(7L, "zhouxy7", "0", 108L),
            buildParams(8L, "zhouxy8", "0", 108L),
            buildParams(9L, "zhouxy9", "0", 108L)
        ), 10);
        jdbcTemplate.batchUpdate("INSERT INTO sys_account(id, username, account_status, created_by, create_time, update_time, version) VALUES (?, ?, ?, ?, ?, ?, ?)", Lists.newArrayList(
            buildParams(10L, "zhouxy10", "1", 118L, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 29), 31),
            buildParams(11L, "zhouxy11", "1", 118L, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 29), 28),
            buildParams(12L, "zhouxy12", "1", 118L, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 29), 25),
            buildParams(13L, "zhouxy13", "1", 118L, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 29), 22),
            buildParams(14L, "zhouxy14", "1", 118L, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 29), 19),
            buildParams(15L, "zhouxy15", "1", 118L, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 29), 16),
            buildParams(16L, "zhouxy16", "1", 118L, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 29), 13),
            buildParams(17L, "zhouxy17", "1", 118L, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 29), 10),
            buildParams(18L, "zhouxy18", "1", 118L, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 29), 7),
            buildParams(19L, "zhouxy19", "1", 118L, LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 29), 0)
        ), 10);
        jdbcTemplate.update("INSERT INTO sys_account(id, username, account_status, created_by, create_time, updated_by, update_time, version) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
            buildParams(20L, "zhouxy20", "2", 118L, LocalDateTime.of(2008, 8, 8, 20, 8), 31L, LocalDateTime.now(), 88L));
    }

    @Test
    void testQuery() throws SQLException {
        Object[] ids = buildParams(5, 9, 13, 14, 17, 20, 108);
        String sql = "SELECT id, username, account_status"
                + "\n FROM sys_account"
                + "\n WHERE id IN ("
                + "\n     ?, ?, ?, ?, ?, ?, ?"
                + "\n )";
        log.info(sql);
        List<Map<String, Object>> rs = jdbcTemplate.queryList(sql, ids);
        for (Map<String, Object> dbRecord : rs) {
            log.info("{}", dbRecord);
        }
        assertEquals(
            Lists.newArrayList(
                ImmutableMap.of("id", 5L, "account_status", "0", "username", "zhouxy5"),
                ImmutableMap.of("id", 9L, "account_status", "0", "username", "zhouxy9"),
                ImmutableMap.of("id", 13L, "account_status", "1", "username", "zhouxy13"),
                ImmutableMap.of("id", 14L, "account_status", "1", "username", "zhouxy14"),
                ImmutableMap.of("id", 17L, "account_status", "1", "username", "zhouxy17"),
                ImmutableMap.of("id", 20L, "account_status", "2", "username", "zhouxy20")
            ),
            rs
        );
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
