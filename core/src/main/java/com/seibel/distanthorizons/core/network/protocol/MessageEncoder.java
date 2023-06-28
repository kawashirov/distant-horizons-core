package com.seibel.distanthorizons.core.network.protocol;

import com.seibel.distanthorizons.core.network.messages.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MessageEncoder extends MessageToByteEncoder<Message> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws IllegalArgumentException {
        out.writeShort(MessageRegistry.INSTANCE.getMessageId(msg));
        msg.encode(ctx, out);
    }
}
