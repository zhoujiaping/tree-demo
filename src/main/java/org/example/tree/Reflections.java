package org.example.tree;

import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class Reflections {
    private static ConcurrentHashMap<Class, Map<String,Field>> fieldCache = new ConcurrentHashMap<>();
    public static Field findField(Class<?> clazz,String fieldName){
        Map<String,Field> fieldMap = fieldCache.get(clazz);
        if(fieldMap == null){
            synchronized (Reflections.class){
                if(fieldMap == null){
                    fieldMap = new HashMap<>();
                    fieldCache.put(clazz,fieldMap);
                    for(;!clazz.equals(Object.class);clazz = clazz.getSuperclass()){
                        Field[] fields = clazz.getDeclaredFields();
                        for(Field field: fields){
                            fieldMap.putIfAbsent(field.getName(),field);
                        }
                    }
                }
            }
        }
        return fieldMap.get(fieldName);
    }
    @SneakyThrows
    public static Object getFieldValue(Object obj,String fieldName){
        Field field = findField(obj.getClass(),fieldName);
        if(!field.canAccess(obj)){
            field.setAccessible(true);
        }
        return field.get(obj);
    }
    @SneakyThrows
    public static void setFieldValue(Object obj,String fieldName,Object value){
        Field field = findField(obj.getClass(),fieldName);
        if(!field.canAccess(obj)){
            field.setAccessible(true);
        }
        field.set(obj,value);
    }
}
