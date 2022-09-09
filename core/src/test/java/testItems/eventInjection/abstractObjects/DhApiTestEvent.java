package testItems.eventInjection.abstractObjects;

import com.seibel.lod.core.api.external.coreImplementations.interfaces.events.ICoreDhApiEvent;

/**
 * A dummy event implementation used for unit testing.
 *
 * @author James Seibel
 * @version 2022-9-8
 */
public abstract class DhApiTestEvent implements ICoreDhApiEvent<Boolean>
{
	/**
	 * Test event.
	 *
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
	public final boolean fireEvent(Boolean input) { return test(input); }
	
	@Override
	public final boolean getCancelable() { return true; }
	
}