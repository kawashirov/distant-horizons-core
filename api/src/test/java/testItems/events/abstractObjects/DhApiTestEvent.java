package testItems.events.abstractObjects;

import com.seibel.lod.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.lod.api.objects.events.DhApiEventDefinition;
import com.seibel.lod.core.events.ApiEventDefinitionHandler;

/**
 * A dummy event implementation used for unit testing.
 *
 * @author James Seibel
 * @version 2022-11-20
 */
public abstract class DhApiTestEvent implements IDhApiEvent<Boolean>
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
	
	public static boolean firstTimeSetupComplete = false;
	public DhApiTestEvent()
	{
		if (!firstTimeSetupComplete)
		{
			firstTimeSetupComplete = true;
			ApiEventDefinitionHandler.setEventDefinition(DhApiTestEvent.class, new DhApiEventDefinition(false, false));
		}
	}
	
	@Override
	public final DhApiEventDefinition getEventDefinition() { return ApiEventDefinitionHandler.getEventDefinition(DhApiTestEvent.class); }
	
}