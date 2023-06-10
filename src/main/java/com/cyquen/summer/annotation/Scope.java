package com.cyquen.summer.annotation;

import com.cyquen.summer.enums.ScopeEnum;

import java.lang.annotation.*;

@Documented
@Inherited
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Scope {

    ScopeEnum scope() default ScopeEnum.Singleton;

}
