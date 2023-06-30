package com.seibel.distanthorizons.core.network.messages;

import io.netty.buffer.ByteBuf;

public class CloseReasonMessage extends Message {
    public String reason;

    public CloseReasonMessage(String reason) {
        this.reason = reason;
    }

    @Override
    public void encode(ByteBuf out) {
        encodeString(reason, out);
    }

    @Override
    public void decode(ByteBuf in) {
        reason = decodeString(in);
    }
}
