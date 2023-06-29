package com.seibel.distanthorizons.core.network;

import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.network.messages.HelloMessage;
import com.seibel.distanthorizons.core.network.protocol.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.apache.logging.log4j.Logger;

public class NetworkServer extends NetworkEventSource implements AutoCloseable {
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();

    // TODO move to config of some sort
    private final int port;

    private final EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    Channel channel;

    public NetworkServer(int port) {
        this.port = port;

        LOGGER.info("Starting server on port {}", port);

        ServerBootstrap bootstrap = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .childHandler(new DhNetworkChannelInitializer(messageHandler));

        ChannelFuture bindFuture = bootstrap.bind(port);
        bindFuture.addListener((ChannelFuture channelFuture) -> {
            if (!channelFuture.isSuccess())
                throw new RuntimeException("Failed to bind: " + channelFuture.cause());

            LOGGER.info("Server is ready");
        });

        channel = bindFuture.channel();
        channel.closeFuture().addListener(future -> close());

        registerHandler(HelloMessage.class, (msg, ctx) -> {
            LOGGER.info("Client connected: {}", ctx.channel().remoteAddress());
            ctx.channel().writeAndFlush(new HelloMessage());
        });

        registerDisconnectHandler(ctx -> {
            LOGGER.info("Client disconnected: {}", ctx.channel().remoteAddress());
        });
    }

    @Override
    public void close() {
        if (closeReason != null)
            LOGGER.error(closeReason);

        bossGroup.shutdownGracefully().syncUninterruptibly();
        workerGroup.shutdownGracefully().syncUninterruptibly();
        channel.close().syncUninterruptibly();
    }
}
