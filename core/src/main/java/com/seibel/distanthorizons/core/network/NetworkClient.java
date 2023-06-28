package com.seibel.distanthorizons.core.network;

import com.seibel.distanthorizons.core.network.messages.HelloMessage;
import com.seibel.distanthorizons.core.util.NetworkUtil;
import com.seibel.distanthorizons.core.network.protocol.MessageHandlerSide;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class NetworkClient implements Closeable {
    private enum State {
        OPEN,
        RECONNECT,
        CLOSED
    }

    private static final int FAILURE_RECONNECT_DELAY_SEC = 5;

    // TODO move to config of some sort
    private final String host;
    private final int port;
    private State state = State.OPEN;

    private final Bootstrap clientBootstrap;
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private Channel channel;

    public NetworkClient(String host, int port) {
        this.host = host;
        this.port = port;

        clientBootstrap = new Bootstrap()
                .group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(NetworkUtil.getChannelInitializer(MessageHandlerSide.CLIENT));
        connect();
    }

    private void connect() {
        ChannelFuture connectFuture = clientBootstrap.connect(host, port);
        connectFuture.addListener((ChannelFuture channelFuture) -> {
            if (!channelFuture.isSuccess()) return;

            channel = channelFuture.channel();
            channel.writeAndFlush(new HelloMessage());
        });

        channel = connectFuture.channel();
        channel.closeFuture().addListener((ChannelFuture channelFuture) -> {
            if (state == State.CLOSED) return;

            workerGroup.schedule(this::connect, state == State.RECONNECT ? 0 : FAILURE_RECONNECT_DELAY_SEC, TimeUnit.SECONDS);
            state = State.OPEN;
        });
    }

    /** Kills the current connection, triggering auto-reconnection immediately. */
    public void reconnect() {
        state = State.RECONNECT;
        channel.disconnect();
    }

    @Override
    public void close() throws IOException {
        state = State.CLOSED;
        workerGroup.shutdownGracefully();
    }
}
