package com.gs.rpcsimulate.provider.proxy;

import com.gs.rpcsimulate.common.RpcRequest;
import com.gs.rpcsimulate.provider.handler.NettyServerHandler;
import org.springframework.cglib.reflect.FastClass;
import org.springframework.cglib.reflect.FastMethod;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;

/* 为何要创建代理对象来调用方法？因为一个server会提供很多service，同一个service也会被不同的请求调用
* 现在的需求是，根据过来的不同请求，选择IOC容器中的特定serviceBean，然后调用该bean的特定方法去执行，返回结果
* 为了抽象出来，没人知道调用的是那个serviceBean，也没人实现知道调用的是哪个方法。都是从请求中来的，需要去容器中匹配的
* 如何让这些bean自动找到去调用自己呢？用动态代理！根据请求的服务名获得serviceBean
* 让它自己调用自己的方法，就是动态代理！（通过生成代理对象来间接调用自身的方法或对象）*/
@Service
public class ServerSkeleton {
    /* 用java提供的原生的动态代理也可以实现。但spring提供的cglib更加方便 */
    public Object process(RpcRequest rpcRequest) throws InvocationTargetException { // processor抛出了异常，在channelRead0中做了捕获
        // 3 根据传递过来的beanName从缓存中查找
        Object serviceBean = NettyServerHandler.SERVICE_INSTANCE_MAP.get(rpcRequest.getClassName());
        // 找到了serviceBean了，不用代理来调用方法，不能实现服务的被动调用！因为要的即使执行找到的这个服务的某个方法。
        if (serviceBean == null) {
            throw new RuntimeException("服务端没有找到服务");
        }
        // 4 通过反射调用bean的方法，这里使用cglib提供的方法，使用cglib获得动态代理对象
        FastClass proxy = FastClass.create(serviceBean.getClass()); // serviceBean是UserServiceImpl，method是getUserById
        FastMethod method = proxy.getMethod(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
        return method.invoke(serviceBean, rpcRequest.getParameters());
        /*
        选择使用JDK动态代理还是CGLIB代理通常取决于目标类是否实现了接口。
        如果实现了接口，通常会选择JDK动态代理，因为它简单且性能较好。
        如果没有实现接口，则使用CGLIB代理来动态生成目标类的子类。
         */
        /*
        代理不仅要知道代理的谁？还要知道代理的类有没有实现接口
        public class UserServiceImpl implements IUserService
        代理的是UserServiceImpl，它实现了IUserService接口。
        Server端的代理，代理的是各式各样的服务！
         */
        /** 上面是通过代理调用serviceBean，是被动调用，符合RPC框架的核心思想之一就是通过代理来实现远程方法调用的透明性和便捷性
         * 毕竟放到MAP缓存之前，并没有检测这些bean有没有实现服务。
         * 下面直接调用，有了serviceBean之后，不仅要判断实现的接口，也要知道接口下的方法，方法有多少个，就得写多少次。
         * 用代理简介调用，直接抽象出了Method类，直接用它自己根据提供的名字调用即可。
         *     if (SERVICE_INSTANCE_MAP.containsKey(rpcRequest.getClassName())) {
         *         Object serviceBean = SERVICE_INSTANCE_MAP.get(rpcRequest.getClassName());
         *         if (serviceBean != null) {
         *             if (serviceBean instanceof IUserService) {
         *                 IUserService userService = (IUserService) serviceBean;
         *                 result = userService.getById(rpcRequest.getParameters()[0]);
         *             }
         *         }
         *     }
         */
    }
}
