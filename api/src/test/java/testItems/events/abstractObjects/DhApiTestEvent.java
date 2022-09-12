package testItems.events.abstractObjects;

import com.seibel.lod.api.items.interfaces.world.IDhApiLevelWrapper;
import com.seibel.lod.api.items.objects.wrappers.DhApiLevelWrapper;
import com.seibel.lod.api.methods.events.abstractEvents.DhApiLevelLoadEvent;
import com.seibel.lod.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.lod.core.api.external.coreImplementations.objects.events.abstractEvents.CoreDhApiLevelLoadEvent;
import com.seibel.lod.core.api.external.coreImplementations.objects.events.abstractEvents.CoreDhApiTestEvent;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;

/**
 * A dummy event implementation used for unit testing.
 *
 * @author James Seibel
 * @version 2022-9-11
 */
public abstract class DhApiTestEvent
		extends CoreDhApiTestEvent
		implements IDhApiEvent<Boolean, Boolean>
{
	
	public abstract void onTestEvent(Boolean input);
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final boolean fireEvent(Boolean input)
	{
		onTestEvent(input);
		return false;
	}
	
	@Override
	public final boolean getCancelable() { return false; }
	
}