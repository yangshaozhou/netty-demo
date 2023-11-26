package com.skyfree.server.handler;

import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@ChannelHandler.Sharable
public class NettyServerHandler extends SimpleChannelInboundHandler<String> {

    /**
     * 所有的活动用户
     */

    private static final ChannelGroup group = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private static final ConcurrentHashMap<Channel,Long> concurrentHashMap = new ConcurrentHashMap<>();
    /**
     * 读取消息通通道
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String s) throws Exception {
        Channel channel = channelHandlerContext.channel();
//        当有用户发送消息时候，对其他的用户发送消息
        for (Channel ch : group) {
            if(ch == channel) {
                ch.writeAndFlush("[you]:" + s + "\n");
            }
            else {
//                remoteAddress 获取远程地址
                ch.writeAndFlush("[" + channel.remoteAddress() + "]" + s + "\n");
            }
        }
        System.out.println("["+ channel.remoteAddress() + "]:" + s + "\n");
    }

    /**
     * 处理新加的消息通道
     */

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
       Channel channel = ctx.channel();
       for(Channel ch : group) {
           if(ch == channel) {
               ch.writeAndFlush("[" + channel.remoteAddress() + "] coming");
           }
       }
       group.add(channel);
    }

    /**
     * 处理退出消息通道
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
      Channel channel = ctx.channel();
      for(Channel ch : group) {
          if(ch == channel) {
              ch.writeAndFlush("[" + channel.remoteAddress() + "] leaving");
          }
      }
      group.remove(channel);
    }

    /**
     * 在建立链接时发送消息
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        boolean active = channel.isActive();
        if(active) {
            System.out.println("[" + channel.remoteAddress() + "] is online");
        }
        else {
            System.out.println("[" + channel.remoteAddress() + "] is offline");
        }
        ctx.writeAndFlush("[server] : welocome");
    }

    /**
     * 退出时发送
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            Channel channel = ctx.channel();
            if(!channel.isActive()) {
                System.out.println("[" + channel.remoteAddress() + "] is offline");
            }
            else {
                System.out.println("[" + channel.remoteAddress() + "] is online");
            }
    }

    /**
     * 异常捕获
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
       Channel channel = ctx.channel();
        System.out.println("[" + channel.remoteAddress() + "] leave the room");
        ctx.close().sync();
    }

    /**
     * 如果链接闲置时间过长，会触发IdleStateEvent事件，在ChannelInboundHandler中可以覆盖userEventrighered方法处理IdleStateEvent
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
//         判断事件是否为IdleStateEvent
        if(evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            IdleState state = idleStateEvent.state();
            String evtState = null;
            Channel channel = ctx.channel();
            Long count = concurrentHashMap.getOrDefault(channel,0L);
            switch (state)
            {
                    case READER_IDLE:
                    evtState = "读空闲";
                    break;
                    case WRITER_IDLE:
                    evtState = "写空闲";
                    break;
                    case ALL_IDLE:
                        evtState = "读写空闲";
                        count ++;
                    break;
                default:
                    break;
            }
            log.info("userServerEventTriggered-evtState:{}",evtState);
//            空闲计数达5次，进行测试链接
            if(count > 2L)
            {
                ctx.writeAndFlush("测试客户端是佛能接受信息")
//                        发送失败时关闭通道，再或者可以达到空间多少次后，进行关闭通道
                        .addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
                concurrentHashMap.remove(channel);
            }
            concurrentHashMap.put(channel,count);
        } else {
//           事件不是一个IdleStateEvent,就将它传递给下一个处理程序
            super.userEventTriggered(ctx,evt);
        }
    }
}
