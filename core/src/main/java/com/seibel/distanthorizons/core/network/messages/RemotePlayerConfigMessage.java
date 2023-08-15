package com.seibel.distanthorizons.core.network.messages;

import com.seibel.distanthorizons.core.network.protocol.INetworkObject;
import com.seibel.distanthorizons.core.network.protocol.INetworkMessage;
import com.seibel.distanthorizons.core.network.objects.RemotePlayer;
//import io.netty.buffer.ByteBuf;

public class RemotePlayerConfigMessage implements INetworkMessage
{
	public RemotePlayer.Payload payload;
	
	
	
	public RemotePlayerConfigMessage() { }
	public RemotePlayerConfigMessage(RemotePlayer.Payload payload) { this.payload = payload; }

//    @Override
//    public void encode(ByteBuf out) { this.payload.encode(out); }
//	
//    @Override
//    public void decode(ByteBuf in) { this.payload = INetworkObject.decode(new RemotePlayer.Payload(), in); }
	
}
