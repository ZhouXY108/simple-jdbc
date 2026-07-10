package xyz.zhouxy.jdbc.test;

import static org.junit.jupiter.api.Assertions.*;
import static xyz.zhouxy.jdbc.ParamBuilder.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.zhouxy.jdbc.ParamBuilder;

/**
 * ParamBuilder 单元测试。
 *
 * <p>验证 {@code buildParams} 对各种 Optional 类型的拆箱处理，
 * 以及 {@code buildBatchParams} 对集合的批量映射逻辑。</p>
 *
 * @see xyz.zhouxy.jdbc.ParamBuilder
 */
@DisplayName("ParamBuilder 参数构建测试")
class ParamBuilderTest {

    // ====================================================================
    // #region - buildParams：空参数
    // --------------------------------------------------------------------

    @Test
    @DisplayName("buildParams：无参 / null / 空数组均返回 EMPTY_OBJECT_ARRAY")
    void testBuildParamsEmpty() {
        assertSame(EMPTY_OBJECT_ARRAY, buildParams());
        assertSame(EMPTY_OBJECT_ARRAY, buildParams((Object[]) null));
        assertSame(EMPTY_OBJECT_ARRAY, buildParams(new Object[0]));
    }

    // --------------------------------------------------------------------
    // #endregion
    // ====================================================================

    // ====================================================================
    // #region - buildParams：普通参数
    // --------------------------------------------------------------------

    @Test
    @DisplayName("buildParams：普通参数原样返回，null 元素保留（README 示例）")
    void testBuildParamsPlain() {
        Object[] result = buildParams("admin%", "0000", null, 100, 200L, 3.14, true);

        assertEquals(7, result.length);
        assertEquals("admin%", result[0]);
        assertEquals("0000", result[1]);
        assertNull(result[2]);
        assertEquals(100, result[3]);
        assertEquals(200L, result[4]);
        assertEquals(3.14, result[5]);
        assertEquals(true, result[6]);
    }

    // --------------------------------------------------------------------
    // #endregion
    // ====================================================================

    // ====================================================================
    // #region - buildParams：Optional<?>
    // --------------------------------------------------------------------

    @Test
    @DisplayName("buildParams：Optional.of 拆箱为值，Optional.empty 拆箱为 null（README 示例）")
    void testBuildParamsOptional() {
        // README: Optional.of("hello") → "hello", Optional.empty() → null
        Object[] result = buildParams(Optional.of("hello"), Optional.empty(), Optional.of(42));

        assertEquals(3, result.length);
        assertEquals("hello", result[0]);
        assertNull(result[1]);
        assertEquals(42, result[2]);
    }

    // --------------------------------------------------------------------
    // #endregion
    // ====================================================================

    // ====================================================================
    // #region - buildParams：OptionalInt / OptionalLong / OptionalDouble
    // --------------------------------------------------------------------

    @Test
    @DisplayName("buildParams：OptionalInt.of → Integer，empty → null")
    void testBuildParamsOptionalInt() {
        Object[] result = buildParams(OptionalInt.of(42), OptionalInt.empty());

        assertEquals(2, result.length);
        assertEquals(42, result[0]);
        assertInstanceOf(Integer.class, result[0]);
        assertNull(result[1]);
    }

    @Test
    @DisplayName("buildParams：OptionalLong.of → Long，empty → null")
    void testBuildParamsOptionalLong() {
        Object[] result = buildParams(OptionalLong.of(100L), OptionalLong.empty());

        assertEquals(2, result.length);
        assertEquals(100L, result[0]);
        assertInstanceOf(Long.class, result[0]);
        assertNull(result[1]);
    }

    @Test
    @DisplayName("buildParams：OptionalDouble.of → Double，empty → null")
    void testBuildParamsOptionalDouble() {
        Object[] result = buildParams(OptionalDouble.of(3.14), OptionalDouble.empty());

        assertEquals(2, result.length);
        assertEquals(3.14, result[0]);
        assertInstanceOf(Double.class, result[0]);
        assertNull(result[1]);
    }

    // --------------------------------------------------------------------
    // #endregion
    // ====================================================================

    // ====================================================================
    // #region - buildParams：混合所有类型
    // --------------------------------------------------------------------

    @Test
    @DisplayName("buildParams：混合所有 Optional + 普通类型 + null")
    void testBuildParamsMixedAll() {
        Object[] result = buildParams(
                Optional.of("present"), Optional.empty(),
                OptionalInt.of(10), OptionalInt.empty(),
                OptionalLong.of(200L), OptionalLong.empty(),
                OptionalDouble.of(1.5), OptionalDouble.empty(),
                "plain", 999, null);

        assertEquals(11, result.length);
        assertEquals("present", result[0]);
        assertNull(result[1]);
        assertEquals(10, result[2]);
        assertNull(result[3]);
        assertEquals(200L, result[4]);
        assertNull(result[5]);
        assertEquals(1.5, result[6]);
        assertNull(result[7]);
        assertEquals("plain", result[8]);
        assertEquals(999, result[9]);
        assertNull(result[10]);
    }

    // --------------------------------------------------------------------
    // #endregion
    // ====================================================================

    // ====================================================================
    // #region - buildParams：Temporal 时间类型
    // --------------------------------------------------------------------

    @Test
    @DisplayName("buildParams：Temporal 类型（LocalDate / LocalTime / LocalDateTime）")
    void testBuildParamsTemporal() {
        LocalDate date = LocalDate.of(2024, 6, 15);
        LocalTime time = LocalTime.of(14, 30, 0);
        LocalDateTime dateTime = LocalDateTime.of(date, time);

        Object[] result = buildParams(date, time, dateTime);

        assertEquals(3, result.length);
        assertSame(date, result[0]);
        assertSame(time, result[1]);
        assertSame(dateTime, result[2]);
    }

    // --------------------------------------------------------------------
    // #endregion
    // ====================================================================

    // ====================================================================
    // #region - buildBatchParams
    // --------------------------------------------------------------------

    @Test
    @DisplayName("buildBatchParams：集合映射为 List<Object[]>，配合 buildParams 使用（README 示例风格）")
    void testBuildBatchParams() {
        List<String[]> data = Arrays.asList(
                new String[]{"admin", "123456", "0000"},
                new String[]{"user1", "pass1", "0001"},
                new String[]{"user2", "pass2", "0002"});

        // README 风格: buildBatchParams(collection, item -> buildParams(...))
        List<Object[]> result = buildBatchParams(data,
                row -> buildParams(row[0], row[1], row[2]));

        assertEquals(3, result.size());
        assertArrayEquals(new Object[]{"admin", "123456", "0000"}, result.get(0));
        assertArrayEquals(new Object[]{"user1", "pass1", "0001"}, result.get(1));
        assertArrayEquals(new Object[]{"user2", "pass2", "0002"}, result.get(2));
    }

    @Test
    @DisplayName("buildBatchParams：边界情况——空集合 / null collection / null func")
    @SuppressWarnings("null")
    void testBuildBatchParamsBoundary() {
        // 空集合返回 Collections.emptyList()
        List<Object[]> emptyResult = buildBatchParams(Collections.emptyList(),
                obj -> new Object[]{obj});
        assertTrue(emptyResult.isEmpty());
        assertSame(Collections.emptyList(), emptyResult);

        // null collection 抛异常
        assertThrows(Exception.class, () ->
                buildBatchParams(null, obj -> new Object[]{obj}));

        // null func 抛异常
        assertThrows(Exception.class, () ->
                buildBatchParams(Arrays.asList("a", "b"), null));
    }

    // --------------------------------------------------------------------
    // #endregion
    // ====================================================================

    // ====================================================================
    // #region - 私有构造器
    // --------------------------------------------------------------------

    @Test
    @DisplayName("私有构造器抛 IllegalStateException")
    void testPrivateConstructor() throws Exception {
        java.lang.reflect.Constructor<ParamBuilder> ctor =
                ParamBuilder.class.getDeclaredConstructor();
        ctor.setAccessible(true);

        java.lang.reflect.InvocationTargetException ex = assertThrows(
                java.lang.reflect.InvocationTargetException.class, ctor::newInstance);
        assertInstanceOf(IllegalStateException.class, ex.getCause());
        assertEquals("Utility class", ex.getCause().getMessage());
    }

    // --------------------------------------------------------------------
    // #endregion
    // ====================================================================
}
