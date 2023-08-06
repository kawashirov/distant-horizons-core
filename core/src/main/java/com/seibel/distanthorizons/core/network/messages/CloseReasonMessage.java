package com.seibel.distanthorizons.core.network.messages;

import com.seibel.distanthorizons.core.network.protocol.INetworkMessage;
import com.seibel.distanthorizons.core.network.protocol.INetworkObject;
//import io.netty.buffer.ByteBuf;

public class CloseReasonMessage implements INetworkMessage
{
	public String reason;
	
	
	
	public CloseReasonMessage() { }
	public CloseReasonMessage(String reason) { this.reason = reason; }
	
//	@Override
//	public void encode(ByteBuf out) { INetworkObject.encodeString(this.reason, out); }
//	
//	@Override
//	public void decode(ByteBuf in) { this.reason = INetworkObject.decodeString(in); }
	
}
