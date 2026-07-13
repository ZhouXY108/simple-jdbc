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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 批量更新结果
 *
 * <p>
 * 封装 {@code batchUpdate} 操作的执行结果，包含：
 * <ul>
 *   <li>整体执行状态 {@link BatchUpdateStatus}</li>
 *   <li>批次统计信息（总数据量、批次数、成功/失败/剩余批次数）</li>
 *   <li>各批次的更新结果及各错误批次的异常信息</li>
 * </ul>
 *
 * @author ZhouXY
 *
 * @see BatchUpdateStatus
 * @see BatchUpdateErrorInfo
 * @see JdbcOperations#batchUpdate(String, java.util.Collection, int)
 * @see JdbcOperations#batchUpdate(String, java.util.Collection, int, boolean)
 */
@NullMarked
public class BatchUpdateResult {
    /**
     * 总数据量
     */
    private final int total;
    /**
     * 批次数量
     */
    private final int batchCount;
    /**
     * 批次大小
     */
    private final int batchSize;

    /**
     * 是否静默模式
     */
    private final boolean quietly;

    /**
     * 本次分批更新的状态
     */
    private final BatchUpdateStatus status;

    /**
     * 所有批次的更新结果
     */
    private final Map<Integer, int[]> allUpdateCounts;
    /**
     * 所有出错的批次的错误信息
     */
    private final Map<Integer, BatchUpdateErrorInfo> allErrorsInfo;

    /**
     * 成功批次数量
     */
    private final int successBatchCount;

    /**
     * 完成批次数量
     */
    private final int completeBatchCount;

    /**
     * 错误批次索引
     */
    private final int[] errorBatchIndexes;

    /**
     * 错误批次数量
     */
    private final int errorBatchCount;

    /**
     * 剩余批次数量
     */
    private final int remainingBatchCount;

    private BatchUpdateResult(int total,
                              int batchCount,
                              int batchSize,
                              boolean quietly,
                              BatchUpdateStatus status,
                              Map<Integer, int[]> allUpdateCounts,
                              Map<Integer, BatchUpdateErrorInfo> allErrorsInfo,
                              int successBatchCount,
                              int completeBatchCount,
                              int[] errorBatchIndexes,
                              int errorBatchCount,
                              int remainingBatchCount) {
        this.total = total;
        this.batchCount = batchCount;
        this.batchSize = batchSize;
        this.quietly = quietly;
        this.status = status;
        this.allUpdateCounts = allUpdateCounts;
        this.allErrorsInfo = allErrorsInfo;
        this.successBatchCount = successBatchCount;
        this.completeBatchCount = completeBatchCount;
        this.errorBatchIndexes = errorBatchIndexes;
        this.errorBatchCount = errorBatchCount;
        this.remainingBatchCount = remainingBatchCount;
    }


    /**
     * 获取指定批次更新结果
     *
     * @param batchIndex 批次号
     * @return 批次更新结果
     */
    public int @Nullable[] getUpdateCounts(int batchIndex) {
        if (this.allUpdateCounts.containsKey(batchIndex)) {
            int[] updateCounts = this.allUpdateCounts.get(batchIndex);
            return updateCounts.clone();
        }
        return null;
    }

    /**
     * 获取所有出错的批次号
     *
     * @return 错误批次号
     */
    public int[] getErrorBatchIndexes() {
        return this.errorBatchIndexes.clone();
    }

    /**
     * 获取指定批次的错误信息
     *
     * @param batchIndex 批次号
     * @return 批次错误信息
     */
    public @Nullable BatchUpdateErrorInfo getBatchUpdateErrorInfo(int batchIndex) {
        return this.allErrorsInfo.get(batchIndex);
    }

    /**
     * 获取所有出错的批次的错误信息
     *
     * @return 批次错误信息
     */
    public Map<Integer, BatchUpdateErrorInfo> getAllErrorsInfo() {
        return allErrorsInfo;
    }

    /**
     * 获取总数据量
     *
     * @return 总数据量
     */
    public int getTotal() {
        return total;
    }

    /**
     * 获取批次数量
     *
     * @return 批次数量
     */
    public int getBatchCount() {
        return batchCount;
    }

    /**
     * 获取批次大小
     *
     * @return 批次大小
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * 获取批量更新状态
     *
     * @return 批量更新状态
     */
    public BatchUpdateStatus getStatus() {
        return status;
    }

    /**
     * 获取完成批次数量
     *
     * @return 完成批次数量
     */
    public int getCompleteBatchCount() {
        return completeBatchCount;
    }

    /**
     * 获取成功批次数量
     *
     * @return 成功批次数量
     */
    public int getSuccessBatchCount() {
        return successBatchCount;
    }

    /**
     * 获取错误批次数量
     *
     * @return 错误批次数量
     */
    public int getErrorBatchCount() {
        return this.errorBatchCount;
    }

    /**
     * 获取剩余批次数量
     *
     * <p>
     * 一般是中断后未执行的批次数量
     *
     * @return 剩余批次数量
     */
    public int getRemainingBatchCount() {
        return remainingBatchCount;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "BatchUpdateResult ["
                + "status=" + status
                + ", total=" + total
                + ", batchSize=" + batchSize
                + ", batchCount=" + batchCount
                + ", quietly=" + quietly
                + ", completeBatchCount=" + completeBatchCount
                + ", successBatchCount=" + successBatchCount
                + ", errorBatchCount=" + errorBatchCount
                + ", remainingBatchCount=" + remainingBatchCount
                + "]";
    }

    /**
     * 创建空的 BatchUpdateResult（无参数时快速创建）
     *
     * @param batchSize 批次大小
     * @param quietly   是否静默模式
     * @return 空的 BatchUpdateResult 实例
     */
    public static BatchUpdateResult empty(int batchSize, boolean quietly) {
        return new BatchUpdateResult(
                0,
                0,
                batchSize,
                quietly,
                BatchUpdateStatus.SUCCESS,
                Collections.emptyMap(),
                Collections.emptyMap(),
                0,
                0,
                new int[0],
                0,
                0
        );
    }

    /**
     * 创建 Builder
     *
     * @param total     总数据量
     * @param batchCount 批次数量
     * @param batchSize  批次大小
     * @param quietly    是否静默模式
     * @return Builder 实例
     */
    public static Builder builder(int total, int batchCount, int batchSize, boolean quietly) {
        return new Builder(total, batchCount, batchSize, quietly);
    }

    /**
     * Builder 类，用于构建不可变的 BatchUpdateResult
     */
    public static class Builder {
        private final int total;
        private final int batchCount;
        private final int batchSize;
        private final boolean quietly;

        private BatchUpdateStatus status = BatchUpdateStatus.SUCCESS;
        private final Map<Integer, int[]> allUpdateCounts = new HashMap<>();
        private final Map<Integer, BatchUpdateErrorInfo> allErrorsInfo = new HashMap<>();
        private int successBatchCount = 0;
        private int completeBatchCount = 0;

        private Builder(int total, int batchCount, int batchSize, boolean quietly) {
            this.total = total;
            this.batchCount = batchCount;
            this.batchSize = batchSize;
            this.quietly = quietly;
        }

        /**
         * 记录成功批次
         */
        public Builder recordSuccessBatch(int batchIndex, int[] updateCounts) {
            this.completeBatchCount++;
            this.allUpdateCounts.put(batchIndex, updateCounts);
            this.successBatchCount++;
            return this;
        }

        /**
         * 记录失败批次
         */
        public Builder recordErrorBatch(int batchIndex, int[] updateCounts, Throwable cause) {
            this.completeBatchCount++;
            this.allUpdateCounts.put(batchIndex, updateCounts);
            this.allErrorsInfo.put(batchIndex, new BatchUpdateErrorInfo(batchIndex, cause));
            if (this.status == BatchUpdateStatus.SUCCESS) {
                if (this.quietly) {
                    this.status = BatchUpdateStatus.COMPLETED_WITH_ERRORS;
                }
                else {
                    this.status = BatchUpdateStatus.INTERRUPTED;
                }
            }
            return this;
        }

        /**
         * 构建不可变的 BatchUpdateResult
         *
         * @return BatchUpdateResult 实例
         */
        public BatchUpdateResult build() {
            // 预计算错误批次索引
            int[] errorBatchIndexes = this.allErrorsInfo.keySet().stream()
                    .mapToInt(Integer::intValue)
                    .toArray();

            // 预计算统计信息
            int errorBatchCount = this.allErrorsInfo.size();
            int remainingBatchCount = this.batchCount - this.successBatchCount - errorBatchCount;

            // 构建不可变 Map
            Map<Integer, int[]> unmodifiableUpdateCounts = Collections.unmodifiableMap(
                    new HashMap<>(this.allUpdateCounts));
            Map<Integer, BatchUpdateErrorInfo> unmodifiableErrorsInfo = Collections.unmodifiableMap(
                    new HashMap<>(this.allErrorsInfo));

            return new BatchUpdateResult(
                    this.total,
                    this.batchCount,
                    this.batchSize,
                    this.quietly,
                    this.status,
                    unmodifiableUpdateCounts,
                    unmodifiableErrorsInfo,
                    this.successBatchCount,
                    this.completeBatchCount,
                    errorBatchIndexes,
                    errorBatchCount,
                    remainingBatchCount
            );
        }
    }
}
