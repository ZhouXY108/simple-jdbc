package xyz.zhouxy.jdbc.test;

import static org.junit.jupiter.api.Assertions.*;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.zhouxy.jdbc.DefaultBeanRowMapper;
import xyz.zhouxy.jdbc.RowMapper;
import xyz.zhouxy.jdbc.SimpleJdbcTemplate;

/**
 * RowMapper 测试：DefaultBeanRowMapper、HASH_MAP_MAPPER、自定义 RowMapper。
 *
 * <p>验证 DefaultBeanRowMapper 的默认映射和自定义列映射，以及 RowMapper 接口的静态工厂方法。</p>
 */
@DisplayName("RowMapper 映射测试")
class RowMapperTest extends BaseH2Test {

    private static final Logger logger = LoggerFactory.getLogger(RowMapperTest.class);

    // ==================== DefaultBeanRowMapper 默认映射 ====================

    @Test
    @DisplayName("DefaultBeanRowMapper：默认小驼峰→小写下划线映射")
    void testDefaultBeanRowMapperDefaultMapping() throws SQLException {
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

        logger.info("DefaultBeanRowMapper 默认映射: {}", u);
    }

    @Test
    @DisplayName("DefaultBeanRowMapper：null 值字段映射为 null")
    void testDefaultBeanRowMapperNullFields() throws SQLException {
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

    // ==================== DefaultBeanRowMapper 自定义列映射 ====================

    @Test
    @DisplayName("DefaultBeanRowMapper：propertyColMap 自定义列名映射")
    void testDefaultBeanRowMapperWithPropertyColMap() throws SQLException {
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
    @DisplayName("DefaultBeanRowMapper：propertyColMap 未覆盖的属性走默认映射")
    void testDefaultBeanRowMapperPartialPropertyColMap() throws SQLException {
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

    // ==================== DefaultBeanRowMapper 边界 ====================

    @Test
    @DisplayName("DefaultBeanRowMapper：Bean 包含不匹配列时正常忽略")
    void testDefaultBeanRowMapperUnmatchedColumns() throws SQLException {
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
    @DisplayName("DefaultBeanRowMapper：无无参构造器的 Bean 抛出 SQLException")
    void testDefaultBeanRowMapperNoNoArgConstructor() {
        assertThrows(SQLException.class, () ->
                DefaultBeanRowMapper.of(BeanWithoutNoArgConstructor.class));
    }

    @Test
    @DisplayName("DefaultBeanRowMapper：RowMapper.beanRowMapper 静态工厂方法")
    void testRowMapperStaticBeanRowMapper() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();
        RowMapper<User> rowMapper = RowMapper.beanRowMapper(User.class);

        Optional<User> user = template.queryFirst(
                "SELECT * FROM users WHERE username = ?",
                new Object[]{"alice"}, rowMapper);

        assertTrue(user.isPresent());
        assertEquals("alice", user.get().getUsername());
    }

    // ==================== HASH_MAP_MAPPER ====================

    @Test
    @DisplayName("HASH_MAP_MAPPER：所有列映射为 Map")
    void testHashMapMapper() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Optional<Map<String, Object>> user = template.queryFirst(
                "SELECT id, username, email, age FROM users WHERE username = ?",
                new Object[]{"bob"});

        assertTrue(user.isPresent());
        Map<String, Object> map = user.get();
        assertEquals(2L, map.get("id"));
        assertEquals("bob", map.get("username"));
        assertEquals("bob@example.com", map.get("email"));
        assertEquals(35, map.get("age"));

        logger.info("HASH_MAP_MAPPER 映射结果: {}", map);
    }

    @Test
    @DisplayName("HASH_MAP_MAPPER：空结果返回 Optional.empty()")
    void testHashMapMapperEmpty() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        Optional<Map<String, Object>> user = template.queryFirst(
                "SELECT * FROM users WHERE id = ?",
                new Object[]{999});

        assertFalse(user.isPresent());
    }

    @Test
    @DisplayName("HASH_MAP_MAPPER：查询列表返回 List<Map>")
    void testHashMapMapperList() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        List<Map<String, Object>> users = template.queryList(
                "SELECT id, username FROM users ORDER BY id");

        assertEquals(5, users.size());
        // 第一行
        assertEquals("alice", users.get(0).get("username"));
        assertEquals(1L, users.get(0).get("id"));
    }

    // ==================== 自定义 RowMapper 对比 ====================

    @Test
    @DisplayName("自定义 RowMapper 与 DefaultBeanRowMapper 结果一致")
    void testCustomVsDefaultRowMapper() throws SQLException {
        SimpleJdbcTemplate template = createTemplate();

        UserRowMapper customMapper = new UserRowMapper();
        DefaultBeanRowMapper<User> defaultMapper = DefaultBeanRowMapper.of(User.class);

        Optional<User> userByCustom = template.queryFirst(
                "SELECT * FROM users WHERE username = ?",
                new Object[]{"alice"}, customMapper);

        Optional<User> userByDefault = template.queryFirst(
                "SELECT * FROM users WHERE username = ?",
                new Object[]{"alice"}, defaultMapper);

        assertTrue(userByCustom.isPresent());
        assertTrue(userByDefault.isPresent());

        assertEquals(userByCustom.get().getId(), userByDefault.get().getId());
        assertEquals(userByCustom.get().getUsername(), userByDefault.get().getUsername());
        assertEquals(userByCustom.get().getEmail(), userByDefault.get().getEmail());

        logger.info("自定义 RowMapper: {}", userByCustom.get());
        logger.info("DefaultBeanRowMapper: {}", userByDefault.get());
    }

    // ==================== 辅助类 ====================

    /**
     * 无无参构造器的 Bean，用于验证 DefaultBeanRowMapper 的异常处理。
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
}
