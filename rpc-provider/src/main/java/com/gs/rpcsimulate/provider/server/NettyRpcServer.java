package com.gs.rpcsimulate.provider.server;

import com.gs.rpcsimulate.provider.handler.NettyServerHandler;
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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Netty服务端
 * 启动服务端监听端口
 * 该类要被Spring容器所管理，因为继承了Springboot
 * 另外，该类是被Spring容器管理的，若是容器关闭了，也需要把类中资源关闭掉，
 * 所以利用Spring容器生命周期的接口DisposableBean，就把自定义组件的资源释放或称也交给容器管理了
 */
@Component
public class NettyRpcServer implements DisposableBean {
    @Autowired
    private NettyServerHandler nettyServerHandler;

    EventLoopGroup bossGroup = null;
    EventLoopGroup workerGroup = null;
    public void start(String host, int port) {
        try {
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            ServerBootstrap serverBootstrap = new ServerBootstrap();

            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        // pipeline底层就是双向链表，放的就是类似编解码器等处理器
                        // 使用rpc通信，因为rpc的效率比较高
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            ChannelPipeline pipeline = socketChannel.pipeline();
                            pipeline.addLast(new StringDecoder())
                                    .addLast(new StringEncoder())
                                    .addLast(nettyServerHandler);
                        }
                    });

            // 如果出现异常，还需要释放之前的资源——bossGroup,workerGroup，所以处理办法是整个方法的处理办法
            ChannelFuture channelFuture = serverBootstrap.bind(host, port).sync();

            System.out.println("============Netty server starts===============");

            // 需要监听通道关闭的状态
            channelFuture.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
        }
    }

    @Override
    public void destroy() throws Exception {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
}
