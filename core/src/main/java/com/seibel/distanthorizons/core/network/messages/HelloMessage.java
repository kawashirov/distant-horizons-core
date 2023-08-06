package com.seibel.distanthorizons.core.network.messages;

import com.seibel.distanthorizons.core.network.protocol.INetworkMessage;
import com.seibel.distanthorizons.coreapi.ModInfo;
//import io.netty.buffer.ByteBuf;

public class HelloMessage implements INetworkMessage 
{
    public int version = ModInfo.PROTOCOL_VERSION;
	
	
	
//    @Override
//    public void encode(ByteBuf out) { out.writeInt(this.version); }
//	
//    @Override
//    public void decode(ByteBuf in) { this.version = in.readInt(); }
	
}
