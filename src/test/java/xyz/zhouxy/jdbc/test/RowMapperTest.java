package xyz.zhouxy.jdbc.test;

import static org.junit.jupiter.api.Assertions.*;
import static xyz.zhouxy.jdbc.test.JdbcTestAssertions.assertLinkedHashMapOrder;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.zhouxy.jdbc.DefaultBeanRowMapper;
import xyz.zhouxy.jdbc.MapRowMapper;
import xyz.zhouxy.jdbc.RowMapper;
import xyz.zhouxy.jdbc.SimpleBeanRowMapper;
import xyz.zhouxy.jdbc.SimpleJdbcTemplate;

/**
 * RowMapper 测试：SimpleBeanRowMapper、MapRowMapper、自定义 RowMapper。
 *
 * <p>验证 SimpleBeanRowMapper / DefaultBeanRowMapper 的默认映射和自定义列映射，以及 RowMapper 接口的静态工厂方法。</p>
 */
@DisplayName("RowMapper 映射测试")
class RowMapperTest extends BaseH2Test {

    private static final Logger logger = LoggerFactory.getLogger(RowMapperTest.class);

    // ==================== SimpleBeanRowMapper 默认映射 ====================

    @Test
    @DisplayName("SimpleBeanRowMapper：默认小驼峰→小写下划线映射")
    void testSimpleBeanRowMapperDefaultMapping() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        RowMapper<User> rowMapper = RowMapper.beanRowMapper(User.class);

        Optional<User> user = template.queryFirst(
                "SELECT * FROM users WHERE username = ?",
                new Object[]{"alice"}, rowMapper);

        assertTrue(user.isPresent());
        User u = user.get();
        assertEquals(Long.valueOf(1), u.getId());
        assertEquals("alice", u.getUsername());
        assertEquals("alice@example.com", u.getEmail());
        assertEquals(Integer.valueOf(28), u.getAge());
        assertEquals(Long.valueOf(15000), u.getBalance());
        assertTrue(u.getActive());

        logger.info("SimpleBeanRowMapper 默认映射: {}", u);
    }

    @Test
    @DisplayName("SimpleBeanRowMapper：null 值字段映射为 null")
    void testSimpleBeanRowMapperNullFields() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        RowMapper<User> rowMapper = RowMapper.beanRowMapper(User.class);

        Optional<User> user = template.queryFirst(
                "SELECT * FROM users WHERE username = ?",
                new Object[]{"charlie"}, rowMapper);

        assertTrue(user.isPresent());
        User u = user.get();
        assertNull(u.getEmail());
        assertNull(u.getAge());
        assertNull(u.getBirthDate());
        assertNull(u.getWorkStartTime());
        // 非 null 字段应有值
        assertEquals("charlie", u.getUsername());
    }

    // ==================== SimpleBeanRowMapper 自定义列映射 ====================

    @Test
    @DisplayName("SimpleBeanRowMapper：propertyColMap 自定义列名映射")
    void testSimpleBeanRowMapperWithPropertyColMap() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        // 自定义映射：属性名 -> 列名
        Map<String, String> propertyColMap = new HashMap<>();
        propertyColMap.put("username", "user_name_1"); // 使用别名

        RowMapper<User> rowMapper = RowMapper.beanRowMapper(User.class, propertyColMap);

        // 查询时使用别名匹配自定义映射
        Optional<User> user = template.queryFirst(
                "SELECT id, username AS user_name_1, email, age, balance, active FROM users WHERE username = ?",
                new Object[]{"bob"}, rowMapper);

        assertTrue(user.isPresent());
        assertEquals("bob", user.get().getUsername());

        logger.info("自定义列映射: {}", user.get());
    }

    @Test
    @DisplayName("SimpleBeanRowMapper：propertyColMap 未覆盖的属性走默认映射")
    void testSimpleBeanRowMapperPartialPropertyColMap() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        // 只映射 username，其他走默认小驼峰→下划线
        Map<String, String> propertyColMap = new HashMap<>();
        propertyColMap.put("username", "user_label");

        RowMapper<User> rowMapper = RowMapper.beanRowMapper(User.class, propertyColMap);

        Optional<User> user = template.queryFirst(
                "SELECT id, username AS user_label, email, age FROM users WHERE username = ?",
                new Object[]{"eve"}, rowMapper);

        assertTrue(user.isPresent());
        assertEquals("eve", user.get().getUsername());
        assertEquals("eve@example.com", user.get().getEmail());
        assertEquals(Integer.valueOf(31), user.get().getAge());
    }

    // ==================== SimpleBeanRowMapper 边界 ====================

    @Test
    @DisplayName("SimpleBeanRowMapper：Bean 包含不匹配列时正常忽略")
    void testSimpleBeanRowMapperUnmatchedColumns() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        RowMapper<User> rowMapper = RowMapper.beanRowMapper(User.class);

        // 查询返回的列比 Bean 定义的少
        Optional<User> user = template.queryFirst(
                "SELECT id, username FROM users WHERE username = ?",
                new Object[]{"alice"}, rowMapper);

        assertTrue(user.isPresent());
        assertEquals("alice", user.get().getUsername());
        // 未映射的属性应为 null
        assertNull(user.get().getEmail());
        assertNull(user.get().getAge());
    }

    @Test
    @DisplayName("SimpleBeanRowMapper：无无参构造器的 Bean 抛出 IllegalStateException")
    void testSimpleBeanRowMapperNoNoArgConstructor() {
        assertThrows(IllegalStateException.class, () ->
                RowMapper.beanRowMapper(BeanWithoutNoArgConstructor.class));
    }

    @Test
    @DisplayName("SimpleBeanRowMapper：RowMapper.beanRowMapper 静态工厂方法")
    void testRowMapperStaticBeanRowMapper() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        RowMapper<User> rowMapper = RowMapper.beanRowMapper(User.class);

        Optional<User> user = template.queryFirst(
                "SELECT * FROM users WHERE username = ?",
                new Object[]{"alice"}, rowMapper);

        assertTrue(user.isPresent());
        assertEquals("alice", user.get().getUsername());
    }

    // ==================== SimpleBeanRowMapper 连续大写缩写映射 ====================

    @Test
    @DisplayName("SimpleBeanRowMapper：连续大写缩写属性正确映射为 snake_case")
    void testSimpleBeanRowMapperAcronymMapping() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        // 创建测试表，列名使用 snake_case
        template.update("CREATE TABLE acronym_test ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "home_url VARCHAR(100),"
                + "xml_parser VARCHAR(100),"
                + "parse_url VARCHAR(100),"
                + "user_id VARCHAR(100),"
                + "parse_html VARCHAR(100),"
                + "multi_http_client VARCHAR(100))");
        template.update(
                "INSERT INTO acronym_test (home_url, xml_parser, parse_url, user_id, parse_html, multi_http_client)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                new Object[]{"https://example.com", "SAXParser", "/api/v1",
                        "user-001", "<div>test</div>", "ApacheHttpClient"});

        RowMapper<AcronymBean> rowMapper = RowMapper.beanRowMapper(AcronymBean.class);
        Optional<AcronymBean> result = template.queryFirst(
                "SELECT * FROM acronym_test WHERE id = ?",
                new Object[]{1L}, rowMapper);

        assertTrue(result.isPresent());
        AcronymBean bean = result.get();
        assertEquals("https://example.com", bean.getHomeURL());
        assertEquals("SAXParser", bean.getXmlParser());
        assertEquals("/api/v1", bean.getParseURL());
        assertEquals("user-001", bean.getUserID());
        assertEquals("<div>test</div>", bean.getParseHTML());
        assertEquals("ApacheHttpClient", bean.getMultiHttpClient());

        logger.info("缩写映射: homeURL={}, xmlParser={}, parseURL={}, userID={}, parseHTML={}, multiHttpClient={}",
                bean.getHomeURL(), bean.getXmlParser(), bean.getParseURL(),
                bean.getUserID(), bean.getParseHTML(), bean.getMultiHttpClient());
    }

    @Test
    @DisplayName("SimpleBeanRowMapper：纯小写属性名映射为同名列")
    void testSimpleBeanRowMapperAllLowercaseMapping() throws SQLException {
        // 通过 User Bean 验证纯小写属性映射（username → username, email → email）
        SimpleJdbcTemplate template = createTemplate();
        RowMapper<User> rowMapper = RowMapper.beanRowMapper(User.class);

        Optional<User> user = template.queryFirst(
                "SELECT username, email FROM users WHERE username = ?",
                new Object[]{"alice"}, rowMapper);

        assertTrue(user.isPresent());
        assertEquals("alice", user.get().getUsername());
        assertEquals("alice@example.com", user.get().getEmail());
    }

    // ==================== DefaultBeanRowMapper 兼容别名测试 ====================

    @Test
    @DisplayName("DefaultBeanRowMapper：默认小驼峰→小写下划线映射")
    @SuppressWarnings("deprecation")
    void testDefaultBeanRowMapperDefaultMapping() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        RowMapper<User> rowMapper = DefaultBeanRowMapper.of(User.class);

        Optional<User> user = template.queryFirst(
                "SELECT * FROM users WHERE username = ?",
                new Object[]{"alice"}, rowMapper);

        assertTrue(user.isPresent());
        User u = user.get();
        assertEquals(Long.valueOf(1), u.getId());
        assertEquals("alice", u.getUsername());
        assertEquals("alice@example.com", u.getEmail());
        assertEquals(Integer.valueOf(28), u.getAge());
        assertEquals(Long.valueOf(15000), u.getBalance());
        assertTrue(u.getActive());

        logger.info("DefaultBeanRowMapper 默认映射: {}", u);
    }

    @Test
    @DisplayName("DefaultBeanRowMapper：null 值字段映射为 null")
    @SuppressWarnings("deprecation")
    void testDefaultBeanRowMapperNullFields() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        RowMapper<User> rowMapper = DefaultBeanRowMapper.of(User.class);

        Optional<User> user = template.queryFirst(
                "SELECT * FROM users WHERE username = ?",
                new Object[]{"charlie"}, rowMapper);

        assertTrue(user.isPresent());
        User u = user.get();
        assertNull(u.getEmail());
        assertNull(u.getAge());
        assertNull(u.getBirthDate());
        assertNull(u.getWorkStartTime());
        assertEquals("charlie", u.getUsername());
    }

    @Test
    @DisplayName("DefaultBeanRowMapper：propertyColMap 自定义列名映射")
    @SuppressWarnings("deprecation")
    void testDefaultBeanRowMapperWithPropertyColMap() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Map<String, String> propertyColMap = new HashMap<>();
        propertyColMap.put("username", "user_name_1");

        RowMapper<User> rowMapper = DefaultBeanRowMapper.of(User.class, propertyColMap);

        Optional<User> user = template.queryFirst(
                "SELECT id, username AS user_name_1, email, age, balance, active FROM users WHERE username = ?",
                new Object[]{"bob"}, rowMapper);

        assertTrue(user.isPresent());
        assertEquals("bob", user.get().getUsername());
    }

    @Test
    @DisplayName("DefaultBeanRowMapper：propertyColMap 未覆盖的属性走默认映射")
    @SuppressWarnings("deprecation")
    void testDefaultBeanRowMapperPartialPropertyColMap() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Map<String, String> propertyColMap = new HashMap<>();
        propertyColMap.put("username", "user_label");

        RowMapper<User> rowMapper = DefaultBeanRowMapper.of(User.class, propertyColMap);

        Optional<User> user = template.queryFirst(
                "SELECT id, username AS user_label, email, age FROM users WHERE username = ?",
                new Object[]{"eve"}, rowMapper);

        assertTrue(user.isPresent());
        assertEquals("eve", user.get().getUsername());
        assertEquals("eve@example.com", user.get().getEmail());
        assertEquals(Integer.valueOf(31), user.get().getAge());
    }

    @Test
    @DisplayName("DefaultBeanRowMapper：Bean 包含不匹配列时正常忽略")
    @SuppressWarnings("deprecation")
    void testDefaultBeanRowMapperUnmatchedColumns() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        RowMapper<User> rowMapper = DefaultBeanRowMapper.of(User.class);

        Optional<User> user = template.queryFirst(
                "SELECT id, username FROM users WHERE username = ?",
                new Object[]{"alice"}, rowMapper);

        assertTrue(user.isPresent());
        assertEquals("alice", user.get().getUsername());
        assertNull(user.get().getEmail());
        assertNull(user.get().getAge());
    }

    @Test
    @DisplayName("DefaultBeanRowMapper：无无参构造器的 Bean 抛出 IllegalStateException")
    @SuppressWarnings("deprecation")
    void testDefaultBeanRowMapperNoNoArgConstructor() {
        assertThrows(IllegalStateException.class, () ->
                DefaultBeanRowMapper.of(BeanWithoutNoArgConstructor.class));
    }

    @Test
    @DisplayName("DefaultBeanRowMapper：连续大写缩写属性正确映射为 snake_case")
    @SuppressWarnings("deprecation")
    void testDefaultBeanRowMapperAcronymMapping() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        template.update("CREATE TABLE acronym_test_default ("
                + "id BIGINT AUTO_INCREMENT PRIMARY KEY,"
                + "home_url VARCHAR(100),"
                + "xml_parser VARCHAR(100),"
                + "parse_url VARCHAR(100),"
                + "user_id VARCHAR(100),"
                + "parse_html VARCHAR(100),"
                + "multi_http_client VARCHAR(100))");
        template.update(
                "INSERT INTO acronym_test_default (home_url, xml_parser, parse_url, user_id, parse_html, multi_http_client)"
                        + " VALUES (?, ?, ?, ?, ?, ?)",
                new Object[]{"https://example.com", "SAXParser", "/api/v1",
                        "user-001", "<div>test</div>", "ApacheHttpClient"});

        RowMapper<AcronymBean> rowMapper = DefaultBeanRowMapper.of(AcronymBean.class);
        Optional<AcronymBean> result = template.queryFirst(
                "SELECT * FROM acronym_test_default WHERE id = ?",
                new Object[]{1L}, rowMapper);

        assertTrue(result.isPresent());
        AcronymBean bean = result.get();
        assertEquals("https://example.com", bean.getHomeURL());
        assertEquals("SAXParser", bean.getXmlParser());
        assertEquals("/api/v1", bean.getParseURL());
        assertEquals("user-001", bean.getUserID());
        assertEquals("<div>test</div>", bean.getParseHTML());
        assertEquals("ApacheHttpClient", bean.getMultiHttpClient());
    }

    @Test
    @DisplayName("DefaultBeanRowMapper：纯小写属性名映射为同名列")
    @SuppressWarnings("deprecation")
    void testDefaultBeanRowMapperAllLowercaseMapping() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        RowMapper<User> rowMapper = DefaultBeanRowMapper.of(User.class);

        Optional<User> user = template.queryFirst(
                "SELECT username, email FROM users WHERE username = ?",
                new Object[]{"alice"}, rowMapper);

        assertTrue(user.isPresent());
        assertEquals("alice", user.get().getUsername());
        assertEquals("alice@example.com", user.get().getEmail());
    }

    // ==================== MapRowMapper 公共行为 ====================

    static Stream<Arguments> mapRowMappers() {
        return Stream.of(
                Arguments.of(RowMapper.HASH_MAP_MAPPER, "HASH_MAP_MAPPER"),
                Arguments.of(RowMapper.LINKED_HASH_MAP_MAPPER, "LINKED_HASH_MAP_MAPPER"));
    }

    @ParameterizedTest(name = "{1}：所有列映射为 Map")
    @MethodSource("mapRowMappers")
    void testMapRowMapper(RowMapper<Map<String, Object>> rowMapper, String mapperName) throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Optional<Map<String, Object>> user = template.queryFirst(
                "SELECT id, username, email, age FROM users WHERE username = ?",
                new Object[]{"bob"}, rowMapper);

        assertTrue(user.isPresent());
        Map<String, Object> map = user.get();
        assertEquals(2L, map.get("id"));
        assertEquals("bob", map.get("username"));
        assertEquals("bob@example.com", map.get("email"));
        assertEquals(35, map.get("age"));

        logger.info("{} 映射结果: {}", mapperName, map);
    }

    @ParameterizedTest(name = "{1}：空结果返回 Optional.empty()")
    @MethodSource("mapRowMappers")
    void testMapRowMapperEmpty(RowMapper<Map<String, Object>> rowMapper, String mapperName) throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Optional<Map<String, Object>> user = template.queryFirst(
                "SELECT * FROM users WHERE id = ?",
                new Object[]{999}, rowMapper);

        assertFalse(user.isPresent());
    }

    @ParameterizedTest(name = "{1}：查询列表返回 List<Map>")
    @MethodSource("mapRowMappers")
    void testMapRowMapperList(RowMapper<Map<String, Object>> rowMapper, String mapperName) throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        List<Map<String, Object>> users = template.queryList(
                "SELECT id, username FROM users ORDER BY id", rowMapper);

        assertEquals(5, users.size());
        // 第一行
        assertEquals("alice", users.get(0).get("username"));
        assertEquals(1L, users.get(0).get("id"));
    }

    // ==================== 具体实现差异 ====================

    @Test
    @DisplayName("LINKED_HASH_MAP_MAPPER：保持列的查询顺序")
    void testLinkedHashMapMapperPreservesOrder() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Optional<Map<String, Object>> user = template.queryFirst(
                "SELECT email, age, id, username FROM users WHERE username = ?",
                new Object[]{"bob"},
                RowMapper.LINKED_HASH_MAP_MAPPER);

        assertTrue(user.isPresent());
        Map<String, Object> map = user.get();
        assertLinkedHashMapOrder(map, "email", "age", "id", "username");
    }

    @Test
    @DisplayName("HASH_MAP_MAPPER：返回 HashMap 实例")
    void testHashMapMapperInstanceType() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Optional<Map<String, Object>> user = template.queryFirst(
                "SELECT id, username FROM users WHERE username = ?",
                new Object[]{"bob"},
                RowMapper.HASH_MAP_MAPPER);

        assertTrue(user.isPresent());
        assertInstanceOf(HashMap.class, user.get());
    }

    @Test
    @DisplayName("自定义 MapRowMapper 子类：使用 TreeMap 按自然顺序排序")
    @NullMarked
    void testCustomMapRowMapperTreeMap() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        RowMapper<Map<String, Object>> treeMapMapper = new MapRowMapper<Map<String, Object>>() {
            @Override
            protected Map<String, Object> createMap() {
                return new TreeMap<>();
            }
        };

        Optional<Map<String, Object>> user = template.queryFirst(
                "SELECT email, age, id, username FROM users WHERE username = ?",
                new Object[]{"bob"},
                treeMapMapper);

        assertTrue(user.isPresent());
        Map<String, Object> map = user.get();
        assertInstanceOf(TreeMap.class, map);
        assertEquals("bob", map.get("username"));
        assertEquals("bob@example.com", map.get("email"));
        assertEquals(35, map.get("age"));
        assertEquals(2L, map.get("id"));

        // TreeMap 按列名自然顺序排序，而非查询顺序
        assertIterableEquals(Arrays.asList("age", "email", "id", "username"), map.keySet());
    }

    // ==================== 自定义 RowMapper 对比 ====================

    @Test
    @DisplayName("自定义 RowMapper 与 SimpleBeanRowMapper 结果一致")
    void testCustomVsSimpleRowMapper() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        UserRowMapper customMapper = new UserRowMapper();
        SimpleBeanRowMapper<User> simpleMapper = SimpleBeanRowMapper.of(User.class);

        Optional<User> userByCustom = template.queryFirst(
                "SELECT * FROM users WHERE username = ?",
                new Object[]{"alice"}, customMapper);

        Optional<User> userBySimple = template.queryFirst(
                "SELECT * FROM users WHERE username = ?",
                new Object[]{"alice"}, simpleMapper);

        assertTrue(userByCustom.isPresent());
        assertTrue(userBySimple.isPresent());

        assertEquals(userByCustom.get().getId(), userBySimple.get().getId());
        assertEquals(userByCustom.get().getUsername(), userBySimple.get().getUsername());
        assertEquals(userByCustom.get().getEmail(), userBySimple.get().getEmail());

        logger.info("自定义 RowMapper: {}", userByCustom.get());
        logger.info("SimpleBeanRowMapper: {}", userBySimple.get());
    }

    // ==================== 辅助类 ====================

    /**
     * 无无参构造器的 Bean，用于验证 SimpleBeanRowMapper / DefaultBeanRowMapper 的异常处理。
     */
    public static class BeanWithoutNoArgConstructor {
        private String name;

        public BeanWithoutNoArgConstructor(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * 包含连续大写缩写属性的 Bean，用于验证 camelToSnake 的缩写处理。
     *
     * <p>覆盖场景：
     * <ul>
     * <li>homeURL — 缩写在末尾（三字母 URL）</li>
     * <li>xmlParser — 缩写在前（三字母 XML）</li>
     * <li>parseURL — 缩写在末尾</li>
     * <li>userID — 两字母缩写在末尾（ID）</li>
     * <li>parseHTML — 四字母缩写在末尾（HTML）</li>
     * <li>multiHttpClient — 缩写夹在词中（HTTP）</li>
     * </ul>
     */
    public static class AcronymBean {
        private String homeURL;
        private String xmlParser;
        private String parseURL;
        private String userID;
        private String parseHTML;
        private String multiHttpClient;

        public String getHomeURL() {
            return homeURL;
        }

        public void setHomeURL(String homeURL) {
            this.homeURL = homeURL;
        }

        public String getXmlParser() {
            return xmlParser;
        }

        public void setXmlParser(String xmlParser) {
            this.xmlParser = xmlParser;
        }

        public String getParseURL() {
            return parseURL;
        }

        public void setParseURL(String parseURL) {
            this.parseURL = parseURL;
        }

        public String getUserID() {
            return userID;
        }

        public void setUserID(String userID) {
            this.userID = userID;
        }

        public String getParseHTML() {
            return parseHTML;
        }

        public void setParseHTML(String parseHTML) {
            this.parseHTML = parseHTML;
        }

        public String getMultiHttpClient() {
            return multiHttpClient;
        }

        public void setMultiHttpClient(String multiHttpClient) {
            this.multiHttpClient = multiHttpClient;
        }
    }
}
