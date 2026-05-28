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
 * 批量更新错误信息
 *
 * @author ZhouXY
 */
public class BatchUpdateErrorInfo {

    private final int batchIndex;
    private final Throwable cause;
    private final Class<? extends Throwable> errorType;

    public BatchUpdateErrorInfo(int batchIndex, Throwable cause) {
        this.batchIndex = batchIndex;
        this.cause = cause;
        this.errorType = cause.getClass();
    }

    public int getBatchIndex() {
        return batchIndex;
    }

    public Throwable getCause() {
        return cause;
    }

    public Class<? extends Throwable> getErrorType() {
        return errorType;
    }
}
