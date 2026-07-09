package xyz.zhouxy.jdbc.test.namedparam;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.zhouxy.jdbc.BatchUpdateResult;
import xyz.zhouxy.jdbc.SimpleJdbcTemplate;
import xyz.zhouxy.jdbc.namedparam.NamedParamJdbcOperations;
import xyz.zhouxy.jdbc.namedparam.NamedParamSql;
import xyz.zhouxy.jdbc.test.BaseH2Test;

/**
 * 命名参数批量更新 API 测试。
 *
 * <p>验证 {@link NamedParamJdbcOperations} 中命名参数（<code>#{paramName}</code>）形式的
 * 批量更新方法的两种参数模式：直接传参（String + List&lt;Map&gt;）、模板传参（NamedParamSql + List&lt;Map&gt;）。</p>
 */
@DisplayName("NamedParamJdbcOperations 命名参数批量更新")
class NamedParamBatchUpdateTest extends BaseH2Test {

    @BeforeEach
    void setUp() throws Exception {
        resetDatabase();
    }

    // ==================== batchUpdate（String + List<Map>） ====================

    @Test
    @DisplayName("batchUpdate(Map)：批量插入")
    void testBatchUpdateWithNamedParams() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        List<Map<String, ?>> batchParams = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("name", "batch1");
        row1.put("email", "batch1@test.com");
        batchParams.add(row1);
        Map<String, Object> row2 = new HashMap<>();
        row2.put("name", "batch2");
        row2.put("email", "batch2@test.com");
        batchParams.add(row2);

        BatchUpdateResult result = template.batchUpdate(
                "INSERT INTO users(username, email) VALUES(#{name}, #{email})",
                batchParams, 10);

        assertEquals(2, result.getTotal());
    }

    @Test
    @DisplayName("batchUpdate(Map)：quietly=true 遇错继续")
    void testBatchUpdateNamedQuietly() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        List<Map<String, ?>> batchParams = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("name", "q1");
        row1.put("email", "q1@test.com");
        batchParams.add(row1);
        Map<String, Object> row2 = new HashMap<>();
        row2.put("name", "q2");
        row2.put("email", "q2@test.com");
        batchParams.add(row2);

        BatchUpdateResult result = template.batchUpdate(
                "INSERT INTO users(username, email) VALUES(#{name}, #{email})",
                batchParams, 10, true);

        assertEquals(2, result.getTotal());
    }

    // ==================== batchUpdate（NamedParamSql + List<Map>） ====================

    @Test
    @DisplayName("batchUpdate(NamedParamSql)：批量插入")
    void testBatchUpdateWithNamedParamSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamSql tmpl = NamedParamSql.of(
                "INSERT INTO users (username, email, age, active) "
                        + "VALUES (#{username}, #{email}, #{age}, #{active})");

        Map<String, Object> row1 = new HashMap<>();
        row1.put("username", "gina");
        row1.put("email", "gina@e.com");
        row1.put("age", 28);
        row1.put("active", true);

        Map<String, Object> row2 = new HashMap<>();
        row2.put("username", "hank");
        row2.put("email", "hank@e.com");
        row2.put("age", 32);
        row2.put("active", false);

        List<Map<String, ?>> batch = Arrays.asList(row1, row2);

        BatchUpdateResult result = template.batchUpdate(tmpl, batch, 10);
        assertEquals(2, result.getTotal());
    }

    @Test
    @DisplayName("batchUpdate(NamedParamSql)：quietly=true 遇错继续")
    void testBatchUpdateQuietlyWithNamedParamSql() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        NamedParamSql tmpl = NamedParamSql.of(
                "INSERT INTO users (username, email, age, active) "
                        + "VALUES (#{username}, #{email}, #{age}, #{active})");

        Map<String, Object> row1 = new HashMap<>();
        row1.put("username", "iris");
        row1.put("email", "iris@e.com");
        row1.put("age", 26);
        row1.put("active", true);

        Map<String, Object> row2 = new HashMap<>();
        row2.put("username", "jack");
        row2.put("email", "jack@e.com");
        row2.put("age", 30);
        row2.put("active", true);

        List<Map<String, ?>> batch = Arrays.asList(row1, row2);

        BatchUpdateResult result = template.batchUpdate(tmpl, batch, 10, true);
        assertEquals(2, result.getTotal());
    }

}
