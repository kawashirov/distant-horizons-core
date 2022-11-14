package testItems.events.objects;

import testItems.events.abstractObjects.DhApiTestEvent;

/**
 * Dummy test event for unit tests.
 *
 * @author James Seibel
 * @version 2022-9-11
 */
public class DhTestEvent extends DhApiTestEvent
{
	public Boolean eventFiredValue = null;
	
	@Override
	public void onTestEvent(Boolean input)
	{
		this.eventFiredValue = input;
	}
	
	@Override
	public boolean removeAfterFiring() { return false; }
	
	
	
	// test (non standard) methods //
	@Override
	public Boolean getTestValue() { return eventFiredValue; }
	
}