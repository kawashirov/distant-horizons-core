package com.seibel.distanthorizons.core.network.protocol;

import com.seibel.distanthorizons.core.network.messages.Message;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class MessageHandler extends SimpleChannelInboundHandler<Message> {
    private final MessageHandlerSide side;

    public MessageHandler(MessageHandlerSide side) {
        this.side = side;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Message msg) {
        switch (side) {
            case CLIENT:
                msg.handle_Client(ctx);
                break;
            case SERVER:
                msg.handle_Server(ctx);
                break;
            default:
                throw new IllegalStateException("Invalid handler side");
        }
    }
}
