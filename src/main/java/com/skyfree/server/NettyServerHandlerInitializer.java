package com.skyfree.server;

import com.skyfree.server.handler.NettyServerHandler;
import com.skyfree.server.handler.ServerIdleStateHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class NettyServerHandlerInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel channel) throws Exception {
        channel.pipeline()
//                编码通道处理
                .addLast("decode",new StringDecoder())
//                转码通道处理
                .addLast("encode",new StringEncoder())

                .addLast(new ServerIdleStateHandler())
//                聊天服务通道处理
//                自定义的处理器如果客户端有前缀字段，在服务端必须要添加相同的字段，才能对应处理
                .addLast("chat",new NettyServerHandler());

    }
}
