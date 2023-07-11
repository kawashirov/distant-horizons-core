package com.seibel.distanthorizons.core.network.objects;

import com.seibel.distanthorizons.core.network.protocol.INetworkObject;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.IServerPlayerWrapper;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public class RemotePlayer 
{
    public IServerPlayerWrapper serverPlayer;
    public Payload payload;
    public ChannelHandlerContext channelContext;
	
	
	
    public RemotePlayer(IServerPlayerWrapper serverPlayer) { this.serverPlayer = serverPlayer; }
	
    public static class Payload implements INetworkObject
	{
        // TODO Replace this example with useful fields, 
		//  this should include any information the server needs to know about the connected client
        public int renderDistance;
		
		
		
        @Override
        public void encode(ByteBuf out) { out.writeInt(this.renderDistance); }
		
        @Override
        public void decode(ByteBuf in) { this.renderDistance = in.readInt(); }
		
    }
	
}
