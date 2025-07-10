package com.gs.rpcsimulate.consumer.processor;

import com.gs.rpcsimulate.consumer.anno.RpcClientProxyReference;
import com.gs.rpcsimulate.consumer.proxy.ClientStub;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

/**
 * Bean的后置增强
 * 每一个Bean都会经过后置处理器，所以也要放到IOC容器管理，这样这里的每一个Bean都会去执行自定义的后置处理器
 */
@Component
public class MyBeanPostProcessor implements BeanPostProcessor {

    @Autowired
    ClientStub clientStub;
    /**
     * 让自定义引用注解提供自动注入其被引用类
     * @param bean the new bean instance
     * @param beanName the name of the bean
     * @return
     * @throws BeansException
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException { // 这里的bean是controller，就是使用远程调用工具的那个类
        // 1. 先查看经过该处理器的Bean的字段中有没有自定义的那个注解
        Field[] declaredFields = bean.getClass().getDeclaredFields();// 得到bean的所有字段，eg：IUserService userService;
        for (Field field : declaredFields) {
            // 2. 查找字段中是否包含注解
            RpcClientProxyReference annotation = field.getAnnotation(RpcClientProxyReference.class); // 看看IUserService userService头上有没有注解
            if (annotation != null) {
                // 3. 想要获取注解特定的代理对象，就得用代理生成类，调用它泛化的获取具体代理的方法，就能得到具体的代理类
                Object proxy = clientStub.getProxy(field.getType());// eg:IUserService
                // 4. 属性注入，通过反射机制给一个Bean对象的属性完成赋值
                field.setAccessible(true);
                try {
                    field.set(bean, proxy); // 这里的bean是controller对象，proxy就是field的值
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return bean;
    }
}
