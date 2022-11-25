package testItems.events.abstractObjects;

import com.seibel.lod.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.lod.api.objects.events.DhApiEventDefinition;

/**
 * A dummy event implementation used for unit testing.
 *
 * @author James Seibel
 * @version 2022-11-20
 */
public abstract class DhApiOneTimeTestEvent implements IDhApiEvent<Boolean>
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
		this.onTestEvent(input);
		return input;
	}
	
	public final static DhApiEventDefinition EVENT_DEFINITION = new DhApiEventDefinition(false, true);
	@Override
	public final DhApiEventDefinition getEventDefinition() { return EVENT_DEFINITION; }
	
}