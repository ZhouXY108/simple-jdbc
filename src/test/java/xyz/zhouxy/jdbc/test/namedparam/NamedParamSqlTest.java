/*
 * Copyright 2026-present ZhouXY
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.zhouxy.jdbc.test.namedparam;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.zhouxy.jdbc.namedparam.NamedParamSql;
import xyz.zhouxy.jdbc.namedparam.PreparedSql;

/**
 * {@link NamedParamSql} 和 {@link PreparedSql} 的单元测试。
 *
 * <p>纯逻辑测试，无需数据库环境。</p>
 */
@DisplayName("NamedParamSql / PreparedSql 命名参数 SQL")
class NamedParamSqlTest {

    // ==================== NamedParamSql：of(null) 校验 ====================

    @Test
    @DisplayName("of(null) 抛异常")
    @SuppressWarnings({"null", "DataFlowIssue"})
    void testOfNull() {
        assertThrows(Exception.class,
                () -> NamedParamSql.of(null));
    }

    // ==================== NamedParamSql：解析无命名参数 ====================

    @Test
    @DisplayName("SQL 中无命名参数，原样返回")
    void testPlainSql() {
        NamedParamSql nps = NamedParamSql.of("SELECT * FROM users ORDER BY id");

        assertEquals("SELECT * FROM users ORDER BY id", nps.getSql());
        assertTrue(nps.getParamNames().isEmpty());
    }

    // ==================== NamedParamSql：解析单个命名参数 ====================

    @Test
    @DisplayName("单个命名参数：SQL 正确转换，参数名正确记录")
    void testSingleNamedParam() {
        NamedParamSql nps = NamedParamSql.of("SELECT * FROM users WHERE id = #{id}");

        assertEquals("SELECT * FROM users WHERE id = ?", nps.getSql());
        assertEquals(Collections.singletonList("id"), nps.getParamNames());
        assertArrayEquals(new Object[] { 1 }, nps.toArgs(Collections.singletonMap("id", 1)));
    }

    // ==================== NamedParamSql：解析多个命名参数 ====================

    @Test
    @DisplayName("多个命名参数，按出现顺序记录")
    void testMultipleNamedParams() {
        NamedParamSql nps = NamedParamSql.of(
                "SELECT * FROM users WHERE name = #{name} AND age = #{age}");

        assertEquals("SELECT * FROM users WHERE name = ? AND age = ?", nps.getSql());
        assertEquals(Arrays.asList("name", "age"), nps.getParamNames());

        Map<String, Object> params = new HashMap<>();
        params.put("name", "Alice");
        params.put("age", 25);
        assertArrayEquals(new Object[] { "Alice", 25 }, nps.toArgs(params));
    }

    // ==================== NamedParamSql：参数顺序 ====================

    @Test
    @DisplayName("toArgs 参数顺序由 SQL 出现顺序决定，非 Map 顺序")
    void testParamsOrderBySql() {
        NamedParamSql nps = NamedParamSql.of(
                "SELECT * FROM users WHERE name = #{name} AND age = #{age}");

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("age", 25);
        paramMap.put("name", "Bob");

        // 顺序由 SQL 出现顺序决定
        assertArrayEquals(new Object[] { "Bob", 25 }, nps.toArgs(paramMap));
    }

    // ==================== NamedParamSql：同名参数出现多次 ====================

    @Test
    @DisplayName("SQL 中同一参数出现多次，每个位置都被记录")
    void testRepeatedParamName() {
        NamedParamSql nps = NamedParamSql.of(
                "SELECT * FROM users WHERE x = #{val} OR y = #{val}");

        assertEquals("SELECT * FROM users WHERE x = ? OR y = ?", nps.getSql());
        assertEquals(Arrays.asList("val", "val"), nps.getParamNames());

        Object[] args = nps.toArgs(Collections.singletonMap("val", 42));
        assertEquals(2, args.length);
        assertEquals(42, args[0]);
        assertEquals(42, args[1]);
    }

    // ==================== NamedParamSql：缺失参数名 ====================

    @Test
    @DisplayName("toArgs 缺少 SQL 引用的参数名，抛 IllegalArgumentException")
    void testMissingParamName() {
        NamedParamSql nps = NamedParamSql.of(
                "SELECT * FROM users WHERE id = #{missing}");
        Map<String, Integer> params = Collections.singletonMap("id", 1);
        assertThrows(IllegalArgumentException.class,
                () -> nps.toArgs(params));
    }

    // ==================== NamedParamSql：toBatchArgs ====================

    @Test
    @DisplayName("toBatchArgs 批量提取参数值")
    void testToBatchArgs() {
        NamedParamSql nps = NamedParamSql.of("INSERT INTO t VALUES(#{name}, #{age})");

        List<Map<String, ?>> batchParams = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("name", "Alice");
        row1.put("age", 25);
        batchParams.add(row1);
        Map<String, Object> row2 = new HashMap<>();
        row2.put("name", "Bob");
        row2.put("age", 30);
        batchParams.add(row2);

        List<Object[]> result = nps.toBatchArgs(batchParams);
        assertEquals(2, result.size());
        assertArrayEquals(new Object[] { "Alice", 25 }, result.get(0));
        assertArrayEquals(new Object[] { "Bob", 30 }, result.get(1));
    }

    @Test
    @DisplayName("toBatchArgs 元素为 null 抛异常")
    @SuppressWarnings("RedundantTypeArguments")
    void testToBatchArgsNullElement() {
        NamedParamSql nps = NamedParamSql.of("SELECT #{a}");
        assertThrows(Exception.class,
                () -> nps.toBatchArgs(Collections.<Map<String, ?>>singletonList(null)));
    }

    @Test
    @DisplayName("toBatchArgs 缺少参数名抛异常")
    void testToBatchArgsMissingParam() {
        NamedParamSql nps = NamedParamSql.of("SELECT #{a}, #{b}");
        List<Map<String, ?>> batchParams = Collections.singletonList(
                Collections.singletonMap("a", 1));
        assertThrows(IllegalArgumentException.class,
                () -> nps.toBatchArgs(batchParams));
    }

    // ==================== NamedParamSql：不可变性 ====================

    @Test
    @DisplayName("getParamNames() 返回不可变列表")
    @SuppressWarnings("DataFlowIssue")
    void testGetParamNamesImmutable() {
        NamedParamSql nps = NamedParamSql.of("SELECT #{a}, #{b}");
        List<String> paramNames = nps.getParamNames();
        assertThrows(UnsupportedOperationException.class,
                () -> paramNames.add("c"));
    }

    @Test
    @DisplayName("toArgs() 每次返回新数组")
    void testToArgsReturnsNewArray() {
        NamedParamSql nps = NamedParamSql.of("SELECT #{a}");
        Map<String, Object> params = Collections.singletonMap("a", 1);
        Object[] p1 = nps.toArgs(params);
        Object[] p2 = nps.toArgs(params);
        assertNotSame(p1, p2);
        assertArrayEquals(p1, p2);
    }

}
