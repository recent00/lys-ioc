package com.lys;

import com.lys.service.UserInterface;
import com.lys.service.UserService;
import com.spring.LysApplicationContext;

public class Test {
    public static void main(String[] args) {

        //扫描---->创建单例bean
        LysApplicationContext applicationContext = new LysApplicationContext(AppConfig.class);

        UserInterface userService = (UserInterface) applicationContext.getBean("userService");

        userService.test();
    }
}
