package xyz.zhouxy.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static xyz.zhouxy.jdbc.ParamBuilder.*;
import static xyz.zhouxy.plusone.commons.sql.JdbcSql.IN;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import xyz.zhouxy.jdbc.SimpleJdbcTemplate.JdbcExecutor;
import xyz.zhouxy.plusone.commons.sql.SQL;
import xyz.zhouxy.plusone.commons.util.ArrayTools;
import xyz.zhouxy.plusone.commons.util.IdGenerator;
import xyz.zhouxy.plusone.commons.util.IdWorker;
import xyz.zhouxy.plusone.commons.util.Numbers;

class SimpleJdbcTemplateTests {

    private static final Logger log = LoggerFactory.getLogger(SimpleJdbcTemplateTests.class);

    private static final DataSource dataSource;

    String[] cStruct = {
            "id",
            "created_by",
            "create_time",
            "updated_by",
            "update_time",
            "status"
    };

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/plusone");
        config.setUsername("postgres");
        config.setPassword("zhouxy108");
        config.setMaximumPoolSize(8);
        config.setConnectionTimeout(1000000);
        dataSource = new HikariDataSource(config);
    }

    @Test
    void testQuery() throws SQLException {
        Object[] ids = buildParams("501533", "501554", "544599");
        String sql = SQL.newJdbcSql()
                .SELECT("*")
                .FROM("test_table")
                .WHERE(IN("id", ids))
                .toString();
        log.info(sql);
        try (Connection conn = dataSource.getConnection()) {
            List<DbRecord> rs = SimpleJdbcTemplate.connect(conn)
                    .queryToRecordList(sql, ids);
            assertNotNull(rs);
            for (DbRecord baseEntity : rs) {
                // log.info("id: {}", baseEntity.getValueAsString("id")); // NOSONAR
                log.info(baseEntity.toString());
                assertEquals(Optional.empty(), baseEntity.getValueAsString("updated_by"));
            }
        }
    }

    @Test
    void testInsert() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            List<DbRecord> keys = SimpleJdbcTemplate.connect(conn).update(
                    "INSERT INTO base_table(status, created_by) VALUES (?, ?)",
                    buildParams(1, 886L),
                    ResultMap.recordResultMap);
            log.info("keys: {}", keys);
            assertEquals(1, keys.size());
            DbRecord result = keys.get(0);
            assertEquals(1, result.getValueAsInt("status").getAsInt());
            assertEquals(886L, result.getValueAsLong("created_by").getAsLong());
            assertTrue(result.get("id").isPresent());
        }
    }

    @Test
    void testUpdate() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            List<DbRecord> keys = SimpleJdbcTemplate.connect(conn).update(
                    "UPDATE base_table SET status = ?, version = version + 1, update_time = now(), updated_by = ? WHERE id = ? AND version = ?",
                    buildParams(2, 886, 9, 0),
                    ResultMap.recordResultMap);
            log.info("keys: {}", keys);
        }
    }

    final IdWorker idGenerator = IdGenerator.getSnowflakeIdGenerator(0);

    @Test
    void testTransaction() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            long id = this.idGenerator.nextId();
            JdbcExecutor jdbcExecutor = SimpleJdbcTemplate.connect(conn);
            jdbcExecutor.executeTransaction(jdbc -> {
                jdbc.update("INSERT INTO base_table (id, created_by, create_time, status) VALUES (?, ?, ?, ?)",
                        buildParams(id, 585757, LocalDateTime.now(), 0));
                throw new NullPointerException();
            });
            Optional<Map<String, Object>> first = jdbcExecutor
                    .queryFirst("SELECT * FROM base_table WHERE id = ?", buildParams(id));
            log.info("first: {}", first);
            assertTrue(!first.isPresent());
        }

        try (Connection conn = dataSource.getConnection()) {
            long id = this.idGenerator.nextId();
            JdbcExecutor jdbcExecutor = SimpleJdbcTemplate.connect(conn);
            jdbcExecutor.executeTransaction(jdbc -> {
                jdbc.update("INSERT INTO base_table (id, created_by, create_time, status) VALUES (?, ?, ?, ?)",
                        buildParams(id, 585757, LocalDateTime.now(), 0));
                // throw new NullPointerException(); // NOSONAR
            });
            Optional<Map<String, Object>> first = jdbcExecutor
                    .queryFirst("SELECT * FROM base_table WHERE id = ?", buildParams(id));
            log.info("first: {}", first);
            assertTrue(first.isPresent());
        }

        try (Connection conn = dataSource.getConnection()) {
            long id = this.idGenerator.nextId();
            JdbcExecutor jdbcExecutor = SimpleJdbcTemplate.connect(conn);
            jdbcExecutor.commitIfTrue(jdbc -> {
                jdbc.update("INSERT INTO base_table (id, created_by, create_time, status) VALUES (?, ?, ?, ?)",
                        buildParams(id, 585757, LocalDateTime.now(), 0));
                throw new NullPointerException();
            });
            Optional<Map<String, Object>> first = jdbcExecutor
                    .queryFirst("SELECT * FROM base_table WHERE id = ?", buildParams(id));
            log.info("first: {}", first);
            assertTrue(!first.isPresent());
        }

        try (Connection conn = dataSource.getConnection()) {
            long id = this.idGenerator.nextId();
            JdbcExecutor jdbcExecutor = SimpleJdbcTemplate.connect(conn);
            jdbcExecutor.commitIfTrue(jdbc -> {
                jdbc.update("INSERT INTO base_table (id, created_by, create_time, status) VALUES (?, ?, ?, ?)",
                        buildParams(id, 585757, LocalDateTime.now(), 0));
                return false;
            });
            Optional<Map<String, Object>> first = jdbcExecutor
                    .queryFirst("SELECT * FROM base_table WHERE id = ?", buildParams(id));
            log.info("first: {}", first);
            assertTrue(!first.isPresent());
        }

        try (Connection conn = dataSource.getConnection()) {
            long id = this.idGenerator.nextId();
            JdbcExecutor jdbcExecutor = SimpleJdbcTemplate.connect(conn);
            jdbcExecutor.commitIfTrue(jdbc -> {
                jdbc.update("INSERT INTO base_table (id, created_by, create_time, status) VALUES (?, ?, ?, ?)",
                        buildParams(id, 585757, LocalDateTime.now(), 0));
                return true;
            });
            Optional<Map<String, Object>> first = jdbcExecutor
                    .queryFirst("SELECT * FROM base_table WHERE id = ?", buildParams(id));
            log.info("first: {}", first);
            assertTrue(first.isPresent());
        }
    }

    @Test
    void testBatch() throws Exception {

        Random random = ThreadLocalRandom.current();

        LocalDate handleDate = LocalDate.of(1949, 10, 1);
        List<DbRecord> datas = Lists.newArrayList();
        while (handleDate.isBefore(LocalDate.of(2008, 8, 8))) {
            DbRecord r1 = new DbRecord();
            r1.put("username", "张三2");
            r1.put("usage_date", handleDate);
            r1.put("usage_duration", random.nextInt(500));
            datas.add(r1);
            DbRecord r2 = new DbRecord();
            r2.put("username", "李四2");
            r2.put("usage_date", handleDate);
            r2.put("usage_duration", random.nextInt(500));
            datas.add(r2);

            handleDate = handleDate.plusDays(1L);
        }
        try (Connection conn = dataSource.getConnection()) {
            List<int[]> result = SimpleJdbcTemplate.connect(conn)
                    .batchUpdate("insert into test_table (username, usage_date, usage_duration) values (?,?,?)",
                            buildBatchParams(datas, item -> buildParams(
                                    item.getValueAsString("username"),
                                    item.getValueAsString("usage_date"),
                                    item.getValueAsString("usage_duration"))),
                            400);
            long sum = Numbers.sum(ArrayTools.concatIntArray(result));
            assertEquals(datas.size(), sum);
            log.info("sum: {}", sum);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
}
