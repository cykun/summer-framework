package com.example;

import com.cyquen.summer.annotation.Component;

@Component(beanName = "userMapper")
public class UserMapper {

    public String findUser() {
        return "admin";
    }

}
