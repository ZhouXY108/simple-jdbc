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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
 * <li>使用反射获取类型信息，也是使用反射调用无参构造器和 {@code setter} 方法。</li>
 * <li>支持自定义列名和属性名的映射，当未指定 {@code propertyColMap} 时，默认 JavaBean 的属性名为小驼峰，列名为小写蛇形命名。</li>
 * <li>使用 {@link ResultSet#getObject(String, Class)} 从 {@link ResultSet} 中获取属性值。</li>
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

    /** Bean 的无参构造器 */
    private final Constructor<T> constructor;

    /** 列名到属性的映射 */
    private final Map<String, PropertyDescriptor> colPropertyMap;

    /** 列名与 setter 的映射 */
    private final Map<String, Method> colSetterMap;

    private SimpleBeanRowMapper(Class<@NonNull T> beanType,
                                 Constructor<@NonNull T> constructor,
                                 Map<String, PropertyDescriptor> colPropertyMap,
                                 Map<String, Method> colSetterMap) {
        this.beanType = beanType;
        this.constructor = constructor;
        this.colPropertyMap = colPropertyMap;
        this.colSetterMap = colSetterMap;
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
            // 获取无参构造器
            Constructor<@NonNull T> constructor = beanType.getDeclaredConstructor();
            constructor.setAccessible(true); // NOSONAR

            final Map<String, PropertyDescriptor> colPropertyMap = buildColPropertyMap(beanType, propertyColMap);
            final Map<String, Method> colSetterMap = buildColSetterMap(colPropertyMap);
            return new SimpleBeanRowMapper<>(beanType, constructor, colPropertyMap, colSetterMap);
        }
        catch (IntrospectionException e) {
            throw new IllegalStateException("There is an exception occurs during introspection.", e);
        }
        catch (NoSuchMethodException e) {
            throw new IllegalStateException("Could not find a no-args constructor in " + beanType.getName(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public T mapRow(ResultSet rs, int rowNumber) throws SQLException {
        try {
            // 调用无参构造器创建实例
            T newInstance = this.constructor.newInstance();
            ResultSetMetaData metaData = rs.getMetaData();
            // 遍历结果的每一列
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String colName = metaData.getColumnLabel(i);
                // 获取查询结果列名对应的属性，调用 setter
                PropertyDescriptor propertyDescriptor = this.colPropertyMap.get(colName);
                Method setter = this.colSetterMap.get(colName);
                if (propertyDescriptor != null && setter != null) {
                    Class<?> propertyType = propertyDescriptor.getPropertyType();
                    setter.invoke(newInstance, rs.getObject(colName, propertyType));
                }
            }
            return newInstance;
        }
        catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new IllegalStateException("Could not map row to " + beanType.getName(), e);
        }
    }

    /**
     * 构建 column name 和 PropertyDescriptor 的 映射
     *
     * @param <T> Java bean 类型
     * @param beanType       Java bean 类型
     * @param propertyColMap 属性与列名的映射
     * @return column name 和 PropertyDescriptor 的映射
     * @throws IntrospectionException if an exception occurs during introspection.
     */
    private static <T extends @Nullable Object> Map<String, PropertyDescriptor> buildColPropertyMap(
            Class<@NonNull T> beanType, @Nullable Map<String, String> propertyColMap) throws IntrospectionException {

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
        return Arrays.stream(propertyDescriptors)
                .collect(Collectors.toMap(keyMapper, Function.identity(), (a, b) -> b));
    }

    private static Map<String, Method> buildColSetterMap(Map<String, PropertyDescriptor> colPropertyMap) {
        final Map<String, Method> colSetterMap = new HashMap<>(colPropertyMap.size());
        colPropertyMap.forEach((col, propertyDescriptor) -> {
            Method setter = propertyDescriptor.getWriteMethod();
            if (setter != null) {
                setter.setAccessible(true); // NOSONAR
                colSetterMap.put(col, setter);
            }
        });
        return colSetterMap;
    }
}
