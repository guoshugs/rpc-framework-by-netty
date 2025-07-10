package com.gs.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class NettyServer {
    public static void main(String[] args) throws InterruptedException {
        // 1 创建bossGroup线程组：处理网络事件--连接事件
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);//参数代表线程的个数，boss一般是1
        // 2 创建workerGroup线程组，处理网络事件--读写事件
        EventLoopGroup workerGroup = new NioEventLoopGroup();//默认线程是取逻辑处理器数量*2
        // 3 创建服务端启动助手
        ServerBootstrap bootstrap = new ServerBootstrap();
        // 4 设置bossGroup线程组和workerGroup线程组
        // 5 设置服务端通道，变成NIO
        // 6 创建一个通道初始化对象
        // 7 向pipeline中添加自定义业务处理handler
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        // 7.1 得在pipeline中添加String类型的编解码器，是客户端服务端直接使用String类型进行通信，不然就要发送二进制数据
                        ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast(new StringDecoder())
                                .addLast(new StringEncoder())
                                .addLast(new NettyServerHandler());
                    }
                });
        // 8 启动服务端并绑定端口，同时将异步改为同步。因为netty底层是基于异步实现的，而必须先绑定端口才能继续后面的消息发送，所以这里不是异步的过程
        ChannelFuture channelFuture = bootstrap.bind(8090).sync();
        System.out.println("===============rpc server 启动成功===============");
        // 9 关闭通道和关闭连接池
        channelFuture.channel().closeFuture().sync();//closeFuture并非是真正的关闭通道，而是监听通道关闭的状态，当有通过关闭的时候，才会去出发代码
        workerGroup.shutdownGracefully();
        bossGroup.shutdownGracefully();
    }
}
