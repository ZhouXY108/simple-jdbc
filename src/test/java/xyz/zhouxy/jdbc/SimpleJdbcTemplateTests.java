package xyz.zhouxy.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static xyz.zhouxy.jdbc.SimpleJdbcTemplate.ParamBuilder.*;
import static xyz.zhouxy.plusone.commons.sql.JdbcSql.IN;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import xyz.zhouxy.jdbc.SimpleJdbcTemplate.JdbcExecutor;
import xyz.zhouxy.plusone.commons.sql.SQL;
import xyz.zhouxy.plusone.commons.util.IdGenerator;
import xyz.zhouxy.plusone.commons.util.IdWorker;

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
        config.setMaximumPoolSize(800);
        config.setConnectionTimeout(1000000);
        dataSource = new HikariDataSource(config);
    }

    @Test
    void testQuery() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            Object[] ids = buildParams("501533", "501554", "544599");
            String sql = SQL.newJdbcSql()
                    .SELECT("*")
                    .FROM("test_table")
                    .WHERE(IN("id", ids))
                    .toString();
            log.info(sql);
            List<DbRecord> rs = SimpleJdbcTemplate.connect(conn)
                    .queryToRecordList(sql, ids);
            assertNotNull(rs);
            for (DbRecord baseEntity : rs) {
                // log.info("id: {}", baseEntity.getValueAsString("id"));
                log.info(baseEntity.toString());
                assertEquals(Optional.empty(), baseEntity.getValueAsString("updated_by"));
            }
        }
    }

    @Test
    void testInsert() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            List<Map<String, Object>> keys = new ArrayList<>();
            SimpleJdbcTemplate.connect(conn).update("INSERT INTO base_table(status, created_by) VALUES (?, ?)",
                    buildParams(1, 886), keys);
            log.info("keys: {}", keys);
        }
    }

    @Test
    void testUpdate() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            List<Map<String, Object>> keys = new ArrayList<>();
            SimpleJdbcTemplate.connect(conn).update("UPDATE base_table SET status = ?, version = version + 1, update_time = now(), updated_by = ? WHERE id = ?",
                    buildParams(2, 886, 9), keys);
            log.info("keys: {}", keys);
        }
    }

    final IdWorker idGenerator = IdGenerator.getSnowflakeIdGenerator(0);

    @Test
    void testTransaction() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            long id = this.idGenerator.nextId();
            JdbcExecutor jdbcExecutor = SimpleJdbcTemplate.connect(conn);
            try {
                jdbcExecutor.tx(jdbc -> {
                    jdbc.update("INSERT INTO base_table (id, created_by, create_time, status) VALUES (?, ?, ?, ?)",
                            buildParams(id, 585757, LocalDateTime.now(), 0));
                    throw new NullPointerException();
                });
            }
            catch (NullPointerException e) {
                // ignore
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            Optional<Map<String, Object>> first = jdbcExecutor
                    .queryFirst("SELECT * FROM base_table WHERE id = ?", id);
            log.info("first: {}", first);
            assertTrue(!first.isPresent());
        }

        try (Connection conn = dataSource.getConnection()) {
            long id = this.idGenerator.nextId();
            JdbcExecutor jdbcExecutor = SimpleJdbcTemplate.connect(conn);
            try {
                jdbcExecutor.tx(jdbc -> {
                    jdbc.update("INSERT INTO base_table (id, created_by, create_time, status) VALUES (?, ?, ?, ?)",
                            buildParams(id, 585757, LocalDateTime.now(), 0));
                    // throw new NullPointerException();
                });
            }
            catch (NullPointerException e) {
                // ignore
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            Optional<Map<String, Object>> first = jdbcExecutor
                    .queryFirst("SELECT * FROM base_table WHERE id = ?", id);
            log.info("first: {}", first);
            assertTrue(first.isPresent());
        }
    }
}
