package testItems.events.objects;

import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;
import testItems.events.abstractObjects.AbstractDhApiRemoveAfterFireTestEvent;
import testItems.events.abstractObjects.AbstractDhApiTestEvent;

/**
 * Dummy test event for unit tests.
 *
 * @author James Seibel
 * @version 2023-6-23
 */
public class DhRemoveAfterFireTestEventHandler extends AbstractDhApiRemoveAfterFireTestEvent
{
	public Boolean eventFiredValue = null;
	
	@Override
	public void onTestEvent(DhApiEventParam<Boolean> input) { this.eventFiredValue = input.value; }
	
	
	// test (non standard) methods //
	@Override
	public Boolean getTestValue() { return this.eventFiredValue; }
	
}
