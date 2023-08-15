package com.seibel.distanthorizons.core.network.messages;

import com.seibel.distanthorizons.core.network.protocol.INetworkMessage;
//import io.netty.buffer.ByteBuf;

import java.util.UUID;

public class PlayerUUIDMessage implements INetworkMessage
{
	public UUID playerUUID;
	
	
	
	public PlayerUUIDMessage() { }
	public PlayerUUIDMessage(UUID playerUUID) { this.playerUUID = playerUUID; }

//    @Override
//    public void encode(ByteBuf out)
//	{
//        out.writeLong(this.playerUUID.getMostSignificantBits());
//        out.writeLong(this.playerUUID.getLeastSignificantBits());
//    }
//
//    @Override
//    public void decode(ByteBuf in) { this.playerUUID = new UUID(in.readLong(), in.readLong()); }
	
}
