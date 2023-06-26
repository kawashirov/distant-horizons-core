package com.seibel.distanthorizons.core.network.protocol;

import com.seibel.distanthorizons.core.network.messages.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MessageEncoder extends MessageToByteEncoder<Message> {
    private final MessageRegistry messageRegistry;

    public MessageEncoder(MessageRegistry messageRegistry) {
        this.messageRegistry = messageRegistry;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws IllegalArgumentException {
        out.writeShort(messageRegistry.getMessageId(msg));
        msg.encode(ctx, out);
    }
}
