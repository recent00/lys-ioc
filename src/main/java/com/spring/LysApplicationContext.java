package com.spring;

import com.lys.AppConfig;

import java.beans.Introspector;
import java.io.File;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LysApplicationContext {

    private Class configClass;
    private Map<String,BeanDefinition> beanDefinitionMap = new HashMap<String,BeanDefinition>();
    private Map<String,Object> singletonObjects = new HashMap<String, Object>();//单例池
    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();

    public LysApplicationContext(Class<AppConfig> configClass) {
        this.configClass = configClass;


        //扫描
        //判断configClass这个类上是否有@ComponentScan注解
        scan(configClass);

        //扫描完要创建单例bean
        //遍历beanDefinitionMap，拿到单例bean，并创建
        for (Map.Entry<String, BeanDefinition> entry : beanDefinitionMap.entrySet()) {
            String beanName = entry.getKey();
            BeanDefinition beanDefinition = entry.getValue();

            //单例bean
            if(beanDefinition.getScope().equals("singleton")) {
                Object bean = createBean(beanName, beanDefinition);
                singletonObjects.put(beanName,bean);
            }

        }
    }

    //创建bean
    private Object createBean(String beanName,BeanDefinition beanDefinition) {
        Class clazz = beanDefinition.getType();

        Object instance = null;
        try {
            instance = clazz.getConstructor().newInstance();//调用无参构造方法创建对象

            //依赖注入
            //遍历每个属性
            for (Field field : clazz.getDeclaredFields()) {
                //检查属性是否需要依赖注入
                if(field.isAnnotationPresent(Autowired.class)) {
                    field.setAccessible(true);

                    field.set(instance,getBean(field.getName()));
                }
            }


            //执行实现Aware接口的类中的方法
            if(instance instanceof BeanNameAware) {
                ((BeanNameAware) instance).setBeanName(beanName);
            }


            //初始化前
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessBeforeInitialization(instance,beanName);
            }

            if(instance instanceof InitializingBean) {
                ((InitializingBean) instance).afterPropertiesSet();
            }

            //初始化后
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessAfterInitialization(instance,beanName);//这步可以实现AOP功能，返回一个代理对象，如果不用实现AOP就返回一个原对象
            }

        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        return instance;
    }

    //获取bean
    public Object getBean(String beanName) {
        if(!beanDefinitionMap.containsKey(beanName)) {
            //没有这个bean
            throw new NullPointerException();
        }
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);

        if(beanDefinition.getScope().equals("singleton")) {
            Object singletonBean = singletonObjects.get(beanName);

            //依赖注入的时候可能要注入的bean还没创建，因此需要创建
            if(singletonBean == null) {
                singletonBean = createBean(beanName,beanDefinition);
                singletonObjects.put(beanName,singletonBean);
            }
            //单例bean
            return singletonBean;
        } else {
            //原型bean--->每次调都要创建对象
            return createBean(beanName,beanDefinition);
        }
    }

    private void scan(Class<AppConfig> configClass) {
        if(configClass.isAnnotationPresent(ComponentScan.class)) {
            ComponentScan componentScanAnnotation = configClass.getAnnotation(ComponentScan.class);//拿到配置类上的注解
            String path = componentScanAnnotation.value();//拿到扫描路径

            path = path.replace(".","/");
            //根据得到的path找到target/classes下编译好的.class文件

            //System.out.println("扫描路径：" + path);

            ClassLoader classLoader = LysApplicationContext.class.getClassLoader();//拿到加载LysApplicationContext的类加载器
            URL resource = classLoader.getResource(path);

            //将拿到的URL封装成file
            File file = new File(resource.getFile());

            if(file.isDirectory()) {
                for (File f : file.listFiles()) {
                    String absolutePath = f.getAbsolutePath();
                    //System.out.println(absolutePath);

                    absolutePath = absolutePath.substring(absolutePath.indexOf("com"),absolutePath.indexOf(".class"));
                    absolutePath = absolutePath.replace("\\",".");
                    //判断类上有没有Component注解
                    Class<?> clazz = null;
                    try {
                        clazz = classLoader.loadClass(absolutePath);//通过反射拿到class

                        if(clazz.isAnnotationPresent(Component.class)) {


                            //判断是否有类实现了BeanPostProcessor接口
                            if(BeanPostProcessor.class.isAssignableFrom(clazz)) {
                                BeanPostProcessor instance = (BeanPostProcessor) clazz.getConstructor().newInstance();//拿到实现BeanPostProcessor接口的类的实例
                                beanPostProcessorList.add(instance);
                            }

                            Component componentAnnotation = clazz.getAnnotation(Component.class);
                            String beanName = componentAnnotation.value();

                            if("".equals(beanName)) {
                                beanName = Introspector.decapitalize(clazz.getSimpleName());//生成默认的名字
                            }

                            //这是一个bean
                            //创建bean的定义
                            BeanDefinition beanDefinition = new BeanDefinition();
                            beanDefinition.setType(clazz);

                            //判断是不是单例bean
                            if(clazz.isAnnotationPresent(Scope.class)) {
                                Scope scopeAnnotation = clazz.getAnnotation(Scope.class);
                                String value = scopeAnnotation.value();
                                beanDefinition.setScope(value);
                                //判断是单例还是原型
                            } else {
                                //单例
                                beanDefinition.setScope("singleton");
                            }
                            beanDefinitionMap.put(beanName,beanDefinition);//把扫描出来的bean存起来
                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
