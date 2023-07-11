package com.seibel.distanthorizons.core.network.protocol;

import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

public interface INetworkObject
{
	void encode(ByteBuf out);
	
	void decode(ByteBuf in);
	
	static <T extends INetworkObject> T decode(T obj, ByteBuf inputByteBuf)
	{
		obj.decode(inputByteBuf);
		return obj;
	}
	
	static void encodeString(String inputString, ByteBuf outputByteBuf)
	{
		outputByteBuf.writeShort(inputString.length());
		outputByteBuf.writeBytes(inputString.getBytes(StandardCharsets.UTF_8));
	}
	
	static String decodeString(ByteBuf inputByteBuf)
	{
		int length = inputByteBuf.readShort();
		return inputByteBuf.readBytes(length).toString(StandardCharsets.UTF_8);
	}
	
}
