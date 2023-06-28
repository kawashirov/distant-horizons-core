package com.seibel.distanthorizons.core.network;

import com.seibel.distanthorizons.core.network.protocol.*;
import com.seibel.distanthorizons.core.util.NetworkUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.io.Closeable;
import java.io.IOException;

public class NetworkServer implements Closeable {
    // TODO move to config of some sort
    private final int port;

    EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    EventLoopGroup workerGroup = new NioEventLoopGroup();

    public NetworkServer(int port) {
        this.port = port;

        ServerBootstrap b = new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(NetworkUtil.getChannelInitializer(MessageHandlerSide.SERVER));

        b.bind(port)
                .addListener((ChannelFuture channelFuture) -> {
                    if (!channelFuture.isSuccess())
                        throw new RuntimeException("Failed to bind: " + channelFuture);
                })
                .channel().closeFuture().addListener(future -> close());
    }

    @Override
    public void close() throws IOException {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
