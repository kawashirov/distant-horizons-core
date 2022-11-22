package testItems.events.objects;

import testItems.events.abstractObjects.DhApiOneTimeTestEvent;

/**
 * Dummy test event for unit tests.
 *
 * @author James Seibel
 * @version 2022-11-20
 */
public class DhOneTimeTestEventHandler extends DhApiOneTimeTestEvent
{
	public Boolean eventFiredValue = null;
	
	@Override
	public void onTestEvent(Boolean input) { this.eventFiredValue = input; }
	
	@Override
	public boolean removeAfterFiring() { return false; }
	
	
	// test (non standard) methods //
	@Override
	public Boolean getTestValue() { return this.eventFiredValue; }
	
}
