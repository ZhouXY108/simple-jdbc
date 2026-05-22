/*
 * Copyright 2026-present the original author or authors.
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
package xyz.zhouxy.jdbc;

import xyz.zhouxy.plusone.commons.base.IWithIntCode;

/**
 * 批量更新状态
 *
 * @author ZhouXY108 <luquanlion@outlook.com>
 */
public enum BatchUpdateStatus implements IWithIntCode {

    /**
     * 成功
     */
    SUCCESS(0, "成功"),

    /**
     * 部分成功
     */
    COMPLETED_WITH_ERRORS(-1, "部分成功"),

    /**
     * 中断
     */
    INTERRUPTED(-2, "中断"),
    ;

    private final int code;
    private final String description;

    BatchUpdateStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    @Override
    public int getCode() {
        return code;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "BatchUpdateStatus ["
                + "name=" + name()
                + ", code=" + code
                + ", description=" + description
                + "]";
    }
}
