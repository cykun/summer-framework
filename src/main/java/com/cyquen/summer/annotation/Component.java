package com.cyquen.summer.annotation;

import java.lang.annotation.*;

@Documented
@Inherited
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Component {

    String beanName() default "";

    Class<?> beanClass() default Object.class;

}
