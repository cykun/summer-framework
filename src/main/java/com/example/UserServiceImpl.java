package com.example;

import com.cyquen.summer.annotation.Autowired;
import com.cyquen.summer.annotation.Component;
import com.cyquen.summer.interfaces.BeanNameAware;
import com.cyquen.summer.interfaces.InitializingBean;

@Component
public class UserServiceImpl implements UserService, InitializingBean, BeanNameAware {

    @Autowired
    UserMapper userMapper;

    @Override
    public String getUserInfo() {
        return userMapper.findUser();
    }

    @Override
    public void afterProperties() {
        System.out.println("initial");
    }

    @Override
    public void setBeanName(String beanName) {
        System.out.println(beanName);
    }
}
