package com.seibel.distanthorizons.core.network;

import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.network.messages.CloseMessage;
import com.seibel.distanthorizons.core.network.messages.CloseReasonMessage;
import com.seibel.distanthorizons.core.network.messages.HelloMessage;
import com.seibel.distanthorizons.core.network.protocol.DhNetworkChannelInitializer;
import com.seibel.distanthorizons.core.network.protocol.MessageHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.logging.log4j.Logger;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class NetworkClient extends NetworkEventSource implements AutoCloseable {
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();

    private enum State {
        OPEN,
        RECONNECT,
        RECONNECT_FORCE,
        CLOSE_WAIT,
        CLOSED
    }

    private static final int FAILURE_RECONNECT_DELAY_SEC = 5;
    private static final int FAILURE_RECONNECT_ATTEMPTS = 5;

    // TODO move to config of some sort
    private final InetSocketAddress address;

    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final Bootstrap clientBootstrap = new Bootstrap()
            .group(workerGroup)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(new DhNetworkChannelInitializer(messageHandler));

    private State state;
    private Channel channel;
    private int reconnectAttempts = FAILURE_RECONNECT_ATTEMPTS;

    public NetworkClient(String host, int port) {
        this.address = new InetSocketAddress(host, port);

        registerHandlers();
        connect();
    }

    private void registerHandlers() {
        registerHandler(HelloMessage.class, (msg, ctx) -> {
            LOGGER.info("Connected to server: {}", ctx.channel().remoteAddress());
        });

        registerHandler(CloseReasonMessage.class, (msg, ctx) -> {
            LOGGER.info(msg.reason);
            state = State.CLOSE_WAIT;
        });

        registerHandler(CloseMessage.class, (msg, ctx) -> {
            LOGGER.info("Disconnected from server: {}", ctx.channel().remoteAddress());
            if (state == State.CLOSE_WAIT)
                close();
        });
    }

    private void connect() {
        LOGGER.info("Connecting to server: {}", address);
        state = State.OPEN;

        ChannelFuture connectFuture = clientBootstrap.connect(address);
        connectFuture.addListener((ChannelFuture channelFuture) -> {
            if (!channelFuture.isSuccess()) {
                LOGGER.warn("Connection failed: {}", channelFuture.cause());
                return;
            }
            channel.writeAndFlush(new HelloMessage());
        });

        channel = connectFuture.channel();
        channel.closeFuture().addListener((ChannelFuture channelFuture) -> {
            switch (state) {
                case OPEN:
                    reconnectAttempts--;
                    LOGGER.info("Reconnection attempts left: {} of {}", reconnectAttempts, FAILURE_RECONNECT_ATTEMPTS);
                    if (reconnectAttempts == 0) {
                        state = State.CLOSE_WAIT;
                        return;
                    }

                    state = State.RECONNECT;
                    workerGroup.schedule(this::connect, FAILURE_RECONNECT_DELAY_SEC, TimeUnit.SECONDS);
                    break;
                case RECONNECT_FORCE:
                    LOGGER.info("Reconnecting forcefully.");
                    reconnectAttempts = FAILURE_RECONNECT_ATTEMPTS;

                    state = State.RECONNECT;
                    workerGroup.schedule(this::connect, 0, TimeUnit.SECONDS);
                    break;
            }
        });
    }

    /** Kills the current connection, triggering auto-reconnection immediately. */
    public void reconnect() {
        state = State.RECONNECT_FORCE;
        channel.disconnect();
    }

    @Override
    public void close() {
        if (closeReason != null)
            LOGGER.error(closeReason);

        if (state == State.CLOSED) return;
        state = State.CLOSED;
        workerGroup.shutdownGracefully().syncUninterruptibly();
        channel.close().syncUninterruptibly();
    }
}
