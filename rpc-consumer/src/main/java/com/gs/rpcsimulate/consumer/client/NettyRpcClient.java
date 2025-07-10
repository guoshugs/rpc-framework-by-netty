package com.gs.rpcsimulate.consumer.client;

import com.gs.rpcsimulate.consumer.handler.NettyRpcClientHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
/*
afterPropertiesSet这里只针对一个bean对象，做的是连接资源的动作，就好像给这个bean对象画一个连接箭头！这个bean到这步就要连接到其他地方了，这种作用。
postProcessAfterInitialization，这里是做了一个过滤器，所有的bean都过一遍，找注解。也可以只针对某一个bean，只要让bean实现initializingBean接口就行。
但是它的作用就是设置属性！
 */
/**
 * Netty客户端，需要做哪些事？
 * 1、连接服务端
 * 2、关闭资源
 * 3、能提供发送消息的方法
 */
@Component  // 因为集成了Spring，要加Component注解
public class NettyRpcClient implements InitializingBean, DisposableBean {
    // 需求：当创建该类对象的时候，就要去连接Netty的服务端，所以使用Spring底层框架生命周期的接口InitializingBean

    EventLoopGroup group = null;
    Channel channel = null;

    @Autowired
    NettyRpcClientHandler nettyRpcClientHandler;

    /**
     * 1、连接服务端
     * 通常用于在 bean 的属性都被设置完毕后，进行一些初始化操作。
     * 实现 InitializingBean 接口并重写 afterPropertiesSet 方法，可以确保在 bean 被使用之前完成一些必要的初始化工作。
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {

        // 连接服务器时会有异常，要捕捉，并在异常时释放资源，在catch时，将group和channel都关闭掉
        try {
            group = new NioEventLoopGroup();
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast(new StringDecoder())
                                    .addLast(new StringEncoder())
                                    .addLast(nettyRpcClientHandler);
                        }
                    });

            channel = bootstrap.connect("127.0.0.1", 8899).sync().channel();
        } catch (Exception e) {
            e.printStackTrace();
            if (channel != null) {
                channel.close();
            }
            if (group != null) {
                group.shutdownGracefully();
            }
            // 关闭资源之后，在Spring工程结束后也要将资源销毁，不然会内存移除，所以也要实现DisposableBean
        }

    }

    /**
     * 正常流程下，工程结束正常销毁资源
     * @throws Exception
     */
    @Override
    public void destroy() throws Exception {
        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
    }

    /*
     * handler中虽然有call方法同步发送消息，但外层的client却需要通过线程池来将发消息线程执行。
     * 因为handler实现的Callable接口是并发的，本身就是需要多个线程来执行handler的。
     * 为何要多个线程执行handler？因为netty是异步事件驱动的网络应用框架，没有同步机制
     * 为何要用netty？因为要同时处理多个client，支持高并发，但每个client通道内部却需要同步等待响应！
     * 线程池.submit(可执行任务)，其实就是线程池.execute(任务中的call方法)！
     * 通过线程池执行 NettyRpcClientHandler 的 call 方法可以更好地利用系统资源和处理多个并发请求。
     * 这样可以确保每个消息的发送和处理都是异步的，并且能够有效地管理线程资源。
     * 在这种情况下，是一个 client 客户端支持同时发送多个消息。通过线程池的方式来执行 NettyRpcClientHandler，
     * 可以并发地处理多个消息发送和响应处理，但这仅限于一个客户端的操作。
     */
    ExecutorService executorService = Executors.newCachedThreadPool();
    /**
     * 定义发送消息的方法
     * handler是支持并发的，实现了Callable。并且Nio模型底层的事件驱动，监听到什么事件，都会用EventLoop线程池去执行
     * 所以发消息也用线程池.并发地处理多个消息发送和响应处理，但这仅限于一个客户端的操作
     * 使用了线程池来执行 NettyRpcClientHandler,可能会导致一些线程安全的问题，这涉及到资源共享
     * 解决方法是确保 NettyRpcClientHandler 中的状态都是线程安全的，可以使用线程安全的数据结构或者加锁来保护共享资源。
     */
    public Object send(String msg) throws ExecutionException, InterruptedException {
        nettyRpcClientHandler.setRequestMsg(msg);
        // 线程池的submit里面能放Callable并发接口
        Future submit = executorService.submit(nettyRpcClientHandler);// 这是代表一个客户端可以同时发很多消息
        // Future是一种未来的事件，是异步结果类，get能得到异步的计算结果
        return submit.get();
    }
}
