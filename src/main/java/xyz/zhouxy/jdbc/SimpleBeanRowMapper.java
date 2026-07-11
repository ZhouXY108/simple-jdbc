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

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import xyz.zhouxy.jdbc.util.NamingTools;

/**
 * SimpleBeanRowMapper
 *
 * <p>
 * 将 {@link ResultSet} 转换为 Java Bean 的 {@link RowMapper} 的简易实现。
 * <p>
 * <i>性能和规则上的限制都比较大，仅在对性能不敏感的场景下便捷使用，
 * 一般情况下你应该自定义 {@link RowMapper}。</i>
 *
 * <p>
 * 说明：
 * <ul>
 * <li>使用 {@link MethodHandle} 调用无参构造器和 {@code setter} 方法，避免传统反射的运行时开销。
 *     要求 Bean 具有 {@code public} 无参构造器且 setter 为 {@code public} 方法。</li>
 * <li>支持自定义列名和属性名的映射，当未指定 {@code propertyColMap} 时，默认 JavaBean 的属性名为小驼峰，列名为小写蛇形命名。</li>
 * <li>使用 {@link ResultSet#getObject(int, Class)} 从 {@link ResultSet} 中获取属性值。</li>
 * <li>JavaBean 属性仅支持引用类型，不支持基本数据类型。</li>
 * </ul>
 *
 * @author ZhouXY
 * @since 1.1.0
 */
@NullMarked
public class SimpleBeanRowMapper<T extends @Nullable Object> implements RowMapper<T> {

    /** JavaBean 类型 */
    private final Class<T> beanType;

    /** Bean 的无参构造器 MethodHandle */
    private final MethodHandle constructorHandle;

    /** 列名到属性类型与 setter MethodHandle 的映射 */
    private final Map<String, ColMapping> colMappings;

    /**
     * 列映射信息：属性类型 + setter MethodHandle
     */
    private static final class ColMapping {
        final Class<?> propertyType;
        final MethodHandle setterHandle;

        ColMapping(Class<?> propertyType, MethodHandle setterHandle) {
            this.propertyType = propertyType;
            this.setterHandle = setterHandle;
        }
    }

    private SimpleBeanRowMapper(Class<T> beanType,
                                MethodHandle constructorHandle,
                                Map<String, ColMapping> colMappings) {
        this.beanType = beanType;
        this.constructorHandle = constructorHandle;
        this.colMappings = colMappings;
    }

    /**
     * 创建一个 {@code SimpleBeanRowMapper}
     *
     * @param <T>      Bean 类型
     * @param beanType Bean 类型
     * @return SimpleBeanRowMapper 对象
     * @throws IllegalStateException 创建 {@code SimpleBeanRowMapper} 出现错误的异常时抛出
     */
    public static <T extends @Nullable Object> SimpleBeanRowMapper<T> of(Class<@NonNull T> beanType) {
        return of(beanType, null);
    }

    /**
     * 创建一个 {@code SimpleBeanRowMapper}
     *
     * @param <T>            Bean 类型
     * @param beanType       Bean 类型
     * @param propertyColMap Bean 字段与列名的映射关系。key 是字段，value 是列名。
     * @return {@code SimpleBeanRowMapper} 对象
     * @throws IllegalStateException 创建 {@code SimpleBeanRowMapper} 出现错误的异常时抛出
     */
    public static <T extends @Nullable Object> SimpleBeanRowMapper<T> of(
            Class<@NonNull T> beanType,
            @Nullable Map<String, String> propertyColMap) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();

            // 无参构造器 → MethodHandle
            Constructor<@NonNull T> constructor = beanType.getDeclaredConstructor();
            MethodHandle ctorHandle = lookup.unreflectConstructor(constructor);

            final Map<String, ColMapping> colMappings = buildColMappings(
                    lookup, beanType, propertyColMap);
            return new SimpleBeanRowMapper<>(beanType, ctorHandle, colMappings);
        }
        catch (IntrospectionException e) {
            throw new IllegalStateException("There is an exception occurs during introspection.", e);
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException("Could not find a no-args constructor in " + beanType.getName(), e);
        }
        catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not access constructor in " + beanType.getName(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public T mapRow(ResultSet rs, int rowNumber) throws SQLException {
        try {
            @SuppressWarnings("unchecked")
            T newInstance = (T) this.constructorHandle.invoke();
            ResultSetMetaData metaData = rs.getMetaData();
            // 遍历结果的每一列
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String colName = metaData.getColumnLabel(i);
                ColMapping mapping = this.colMappings.get(colName);
                if (mapping != null) {
                    mapping.setterHandle.invoke(newInstance, rs.getObject(i, mapping.propertyType));
                }
            }
            return newInstance;
        }
        catch (SQLException e) {
            throw e;
        }
        catch (Throwable e) {
            throw new IllegalStateException("Could not map row to " + beanType.getName(), e);
        }
    }

    /**
     * 构建 column name 到 ColMapping 的映射。
     */
    private static <T extends @Nullable Object> Map<String, ColMapping> buildColMappings(
            MethodHandles.Lookup lookup,
            Class<@NonNull T> beanType,
            @Nullable Map<String, String> propertyColMap) throws IntrospectionException, IllegalAccessException {

        BeanInfo beanInfo = Introspector.getBeanInfo(beanType);
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();

        // Bean 的属性名为小驼峰，对应的列名为下划线
        Function<? super PropertyDescriptor, String> keyMapper;
        if (propertyColMap == null || propertyColMap.isEmpty()) {
            keyMapper = p -> NamingTools.camelToSnake(p.getName());
        }
        else {
            keyMapper = p -> {
                String propertyName = p.getName();
                String colName = propertyColMap.get(propertyName);
                return colName != null ? colName
                    : NamingTools.camelToSnake(propertyName);
            };
        }

        final Map<String, ColMapping> result = new HashMap<>();
        for (PropertyDescriptor pd : propertyDescriptors) {
            Method setter = pd.getWriteMethod();
            if (setter != null) {
                String colName = keyMapper.apply(pd);
                ColMapping mapping = new ColMapping(
                        pd.getPropertyType(),
                        lookup.unreflect(setter));
                result.put(colName, mapping);
            }
        }
        return result;
    }
}
