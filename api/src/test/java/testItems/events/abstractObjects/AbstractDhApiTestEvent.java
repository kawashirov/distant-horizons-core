package testItems.events.abstractObjects;

import com.seibel.distanthorizons.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.distanthorizons.api.methods.events.sharedParameterObjects.DhApiEventParam;

/**
 * A dummy event implementation used for unit testing.
 *
 * @author James Seibel
 * @version 2023-6-23
 */
public abstract class AbstractDhApiTestEvent implements IDhApiEvent<Boolean>
{
	
	public abstract void onTestEvent(DhApiEventParam<Boolean> input);
	
	/** just used for testing */
	public abstract Boolean getTestValue();
	
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final void fireEvent(DhApiEventParam<Boolean> input) { this.onTestEvent(input); }
	
}