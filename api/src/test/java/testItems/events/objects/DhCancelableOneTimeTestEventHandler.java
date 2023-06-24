package testItems.events.objects;

import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiCancelableEventParam;
import testItems.events.abstractObjects.AbstractDhApiCancelableOneTimeTestEvent;

/**
 * Dummy test event for unit tests.
 *
 * @author James Seibel
 * @version 2023-6-23
 */
public class DhCancelableOneTimeTestEventHandler extends AbstractDhApiCancelableOneTimeTestEvent
{
	public Boolean eventFiredValue = null;
	
	@Override
	public void onTestEvent(DhApiCancelableEventParam<Boolean> input) { this.eventFiredValue = input.value; }
	
	
	// test (non standard) methods //
	@Override
	public Boolean getTestValue() { return this.eventFiredValue; }
	
}
