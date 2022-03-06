package com.lys.service;


import com.spring.*;

@Component("userService")
//@Scope("prototype")
public class UserService implements InitializingBean,UserInterface, BeanNameAware {



    @Autowired
    private OrderService orderService;

    @MyValue("abc")
    private String name;

    private String beanName;//获取bean的名字

    public void test() {
        System.out.println(name);
        System.out.println(beanName);
        //System.out.println(orderService);
    }

    public void afterPropertiesSet() {
        //System.out.println("初始化");
    }

    @Override
    public void setBeanName(String name) {
        this.beanName = name;
    }
}
