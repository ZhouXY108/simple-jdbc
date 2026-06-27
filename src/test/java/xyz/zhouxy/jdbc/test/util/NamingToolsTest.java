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
import java.util.Arrays;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import xyz.zhouxy.jdbc.util.NamingTools;

/**
 * NamingTools 单元测试。
 *
 * <p>验证 {@code camelToSnake} 的小驼峰转下划线逻辑，
 * 覆盖边界条件、无需转换场景、基本驼峰、连续大写缩写及含数字等各类场景。
 */
@DisplayName("NamingTools 命名转换")
class NamingToolsTest {

    // ==================== 边界条件 ====================

    @Test
    @DisplayName("camelToSnake：null 输入返回 null")
    void testCamelToSnakeNull() {
        assertNull(NamingTools.camelToSnake(null));
    }

    @Test
    @DisplayName("camelToSnake：空字符串返回空字符串")
    void testCamelToSnakeEmpty() {
        assertEquals("", NamingTools.camelToSnake(""));
    }

    // ==================== 无需转换（非大写字符直通） ====================

    @Test
    @DisplayName("camelToSnake：纯小写保持不变")
    void testCamelToSnakeAllLowercase() {
        assertEquals("username", NamingTools.camelToSnake("username"));
        assertEquals("email", NamingTools.camelToSnake("email"));
    }

    @Test
    @DisplayName("camelToSnake：单字符保持不变")
    void testCamelToSnakeSingleChar() {
        assertEquals("a", NamingTools.camelToSnake("a"));
    }

    @Test
    @DisplayName("camelToSnake：已有下划线保持不变")
    void testCamelToSnakeAlreadySnakeCase() {
        assertEquals("user_name", NamingTools.camelToSnake("user_name"));
    }

    // ==================== 基本驼峰 ====================

    @Test
    @DisplayName("camelToSnake：单处大写插入下划线")
    void testCamelToSnakeSimpleCamel() {
        assertEquals("user_name", NamingTools.camelToSnake("userName"));
        assertEquals("created_at", NamingTools.camelToSnake("createdAt"));
    }

    @Test
    @DisplayName("camelToSnake：多处大写插入多处下划线")
    void testCamelToSnakeMultiCamel() {
        assertEquals("user_first_name", NamingTools.camelToSnake("userFirstName"));
    }

    @Test
    @DisplayName("camelToSnake：首字母大写 PascalCase UserName → user_name")
    void testCamelToSnakePascalCase() {
        assertEquals("user_name", NamingTools.camelToSnake("UserName"));
    }

    @Test
    @DisplayName("camelToSnake：含数字 user123Name → user123_name")
    void testCamelToSnakeWithDigits() {
        assertEquals("user123_name", NamingTools.camelToSnake("user123Name"));
    }

    // ==================== 连续大写缩写 ====================

    @Test
    @DisplayName("camelToSnake：首部缩写 XML → xml_")
    void testCamelToSnakeLeadingAcronym() {
        assertEquals("xml_parser", NamingTools.camelToSnake("XMLParser"));
    }

    @Test
    @DisplayName("camelToSnake：尾部缩写 URL → _url")
    void testCamelToSnakeTrailingAcronym() {
        assertEquals("parse_url", NamingTools.camelToSnake("parseURL"));
        assertEquals("home_url", NamingTools.camelToSnake("homeURL"));
    }

    @Test
    @DisplayName("camelToSnake：双字母尾部缩写 ID → _id")
    void testCamelToSnakeTwoLetterAcronym() {
        assertEquals("user_id", NamingTools.camelToSnake("userID"));
    }

    @Test
    @DisplayName("camelToSnake：四字母尾部缩写 HTML → _html")
    void testCamelToSnakeFourLetterAcronym() {
        assertEquals("parse_html", NamingTools.camelToSnake("parseHTML"));
    }

    @Test
    @DisplayName("camelToSnake：缩写夹在词中 HTTP → _http_")
    void testCamelToSnakeMiddleAcronym() {
        assertEquals("multi_http_client", NamingTools.camelToSnake("multiHttpClient"));
    }

    @Test
    @DisplayName("camelToSnake：全大写 URL → url")
    void testCamelToSnakeAllUpperCase() {
        assertEquals("url", NamingTools.camelToSnake("URL"));
    }

    @Test
    @DisplayName("camelToSnake：双大写接小写 ABc → a_bc")
    void testCamelToSnakeTwoUpperThenLower() {
        assertEquals("a_bc", NamingTools.camelToSnake("ABc"));
    }

    // ==================== 私有构造器 ====================

    @Test
    @DisplayName("私有构造器抛 IllegalStateException")
    void testPrivateConstructor() throws Exception {
        Constructor<?>[] constructors = NamingTools.class.getDeclaredConstructors();
        Arrays.stream(constructors)
                .forEach(constructor -> {
                    assertFalse(constructor.isAccessible());
                    constructor.setAccessible(true);
                    Throwable cause = assertThrows(Exception.class, constructor::newInstance)
                            .getCause();
                    assertInstanceOf(IllegalStateException.class, cause);
                    assertEquals("Utility class", cause.getMessage());
                });
    }
}
