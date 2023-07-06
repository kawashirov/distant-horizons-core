package com.seibel.distanthorizons.core.world;

import com.seibel.distanthorizons.core.network.protocol.INetworkObject;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IServerPlayerWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class DhRemotePlayer {
    public IServerPlayerWrapper serverPlayer;
    public Config config;
    public ChannelHandlerContext ctx;

    public DhRemotePlayer(IServerPlayerWrapper serverPlayer) {
        this.serverPlayer = serverPlayer;
    }

    public static class Config implements INetworkObject {
        // TODO Replace this example with actually useful fields
        public int renderDistance;

        @Override
        public void encode(ByteBuf out) {
            out.writeInt(renderDistance);
        }

        @Override
        public void decode(ByteBuf in) {
            renderDistance = in.readInt();
        }
    }
}
