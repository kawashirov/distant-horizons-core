package testItems.eventInjection.objects;

import testItems.eventInjection.abstractObjects.DhApiTestEvent;

/**
 * Dummy test event for unit tests.
 *
 * @author James Seibel
 * @version 2022-7-16
 */
public class DhTestEventAlt extends DhApiTestEvent
{
	public Boolean eventFiredValue = null;
	
	@Override
	public boolean test(Boolean cancelEvent)
	{
		this.eventFiredValue = cancelEvent;
		return cancelEvent;
	}
	
	@Override
	public Boolean getTestValue() { return this.eventFiredValue; }
	
}
