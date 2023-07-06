package com.seibel.distanthorizons.core.network.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MessageEncoder extends MessageToByteEncoder<INetworkMessage> {
    @Override
    protected void encode(ChannelHandlerContext ctx, INetworkMessage msg, ByteBuf out) throws IllegalArgumentException {
        out.writeShort(MessageRegistry.INSTANCE.getMessageId(msg));
        msg.encode(out);
    }
}
