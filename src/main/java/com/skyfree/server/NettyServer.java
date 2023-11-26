package com.skyfree.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

@Slf4j
public class NettyServer {

    private int port;

    public NettyServer(int port) {
        this.port = port;
    }

    /**
     * 初始化创建netty
     */
    public void start() throws InterruptedException {
        EventLoopGroup boosGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();


//        启动NIO服务的辅助启动类
        ServerBootstrap bootstrap = new ServerBootstrap()
//                EventLoopGroup用来处理IO操作的多线程事件循环器负责接收客户端链接线程
//                负责处理客户端io事件、task任务、监听任务组
                .group(boosGroup, workerGroup)
//                指定Channel
                .channel(NioServerSocketChannel.class)
//              BACKLOG 用于构造服务端套接字ServerSocket对象
//              标识当服务器请求处理线程全满时，用于临时存放已完成三次握手的请求的队列的最大长度
                .option(ChannelOption.SO_BACKLOG, 1024)
//                是否启用心跳保活机制
                .childOption(ChannelOption.SO_KEEPALIVE, true)
//                是否启用Nagle算法，数据无论多小都会立即发送，不会等待缓冲区填满
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new NettyServerHandlerInitializer());

        try {
            Channel channel = bootstrap.bind(port).sync().channel();
            System.out.println("server run in port" + port);
            /**
             * 服务器关闭监听
             * channel.closeFuture不做任何操作，只是简单的返回channel对象中closeFuture对象，对于每个Channel对象，都会有唯一的CloseFuture,用来表示关闭Future,
             * 所有执行channel.closeFuture().sync就是执行的CloseFuturn的sync方法，会将当前线程阻塞在CloseFuture
             */
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            boosGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        new NettyServer(8899).start();
    }
}
