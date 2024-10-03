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
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

public class DefaultBeanResultMap<T> implements ResultMap<T> {

    private final Constructor<T> constructor;
    private final Map<String, PropertyDescriptor> colPropertyMap;

    private DefaultBeanResultMap(Constructor<T> constructor, Map<String, PropertyDescriptor> colPropertyMap) {
        this.constructor = constructor;
        this.colPropertyMap = colPropertyMap;
    }

    public static <T> DefaultBeanResultMap<T> of(Class<T> beanType) throws SQLException {
        return of(beanType, null);
    }

    public static <T> DefaultBeanResultMap<T> of(Class<T> beanType, @Nullable Map<String, String> propertyColMap)
            throws SQLException {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(beanType);
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            Map<String, PropertyDescriptor> colPropertyMap;
            if (propertyColMap != null && !propertyColMap.isEmpty()) {
                colPropertyMap = Arrays.stream(propertyDescriptors)
                        .collect(Collectors.toMap(p -> {
                            String propertyName = p.getName();
                            String colName = propertyColMap.get(propertyName);
                            return colName != null ? colName : propertyName;
                        }, Function.identity(), (a, b) -> b));
            }
            else {
                colPropertyMap = Arrays.stream(propertyDescriptors)
                        .collect(Collectors.toMap(PropertyDescriptor::getName, Function.identity(), (a, b) -> b));
            }

            Constructor<T> constructor = beanType.getDeclaredConstructor();
            constructor.setAccessible(true); // NOSONAR
            return new DefaultBeanResultMap<>(constructor, colPropertyMap);
        }
        catch (IntrospectionException e) {
            throw new SQLException("There is an exception occurs during introspection.", e);
        }
        catch (NoSuchMethodException e) {
            throw new SQLException("Could not find a no-args constructor in " + beanType.getName(), e);
        }
    }

    @Override
    public T map(ResultSet rs, int rowNumber) throws SQLException {
        try {
            T newInstance = this.constructor.newInstance();
            ResultSetMetaData metaData = rs.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String colName = metaData.getColumnName(i);
                PropertyDescriptor propertyDescriptor = this.colPropertyMap.get(colName);
                if (propertyDescriptor != null) {
                    Method setter = propertyDescriptor.getWriteMethod();
                    if (setter != null) {
                        Class<?> propertyType = propertyDescriptor.getPropertyType();
                        setter.setAccessible(true); // NOSONAR
                        setter.invoke(newInstance, rs.getObject(colName, propertyType));
                    }
                }
            }
            return newInstance;
        }
        catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new SQLException(e);
        }
    }
}
