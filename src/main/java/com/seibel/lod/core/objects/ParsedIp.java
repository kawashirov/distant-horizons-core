package com.seibel.lod.core.objects;


/**
 * Represents an IP and includes a couple helper methods.
 * 
 * @author James Seibel
 * @version 3-7-2022
 */
public class ParsedIp
{
	/** can be used to find numeric IPs, IE: "192.168.1.19" */
	public static final String NUMERIC_IP_REGEX = "^[0-9]*\\.[0-9]*\\.[0-9]*\\.[0-9]*(:[0-9]*)?$";
	
	/**
	 * Can be used to find if a numeric IP is a LAN IP
	 * 
	 * Ip list source: <br>
	 * https://networkengineering.stackexchange.com/questions/5825/why-192-168-for-local-addresses
	 */
	public static final String LAN_IP_REGEX = "(10|172\\.16|192\\.168).*";
	
	
	/** Examples: "192.168.1.19", "mc.hypixel.net", or "localhost" */
	public final String ip;
	/** 
	 * null if the ip isn't numeric (IE: "mc.hypixel.net" or "localhost") <br>
	 * Example: "25586"
	 */
	public final String port;
	public final boolean isNumeric;
	
	
	/** parses a standard IP string */
	public ParsedIp(String fullIp)
	{
		fullIp = fullIp.trim();
		
		isNumeric = fullIp.matches(NUMERIC_IP_REGEX);
		if (isNumeric)
		{
			// attempt to separate the IP and the Port
			String[] list = fullIp.split(":");
			if (list.length == 2)
			{
				// IP and Port successfully separated
				ip = list[0];
				port = list[1];
			}
			else
			{
				// this IP must not have a port
				ip = fullIp;
				port = null;
			}
		}
		else
		{
			// text based IP, IE: "localhost"
			ip = fullIp;
			port = null;
		}
	}
	
	public ParsedIp(String newIp, String newPort)
	{
		ip = newIp;
		port = newPort;
		isNumeric = ip.matches(NUMERIC_IP_REGEX);
	}
	
	
	
	
	/** Returns if this IP is for a Local Area Network connection */
	public boolean isLan()
	{
		return ip.toLowerCase().equals("localhost") || ip.matches(LAN_IP_REGEX);
	}
	
	@Override
	public String toString()
	{
		return ip + 
				// only print the ":port" if a port is present
				(port != null ? (":" + port) : "");
	}
}


