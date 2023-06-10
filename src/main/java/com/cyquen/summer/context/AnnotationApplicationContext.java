package com.cyquen.summer.context;

import com.cyquen.summer.BeanDefinition;
import com.cyquen.summer.annotation.Autowired;
import com.cyquen.summer.annotation.Component;
import com.cyquen.summer.annotation.ComponentScan;
import com.cyquen.summer.annotation.Scope;
import com.cyquen.summer.enums.ScopeEnum;
import com.cyquen.summer.interfaces.BeanNameAware;
import com.cyquen.summer.interfaces.BeanPostProcessor;
import com.cyquen.summer.interfaces.InitializingBean;

import java.beans.Introspector;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class AnnotationApplicationContext implements ApplicationContext {

    private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();

    private final Map<String, Object> singletonPool = new ConcurrentHashMap<>();

    private final List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();

    public AnnotationApplicationContext(Class<?> configClass) {
        if (configClass.isAnnotationPresent(ComponentScan.class)) {
            ComponentScan componentScanAnnotation = configClass.getAnnotation(ComponentScan.class);
            String componentPath = componentScanAnnotation.componentPath();
            componentPath = componentPath.replace(".", File.separator);

            ClassLoader classLoader = AnnotationApplicationContext.class.getClassLoader();
            URL resource = classLoader.getResource(componentPath);
            File file = new File(URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8));
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                assert files != null;
                for (File f : files) {
                    String absolutePath = f.getAbsolutePath();
                    if (absolutePath.endsWith(".class")) {
                        String relativelyClassPath = absolutePath.substring(absolutePath.indexOf(componentPath), absolutePath.indexOf(".class"));
                        relativelyClassPath = relativelyClassPath.replace(File.separator, ".");

                        Class<?> clazz;
                        try {
                            clazz = classLoader.loadClass(relativelyClassPath);

                            if (clazz.isAnnotationPresent(Component.class)) {
                                if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
                                    try {
                                        beanPostProcessorList.add((BeanPostProcessor) clazz.getDeclaredConstructor().newInstance());
                                    } catch (InvocationTargetException | InstantiationException |
                                             IllegalAccessException | NoSuchMethodException e) {
                                        throw new RuntimeException(e);
                                    }
                                }

                                Component componentAnnotation = clazz.getAnnotation(Component.class);
                                String beanName = componentAnnotation.beanName();
                                if (Objects.isNull(beanName) || beanName.length() == 0) {
                                    String allPackageName = clazz.getName();
                                    beanName = allPackageName.substring(allPackageName.indexOf(".") + 1);
                                    beanName = Introspector.decapitalize(beanName);
                                }

                                BeanDefinition beanDefinition = new BeanDefinition();
                                beanDefinition.setClazz(clazz);
                                if (clazz.isAnnotationPresent(Scope.class)) {
                                    Scope scopeAnnotation = clazz.getAnnotation(Scope.class);
                                    beanDefinition.setScope(scopeAnnotation.scope());
                                } else {
                                    beanDefinition.setScope(ScopeEnum.Singleton);
                                }

                                beanDefinitionMap.put(beanName, beanDefinition);
                            }
                        } catch (ClassNotFoundException ignored) {

                        }
                    }
                }
            }
            for (String beanName : beanDefinitionMap.keySet()) {
                BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
                if (beanDefinition.getScope().equals(ScopeEnum.Singleton)) {
                    Object bean = createBean(beanName, beanDefinition);
                    singletonPool.put(beanName, bean);
                }
            }
        }
    }

    private Object createBean(String beanName, BeanDefinition beanDefinition) {
        Class<?> clazz = beanDefinition.getClazz();
        Constructor<?> constructor = null;
        try {
            constructor = clazz.getConstructor();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        Object instance = null;
        try {
            assert constructor != null;
            try {
                instance = constructor.newInstance();

                Field[] fields = instance.getClass().getDeclaredFields();
                for (Field field : fields) {
                    if (field.isAnnotationPresent(Autowired.class)) {
                        field.setAccessible(true);
                        Object bean = getBeanByName(field.getName());
                        field.set(instance, bean);
                    }
                }

                if (instance instanceof BeanNameAware) {
                    ((BeanNameAware) instance).setBeanName(beanName);
                }

                for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                    instance = beanPostProcessor.postProcessBeforeInitialization(beanName, instance);
                }

                if (instance instanceof InitializingBean) {
                    ((InitializingBean) instance).afterProperties();
                }

                for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                    instance = beanPostProcessor.postProcessAfterInitialization(beanName, instance);
                }
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } catch (IllegalArgumentException | SecurityException e) {
            throw new RuntimeException(e);
        }
        return instance;
    }

    @Override
    public Object getBeanByName(String beanName) {
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if (Objects.isNull(beanDefinition)) {
            throw new NullPointerException();
        }
        ScopeEnum scopeEnum = beanDefinition.getScope();
        if (scopeEnum.equals(ScopeEnum.Singleton)) {
            Object bean = singletonPool.get(beanName);
            if (bean == null) {
                bean = createBean(beanName, beanDefinition);
                singletonPool.put(beanName, bean);
            }
            return bean;
        }
        return createBean(beanName, beanDefinition);
    }

    @Override
    public Object getBeanByClass(Class<?> clazz) {
        for (Map.Entry<String, BeanDefinition> beanDefinitionEntry : beanDefinitionMap.entrySet()) {
            BeanDefinition beanDefinition = beanDefinitionEntry.getValue();
            if (beanDefinition.getClazz().equals(clazz)) {
                String beanName = beanDefinitionEntry.getKey();
                return getBeanByName(beanName);
            }
        }
        return null;
    }

    @Override
    public List<String> getAllBeanName() {
        return new ArrayList<>(beanDefinitionMap.keySet());
    }

}
