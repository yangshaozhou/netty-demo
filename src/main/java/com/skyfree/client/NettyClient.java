package com.skyfree.client;

import cn.hutool.core.util.StrUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Slf4j
public class NettyClient {

    private String ip;

    private int port;

    private boolean stop = false;

    public NettyClient(String ip, int port) {
        this.ip = ip;
        this.port = port;
    }

    public void run() {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap()
                .group(workerGroup)
//                指定所使用NIO传输channel
                .channel(NioSocketChannel.class)
//                指定客户端初始化处理
                .handler(new ClientInitHandler());


        try {
            Channel channel = bootstrap.connect(ip, port).sync().channel();
            while (true) {//                    链接服务
                BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
                String content = reader.readLine();
                if (!StrUtil.isBlank(content)) {
                    if (StrUtil.equals(content, "q")) {
                        System.exit(1);
                    }
                    System.out.println(content);
                    channel.writeAndFlush(content);
                }
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) {
        new NettyClient("127.0.0.1", 8899).run();
    }
}
