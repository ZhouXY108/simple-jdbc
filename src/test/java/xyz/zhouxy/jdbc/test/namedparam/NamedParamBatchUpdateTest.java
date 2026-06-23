package xyz.zhouxy.jdbc.test.namedparam;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.zhouxy.jdbc.BatchUpdateResult;
import xyz.zhouxy.jdbc.SimpleJdbcTemplate;
import xyz.zhouxy.jdbc.namedparam.NamedParamJdbcOperations;
import xyz.zhouxy.jdbc.test.BaseH2Test;

/**
 * 命名参数批量更新 API 测试。
 *
 * <p>验证 {@link NamedParamJdbcOperations} 中命名参数（<code>#{paramName}</code>）形式的
 * 批量更新方法是否通过 {@link SimpleJdbcTemplate} 正确委托到
 * {@link xyz.zhouxy.jdbc.JdbcOperations}。</p>
 */
@DisplayName("NamedParamJdbcOperations 命名参数批量更新")
class NamedParamBatchUpdateTest extends BaseH2Test {

    @BeforeEach
    void setUp() throws Exception {
        resetDatabase();
    }

    // ==================== batchUpdate ====================

    @Test
    @DisplayName("batchUpdate：命名参数批量插入")
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

        assertTrue(result.getTotal() >= 2);
    }

    @Test
    @DisplayName("batchUpdate：命名参数 quietly=true 遇错继续")
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

        assertTrue(result.getTotal() >= 2);
    }

}
