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
package xyz.zhouxy.jdbc;

/**
 * 批量更新状态
 *
 * <p>
 * 用于表示批量更新操作的整体执行状态
 *
 * @author ZhouXY
 * @see BatchUpdateResult
 * @see BatchUpdateResult#getStatus()
 */
public enum BatchUpdateStatus {

    /**
     * 成功
     */
    SUCCESS(0, "成功"),

    /**
     * 执行完成，部分批次失败
     *
     * <p>
     * 通常出现在 batchUpdate 的静默模式下：遇到执行失败的批次时不中断，继续执行后续批次，最终状态为此值。
     *
     * @see BatchUpdateResult#getErrorBatchIndexes()
     */
    COMPLETED_WITH_ERRORS(-1, "执行完成，部分批次失败"),

    /**
     * 中断
     *
     * <p>
     * 通常出现在 batchUpdate 的非静默模式下：遇到执行失败的批次时立即中断，不再执行后续批次。
     */
    INTERRUPTED(-2, "中断"),
    ;

    private final int code;
    private final String description;

    BatchUpdateStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取状态码
     *
     * @return 状态码
     */
    public int getCode() {
        return code;
    }

    /**
     * 获取状态的可读描述
     *
     * @return 描述信息
     */
    public String getDescription() {
        return description;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "BatchUpdateStatus ["
                + "name=" + name()
                + ", code=" + code
                + ", description=" + description
                + "]";
    }
}
