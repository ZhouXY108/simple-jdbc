/*
 * Copyright 2026 the original author or authors.
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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.zhouxy.jdbc.namedparam.NamedParamSql;
import xyz.zhouxy.jdbc.namedparam.PreparedSql;
import xyz.zhouxy.jdbc.namedparam.PreparedSql.Builder;

@DisplayName("PreparedSql 预构建")
class PreparedSqlTests {

    @Test
    @DisplayName("PreparedSql.sql(String) 链式构建")
    void testFromString() {
        PreparedSql ps = PreparedSql
                .sql("SELECT * FROM users WHERE id = #{id}")
                .param("id", 1)
                .build();

        assertEquals("SELECT * FROM users WHERE id = ?", ps.getSql());
        assertArrayEquals(new Object[] { 1 }, ps.getArgs());
    }

    @Test
    @DisplayName("PreparedSql.sql(NamedParamSql) 复用模板")
    void testFromTemplate() {
        NamedParamSql tmpl = NamedParamSql.of(
                "SELECT * FROM users WHERE name = #{name} AND age = #{age}");

        PreparedSql ps = PreparedSql
                .sql(tmpl)
                .param("name", "Alice")
                .param("age", 25)
                .build();

        assertEquals("SELECT * FROM users WHERE name = ? AND age = ?", ps.getSql());
        assertArrayEquals(new Object[] { "Alice", 25 }, ps.getArgs());
    }

    @Test
    @DisplayName(".prepare() 从 NamedParamSql 启动链式构建")
    void testPrepare() {
        PreparedSql ps = NamedParamSql.of("INSERT INTO t VALUES(#{x}, #{y})")
                .prepare()
                .param("x", 100)
                .param("y", 200)
                .build();

        assertEquals("INSERT INTO t VALUES(?, ?)", ps.getSql());
        assertArrayEquals(new Object[] { 100, 200 }, ps.getArgs());
    }

    @Test
    @DisplayName("param() 同名后设覆盖前设")
    void testParamOverride() {
        PreparedSql ps = PreparedSql
                .sql("SELECT #{a}")
                .param("a", 1)
                .param("a", 2)
                .build();

        assertArrayEquals(new Object[] { 2 }, ps.getArgs());
    }

    @Test
    @DisplayName("多余参数静默忽略（宽松策略）")
    void testExtraParamIgnored() {
        PreparedSql ps = PreparedSql
                .sql("SELECT #{a}")
                .param("a", 1)
                .param("extra", "ignored")
                .build();

        assertEquals("SELECT ?", ps.getSql());
        assertArrayEquals(new Object[] { 1 }, ps.getArgs());
    }

    @Test
    @DisplayName("缺少参数抛 IllegalArgumentException")
    void testMissingParam() {
        Builder param = PreparedSql
                .sql("SELECT #{missing}")
                .param("other", 1);
        assertThrows(IllegalArgumentException.class,
                param::build);
    }

    @Test
    @DisplayName("param(null, val) 抛异常")
    @SuppressWarnings({"null", "DataFlowIssue"})
    void testParamNameNull() {
        assertThrows(Exception.class,
                () -> PreparedSql.sql("SELECT 1").param(null, "x"));
    }

    @Test
    @DisplayName("getArgs() 返回防御性拷贝")
    void testGetArgsDefensiveCopy() {
        PreparedSql ps = PreparedSql
                .sql("SELECT #{a}")
                .param("a", 1)
                .build();

        Object[] a1 = ps.getArgs();
        Object[] a2 = ps.getArgs();
        assertNotSame(a1, a2);
        assertArrayEquals(a1, a2);
    }
}
