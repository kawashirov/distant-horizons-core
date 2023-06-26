package com.seibel.distanthorizons.core.network.messageHandling;

import com.seibel.distanthorizons.core.network.messages.Message;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class MessageDecoder extends ByteToMessageDecoder {
    private MessageRegistry messageRegistry;

    public MessageDecoder(MessageRegistry messageRegistry) {
        this.messageRegistry = messageRegistry;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        Message message = messageRegistry.createMessage(in.readShort());
        message.decode(in);
        out.add(message);
    }
}
