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
 * 事务异常
 *
 * <p>
 * 用于包装事务执行过程中发生的原始异常
 *
 * @author ZhouXY
 */
public class TransactionException extends Exception {
    private static final long serialVersionUID = 87276230526383501L;

    /**
     * 事务异常
     *
     * @param cause 原始异常
     */
    public TransactionException(Throwable cause) {
        super("Transaction failed during execution", cause);
    }

    /**
     * 事务异常
     *
     * @param message 异常信息
     * @param cause 原始异常
     */
    public TransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}
