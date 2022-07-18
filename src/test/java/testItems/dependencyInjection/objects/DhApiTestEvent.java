package testItems.dependencyInjection.objects;

import com.seibel.lod.core.api.implementation.interfaces.events.IDhApiEvent;

/**
 * A dummy event implementation used for unit testing.
 *
 * @author James Seibel
 * @version 2022-7-16
 */
public abstract class DhApiTestEvent implements IDhApiEvent<Boolean>
{
	/**
	 * Test event.
	 *
	 * @param input
	 * @return whether the event should be canceled or not.
	 */
	public abstract boolean test(Boolean input);
	
	/**
	 * Normal DhApiEvent classes shouldn't have any other methods like this.
	 * This is just for testing.
	 */
	public abstract Boolean getTestValue();
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final boolean onEvent(Boolean input)
	{
		return test(input);
	}
	
	@Override
	public final boolean getCancelable() { return true; }
}