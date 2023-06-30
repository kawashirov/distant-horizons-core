package com.seibel.distanthorizons.core.network;

import com.seibel.distanthorizons.core.network.messages.HelloMessage;
import com.seibel.distanthorizons.core.network.messages.Message;
import com.seibel.distanthorizons.core.network.protocol.MessageHandler;
import com.seibel.distanthorizons.coreapi.ModInfo;
import io.netty.channel.ChannelHandlerContext;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class NetworkEventSource implements AutoCloseable {
    protected final MessageHandler messageHandler = new MessageHandler();
    protected String closeReason = null;

    public NetworkEventSource() {
        registerHandler(HelloMessage.class, (msg, ctx) -> {
            if (msg.version != ModInfo.PROTOCOL_VERSION) {
                try {
                    close("Protocol version mismatch");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public <T extends Message> void registerHandler(Class<T> clazz, BiConsumer<T, ChannelHandlerContext> handler) {
        messageHandler.registerHandler(clazz, handler);
    }

    public void close(String reason) throws Exception {
        closeReason = reason;
        close();
    }
}
