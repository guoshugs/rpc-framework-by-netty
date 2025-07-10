package com.gs.rpcsimulate.provider;

import com.gs.rpcsimulate.provider.server.NettyRpcServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
// 启动类实现runner，就可以在模块启动的时候就能启动NettyRpcServer
@SpringBootApplication
public class ServerBootstrapApplication implements CommandLineRunner { //这里为何不实现ApplicationRunner呢？
    public static void main(String[] args) {
        SpringApplication.run(ServerBootstrapApplication.class, args);
    }

    @Autowired
    NettyRpcServer nettyRpcServer;

    @Override
    public void run(String... args) throws Exception { // 接收的参数是String数组的。
        // 而ApplicationRunner接收的参数是ApplicationArguments，得这么用args.getOptionNames，args.getOptionValues("myOption")
        new Thread(new Runnable() {
            @Override
            public void run() {
                nettyRpcServer.start("127.0.0.1", 8899);
            }
        }).start();
    }
}
