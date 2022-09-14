package testItems.events.abstractObjects;

import com.seibel.lod.api.methods.events.interfaces.IDhApiEvent;

/**
 * A dummy event implementation used for unit testing.
 *
 * @author James Seibel
 * @version 2022-9-13
 */
public abstract class DhApiTestEvent
		implements IDhApiEvent<Boolean>
{
	
	public abstract void onTestEvent(Boolean input);
	
	/** just used for testing */
	public abstract Boolean getTestValue();
	
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final boolean fireEvent(Boolean input)
	{
		onTestEvent(input);
		return input;
	}
	
	@Override
	public final boolean getCancelable() { return false; }
	
}