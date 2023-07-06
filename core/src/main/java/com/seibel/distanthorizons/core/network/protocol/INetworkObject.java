package com.seibel.distanthorizons.core.network.protocol;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

public interface INetworkObject {
    void encode(ByteBuf out);

    void decode(ByteBuf in);

    static <T extends INetworkObject> T decode(T o, ByteBuf in) {
        o.decode(in);
        return o;
    }

    static void encodeString(String str, ByteBuf out) {
        out.writeShort(str.length());
        out.writeBytes(str.getBytes(StandardCharsets.UTF_8));
    }

    static String decodeString(ByteBuf in) {
        int length = in.readShort();
        return in.readBytes(length).toString(StandardCharsets.UTF_8);
    }
}
