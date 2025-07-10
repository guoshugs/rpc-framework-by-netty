package com.gs.rpcsimulate.consumer.proxy;

import com.alibaba.fastjson.JSON;
import com.gs.rpcsimulate.common.RpcRequest;
import com.gs.rpcsimulate.common.RpcResponse;
import com.gs.rpcsimulate.consumer.client.NettyRpcClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.cglib.proxy.Proxy;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
/*
 * 正常内部调用是注入一个服务，然后controller调用该服务的一个接口，装载参数就可以了。
 * 但rpc远程之间因为协议，传递的是message，不能像正常调用一般。所以需要将方法、参数包装成message，通过通道一并发走，才行。
 * 客户端代理代理的就是客户端！！！代理客户端发消息的过程！将正常调用抽象出方法名、参数等，装进message里，代替客户端发走。
 * client是发消息用的，提供的也是send(String msg)。但用户使用起来就麻烦了，需要包装成message，所以使用代理代替客户端来发消息。
 */
/**
 * 客户端的代理类
 * 现在是调用getUserById方法，需要封装成RpcRequest对象，才能发消息，但不能每调用一个方法都去封装成这样的对象。这不是框架该做的。
 * 所以需要给RpcClient客户端一个代理，代理的是全部服务，每想调用一个方法，就自动封装成RpcRequest对象。
 * 同时可以自动解析服务端返回过来的数据。
 */
@Component
public class ClientStub {

    Map<Class, Object> SERVICE_PROXY = new HashMap<>();

    @Autowired
    NettyRpcClient nettyRpcClient;

    /**
     * 根据一个类型来获取代理对象
     * 该代理对象生成以后，不管是UserController类用，还是其他类用，都不需要再次生成，代理对象只用生成1次，其他再用直接去缓存中拿。
     * 所以需要有个缓存来存该代理对象。
     * @param serviceInterfaceClass
     * @return
     */
    /*
    没有实现接口的代理用cglib，有实现接口的代理用jdk
     */
    public Object getProxy(Class serviceInterfaceClass) {
        Object proxy = SERVICE_PROXY.get(serviceInterfaceClass);
        if (proxy == null) {
            // 创建代理对象，这里用jdk的动态代理，
            // 需要类加载器、装接口的数组（服务端有接口的实现，客户端调用的就是接口）、提供一个invocationHandler
            proxy = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{serviceInterfaceClass}, new InvocationHandler() {
                // 增强
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                    // 1、装请求对象，将RpcRequest对象封装到invoke里面，只要代理对象一生成，就会触发调用
                    RpcRequest rpcRequest = new RpcRequest();
                    rpcRequest.setRequestId(UUID.randomUUID().toString());
                    rpcRequest.setClassName(method.getDeclaringClass().getName()); // 得到调用服务的名称
                    rpcRequest.setMethodName(method.getName());
                    rpcRequest.setParameterTypes(method.getParameterTypes());
                    rpcRequest.setParameters(args);
                    // 2、发送消息，要用RpcClient，注入
                    try {
                        Object responseMsg = nettyRpcClient.send(JSON.toJSONString(rpcRequest));
                        // 3、 解析
                        RpcResponse rpcResponse = JSON.parseObject(responseMsg.toString(), RpcResponse.class);
                        if (rpcResponse.getError() != null) {
                            throw new RuntimeException(rpcResponse.getError());
                        }
                        if (rpcResponse.getResult() != null) {
                            return JSON.parseObject(rpcResponse.getResult().toString(), method.getReturnType());
                        }
                        return null;
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw e;
                    }
                }
            });
            SERVICE_PROXY.put(serviceInterfaceClass, proxy);
            return proxy;
        } else {
            return proxy;
        }
    }
}
