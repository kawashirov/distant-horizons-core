package com.seibel.distanthorizons.core.network.messages;

import io.netty.buffer.ByteBuf;

/**
 * This is not a "real" message, and only used as indication of disconnection.
 * To send a "disconnect reason" message, use {@link CloseReasonMessage}.
 */
public class CloseMessage extends Message {
    @Override
    public void encode(ByteBuf out) {
        throw new UnsupportedOperationException("CloseMessage is not a real message, and must not be sent.");
    }

    @Override
    public void decode(ByteBuf in) {
        throw new UnsupportedOperationException("CloseMessage is not a real message, and must not be received.");
    }
}

