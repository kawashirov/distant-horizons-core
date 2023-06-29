package com.seibel.distanthorizons.core.network.protocol;

import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.network.messages.HelloMessage;
import com.seibel.distanthorizons.core.network.messages.Message;
import com.seibel.distanthorizons.coreapi.ModInfo;
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
import java.util.function.Consumer;

@ChannelHandler.Sharable
public class MessageHandler extends SimpleChannelInboundHandler<Message> {
    private static final Logger LOGGER = DhLoggerBuilder.getLogger();

    private Map<Class<? extends Message>, List<BiConsumer<Message, ChannelHandlerContext>>> handlers = new HashMap<>();
    private List<Consumer<ChannelHandlerContext>> disconnectHandlers = new LinkedList<>();

    public <T extends Message> void registerHandler(Class<T> clazz, BiConsumer<T, ChannelHandlerContext> handler) {
        handlers.computeIfAbsent(clazz, k -> new LinkedList<>())
                .add((BiConsumer<Message, ChannelHandlerContext>) handler);
    }

    public void registerDisconnectHandler(Consumer<ChannelHandlerContext> handler) {
        disconnectHandlers.add(handler);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message msg) {
        List<BiConsumer<Message, ChannelHandlerContext>> handlerList = handlers.get(msg.getClass());
        if (handlerList == null) {
            LOGGER.warn("Unhandled message type: {}", msg.getClass().getSimpleName());
            return;
        }

        for (BiConsumer<Message, ChannelHandlerContext> handler : handlerList)
            handler.accept(msg, ctx);
    }

    @Override
    public void channelInactive(@NotNull ChannelHandlerContext ctx) {
        for (Consumer<ChannelHandlerContext> handler : disconnectHandlers)
            handler.accept(ctx);
    }
}
