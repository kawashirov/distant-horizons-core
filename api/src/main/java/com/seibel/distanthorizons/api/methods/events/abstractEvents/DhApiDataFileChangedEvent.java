package com.seibel.distanthorizons.api.methods.events.abstractEvents;
		
import com.seibel.distanthorizons.api.enums.EDhApiDetailLevel;
import com.seibel.distanthorizons.api.interfaces.data.IDhApiTerrainDataRepo;
import com.seibel.distanthorizons.api.methods.events.interfaces.IDhApiEvent;
import com.seibel.distanthorizons.api.objects.events.DhApiEventDefinition;
import com.seibel.distanthorizons.coreapi.events.ApiEventDefinitionHandler;

/**
 * @author James Seibel
 * @version 2023-6-19
 */
public abstract class DhApiDataFileChangedEvent implements IDhApiEvent<DhApiDataFileChangedEvent.EventParam>
{
	/** 
	 * Fired after any data files handled by Distant Horizons are modified. <br> 
	 * Note: this event may not fire immediately after a change happens, 
	 * this event is only fired after the data is saved to disk.
	 */
	public abstract void onDhDataFileChanged(EventParam input);
	
	
	//=========================//
	// internal DH API methods //
	//=========================//
	
	@Override
	public final boolean fireEvent(EventParam input)
	{
		this.onDhDataFileChanged(input);
		return false;
	}
	
	/**
	 * Note: when creating new events, make sure to bind this definition in {@link ApiEventDefinitionHandler}
	 * Otherwise a bunch of runtime errors will be thrown.
	 */
	public final static DhApiEventDefinition EVENT_DEFINITION = new DhApiEventDefinition(false, false);
	@Override
	public final DhApiEventDefinition getEventDefinition() { return EVENT_DEFINITION; }
	
	
	//==================//
	// parameter object //
	//==================//
	
	/** in order to access the modified data, please use the {@link IDhApiTerrainDataRepo}. */
	public static class EventParam
	{
		/** defines what type of data was modified */
		public final EDataType dataTypeEnum;
		
		/** See {@link EDhApiDetailLevel} for more information on detail levels. */
		public final byte detailLevel;
		public final int posX;
		public final int posZ;
		
		
		public EventParam(EDataType dataTypeEnum, byte detailLevel, int posX, int posZ) 
		{
			this.dataTypeEnum = dataTypeEnum;
			
			this.detailLevel = detailLevel;
			this.posX = posX;
			this.posZ = posZ;
		}
	}
	
	/** when in doubt, use {@link EDataType#Full}. */
	public enum EDataType
	{
		/** color data, based on the currently selected resource packs */
		Render,
		/** ID based data */
		Full;
	}
	
}