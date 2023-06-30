package com.seibel.distanthorizons.core.network.messages;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

import java.nio.charset.StandardCharsets;

public abstract class Message {
    public abstract void encode(ByteBuf out);
    public abstract void decode(ByteBuf in);

    protected void encodeString(String str, ByteBuf out) {
        out.writeShort(str.length());
        out.writeBytes(str.getBytes(StandardCharsets.UTF_8));
    }

    protected String decodeString(ByteBuf in) {
        int length = in.readShort();
        return new String(in.readBytes(length).array(), StandardCharsets.UTF_8);
    }
}

