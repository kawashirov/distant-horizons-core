package com.seibel.distanthorizons.core.util;

import com.seibel.distanthorizons.core.network.protocol.*;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.jetbrains.annotations.NotNull;

public class NetworkUtil {
    public static ChannelInitializer<SocketChannel> getChannelInitializer(MessageHandlerSide side) {
        return new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(@NotNull SocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();

                // Encoding
                pipeline.addLast(new LengthFieldBasedFrameDecoder(Short.MAX_VALUE, 0, Short.BYTES, 0, Short.BYTES));
                pipeline.addLast(new MessageDecoder());

                // Encoder
                pipeline.addLast(new LengthFieldPrepender(Short.BYTES));
                pipeline.addLast(new MessageEncoder());

                pipeline.addLast(new MessageHandler(side));
            }
        };
    }
}
