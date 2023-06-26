package com.seibel.distanthorizons.core.network.messages;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public abstract class Message {
    public Message() { }

    public abstract void encode(ByteBuf out);
    public abstract void decode(ByteBuf in);

    public void handle_Server(ChannelHandlerContext ctx) {
        throw new UnsupportedOperationException();
    }
    public void handle_Client(ChannelHandlerContext ctx) {
        throw new UnsupportedOperationException();
    }
}

