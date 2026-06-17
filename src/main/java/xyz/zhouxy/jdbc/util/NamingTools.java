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

package xyz.zhouxy.jdbc.util;

/**
 * 字符串工具
 *
 * @author ZhouXY
 * @since 1.0.0
 */
public class NamingTools {

    /**
     * 将小驼峰命名转换为小写下划线命名（snake_case）。
     *
     * <p>转换规则：
     * <ul>
     * <li>小写→大写边界插入下划线：{@code userName → user_name}</li>
     * <li>连续大写缩写视为整体，在其末尾小写边界插入下划线：{@code XMLParser → xml_parser}</li>
     * <li>纯小写保持不变：{@code username → username}</li>
     * <li>{@code null} 或空字符串返回原值</li>
     * </ul>
     *
     * @param camelCase 小驼峰命名字符串，可空
     * @return snake_case 命名字符串；{@code null} 输入返回 {@code null}
     */
    public static String camelToSnake(String camelCase) {
        if (camelCase == null || camelCase.isEmpty()) {
            return camelCase;
        }

        StringBuilder sb = new StringBuilder(camelCase.length() * 2);
        int len = camelCase.length();

        for (int i = 0; i < len; i++) {
            char c = camelCase.charAt(i);
            if (isUpperCaseAscii(c)) {
                if (shouldInsertUnderscore(camelCase, i)) {
                    sb.append('_');
                }
                sb.append((char) (c + 32)); // 转小写
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean shouldInsertUnderscore(String str, int index) {
        if (index == 0) {
            return false;
        }

        char prev = str.charAt(index - 1);
        char next = (index + 1 < str.length()) ? str.charAt(index + 1) : 0;

        boolean prevIsBoundary = !isUpperCaseAscii(prev);
        boolean nextIsLower = isLowerCaseAscii(next);

        return prevIsBoundary || nextIsLower;
    }

    private static boolean isUpperCaseAscii(char c) {
        return c >= 'A' && c <= 'Z';
    }

    private static boolean isLowerCaseAscii(char c) {
        return c >= 'a' && c <= 'z';
    }

    // ================================
    // #region - constructor
    // ================================

    private NamingTools() {
        throw new IllegalStateException("Utility class");
    }

    // ================================
    // #endregion
    // ================================
}
