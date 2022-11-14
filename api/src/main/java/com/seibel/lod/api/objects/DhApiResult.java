package com.seibel.lod.api.objects;

/**
 * Allows for more descriptive non-critical failure states.
 * 
 * @param <T> The payload type this result contains, can be Void if the result is just used to notify success/failure. 
 * 
 * @author James Seibel
 * @version 2022-11-12
 */
public class DhApiResult<T>
{
	/** True if the action succeeded, false otherwise. */
	public final boolean success;
	
	/** If the action failed this contains the reason as to why. */
	public final String errorMessage; // TODO rename to just "message"
	
	/** 
	 * Whatever object the API Method generated/returned. <br>
	 * Will be null/Void if this result is just used to notify success/failure. 
	 */
	public final T payload;
	
	
	
	// these constructors are private because the create... methods below are easier to understand
	private DhApiResult(boolean newSuccess, String newErrorMessage) { this(newSuccess, newErrorMessage, null); }
	private DhApiResult(boolean newSuccess, String newErrorMessage, T payload)
	{
		this.success = newSuccess;
		this.errorMessage = newErrorMessage;
		this.payload = payload;
	}
	
	
	
	public static <Pt> DhApiResult<Pt> createSuccess() { return new DhApiResult<>(true, ""); }
	public static <Pt> DhApiResult<Pt> createSuccess(String message) { return new DhApiResult<>(true, message); }
	public static <Pt> DhApiResult<Pt> createSuccess(Pt payload) { return new DhApiResult<Pt>(true, "", payload); }
	public static <Pt> DhApiResult<Pt> createSuccess(String message, Pt payload) { return new DhApiResult<Pt>(true, message, payload); }
	
	public static <Pt> DhApiResult<Pt> createFail() { return new DhApiResult<>(false, ""); }
	public static <Pt> DhApiResult<Pt> createFail(String message) { return new DhApiResult<>(false, message); }
	public static <Pt> DhApiResult<Pt> createFail(String message, Pt payload) { return new DhApiResult<Pt>(false, message, payload); }
	
}
