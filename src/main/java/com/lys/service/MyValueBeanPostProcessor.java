package com.lys.service;

import com.spring.BeanPostProcessor;
import com.spring.Component;

import java.lang.reflect.Field;

@Component
public class MyValueBeanPostProcessor implements BeanPostProcessor {


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {

        //初始化前对bean的属性赋值
        for (Field field : bean.getClass().getDeclaredFields()) {
            if(field.isAnnotationPresent(MyValue.class)) {
                field.setAccessible(true);
                try {
                    field.set(bean,field.getAnnotation(MyValue.class).value());
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return bean;
    }
}
