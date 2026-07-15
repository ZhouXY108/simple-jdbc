package xyz.zhouxy.jdbc.test;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.ResultSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.zhouxy.jdbc.JdbcConfig;

@DisplayName("JdbcConfig 构建与不可变性")
class JdbcConfigTest {

    @Test
    @DisplayName("默认配置")
    void testDefaults() {
        JdbcConfig config = JdbcConfig.defaults();
        assertNull(config.getFetchSize());
        assertNull(config.getMaxRows());
        assertNull(config.getQueryTimeout());
        assertEquals(ResultSet.TYPE_FORWARD_ONLY, config.getResultSetType());
        assertEquals(ResultSet.CONCUR_READ_ONLY, config.getResultSetConcurrency());
    }

    @Test
    @DisplayName("Builder 设置全部字段")
    void testBuilderFull() {
        JdbcConfig config = JdbcConfig.builder()
                .fetchSize(100)
                .maxRows(500)
                .queryTimeout(30)
                .resultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE)
                .resultSetConcurrency(ResultSet.CONCUR_UPDATABLE)
                .build();
        assertEquals(Integer.valueOf(100), config.getFetchSize());
        assertEquals(Integer.valueOf(500), config.getMaxRows());
        assertEquals(Integer.valueOf(30), config.getQueryTimeout());
        assertEquals(ResultSet.TYPE_SCROLL_INSENSITIVE, config.getResultSetType());
        assertEquals(ResultSet.CONCUR_UPDATABLE, config.getResultSetConcurrency());
    }

    @Test
    @DisplayName("Builder 部分设置（未设置项为 null/默认）")
    void testBuilderPartial() {
        JdbcConfig config = JdbcConfig.builder()
                .fetchSize(50)
                .build();
        assertEquals(Integer.valueOf(50), config.getFetchSize());
        assertNull(config.getMaxRows());
        assertNull(config.getQueryTimeout());
        assertEquals(ResultSet.TYPE_FORWARD_ONLY, config.getResultSetType());
        assertEquals(ResultSet.CONCUR_READ_ONLY, config.getResultSetConcurrency());
    }

    @Test
    @DisplayName("非法 resultSetType 抛出异常")
    void testInvalidResultSetType() {
        //noinspection WriteOnlyObject
        assertThrows(IllegalArgumentException.class,
                () -> JdbcConfig.builder().resultSetType(999));
    }

    @Test
    @DisplayName("非法 resultSetConcurrency 抛出异常")
    void testInvalidResultSetConcurrency() {
        //noinspection WriteOnlyObject
        assertThrows(IllegalArgumentException.class,
                () -> JdbcConfig.builder().resultSetConcurrency(999));
    }

    @Test
    @DisplayName("负值 fetchSize 抛出异常")
    void testNegativeFetchSize() {
        //noinspection WriteOnlyObject
        assertThrows(IllegalArgumentException.class,
                () -> JdbcConfig.builder().fetchSize(-1));
    }

    @Test
    @DisplayName("负值 maxRows 抛出异常")
    void testNegativeMaxRows() {
        //noinspection WriteOnlyObject
        assertThrows(IllegalArgumentException.class,
                () -> JdbcConfig.builder().maxRows(-1));
    }

    @Test
    @DisplayName("负值 queryTimeout 抛出异常")
    void testNegativeQueryTimeout() {
        //noinspection WriteOnlyObject
        assertThrows(IllegalArgumentException.class,
                () -> JdbcConfig.builder().queryTimeout(-1));
    }

    @Test
    @DisplayName("不可变性（build 后修改 Builder 不影响已有实例）")
    void testImmutability() {
        JdbcConfig.Builder builder = JdbcConfig.builder();
        builder.fetchSize(10);
        JdbcConfig config1 = builder.build();
        builder.fetchSize(20);
        JdbcConfig config2 = builder.build();
        assertEquals(Integer.valueOf(10), config1.getFetchSize());
        assertEquals(Integer.valueOf(20), config2.getFetchSize());
    }

    @Test
    @DisplayName("equals 与 hashCode")
    void testEqualsAndHashCode() {
        JdbcConfig configA = JdbcConfig.builder()
                .fetchSize(100).maxRows(200).queryTimeout(10)
                .resultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE)
                .build();
        JdbcConfig configB = JdbcConfig.builder()
                .fetchSize(100).maxRows(200).queryTimeout(10)
                .resultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE)
                .build();
        assertEquals(configA, configB);
        assertEquals(configA.hashCode(), configB.hashCode());
    }

    @Test
    @DisplayName("与其他配置不相等")
    void testNotEquals() {
        JdbcConfig config = JdbcConfig.defaults();
        JdbcConfig different = JdbcConfig.builder().fetchSize(1).build();
        assertNotEquals(config, different);
    }
}
