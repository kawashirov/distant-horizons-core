package com.seibel.distanthorizons.core.network.messages;

import com.seibel.distanthorizons.core.network.protocol.INetworkObject;
import com.seibel.distanthorizons.core.network.protocol.INetworkMessage;
import com.seibel.distanthorizons.core.world.DhRemotePlayer;
import io.netty.buffer.ByteBuf;

public class LodConfigMessage implements INetworkMessage {
    public DhRemotePlayer.Config config;

    public LodConfigMessage() { }
    public LodConfigMessage(DhRemotePlayer.Config config) {
        this.config = config;
    }

    @Override
    public void encode(ByteBuf out) {
        config.encode(out);
    }

    @Override
    public void decode(ByteBuf in) {
        config = INetworkObject.decode(new DhRemotePlayer.Config(), in);
    }
}
