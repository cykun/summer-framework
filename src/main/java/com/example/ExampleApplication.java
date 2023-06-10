package com.example;

import com.cyquen.summer.annotation.ComponentScan;
import com.cyquen.summer.context.AnnotationApplicationContext;
import com.cyquen.summer.context.ApplicationContext;

@ComponentScan(componentPath = "com.example")
public class ExampleApplication {

    public static void main(String[] args) {
        ApplicationContext applicationContext = new AnnotationApplicationContext(ExampleApplication.class);
        UserService userService = (UserService) applicationContext.getBeanByClass(UserServiceImpl.class);
        System.out.println(userService.getUserInfo());
    }
}
