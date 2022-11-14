package com.seibel.lod.core.util;

/**
 * A list of helper methods to make code easier to read. <br>
 * Specifically written because bit shifts short circuit James' brain.
 * 
 * @author James Seibel
 * @version 2022-11-6
 */
public class BitShiftUtil
{
	/** 
	 * Equivalent to: <br>
	 * 1 << value, <br>
	 * 2^value, <br>
	 * Math.pow(2, value) <br><br>
	 * 
	 * Note: Math.pow() isn't identical for large values where bits would be lost in the shift, however for medium to small values they function the same. <br><br>
	 * 
	 * Can also be used to replace bit shifts in the format: <br>
	 * multiplier << value; <br>
	 * multiplier * powerOfTwo(value);
	 */
	public static int powerOfTwo(int value) { return 1 << value; }
	
	/**
	 * Equivalent to: <br>
	 * value >> 1, <br>
	 * value / 2 <br><br>
	 * 
	 * Note: value / 2 isn't identical for negative values
	 */
	public static int half(int value) { return value >> 1; }
	
	/**
	 * Equivalent to: <br>
	 * value << 1, <br>
	 * value^2, <br>
	 * Math.pow(value, 2) <br><br>
	 * 
	 * Note: Math.pow() isn't identical for large values where bits would be lost in the shift, however for medium to small values they function the same.
	 */
	public static int square(int value) { return value << 1; }
	
	/**
	 * Equivalent to: <br>
	 * value << power, <br>
	 * value^power, <br>
	 * Math.pow(value, power) <br><br>
	 * 
	 * Note: Math.pow() isn't identical for large values where bits would be lost in the shift, however for medium to small values they function the same.
	 */
	public static int pow(int value, int power) { return value << power; }
	
}