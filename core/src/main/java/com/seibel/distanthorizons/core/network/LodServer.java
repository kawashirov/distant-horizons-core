package com.seibel.distanthorizons.core.network;

import com.seibel.distanthorizons.core.network.protocol.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.jetbrains.annotations.NotNull;

public class LodServer {
    // TODO move to config of some sort
    static final int PORT = 25049;

    public LodServer(/* initial settings */) {

    }

    public void start() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(getInitializer());

            b.bind(PORT).sync().channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public void stop() {

    }

    private ChannelInitializer<SocketChannel> getInitializer() {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(@NotNull SocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();

                MessageRegistry messageRegistry = new MessageRegistry();

                // Encoding
                pipeline.addLast(new LengthFieldBasedFrameDecoder(Short.MAX_VALUE, 0, Short.BYTES, 0, Short.BYTES));
                pipeline.addLast(new MessageDecoder(messageRegistry));

                // Encoder
                pipeline.addLast(new LengthFieldPrepender(Short.BYTES));
                pipeline.addLast(new MessageEncoder(messageRegistry));

                pipeline.addLast(new MessageHandler(MessageHandlerSide.SERVER));
            }
        };
    }
}
