package com.seibel.lod.api.objects;

/**
 * Allows for more descriptive non-critical failure states.
 *
 * @author James Seibel
 * @version 2022-8-15
 */
public class DhApiResult
{
	/** True if the action succeeded, false otherwise. */
	public final boolean success;
	
	/** If the action failed this contains the reason as to why. */
	public final String errorMessage;
	
	
	private DhApiResult(boolean newSuccess, String newErrorMessage)
	{
		this.success = newSuccess;
		this.errorMessage = newErrorMessage;
	}
	
	
	
	public static DhApiResult createSuccess() { return new DhApiResult(true, ""); }
	public static DhApiResult createSuccess(String message) { return new DhApiResult(true, message); }
	
	public static DhApiResult createFail() { return new DhApiResult(false, ""); }
	public static DhApiResult createFail(String message) { return new DhApiResult(false, message); }
	
}
