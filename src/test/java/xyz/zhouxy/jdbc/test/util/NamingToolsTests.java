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

package xyz.zhouxy.jdbc.test.util;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.zhouxy.jdbc.util.NamingTools;

/**
 * {@link NamingTools#camelToSnake(String)} 单元测试。
 */
@DisplayName("NamingTools 命名转换")
class NamingToolsTests {

    // ==================== 基本转换 ====================

    @Test
    @DisplayName("基本小驼峰→下划线：userName → user_name")
    void testBasicCamelToSnake() {
        assertEquals("user_name", NamingTools.camelToSnake("userName"));
    }

    @Test
    @DisplayName("多词小驼峰：parseHtmlContent → parse_html_content")
    void testMultiWordCamelToSnake() {
        assertEquals("parse_html_content", NamingTools.camelToSnake("parseHtmlContent"));
    }

    @Test
    @DisplayName("首字母大写：UserName → user_name")
    void testLeadingUpperCase() {
        assertEquals("user_name", NamingTools.camelToSnake("UserName"));
    }

    // ==================== 纯小写 ====================

    @Test
    @DisplayName("纯小写不变：username → username")
    void testAllLowercase() {
        assertEquals("username", NamingTools.camelToSnake("username"));
    }

    @Test
    @DisplayName("纯小写单词：email → email")
    void testAllLowercaseSimple() {
        assertEquals("email", NamingTools.camelToSnake("email"));
    }

    @Test
    @DisplayName("单字母：a → a")
    void testSingleChar() {
        assertEquals("a", NamingTools.camelToSnake("a"));
    }

    // ==================== 连续大写缩写 ====================

    @Test
    @DisplayName("缩写开头 + 后续：XMLParser → xml_parser")
    void testAcronymAtStart() {
        assertEquals("xml_parser", NamingTools.camelToSnake("XMLParser"));
    }

    @Test
    @DisplayName("缩写结尾（URL）：homeURL → home_url")
    void testAcronymAtEnd() {
        assertEquals("home_url", NamingTools.camelToSnake("homeURL"));
    }

    @Test
    @DisplayName("纯大写缩写：URLParser → url_parser")
    void testUpperCaseStartFollowedByNormal() {
        assertEquals("url_parser", NamingTools.camelToSnake("URLParser"));
    }

    @Test
    @DisplayName("缩写在中间（HTTP）：multiHttpClient → multi_http_client")
    void testAcronymInMiddle() {
        assertEquals("multi_http_client", NamingTools.camelToSnake("multiHttpClient"));
    }

    @Test
    @DisplayName("双字母缩写（ID）：userID → user_id")
    void testTwoLetterAcronym() {
        assertEquals("user_id", NamingTools.camelToSnake("userID"));
    }

    @Test
    @DisplayName("四字母缩写（HTML）：parseHTML → parse_html")
    void testFourLetterAcronym() {
        assertEquals("parse_html", NamingTools.camelToSnake("parseHTML"));
    }

    @Test
    @DisplayName("全大写：URL → url")
    void testAllUpperCase() {
        assertEquals("url", NamingTools.camelToSnake("URL"));
    }

    @Test
    @DisplayName("单字母开头大写：ABc → a_bc")
    void testTwoUpperThenLower() {
        assertEquals("a_bc", NamingTools.camelToSnake("ABc"));
    }

    // ==================== 边界情况 ====================

    @Test
    @DisplayName("null 输入返回 null")
    void testNullInput() {
        assertNull(NamingTools.camelToSnake(null));
    }

    @Test
    @DisplayName("空字符串返回空字符串")
    void testEmptyString() {
        assertEquals("", NamingTools.camelToSnake(""));
    }

    @Test
    @DisplayName("已为 snake_case 的字符串不变")
    void testAlreadySnakeCase() {
        assertEquals("user_name", NamingTools.camelToSnake("user_name"));
    }

    @Test
    @DisplayName("含数字：user123Name → user123_name")
    void testWithDigits() {
        assertEquals("user123_name", NamingTools.camelToSnake("user123Name"));
    }

    // ==================== 私有构造器 ====================

    @Test
    @DisplayName("私有构造器抛 IllegalStateException")
    void testPrivateConstructor() throws Exception {
        Constructor<NamingTools> ctor = NamingTools.class.getDeclaredConstructor();
        ctor.setAccessible(true);

        java.lang.reflect.InvocationTargetException ex = assertThrows(
                java.lang.reflect.InvocationTargetException.class, ctor::newInstance);
        assertInstanceOf(IllegalStateException.class, ex.getCause());
        assertEquals("Utility class", ex.getCause().getMessage());
    }
}
