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
     * 本次分批更新的状态
     */
    private BatchUpdateStatus status = BatchUpdateStatus.SUCCESS;

    /**
     * 所有批次的更新结果
     */
    private Map<Integer, int[]> allUpdateCounts;
    /**
     * 所有出错的批次的错误信息
     */
    private Map<Integer, BatchUpdateErrorInfo> allErrorsInfo;

    /**
     * 成功批次数量
     */
    private int successBatchCount;

    /**
     * 完成批次数量
     */
    private int completeBatchCount;

    BatchUpdateResult(int total, int batchCount, int batchSize) {
        this.total = total;
        this.batchCount = batchCount;
        this.batchSize = batchSize;

        this.allUpdateCounts = new HashMap<>(batchCount);
        this.allErrorsInfo = new HashMap<>(batchCount);
    }

    /**
     * 记录成功批次
     */
    void recordSuccessBatch(int batchIndex, int[] updateCounts) {
        this.completeBatchCount++;
        this.allUpdateCounts.put(batchIndex, updateCounts);
        this.successBatchCount++;
    }

    /**
     * 记录失败批次
     */
    void recordErrorBatch(int batchIndex, int[] updateCounts, Throwable cause) {
        this.completeBatchCount++;
        this.allUpdateCounts.put(batchIndex, updateCounts);
        this.allErrorsInfo.put(batchIndex, new BatchUpdateErrorInfo(batchIndex, cause));
        if (this.status == BatchUpdateStatus.SUCCESS) {
            this.status = BatchUpdateStatus.COMPLETED_WITH_ERRORS;
        }
    }

    /**
     * 中断
     */
    void interrupt() {
        this.status = BatchUpdateStatus.INTERRUPTED;
    }

    /**
     * 获取指定批次更新结果
     *
     * @param batchIndex 批次号
     * @return 批次更新结果
     */
    public int[] getUpdateCounts(int batchIndex) {
        int[] updateCounts = this.allUpdateCounts.get(batchIndex);
        return updateCounts != null ? updateCounts.clone() : null;
    }

    /**
     * 获取所有出错的批次号
     *
     * @return 错误批次号
     */
    public int[] getErrorBatchIndexes() {
        return this.allErrorsInfo.keySet().stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * 获取指定批次的错误信息
     *
     * @param batchIndex 批次号
     * @return 批次错误信息
     */
    public BatchUpdateErrorInfo getBatchUpdateErrorInfo(int batchIndex) {
        return this.allErrorsInfo.get(batchIndex);
    }

    /**
     * 获取所有出错的批次的错误信息
     *
     * @return 批次错误信息
     */
    public Map<Integer, BatchUpdateErrorInfo> getAllErrorsInfo() {
        return Collections.unmodifiableMap(allErrorsInfo);
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
        return allErrorsInfo.size();
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
        return batchCount - successBatchCount - getErrorBatchCount();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return "BatchUpdateResult ["
                + "total=" + total
                + ", batchCount=" + batchCount
                + ", batchSize=" + batchSize
                + ", status=" + status
                + ", completeBatchCount=" + completeBatchCount
                + ", successBatchCount=" + successBatchCount
                + ", errorBatchCount=" + getErrorBatchCount()
                + ", remainingBatchCount=" + getRemainingBatchCount()
                + "]";
    }
}
