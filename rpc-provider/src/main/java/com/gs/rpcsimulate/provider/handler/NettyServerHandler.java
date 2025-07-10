package com.gs.rpcsimulate.provider.handler;

import com.alibaba.fastjson.JSON;
import com.gs.rpcsimulate.common.RpcRequest;
import com.gs.rpcsimulate.common.RpcResponse;
import com.gs.rpcsimulate.provider.anno.RpcService;
import com.gs.rpcsimulate.provider.proxy.ServerSkeleton;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 自定义业务处理类，被Spring容器管理，要加上component，该服务端的处理器类需要做如下事情：
 * 1 将标有@RpcService注解的Bean进行缓存
 * 2 处理器将接收客户端的请求
 * 3 根据传递过来的beanName从缓存中查找
 * 4 通过反射调用bean的方法
 * 5 给客户端响应
 */
@Component
@ChannelHandler.Sharable    // 设置通道共享，
// 因为NettyServerHandler类交给了Spring容器管理之后，它是一个单例的，它只能被一个连接占用。那如果要被多个连接占用，需要将该处理器共享给其他通道。
public class NettyServerHandler extends SimpleChannelInboundHandler<String> implements ApplicationContextAware {

    public static Map<String, Object> SERVICE_INSTANCE_MAP = new HashMap<>();

    /**
     * 1 将标有@RpcService注解的Bean进行缓存
     * 实现了ApplicationContextAware接口，就可以获取到ApplicationContext对象，它就是IOC容器的对象！容器自己有记录，可以通过注解来获取IOC容器中的beans
     * @param applicationContext the ApplicationContext object to be used by this object
     * @throws BeansException
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // 1.1 通过注解能获取到所有标了该注解的服务类的集合
        Map<String, Object> serviceMap = applicationContext.getBeansWithAnnotation(RpcService.class);//通过注解来获取IOC容器中的beans，就是通过RpcService.class这个注解
        for (Map.Entry<String, Object> entry : serviceMap.entrySet()) {
            Object serviceBean = entry.getValue();//就是UserServiceImpl的对象
            // 又因为消费者和提供者之间是基于接口契约的形式，所以要判断该bean有没有实现指定的接口
            if (serviceBean.getClass().getInterfaces().length == 0) {
                throw new RuntimeException("对外暴露的服务没有实现该指定接口");
            }
            // public class UserServiceImpl implements IUserService，一个类可以实现多个接口，取第一个
            // 默认处理，第一个接口作为缓存bean的名字
            String interfaceName = serviceBean.getClass().getInterfaces()[0].getName();
            // 接下来就可以把名字进行缓存了，要放在map容器中
            SERVICE_INSTANCE_MAP.put(interfaceName, serviceBean);
            System.out.println(SERVICE_INSTANCE_MAP);
        }
    }

    /*===============================================================================*/
    @Autowired
    private ServerSkeleton serverSkeleton;
    /**
     * 通道读取就绪事件——读取客户端的消息
     * 那么如何知道哪些服务类上标了注解呢？需要判断。就要去实现Spring提供的接口ApplicationContextAware。只要设置了setApplicationContext，就可以知道哪些服务上标了注解
     * @param ctx           the {@link ChannelHandlerContext} which this {@link SimpleChannelInboundHandler}
     *                      belongs to
     * @param msg           the message to handle
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        // 2 接收客户端的请求，接收的RpcRequest的json格式，要转换成原来的对象
        RpcRequest rpcRequest = JSON.parseObject(msg, RpcRequest.class);
        RpcResponse rpcResponse = new RpcResponse();
        rpcResponse.setRequestId(rpcRequest.getRequestId());
        // 业务处理，真正业务处理可能会有异常
        try {
            rpcResponse.setResult(serverSkeleton.process(rpcRequest));
        } catch (Exception e) {
            e.printStackTrace();
            rpcResponse.setError(e.getMessage());
        }
        // 5 给客户端响应
        ctx.writeAndFlush(JSON.toJSONString(rpcResponse));
    }

    /*===============================================================================*/

/*    private Object insiderProcess(RpcRequest rpcRequest) throws InvocationTargetException { // processor抛出了异常，在channelRead0中做了捕获
         // 3 根据传递过来的beanName从缓存中查找
        Object serviceBean = SERVICE_INSTANCE_MAP.get(rpcRequest.getClassName());
        if (serviceBean == null) {
            throw new RuntimeException("服务端没有找到服务");
        }
        // 4 通过反射调用bean的方法，这里使用cglib提供的方法，使用cglib获得动态代理对象
        FastClass proxy = FastClass.create(serviceBean.getClass());
        FastMethod method = proxy.getMethod(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
        return method.invoke(serviceBean, rpcRequest.getParameters());
    }*/
}
