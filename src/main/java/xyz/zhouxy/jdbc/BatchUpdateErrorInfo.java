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
 * 记录批量更新操作中某个批次的执行错误信息。
 *
 * <p>当批量更新过程中某个批次执行失败时，该类用于封装出错批次的索引、
 * 异常原因及其错误类型，便于调用方进行针对性的错误处理。
 *
 * @author ZhouXY
 * @see BatchUpdateResult
 */
public class BatchUpdateErrorInfo {

    /**
     * 批次索引
     */
    private final int batchIndex;
    /**
     * 错误原因
     */
    private final Throwable cause;
    /**
     * 错误类型
     */
    private final Class<? extends Throwable> errorType;

    /**
     * 构造一个批量更新错误信息实例。
     *
     * @param batchIndex 出错的批次索引
     * @param cause      导致该批次执行失败的异常
     */
    public BatchUpdateErrorInfo(int batchIndex, Throwable cause) {
        this.batchIndex = batchIndex;
        this.cause = cause;
        this.errorType = cause.getClass();
    }

    /**
     * 获取批次索引
     *
     * @return 批次索引
     */
    public int getBatchIndex() {
        return batchIndex;
    }

    /**
     * 获取错误原因
     *
     * @return 错误原因
     */
    public Throwable getCause() {
        return cause;
    }

    /**
     * 获取错误类型
     *
     * @return 错误类型
     */
    public Class<? extends Throwable> getErrorType() {
        return errorType;
    }

    /**
     * 返回该错误信息的字符串表示，包含批次索引、错误类型和错误消息。
     *
     * @return 格式为 {@code "BatchUpdateErrorInfo{batchIndex=..., errorType=..., message=...}"} 的字符串
     */
    @Override
    public String toString() {
        return "BatchUpdateErrorInfo{"
                + "batchIndex=" + batchIndex
                + ", errorType=" + errorType.getName()
                + ", message=" + cause.getMessage()
                + "}";
    }
}
