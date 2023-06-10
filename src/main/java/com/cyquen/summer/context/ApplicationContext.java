package com.cyquen.summer.context;

import java.util.List;

public interface ApplicationContext {

    Object getBeanByName(String beanName);

    Object getBeanByClass(Class<?> clazz);

    List<String> getAllBeanName();

}
