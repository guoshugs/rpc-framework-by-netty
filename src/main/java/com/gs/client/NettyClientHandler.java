package com.gs.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class NettyClientHandler extends SimpleChannelInboundHandler<String> {
    /**
     * 通道读取事件--读取服务端发送的消息，就是s
     * @param channelHandlerContext
     * @param s
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) throws Exception {
        System.out.println("客户端接收的消息：" + s);

    }

    /**
     * client是需要发送消息的，所以之前要将channel连接就绪，用channelActive发送
     * 通道连接就绪事件--与服务端建立连接
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush("I am Netty client");
    }
}
