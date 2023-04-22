package com.seibel.lod.core.level;

import com.seibel.lod.core.file.fullDatafile.FullDataFileHandler;
import com.seibel.lod.core.file.fullDatafile.IFullDataSourceProvider;
import com.seibel.lod.core.file.structure.AbstractSaveStructure;
import com.seibel.lod.core.level.states.ClientRenderState;
import com.seibel.lod.core.logging.f3.F3Screen;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhBlockPos;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicReference;

/** The level used when connected to a server */
public class DhClientLevel extends AbstractDhClientLevel implements IDhClientLevel
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	
	private final IClientLevelWrapper clientLevelWrapper;
	public final F3Screen.NestedMessage f3Message;
	
	
	public FullDataFileHandler fullDataFileHandler;
	
	public final AtomicReference<ClientRenderState> ClientRenderStateRef = new AtomicReference<>();
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public DhClientLevel(AbstractSaveStructure saveStructure, IClientLevelWrapper clientLevelWrapper)
	{
		super(saveStructure, clientLevelWrapper);
		
		this.clientLevelWrapper = clientLevelWrapper;
		this.f3Message = new F3Screen.NestedMessage(super::f3Log);
		
		
		LOGGER.info("Started DHLevel for "+this.clientLevelWrapper+" with saves at "+this.saveStructure);
	}
	
	
	
	//==============//
	// tick methods //
	//==============//
	
	@Override
	public void clientTick()
	{
		if (!this.baseClientTick())
		{
			return;
		}
		
		this.chunkToLodBuilder.tick();
	}
	
	
	
	//========//
	// render //
	//========//
	
	public void startRenderer(IClientLevelWrapper clientLevel)
	{
		LOGGER.info("Starting renderer for "+this);
		this.setAndStartRenderer();
	}
	
	
	
	//================//
	// level handling //
	//================//
	
	@Override
	public int computeBaseColor(DhBlockPos pos, IBiomeWrapper biome, IBlockStateWrapper block) { return this.clientLevelWrapper.computeBaseColor(pos, biome, block); }
	
	@Override
	public IClientLevelWrapper getClientLevelWrapper() { return this.clientLevelWrapper; }
	@Override
	public ILevelWrapper getLevelWrapper() { return this.clientLevelWrapper; }
	
	@Override
	public int getMinY() { return this.clientLevelWrapper.getMinHeight(); }
	
	
	
	//===============//
	// data handling //
	//===============//
	
	
	
	@Override
	public void close()
	{
		this.baseClose();
		LOGGER.info("Closed "+DhClientLevel.class.getSimpleName()+" for "+this.clientLevelWrapper);
	}
	
	
	
	
	//=======================//
	// misc helper functions //
	//=======================//
	
	@Override
	public void dumpRamUsage()
	{
		//TODO
	}
	
	@Override
	public IFullDataSourceProvider getFileHandler() { return this.fullDataFileHandler; }
	
	@Override
	public void clearRenderDataCache()
	{
		ClientRenderState ClientRenderState = this.ClientRenderStateRef.get();
		if (ClientRenderState != null && ClientRenderState.quadtree != null)
		{
			ClientRenderState.quadtree.clearRenderDataCache();
		}
	}
	
}
