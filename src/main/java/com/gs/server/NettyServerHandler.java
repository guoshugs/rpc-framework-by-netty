package com.gs.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class NettyServerHandler extends SimpleChannelInboundHandler<String> {

    /**
     * 通道读取就绪事件--接收客户端请求，请求就是那个s，客户端发送过来的消息
     * @param channelHandlerContext
     * @param s
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) throws Exception {// 因为在NettyServer那添加的是String类型的编解码器
        System.out.println("服务端接收到的消息：" + s);

    }

    /**
     * 通道读取完毕事件，给客户端响应
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.writeAndFlush("I am Netty server");
    }
}


