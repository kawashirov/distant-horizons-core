package com.seibel.distanthorizons.core.network.protocol;

import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.network.messages.CloseMessage;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

@ChannelHandler.Sharable
public class MessageHandler extends SimpleChannelInboundHandler<INetworkMessage> {
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();

    private final Map<Class<? extends INetworkMessage>, List<BiConsumer<INetworkMessage, ChannelHandlerContext>>> handlers = new HashMap<>();

    public <T extends INetworkMessage> void registerHandler(Class<T> clazz, BiConsumer<T, ChannelHandlerContext> handler) {
        handlers.computeIfAbsent(clazz, k -> new LinkedList<>())
                .add((BiConsumer<INetworkMessage, ChannelHandlerContext>) handler);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, INetworkMessage msg) {
        List<BiConsumer<INetworkMessage, ChannelHandlerContext>> handlerList = handlers.get(msg.getClass());
        if (handlerList == null) {
            LOGGER.warn("Unhandled message type: {}", msg.getClass().getSimpleName());
            return;
        }

        for (BiConsumer<INetworkMessage, ChannelHandlerContext> handler : handlerList)
            handler.accept(msg, ctx);
    }

    @Override
    public void channelInactive(@NotNull ChannelHandlerContext ctx) {
        channelRead0(ctx, new CloseMessage());
    }
}
