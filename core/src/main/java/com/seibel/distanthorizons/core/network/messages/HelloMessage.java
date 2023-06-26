package com.seibel.distanthorizons.core.network.messages;

import com.seibel.distanthorizons.coreapi.ModInfo;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

// This message is critical to maintain backwards compatibility
// as it's used to receive version BEFORE everything else.
public class HelloMessage extends Message {
    public int version = ModInfo.PROTOCOL_VERSION;

    @Override
    public void encode(ByteBuf out) {
        out.writeInt(version);
    }

    @Override
    public void decode(ByteBuf in) {
        version = in.readInt();
    }

    @Override
    public void handle_Server(ChannelHandlerContext ctx) {
        // TODO Adjust message handling to client's version
    }
}
