package com.seibel.lod.core.api.external.shared.objects;

/**
 * Allows for more descriptive non-critical failure states.
 *
 * @author James Seibel
 * @version 2022-7-11
 */
public class DhApiResult
{
	/** True if the action succeeded, false otherwise. */
	public final boolean success;
	
	/** If the action failed this contains the reason as to why. */
	public final String errorMessage;
	
	
	public DhApiResult(boolean newSuccess, String newErrorMessage)
	{
		this.success = newSuccess;
		this.errorMessage = newErrorMessage;
	}
}
