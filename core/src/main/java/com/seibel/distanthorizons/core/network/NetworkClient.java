package com.seibel.distanthorizons.core.network;

import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.network.messages.HelloMessage;
import com.seibel.distanthorizons.core.network.protocol.DhNetworkChannelInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.TimeUnit;

public class NetworkClient extends NetworkEventSource implements AutoCloseable {
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();

    private enum State {
        OPEN,
        RECONNECT,
        CLOSED
    }

    private static final int FAILURE_RECONNECT_DELAY_SEC = 5;

    // TODO move to config of some sort
    private final String host;
    private final int port;

    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final Bootstrap clientBootstrap = new Bootstrap()
            .group(workerGroup)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(new DhNetworkChannelInitializer(messageHandler));

    private State state = State.OPEN;
    private Channel channel;

    public NetworkClient(String host, int port) {
        this.host = host;
        this.port = port;
        connect();
    }

    private void connect() {
        LOGGER.info("Connecting to {}:{}", host, port);

        ChannelFuture connectFuture = clientBootstrap.connect(host, port);
        connectFuture.addListener((ChannelFuture channelFuture) -> {
            if (!channelFuture.isSuccess()) return;
            channel.writeAndFlush(new HelloMessage());
        });

        channel = connectFuture.channel();
        channel.closeFuture().addListener((ChannelFuture channelFuture) -> {
            if (state == State.CLOSED) return;

            workerGroup.schedule(this::connect, state == State.RECONNECT ? 0 : FAILURE_RECONNECT_DELAY_SEC, TimeUnit.SECONDS);
            state = State.OPEN;
        });

        registerHandler(HelloMessage.class, (msg, ctx) -> {
            LOGGER.info("Connected");
        });

        registerDisconnectHandler(ctx -> {
            LOGGER.info("Disconnected");
        });
    }

    /** Kills the current connection, triggering auto-reconnection immediately. */
    public void reconnect() {
        state = State.RECONNECT;
        channel.disconnect();
    }

    @Override
    public void close() {
        if (closeReason != null)
            LOGGER.error(closeReason);

        state = State.CLOSED;
        workerGroup.shutdownGracefully().syncUninterruptibly();
        channel.close().syncUninterruptibly();
    }
}
