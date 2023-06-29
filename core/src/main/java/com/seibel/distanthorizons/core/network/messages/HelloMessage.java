package com.seibel.distanthorizons.core.network.messages;

import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.coreapi.ModInfo;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

public class HelloMessage extends Message {
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();

    public int version = ModInfo.PROTOCOL_VERSION;

    @Override
    public void encode(ChannelHandlerContext ctx, ByteBuf out) {
        out.writeInt(version);
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf in) {
        version = in.readInt();
    }
}
