package com.seibel.distanthorizons.core.network.protocol;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.jetbrains.annotations.NotNull;

public class DhNetworkChannelInitializer extends ChannelInitializer<SocketChannel> {
    private final MessageHandler messageHandler;

    public DhNetworkChannelInitializer(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    public void initChannel(@NotNull SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        // Encoding
        pipeline.addLast(new LengthFieldBasedFrameDecoder(Short.MAX_VALUE, 0, Short.BYTES, 0, Short.BYTES));
        pipeline.addLast(new MessageDecoder());

        // Encoder
        pipeline.addLast(new LengthFieldPrepender(Short.BYTES));
        pipeline.addLast(new MessageEncoder());

        pipeline.addLast(messageHandler);
    }
}
