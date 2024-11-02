/*
 * Copyright 2022-2024 the original author or authors.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import javax.annotation.Nonnull;

import com.google.common.annotations.Beta;

import xyz.zhouxy.plusone.commons.collection.AbstractMapWrapper;
import xyz.zhouxy.plusone.commons.util.AssertTools;
import xyz.zhouxy.plusone.commons.util.OptionalTools;
import xyz.zhouxy.plusone.commons.util.StringTools;

/**
 * DbRecord
 *
 * <p>
 * 封装 Map<String, Object>，表示一条 DB 记录
 * </p>
 *
 * @author <a href="http://zhouxy.xyz:3000/ZhouXY108">ZhouXY</a>
 * @since 1.0.0
 */
@Beta
public class DbRecord extends AbstractMapWrapper<String, Object, DbRecord> {

    public DbRecord() {
        super(new HashMap<>(),
                k -> AssertTools.checkArgument(StringTools.isNotBlank(k), "Key must has text."),
                null);
    }

    public DbRecord(Map<String, Object> map) {
        super(map,
                k -> AssertTools.checkArgument(StringTools.isNotBlank(k), "Key must has text."),
                null);
    }

    /**
     * 将值强转为 {@link String}，并放在 {@link Optional} 中。
     * 如果 {@code key} 存在，而值不存在，则返回 {@link Optional#empty()}。
     */
    public Optional<String> getValueAsString(String key) {
        return this.getAndConvert(key);
    }

    /**
     * 将值强转为 {@code int}，并放在 {@link OptionalInt} 中。
     * 如果 {@code key} 存在，而值不存在，则返回 {@link OptionalInt#empty()}。
     */
    @Nonnull
    public OptionalInt getValueAsInt(String key) {
        return OptionalTools.toOptionalInt(this.getAndConvert(key));
    }

    /**
     * 将值强转为 {@code long}，并放在 {@link OptionalLong} 中。
     * 如果 {@code key} 存在，而值不存在，则返回 {@link OptionalLong#empty()}。
     */
    @Nonnull
    public OptionalLong getValueAsLong(String key) {
        return OptionalTools.toOptionalLong(this.getAndConvert(key));
    }

    /**
     * 将值强转为 {@code double}，并放在 {@link OptionalDouble} 中。
     * 如果 {@code key} 存在，而值不存在，则返回 {@link OptionalDouble#empty()}。
     */
    @Nonnull
    public OptionalDouble getValueAsDouble(String key) {
        return OptionalTools.toOptionalDouble(this.getAndConvert(key));
    }

    @Override
    protected DbRecord getSelf() {
        return this;
    }

    private static final String STR_PREFIX = DbRecord.class.getName() + '@';

    @Override
    public String toString() {
        return STR_PREFIX + super.toString();
    }
}
