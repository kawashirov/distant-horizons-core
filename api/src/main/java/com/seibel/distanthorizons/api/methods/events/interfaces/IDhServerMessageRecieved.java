package com.seibel.distanthorizons.api.methods.events.interfaces;

/**
 * 
 * @author Cailin
 */
public interface IDhServerMessageRecieved<T> extends IDhApiEvent<T>
{
	/**
	 * Triggered when a plugin message is received from the server.
	 * @param channel The name of the channel this was received on.
	 * @param message The message sent from the server.
	 */
	void serverMessageRecieved(String channel, byte[] message);
}
