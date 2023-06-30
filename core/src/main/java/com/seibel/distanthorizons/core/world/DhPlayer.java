package com.seibel.distanthorizons.core.world;

import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IServerPlayerWrapper;
import io.netty.channel.ChannelHandlerContext;

public class DhPlayer {
    public IServerPlayerWrapper serverPlayer;
    public Config config;
    public ChannelHandlerContext ctx;

    public DhPlayer(IServerPlayerWrapper serverPlayer) {
        this.serverPlayer = serverPlayer;
    }

    public static class Config {
        // TODO Replace this example with actually useful fields
        public int renderDistance;
    }
}
