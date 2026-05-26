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

import javax.annotation.Nullable;

import com.google.common.base.CaseFormat;

import xyz.zhouxy.plusone.commons.annotation.StaticFactoryMethod;

/**
 * DefaultBeanRowMapper
 *
 * <p>
 * 默认实现的将 {@link ResultSet} 转换为 Java Bean 的 {@link RowMapper}。
 * </p>
 *
 * <p>
 * 说明：
 * <ul>
 * <li>使用反射获取类型信息，也是使用反射调用无参构造器和 {@code setter} 方法。</li>
 * <li>{@code propertyColMap} 未指定的列名和属性名的映射时，默认 JavaBean 的属性名为小驼峰，列名为小写蛇形命名。</li>
 * <li>从{@link ResultSet} 中获取属性值时，使用 {@link ResultSet#getObject(String, Class)} 获取。</li>
 * <li>JavaBean 属性仅支持引用类型，不支持基本数据类型。</li>
 * <li>实际使用中还是建议针对目标类型自定义 {@link RowMapper}。</li>
 * </ul>
 * </p>
 * @author ZhouXY108 <luquanlion@outlook.com>
 * @since 1.0.0
 */
public class DefaultBeanRowMapper<T> implements RowMapper<T> {

    /** JavaBean 类型 */
    private final Class<T> beanType;

    /** Bean 的无参构造器 */
    private final Constructor<T> constructor;

    /** 列名到属性的映射 */
    private final Map<String, PropertyDescriptor> colPropertyMap;

    /** 列名与 setter 的映射 */
    private final Map<String, Method> colSetterMap;

    private DefaultBeanRowMapper(Class<T> beanType,
                                 Constructor<T> constructor,
                                 Map<String, PropertyDescriptor> colPropertyMap,
                                 Map<String, Method> colSetterMap) {
        this.beanType = beanType;
        this.constructor = constructor;
        this.colPropertyMap = colPropertyMap;
        this.colSetterMap = colSetterMap;
    }

    /**
     * 创建一个 {@code DefaultBeanRowMapper}
     *
     * @param <T>      Bean 类型
     * @param beanType Bean 类型
     * @return DefaultBeanRowMapper 对象
     * @throws SQLException 创建 {@code DefaultBeanRowMapper} 出现错误的异常时抛出
     */
    @StaticFactoryMethod(DefaultBeanRowMapper.class)
    public static <T> DefaultBeanRowMapper<T> of(Class<T> beanType) throws SQLException {
        return of(beanType, null);
    }

    /**
     * 创建一个 {@code DefaultBeanRowMapper}
     *
     * @param <T>            Bean 类型
     * @param beanType       Bean 类型
     * @param propertyColMap Bean 字段与列名的映射关系。key 是字段，value 是列名。
     * @return {@code DefaultBeanRowMapper} 对象
     * @throws SQLException 创建 {@code DefaultBeanRowMapper} 出现错误的异常时抛出
     */
    @StaticFactoryMethod(DefaultBeanRowMapper.class)
    public static <T> DefaultBeanRowMapper<T> of(Class<T> beanType, @Nullable Map<String, String> propertyColMap)
            throws SQLException {
        try {
            // 获取无参构造器
            Constructor<T> constructor = beanType.getDeclaredConstructor();
            constructor.setAccessible(true); // NOSONAR

            final Map<String, PropertyDescriptor> colPropertyMap = buildColPropertyMap(beanType, propertyColMap);
            final Map<String, Method> colSetterMap = buildColSetterMap(colPropertyMap);
            return new DefaultBeanRowMapper<>(beanType, constructor, colPropertyMap, colSetterMap);
        }
        catch (IntrospectionException e) {
            throw new SQLException("There is an exception occurs during introspection.", e);
        }
        catch (NoSuchMethodException e) {
            throw new SQLException("Could not find a no-args constructor in " + beanType.getName(), e);
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
            throw new SQLException("Could not map row to " + beanType.getName(), e);
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
    private static <T> Map<String, PropertyDescriptor> buildColPropertyMap(
            Class<T> beanType, Map<String, String> propertyColMap) throws IntrospectionException {

        BeanInfo beanInfo = Introspector.getBeanInfo(beanType);
        PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();

        // Bean 的属性名为小驼峰，对应的列名为下划线
        Function<? super PropertyDescriptor, String> keyMapper;
        if (propertyColMap == null || propertyColMap.isEmpty()) {
            keyMapper = p -> CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, p.getName());
        }
        else {
            keyMapper = p -> {
                String propertyName = p.getName();
                String colName = propertyColMap.get(propertyName);
                return colName != null ? colName
                    : CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, propertyName);
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
