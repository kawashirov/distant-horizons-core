package com.seibel.distanthorizons.core.network;

import com.seibel.distanthorizons.core.network.messageHandling.MessageDecoder;
import com.seibel.distanthorizons.core.network.messageHandling.MessageHandler;
import com.seibel.distanthorizons.core.network.messageHandling.MessageHandlerSide;
import com.seibel.distanthorizons.core.network.messageHandling.MessageRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

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
            public void initChannel(SocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();

                MessageRegistry messageRegistry = new MessageRegistry();

                // Encoding
                pipeline.addLast(new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 2, 0, 2));
                pipeline.addLast(new MessageDecoder(messageRegistry));
                // TODO packet encoder

                pipeline.addLast(new MessageHandler(MessageHandlerSide.SERVER));
            }
        };
    }
}
