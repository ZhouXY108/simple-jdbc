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

package xyz.zhouxy.jdbc.test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 测试断言辅助类。
 */
public final class JdbcTestAssertions {

    private JdbcTestAssertions() {
        // utility class
    }

    /**
     * 断言给定的 Map 是 {@link LinkedHashMap}，并且其键的迭代顺序与期望值一致。
     *
     * @param map 待断言的 Map
     * @param expectedKeys 期望的键顺序
     */
    public static void assertLinkedHashMapOrder(Map<String, Object> map, String... expectedKeys) {
        assertInstanceOf(LinkedHashMap.class, map);
        assertIterableEquals(Arrays.asList(expectedKeys), map.keySet());
    }
}
