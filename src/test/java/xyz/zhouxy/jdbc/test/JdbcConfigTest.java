package xyz.zhouxy.jdbc.test;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import xyz.zhouxy.jdbc.BatchUpdateResult;
import xyz.zhouxy.jdbc.JdbcConfig;
import xyz.zhouxy.jdbc.JdbcExecutor;
import xyz.zhouxy.jdbc.JdbcOperations;
import xyz.zhouxy.jdbc.NullBindingStrategy;
import xyz.zhouxy.jdbc.ParameterBinder;
import xyz.zhouxy.jdbc.SimpleJdbcTemplate;
import xyz.zhouxy.jdbc.TypeBinder;
import xyz.zhouxy.jdbc.namedparam.NamedParamJdbcExecutor;
import xyz.zhouxy.jdbc.namedparam.NamedParamJdbcOperations;

@DisplayName("JdbcConfig 构建与不可变性")
class JdbcConfigTest extends BaseH2Test {

    @Nested
    @DisplayName("配置构建与不可变性（纯单元测试）")
    class ConfigurationTests {

        @Test
        @DisplayName("默认配置")
        void testDefaults() {
            JdbcConfig config = JdbcConfig.defaults();
            assertNull(config.getFetchSize());
            assertNull(config.getMaxRows());
            assertNull(config.getQueryTimeout());
            assertEquals(ResultSet.TYPE_FORWARD_ONLY, config.getResultSetType());
            assertEquals(ResultSet.CONCUR_READ_ONLY, config.getResultSetConcurrency());
            assertEquals(NullBindingStrategy.STANDARD, config.getNullBindingStrategy());
            assertTrue(config.getParameterBinders().isEmpty());
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
                    .nullBindingStrategy(NullBindingStrategy.VARCHAR_FALLBACK)
                    .addParameterBinder((ps, i, v) -> false)
                    .build();
            assertEquals(Integer.valueOf(100), config.getFetchSize());
            assertEquals(Integer.valueOf(500), config.getMaxRows());
            assertEquals(Integer.valueOf(30), config.getQueryTimeout());
            assertEquals(ResultSet.TYPE_SCROLL_INSENSITIVE, config.getResultSetType());
            assertEquals(ResultSet.CONCUR_UPDATABLE, config.getResultSetConcurrency());
            assertEquals(NullBindingStrategy.VARCHAR_FALLBACK, config.getNullBindingStrategy());
            assertEquals(1, config.getParameterBinders().size());
        }

        @Test
        @DisplayName("自定义 nullBindingStrategy")
        void testNullBindingStrategyCustom() {
            JdbcConfig config = JdbcConfig.builder()
                    .nullBindingStrategy(NullBindingStrategy.PARAMETER_METADATA)
                    .build();
            assertEquals(NullBindingStrategy.PARAMETER_METADATA, config.getNullBindingStrategy());
        }

        @Test
        @DisplayName("添加 ParameterBinder 保持顺序")
        void testAddParameterBinder() {
            ParameterBinder binder1 = (ps, i, v) -> false;
            ParameterBinder binder2 = (ps, i, v) -> true;
            JdbcConfig config = JdbcConfig.builder()
                    .addParameterBinder(binder1)
                    .addParameterBinder(binder2)
                    .build();
            assertEquals(2, config.getParameterBinders().size());
            assertSame(binder1, config.getParameterBinders().get(0));
            assertSame(binder2, config.getParameterBinders().get(1));
        }

        @Test
        @DisplayName("parameterBinders 列表不可变")
        void testParameterBindersImmutability() {
            JdbcConfig config = JdbcConfig.builder()
                    .addParameterBinder((ps, i, v) -> false)
                    .build();
            assertThrows(UnsupportedOperationException.class,
                    () -> config.getParameterBinders().add((ps, i, v) -> false));
        }

        @Test
        @DisplayName("addParameterBinder(Class, TypeBinder) 注册")
        void testAddParameterBinderByClass() {
            JdbcConfig config = JdbcConfig.builder()
                    .addParameterBinder(String.class, (ps, i, v) -> ps.setString(i, v))
                    .build();
            assertEquals(1, config.getParameterBinders().size());
        }

        @Test
        @DisplayName("addParameterBinder(Class, TypeBinder) 支持多态类型匹配")
        void testAddParameterBinderByClassPolymorphism() {
            JdbcConfig config = JdbcConfig.builder()
                    .addParameterBinder(Number.class, (ps, i, v) -> ps.setInt(i, v.intValue()))
                    .build();
            assertEquals(1, config.getParameterBinders().size());
        }

        @Test
        @DisplayName("nullBindingStrategy 传入 null 抛出 IllegalArgumentException")
        void testNullBindingStrategyNull() {
            assertThrows(IllegalArgumentException.class,
                    () -> JdbcConfig.builder().nullBindingStrategy(null));
        }

        @Test
        @DisplayName("addParameterBinder 传入 null 抛出 IllegalArgumentException")
        void testAddParameterBinderNull() {
            //noinspection DataFlowIssue
            assertThrows(IllegalArgumentException.class,
                    () -> JdbcConfig.builder().addParameterBinder((ParameterBinder) null));
        }

        @Test
        @DisplayName("addParameterBinder(Class, TypeBinder) 传入 null 抛出 IllegalArgumentException")
        void testAddParameterBinderByClassNull() {
            TypeBinder<String> binder = (ps, i, v) -> ps.setString(i, v);
            //noinspection DataFlowIssue
            assertThrows(IllegalArgumentException.class,
                    () -> JdbcConfig.builder().addParameterBinder(null, binder));
            //noinspection DataFlowIssue
            assertThrows(IllegalArgumentException.class,
                    () -> JdbcConfig.builder().addParameterBinder(String.class, null));
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
            ParameterBinder binder = (ps, i, v) -> false;
            JdbcConfig configA = JdbcConfig.builder()
                    .fetchSize(100).maxRows(200).queryTimeout(10)
                    .resultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE)
                    .nullBindingStrategy(NullBindingStrategy.VARCHAR_FALLBACK)
                    .addParameterBinder(binder)
                    .build();
            JdbcConfig configB = JdbcConfig.builder()
                    .fetchSize(100).maxRows(200).queryTimeout(10)
                    .resultSetType(ResultSet.TYPE_SCROLL_INSENSITIVE)
                    .nullBindingStrategy(NullBindingStrategy.VARCHAR_FALLBACK)
                    .addParameterBinder(binder)
                    .build();
            assertEquals(configA, configB);
            assertEquals(configA.hashCode(), configB.hashCode());
        }

        @Test
        @DisplayName("nullBindingStrategy 不同则不相等")
        void testNotEqualsByNullBindingStrategy() {
            JdbcConfig config = JdbcConfig.builder()
                    .nullBindingStrategy(NullBindingStrategy.STANDARD)
                    .build();
            JdbcConfig different = JdbcConfig.builder()
                    .nullBindingStrategy(NullBindingStrategy.VARCHAR_FALLBACK)
                    .build();
            assertNotEquals(config, different);
        }

        @Test
        @DisplayName("parameterBinders 不同则不相等")
        void testNotEqualsByParameterBinders() {
            JdbcConfig config = JdbcConfig.builder()
                    .addParameterBinder((ps, i, v) -> false)
                    .build();
            JdbcConfig different = JdbcConfig.builder()
                    .addParameterBinder((ps, i, v) -> true)
                    .build();
            assertNotEquals(config, different);
        }

        @Test
        @DisplayName("toString 包含新字段")
        void testToStringContainsNewFields() {
            JdbcConfig config = JdbcConfig.builder()
                    .nullBindingStrategy(NullBindingStrategy.PARAMETER_METADATA)
                    .addParameterBinder((ps, i, v) -> false)
                    .build();
            String str = config.toString();
            assertTrue(str.contains("nullBindingStrategy=" + NullBindingStrategy.PARAMETER_METADATA));
            assertTrue(str.contains("parameterBinders="));
        }
    }

    @Nested
    @DisplayName("NullBindingStrategy DB 执行验证")
    class NullBindingTests {

        private Connection conn;

        @BeforeEach
        void setUp() throws Exception {
            resetDatabase();
            conn = dataSource.getConnection();
        }

        @AfterEach
        void tearDown() throws Exception {
            if (conn != null) {
                conn.close();
            }
        }

        @Test
        @DisplayName("NullBindingStrategy.STANDARD 绑定 null")
        void testNullBindingStrategyStandard() throws SQLException {
            JdbcConfig config = JdbcConfig.builder()
                    .nullBindingStrategy(NullBindingStrategy.STANDARD)
                    .build();
            JdbcExecutor customExecutor = new JdbcExecutor(config);

            int rows = customExecutor.update(conn,
                    "INSERT INTO users (username, email) VALUES (?, ?)",
                    new Object[]{"standard_null", null});
            assertEquals(1, rows);

            Optional<Map<String, Object>> row = customExecutor.queryFirst(conn,
                    "SELECT email FROM users WHERE username = ?",
                    new Object[]{"standard_null"});
            assertTrue(row.isPresent());
            assertTrue(row.get().containsKey("email"));
            assertNull(row.get().get("email"));
        }

        @Test
        @DisplayName("NullBindingStrategy.VARCHAR_FALLBACK 绑定 null")
        void testNullBindingStrategyVarcharFallback() throws SQLException {
            JdbcConfig config = JdbcConfig.builder()
                    .nullBindingStrategy(NullBindingStrategy.VARCHAR_FALLBACK)
                    .build();
            JdbcExecutor customExecutor = new JdbcExecutor(config);

            int rows = customExecutor.update(conn,
                    "INSERT INTO users (username, email) VALUES (?, ?)",
                    new Object[]{"varchar_null", null});
            assertEquals(1, rows);

            Optional<Map<String, Object>> row = customExecutor.queryFirst(conn,
                    "SELECT email FROM users WHERE username = ?",
                    new Object[]{"varchar_null"});
            assertTrue(row.isPresent());
            assertTrue(row.get().containsKey("email"));
            assertNull(row.get().get("email"));
        }

        @Test
        @DisplayName("NullBindingStrategy.PARAMETER_METADATA 绑定 null")
        void testNullBindingStrategyParameterMetadata() throws SQLException {
            JdbcConfig config = JdbcConfig.builder()
                    .nullBindingStrategy(NullBindingStrategy.PARAMETER_METADATA)
                    .build();
            JdbcExecutor customExecutor = new JdbcExecutor(config);

            int rows = customExecutor.update(conn,
                    "INSERT INTO users (username, email) VALUES (?, ?)",
                    new Object[]{"pmd_null", null});
            assertEquals(1, rows);

            Optional<Map<String, Object>> row = customExecutor.queryFirst(conn,
                    "SELECT email FROM users WHERE username = ?",
                    new Object[]{"pmd_null"});
            assertTrue(row.isPresent());
            assertTrue(row.get().containsKey("email"));
            assertNull(row.get().get("email"));
        }
    }

    @Nested
    @DisplayName("ParameterBinder / TypeBinder DB 执行验证")
    class ParameterBinderTests {

        private Connection conn;

        @BeforeEach
        void setUp() throws Exception {
            resetDatabase();
            conn = dataSource.getConnection();
        }

        @AfterEach
        void tearDown() throws Exception {
            if (conn != null) {
                conn.close();
            }
        }

        @Test
        @DisplayName("自定义 ParameterBinder 拦截 LocalDate 转为字符串")
        void testCustomParameterBinder() throws SQLException {
            ParameterBinder localDateToStringBinder = (ps, index, value) -> {
                if (value instanceof LocalDate) {
                    ps.setString(index, value.toString());
                    return true;
                }
                return false;
            };
            JdbcConfig config = JdbcConfig.builder()
                    .addParameterBinder(localDateToStringBinder)
                    .build();
            JdbcExecutor customExecutor = new JdbcExecutor(config);

            LocalDate birthDate = LocalDate.of(1990, 5, 20);
            int rows = customExecutor.update(conn,
                    "INSERT INTO users (username, birth_date) VALUES (?, ?)",
                    new Object[]{"binder_date", birthDate});
            assertEquals(1, rows);

            String stored = customExecutor.queryValue(conn,
                    "SELECT birth_date FROM users WHERE username = ?",
                    new Object[]{"binder_date"}, String.class).orElse(null);
            assertEquals("1990-05-20", stored);
        }

        @Test
        @DisplayName("ParameterBinder 未拦截时走 setObject 兜底")
        void testParameterBinderNotIntercepting() throws SQLException {
            ParameterBinder integerBinder = (ps, index, value) -> {
                if (value instanceof Integer) {
                    ps.setInt(index, (Integer) value);
                    return true;
                }
                return false;
            };
            JdbcConfig config = JdbcConfig.builder()
                    .addParameterBinder(integerBinder)
                    .build();
            JdbcExecutor customExecutor = new JdbcExecutor(config);

            int rows = customExecutor.update(conn,
                    "INSERT INTO users (username, age) VALUES (?, ?)",
                    new Object[]{"binder_fallback", 25});
            assertEquals(1, rows);

            Integer age = customExecutor.queryValue(conn,
                    "SELECT age FROM users WHERE username = ?",
                    new Object[]{"binder_fallback"}, Integer.class).orElse(null);
            assertEquals(Integer.valueOf(25), age);
        }

        @Test
        @DisplayName("addParameterBinder(Class, TypeBinder) 类型绑定器生效")
        void testAddParameterBinderByClass() throws SQLException {
            JdbcConfig config = JdbcConfig.builder()
                    .addParameterBinder(Integer.class, (ps, index, value) -> ps.setInt(index, value + 100))
                    .build();
            JdbcExecutor customExecutor = new JdbcExecutor(config);

            int rows = customExecutor.update(conn,
                    "INSERT INTO users (username, age) VALUES (?, ?)",
                    new Object[]{"class_binder", 25});
            assertEquals(1, rows);

            Integer age = customExecutor.queryValue(conn,
                    "SELECT age FROM users WHERE username = ?",
                    new Object[]{"class_binder"}, Integer.class).orElse(null);
            assertEquals(Integer.valueOf(125), age);
        }

        @Test
        @DisplayName("null 值不受自定义 ParameterBinder 影响")
        void testNullNotAffectedByCustomBinder() throws SQLException {
            ParameterBinder stringBinder = (ps, index, value) -> {
                if (value instanceof String) {
                    ps.setString(index, "intercepted:" + value);
                    return true;
                }
                return false;
            };
            JdbcConfig config = JdbcConfig.builder()
                    .addParameterBinder(stringBinder)
                    .nullBindingStrategy(NullBindingStrategy.STANDARD)
                    .build();
            JdbcExecutor customExecutor = new JdbcExecutor(config);

            int rows = customExecutor.update(conn,
                    "INSERT INTO users (username, email) VALUES (?, ?)",
                    new Object[]{"binder_null", null});
            assertEquals(1, rows);

            Optional<Map<String, Object>> row = customExecutor.queryFirst(conn,
                    "SELECT email FROM users WHERE username = ?",
                    new Object[]{"binder_null"});
            assertTrue(row.isPresent());
            assertTrue(row.get().containsKey("email"));
            assertNull(row.get().get("email"));
        }

        @Test
        @DisplayName("自定义 ParameterBinder 恢复 java.time 显式转换")
        void testCustomParameterBinderForJavaTime() throws SQLException {
            ParameterBinder javaTimeBinder = (ps, index, value) -> {
                if (value instanceof LocalDate) {
                    ps.setDate(index, java.sql.Date.valueOf((LocalDate) value));
                    return true;
                }
                if (value instanceof LocalTime) {
                    ps.setTime(index, java.sql.Time.valueOf((LocalTime) value));
                    return true;
                }
                if (value instanceof LocalDateTime) {
                    ps.setTimestamp(index, java.sql.Timestamp.valueOf((LocalDateTime) value));
                    return true;
                }
                if (value instanceof Instant) {
                    ps.setTimestamp(index, java.sql.Timestamp.from((Instant) value));
                    return true;
                }
                return false;
            };
            JdbcConfig config = JdbcConfig.builder()
                    .addParameterBinder(javaTimeBinder)
                    .build();
            JdbcExecutor customExecutor = new JdbcExecutor(config);

            LocalDateTime dateTime = LocalDateTime.of(2024, 6, 15, 14, 30, 0);
            int rowsDateTime = customExecutor.update(conn,
                    "INSERT INTO users (username, created_at) VALUES (?, ?)",
                    new Object[]{"java_time_datetime", dateTime});
            assertEquals(1, rowsDateTime);

            LocalDate date = LocalDate.of(1996, 5, 20);
            int rowsDate = customExecutor.update(conn,
                    "INSERT INTO users (username, birth_date) VALUES (?, ?)",
                    new Object[]{"java_time_date", date});
            assertEquals(1, rowsDate);

            LocalTime time = LocalTime.of(8, 30, 0);
            int rowsTime = customExecutor.update(conn,
                    "INSERT INTO users (username, work_start_time) VALUES (?, ?)",
                    new Object[]{"java_time_time", time});
            assertEquals(1, rowsTime);

            Instant instant = Instant.parse("2024-06-15T06:30:00Z");
            int rowsInstant = customExecutor.update(conn,
                    "INSERT INTO users (username, created_at) VALUES (?, ?)",
                    new Object[]{"java_time_instant", instant});
            assertEquals(1, rowsInstant);

            LocalDateTime storedDateTime = customExecutor.queryValue(conn,
                    "SELECT created_at FROM users WHERE username = ?",
                    new Object[]{"java_time_datetime"}, LocalDateTime.class).orElse(null);
            assertEquals(dateTime, storedDateTime);

            LocalDate storedDate = customExecutor.queryValue(conn,
                    "SELECT birth_date FROM users WHERE username = ?",
                    new Object[]{"java_time_date"}, LocalDate.class).orElse(null);
            assertEquals(date, storedDate);

            LocalTime storedTime = customExecutor.queryValue(conn,
                    "SELECT work_start_time FROM users WHERE username = ?",
                    new Object[]{"java_time_time"}, LocalTime.class).orElse(null);
            assertEquals(time, storedTime);

            java.sql.Timestamp storedInstant = customExecutor.queryValue(conn,
                    "SELECT created_at FROM users WHERE username = ?",
                    new Object[]{"java_time_instant"}, java.sql.Timestamp.class).orElse(null);
            assertNotNull(storedInstant);
            assertEquals(instant, storedInstant.toInstant());
        }

        @Test
        @DisplayName("java.time 默认走 setObject 往返（破坏性变更默认行为）")
        void testJavaTimeDefaultSetObjectRoundTrip() throws SQLException {
            JdbcConfig config = JdbcConfig.defaults();
            JdbcExecutor executor = new JdbcExecutor(config);

            LocalDateTime dateTime = LocalDateTime.of(2024, 6, 15, 14, 30, 0);
            int rowsDateTime = executor.update(conn,
                    "INSERT INTO users (username, created_at) VALUES (?, ?)",
                    new Object[]{"default_datetime", dateTime});
            assertEquals(1, rowsDateTime);

            LocalDate date = LocalDate.of(1996, 5, 20);
            int rowsDate = executor.update(conn,
                    "INSERT INTO users (username, birth_date) VALUES (?, ?)",
                    new Object[]{"default_date", date});
            assertEquals(1, rowsDate);

            LocalTime time = LocalTime.of(8, 30, 0);
            int rowsTime = executor.update(conn,
                    "INSERT INTO users (username, work_start_time) VALUES (?, ?)",
                    new Object[]{"default_time", time});
            assertEquals(1, rowsTime);

            Instant instant = Instant.parse("2024-06-15T06:30:00Z");
            int rowsInstant = executor.update(conn,
                    "INSERT INTO users (username, created_at) VALUES (?, ?)",
                    new Object[]{"default_instant", instant});
            assertEquals(1, rowsInstant);

            LocalDateTime storedDateTime = executor.queryValue(conn,
                    "SELECT created_at FROM users WHERE username = ?",
                    new Object[]{"default_datetime"}, LocalDateTime.class).orElse(null);
            assertEquals(dateTime, storedDateTime);

            LocalDate storedDate = executor.queryValue(conn,
                    "SELECT birth_date FROM users WHERE username = ?",
                    new Object[]{"default_date"}, LocalDate.class).orElse(null);
            assertEquals(date, storedDate);

            LocalTime storedTime = executor.queryValue(conn,
                    "SELECT work_start_time FROM users WHERE username = ?",
                    new Object[]{"default_time"}, LocalTime.class).orElse(null);
            assertEquals(time, storedTime);

            java.sql.Timestamp storedInstant = executor.queryValue(conn,
                    "SELECT created_at FROM users WHERE username = ?",
                    new Object[]{"default_instant"}, java.sql.Timestamp.class).orElse(null);
            assertNotNull(storedInstant);
            assertEquals(instant, storedInstant.toInstant());
        }

        @Test
        @DisplayName("多个 ParameterBinder：前一个不处理，后一个处理")
        void testMultipleBindersShortCircuit() throws SQLException {
            AtomicBoolean secondBinderCalled = new AtomicBoolean(false);
            ParameterBinder noOpBinder = (ps, index, value) -> false;
            ParameterBinder integerBinder = (ps, index, value) -> {
                secondBinderCalled.set(true);
                if (value instanceof Integer) {
                    ps.setInt(index, (Integer) value + 100);
                    return true;
                }
                return false;
            };
            JdbcConfig config = JdbcConfig.builder()
                    .addParameterBinder(noOpBinder)
                    .addParameterBinder(integerBinder)
                    .build();
            JdbcExecutor customExecutor = new JdbcExecutor(config);

            int rows = customExecutor.update(conn,
                    "INSERT INTO users (username, age) VALUES (?, ?)",
                    new Object[]{"multi_binder", 25});
            assertEquals(1, rows);
            assertTrue(secondBinderCalled.get());

            Integer age = customExecutor.queryValue(conn,
                    "SELECT age FROM users WHERE username = ?",
                    new Object[]{"multi_binder"}, Integer.class).orElse(null);
            assertEquals(Integer.valueOf(125), age);
        }

        @Test
        @DisplayName("多个 ParameterBinder：第一个处理即短路，第二个不应被调用")
        void testMultipleBindersFirstWins() throws SQLException {
            AtomicBoolean secondBinderCalledForInteger = new AtomicBoolean(false);
            ParameterBinder firstBinder = (ps, index, value) -> {
                if (value instanceof Integer) {
                    ps.setInt(index, (Integer) value + 1);
                    return true;
                }
                return false;
            };
            ParameterBinder secondBinder = (ps, index, value) -> {
                if (value instanceof Integer) {
                    secondBinderCalledForInteger.set(true);
                    ps.setInt(index, (Integer) value + 100);
                    return true;
                }
                return false;
            };
            JdbcConfig config = JdbcConfig.builder()
                    .addParameterBinder(firstBinder)
                    .addParameterBinder(secondBinder)
                    .build();
            JdbcExecutor customExecutor = new JdbcExecutor(config);

            int rows = customExecutor.update(conn,
                    "INSERT INTO users (username, age) VALUES (?, ?)",
                    new Object[]{"first_wins", 25});
            assertEquals(1, rows);
            assertFalse(secondBinderCalledForInteger.get());

            Integer age = customExecutor.queryValue(conn,
                    "SELECT age FROM users WHERE username = ?",
                    new Object[]{"first_wins"}, Integer.class).orElse(null);
            assertEquals(Integer.valueOf(26), age);
        }

        @Test
        @DisplayName("TypeBinder 多态：注册 Number.class 拦截 Long/Double")
        void testTypeBinderPolymorphism() throws SQLException {
            JdbcConfig config = JdbcConfig.builder()
                    .addParameterBinder(Number.class, (ps, index, value) -> ps.setLong(index, value.longValue() * 10))
                    .build();
            JdbcExecutor customExecutor = new JdbcExecutor(config);

            int rowsLong = customExecutor.update(conn,
                    "INSERT INTO users (username, balance) VALUES (?, ?)",
                    new Object[]{"poly_long", 100L});
            assertEquals(1, rowsLong);

            Long balanceLong = customExecutor.queryValue(conn,
                    "SELECT balance FROM users WHERE username = ?",
                    new Object[]{"poly_long"}, Long.class).orElse(null);
            assertEquals(Long.valueOf(1000L), balanceLong);

            int rowsInteger = customExecutor.update(conn,
                    "INSERT INTO users (username, age) VALUES (?, ?)",
                    new Object[]{"poly_int", 30});
            assertEquals(1, rowsInteger);

            Integer age = customExecutor.queryValue(conn,
                    "SELECT age FROM users WHERE username = ?",
                    new Object[]{"poly_int"}, Integer.class).orElse(null);
            assertEquals(Integer.valueOf(300), age);
        }
    }

    @Nested
    @DisplayName("命名参数路径 DB 执行验证")
    class NamedParameterTests {

        private Connection conn;

        @BeforeEach
        void setUp() throws Exception {
            resetDatabase();
            conn = dataSource.getConnection();
        }

        @AfterEach
        void tearDown() throws Exception {
            if (conn != null) {
                conn.close();
            }
        }

        @Test
        @DisplayName("命名参数 + NullBindingStrategy.VARCHAR_FALLBACK 绑定 null")
        void testNamedParamNullBindingStrategy() throws SQLException {
            JdbcConfig config = JdbcConfig.builder()
                    .nullBindingStrategy(NullBindingStrategy.VARCHAR_FALLBACK)
                    .build();
            NamedParamJdbcExecutor namedExecutor = new NamedParamJdbcExecutor(config);

            HashMap<String, Object> params = new HashMap<>();
            params.put("username", "np_null");
            params.put("email", null);
            int rows = namedExecutor.update(conn,
                    "INSERT INTO users (username, email) VALUES (#{username}, #{email})",
                    params);
            assertEquals(1, rows);

            Optional<Map<String, Object>> row = namedExecutor.queryFirst(conn,
                    "SELECT email FROM users WHERE username = #{username}",
                    Collections.singletonMap("username", "np_null"));
            assertTrue(row.isPresent());
            assertNull(row.get().get("email"));
        }

        @Test
        @DisplayName("命名参数 + 自定义 ParameterBinder 拦截 LocalDate")
        void testNamedParamCustomBinder() throws SQLException {
            ParameterBinder localDateBinder = (ps, index, value) -> {
                if (value instanceof LocalDate) {
                    ps.setString(index, value.toString());
                    return true;
                }
                return false;
            };
            JdbcConfig config = JdbcConfig.builder()
                    .addParameterBinder(localDateBinder)
                    .build();
            NamedParamJdbcExecutor namedExecutor = new NamedParamJdbcExecutor(config);

            LocalDate birthDate = LocalDate.of(1990, 5, 20);
            HashMap<String, Object> binderParams = new HashMap<>();
            binderParams.put("username", "np_binder");
            binderParams.put("birthDate", birthDate);
            int rows = namedExecutor.update(conn,
                    "INSERT INTO users (username, birth_date) VALUES (#{username}, #{birthDate})",
                    binderParams);
            assertEquals(1, rows);

            String stored = namedExecutor.queryValue(conn,
                    "SELECT birth_date FROM users WHERE username = #{username}",
                    Collections.singletonMap("username", "np_binder"), String.class).orElse(null);
            assertEquals("1990-05-20", stored);
        }

        @Test
        @DisplayName("命名参数 + java.time 默认走 setObject 往返")
        void testNamedParamJavaTimeDefault() throws SQLException {
            JdbcConfig config = JdbcConfig.defaults();
            NamedParamJdbcExecutor namedExecutor = new NamedParamJdbcExecutor(config);

            LocalDateTime dateTime = LocalDateTime.of(2024, 6, 15, 14, 30, 0);
            HashMap<String, Object> javaTimeParams = new HashMap<>();
            javaTimeParams.put("username", "np_java_time");
            javaTimeParams.put("createdAt", dateTime);
            int rows = namedExecutor.update(conn,
                    "INSERT INTO users (username, created_at) VALUES (#{username}, #{createdAt})",
                    javaTimeParams);
            assertEquals(1, rows);

            LocalDateTime stored = namedExecutor.queryValue(conn,
                    "SELECT created_at FROM users WHERE username = #{username}",
                    Collections.singletonMap("username", "np_java_time"), LocalDateTime.class).orElse(null);
            assertEquals(dateTime, stored);
        }
    }

    @Nested
    @DisplayName("批量更新路径 DB 执行验证")
    class BatchUpdateTests {

        private Connection conn;

        @BeforeEach
        void setUp() throws Exception {
            resetDatabase();
            conn = dataSource.getConnection();
        }

        @AfterEach
        void tearDown() throws Exception {
            if (conn != null) {
                conn.close();
            }
        }

        @Test
        @DisplayName("batchUpdate 含 null 参数 + VARCHAR_FALLBACK 策略")
        void testBatchUpdateWithNull() throws SQLException {
            JdbcConfig config = JdbcConfig.builder()
                    .nullBindingStrategy(NullBindingStrategy.VARCHAR_FALLBACK)
                    .build();
            JdbcExecutor customExecutor = new JdbcExecutor(config);

            List<Object[]> params = new ArrayList<>();
            params.add(new Object[]{"batch_null_1", "b1@t.com"});
            params.add(new Object[]{"batch_null_2", null});

            BatchUpdateResult result = customExecutor.batchUpdate(conn,
                    "INSERT INTO users (username, email) VALUES (?, ?)",
                    params, 10);
            assertEquals(2, result.getTotal());

            Optional<Map<String, Object>> row1 = customExecutor.queryFirst(conn,
                    "SELECT email FROM users WHERE username = ?",
                    new Object[]{"batch_null_1"});
            assertTrue(row1.isPresent());
            assertEquals("b1@t.com", row1.get().get("email"));

            Optional<Map<String, Object>> row2 = customExecutor.queryFirst(conn,
                    "SELECT email FROM users WHERE username = ?",
                    new Object[]{"batch_null_2"});
            assertTrue(row2.isPresent());
            assertNull(row2.get().get("email"));
        }

        @Test
        @DisplayName("batchUpdate + 自定义 TypeBinder 逐行生效")
        void testBatchUpdateWithCustomBinder() throws SQLException {
            JdbcConfig config = JdbcConfig.builder()
                    .addParameterBinder(Integer.class, (ps, index, value) -> ps.setInt(index, value + 100))
                    .build();
            JdbcExecutor customExecutor = new JdbcExecutor(config);

            List<Object[]> params = new ArrayList<>();
            params.add(new Object[]{"batch_binder_1", 20});
            params.add(new Object[]{"batch_binder_2", 21});

            BatchUpdateResult result = customExecutor.batchUpdate(conn,
                    "INSERT INTO users (username, age) VALUES (?, ?)",
                    params, 10);
            assertEquals(2, result.getTotal());

            Integer age1 = customExecutor.queryValue(conn,
                    "SELECT age FROM users WHERE username = ?",
                    new Object[]{"batch_binder_1"}, Integer.class).orElse(null);
            assertEquals(Integer.valueOf(120), age1);

            Integer age2 = customExecutor.queryValue(conn,
                    "SELECT age FROM users WHERE username = ?",
                    new Object[]{"batch_binder_2"}, Integer.class).orElse(null);
            assertEquals(Integer.valueOf(121), age2);
        }
    }

    @Nested
    @DisplayName("SimpleJdbcTemplate / TransactionTemplate 集成验证")
    class TemplateIntegrationTests {

        @BeforeEach
        void setUp() throws Exception {
            resetDatabase();
        }

        @Test
        @DisplayName("SimpleJdbcTemplate + 自定义 ParameterBinder 生效")
        void testSimpleJdbcTemplateWithBinder() throws SQLException {
            JdbcConfig config = JdbcConfig.builder()
                    .addParameterBinder(Integer.class, (ps, index, value) -> ps.setInt(index, value + 100))
                    .build();
            SimpleJdbcTemplate template = new SimpleJdbcTemplate(dataSource, config);

            int rows = template.update(
                    "INSERT INTO users (username, age) VALUES (?, ?)",
                    new Object[]{"template_binder", 25});
            assertEquals(1, rows);

            Integer age = template.queryValue(
                    "SELECT age FROM users WHERE username = ?",
                    new Object[]{"template_binder"}, Integer.class).orElse(null);
            assertEquals(Integer.valueOf(125), age);
        }

        @Test
        @DisplayName("SimpleJdbcTemplate + NullBindingStrategy.VARCHAR_FALLBACK 生效")
        void testSimpleJdbcTemplateWithNullStrategy() throws SQLException {
            JdbcConfig config = JdbcConfig.builder()
                    .nullBindingStrategy(NullBindingStrategy.VARCHAR_FALLBACK)
                    .build();
            SimpleJdbcTemplate template = new SimpleJdbcTemplate(dataSource, config);

            int rows = template.update(
                    "INSERT INTO users (username, email) VALUES (?, ?)",
                    new Object[]{"template_null", null});
            assertEquals(1, rows);

            Optional<Map<String, Object>> row = template.queryFirst(
                    "SELECT email FROM users WHERE username = ?",
                    new Object[]{"template_null"});
            assertTrue(row.isPresent());
            assertNull(row.get().get("email"));
        }

        @Test
        @DisplayName("TransactionTemplate 共享 JdbcConfig，binder 在事务内生效")
        void testTransactionTemplateWithBinder() throws Exception {
            JdbcConfig config = JdbcConfig.builder()
                    .addParameterBinder(Integer.class, (ps, index, value) -> ps.setInt(index, value + 100))
                    .build();
            SimpleJdbcTemplate template = new SimpleJdbcTemplate(dataSource, config);

            template.transaction().execute((JdbcOperations ops) -> {
                ops.update(
                        "INSERT INTO users (username, age) VALUES (?, ?)",
                        new Object[]{"tx_binder", 25});
            });

            Integer age = template.queryValue(
                    "SELECT age FROM users WHERE username = ?",
                    new Object[]{"tx_binder"}, Integer.class).orElse(null);
            assertEquals(Integer.valueOf(125), age);
        }

        @Test
        @DisplayName("TransactionTemplate 混用模式共享 JdbcConfig，binder 生效")
        void testTransactionTemplateMixedWithBinder() throws Exception {
            JdbcConfig config = JdbcConfig.builder()
                    .addParameterBinder(Integer.class, (ps, index, value) -> ps.setInt(index, value + 100))
                    .build();
            SimpleJdbcTemplate template = new SimpleJdbcTemplate(dataSource, config);

            template.transaction().execute((JdbcOperations ops, NamedParamJdbcOperations namedOps) -> {
                ops.update(
                        "INSERT INTO users (username, age) VALUES (?, ?)",
                        new Object[]{"tx_mixed_pos", 20});
                HashMap<String, Object> namedParams = new HashMap<>();
                namedParams.put("username", "tx_mixed_np");
                namedParams.put("age", 30);
                namedOps.update(
                        "INSERT INTO users (username, age) VALUES (#{username}, #{age})",
                        namedParams);
            });

            Integer agePos = template.queryValue(
                    "SELECT age FROM users WHERE username = ?",
                    new Object[]{"tx_mixed_pos"}, Integer.class).orElse(null);
            assertEquals(Integer.valueOf(120), agePos);

            Integer ageNp = template.queryValue(
                    "SELECT age FROM users WHERE username = ?",
                    new Object[]{"tx_mixed_np"}, Integer.class).orElse(null);
            assertEquals(Integer.valueOf(130), ageNp);
        }
    }
}
