package com.gs.rpcsimulate.consumer.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;
/* 在处理器内部写成同步的了。这样client实现并发，但处理器是一个，且在内部使用同步，确保获得响应。
 */
/**
 * 客户端业务处理类
 * Netty 使用事件驱动架构，这意味着网络操作（如读、写）是由事件触发的。它采用 Reactor 模式，
 * 核心组件是 EventLoop 和 Channel。EventLoop 负责轮询 I/O 操作，一旦有事件触发，就将事件分发给相应的 Channel 处理。
 * EventLoop线程池就意味着发送线程和接收线程不是同一个
 */
@Component
public class NettyRpcClientHandler extends SimpleChannelInboundHandler<String> implements Callable {

    ChannelHandlerContext channelHandlerContext;
    // 因为netty底层是异步的，发送请求和接收响应不会是一个线程，两个线程之间若是没有通信，那么原请求线程是接收不到响应的。
    // 所以用线程等待唤醒模型，并且用并发编程接口的call方法来实现伪同步
    private String requestMsg; // 全局请求
    private String responseMsg; // 全局响应

    /**
     * channelActive是通道连接就绪事件，可以在这里写发request（注意，不准确）。
     * 可现在通道刚刚连接，并不默认连接通道就是发送消息，还不知道发送什么呢
     * 所以发送的过程是由UserController来完成，当去访问/user/getUserById路径时，才能触发消息的发送
     * 所以不能在这里写消息发送的过程
     * 具体如何发送呢？——要借助线程等待和线程唤醒的模型
     * 消息发送后，要去等待接收，是需要回调的
     * 不是异步、不是同步，是回调！调用方不会停而是继续往下执行，但在需要的地方等待结果。
     * netty底层是异步的机制，没有同步的机制，所以将消息发送，再获取结果，netty是不支持的
     * 所以用线程的等待唤醒来实现上面的过程。——要将该类实现Callable接口——并发编程接口
     * 怎么发送？用通道上下文来发送。所以要再call方法里面使用ChannelHandlerContext，所以定义全局ChannelHandlerContext
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 定义全局ChannelHandlerContext，在通道连接好之后，记录它作为全局通道上下文
        channelHandlerContext = ctx;
    }

    /**
     * 这是并发下的发送请求的线程
     * 并发编程要加锁！
     * @return
     * @throws Exception
     */
    @Override
    public synchronized Object call() throws Exception {
        channelHandlerContext.writeAndFlush(requestMsg);// 发送消息需要消息，将消息定义为全局
        // 发送好消息之后，要等待服务端返回的响应，响应在channelRead0里面了。
        // 但是call和channelRead0是异步的，所以要借助线程的等待唤醒机制来完成伪同步的操作
        // 将发送消息的线程处于等待状态
        wait();
        // 什么时候被接收响应的线程唤醒，什么时候返回发送结果！结果在全局里面
        // 线程是唤醒了，若是不加同步锁，该线程会马上返回，可此时读响应线程可能没有处理完，是会出错的。
        // 所以就算该线程被唤醒了，也得等读响应线程结束才能继续往下执行——这其实就是回调！支部会wait下面没其他过程，所以体现不出异步的好处。
        return responseMsg;
    }

    public void setRequestMsg(String requestMsg) {
        this.requestMsg = requestMsg;
    }

    /*=================================================================================*/

    /**
     * 通道读取就绪事件——读取服务端消息，但事情有先来后到，
     * client先发1.request，2.server接消息，3.server发response，4.client接消息
     * 这个channelRead0是4.client接消息
     * @param ctx           the {@link ChannelHandlerContext} which this {@link SimpleChannelInboundHandler}
     *                      belongs to
     * @param msg           the message to handle
     * @throws Exception
     */
    @Override
    protected synchronized void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        responseMsg = msg;
        // 进入到该方法就意味着响应线程已经就绪了，此时需要唤醒发送请求的线程
        notify();
        /*======   用到的等待唤醒机制必须是同步的，需要给锁起来！ synchronize! ======  */
    }

}
