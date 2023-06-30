package com.seibel.distanthorizons.core.network.messages;

import io.netty.buffer.ByteBuf;

import java.util.UUID;

public class PlayerIdMessage extends Message {
    public UUID playerUUID;

    public PlayerIdMessage(UUID playerUUID) {
        this.playerUUID = playerUUID;
    }

    @Override
    public void encode(ByteBuf out) {
        out.writeLong(playerUUID.getMostSignificantBits());
        out.writeLong(playerUUID.getLeastSignificantBits());
    }

    @Override
    public void decode(ByteBuf in) {
        playerUUID = new UUID(in.readLong(), in.readLong());
    }
}
