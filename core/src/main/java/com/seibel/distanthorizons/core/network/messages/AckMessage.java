package com.seibel.distanthorizons.core.network.messages;

import com.seibel.distanthorizons.core.network.protocol.INetworkMessage;
import com.seibel.distanthorizons.core.network.protocol.MessageRegistry;
//import io.netty.buffer.ByteBuf;

/**
 * Simple empty response message.
 * This message is not sent automatically.
 */
public class AckMessage implements INetworkMessage
{
    public Class<? extends INetworkMessage> messageType;
	
	
	
    public AckMessage() { }
    public AckMessage(Class<? extends INetworkMessage> messageType) { this.messageType = messageType; }
	
//    @Override
//    public void encode(ByteBuf out) { out.writeInt(MessageRegistry.INSTANCE.getMessageId(this.messageType)); }
//	
//    @Override
//    public void decode(ByteBuf in) { this.messageType = MessageRegistry.INSTANCE.getMessageClassById(in.readInt()); }
	
}
