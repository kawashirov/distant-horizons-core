package com.seibel.lod.core.level;

import com.seibel.lod.core.dataObjects.fullData.sources.ChunkSizedFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.sources.FullDataSource;
import com.seibel.lod.core.dataObjects.transformers.ChunkToLodBuilder;
import com.seibel.lod.core.file.fullDatafile.IFullDataSourceProvider;
import com.seibel.lod.core.level.states.ClientRenderState;
import com.seibel.lod.core.logging.f3.F3Screen;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.util.FileScanUtil;
import com.seibel.lod.core.file.fullDatafile.RemoteFullDataFileHandler;
import com.seibel.lod.core.pos.DhBlockPos2D;
import com.seibel.lod.core.file.structure.ClientOnlySaveStructure;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhBlockPos;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.math.Mat4f;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class DhClientLevel implements IDhClientLevel
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	
	public final ClientOnlySaveStructure saveStructure;
	public final RemoteFullDataFileHandler fullDataFileHandler;
	public final ChunkToLodBuilder chunkToLodBuilder;
	public final IClientLevelWrapper clientLevel;
	public F3Screen.NestedMessage f3Message;
	
	public final AtomicReference<ClientRenderState> ClientRenderStateRef = new AtomicReference<>();
	
	
	
	public DhClientLevel(ClientOnlySaveStructure saveStructure, IClientLevelWrapper clientLevel)
	{
		this.clientLevel = clientLevel;
		
		this.saveStructure = saveStructure;
		saveStructure.getDataFolder(clientLevel).mkdirs();
		saveStructure.getRenderCacheFolder(clientLevel).mkdirs();
		
		this.fullDataFileHandler = new RemoteFullDataFileHandler(this, saveStructure.getDataFolder(clientLevel));
		FileScanUtil.scanFiles(saveStructure, clientLevel, this.fullDataFileHandler, null);
		
		this.f3Message = new F3Screen.NestedMessage(this::f3Log);
		this.chunkToLodBuilder = new ChunkToLodBuilder();
		
		
		LOGGER.info("Started DHLevel for "+clientLevel+" with saves at "+saveStructure);
	}
	
	
	
	//=======================//
	// misc helper functions //
	//=======================//
	
	/** Returns what should be displayed in Minecraft's F3 debug menu */
	private String[] f3Log()
	{
		ClientRenderState rs = this.ClientRenderStateRef.get();
		if (rs == null)
		{
			return new String[] { LodUtil.formatLog("level @ {}: Inactive", this.clientLevel.getDimensionType().getDimensionName()) };
		}
		else
		{
			return new String[] {
					LodUtil.formatLog("level @ {}: Active", this.clientLevel.getDimensionType().getDimensionName())
			};
		}
	}
	
	@Override
	public void dumpRamUsage()
	{
		//TODO
	}
	
	
	
	//==============//
	// tick methods //
	//==============//
	
	@Override
	public void clientTick()
	{
		ClientRenderState clientRenderState = this.ClientRenderStateRef.get();
		if (clientRenderState == null)
		{
			return;
		}
		
		if (clientRenderState.quadtree.blockRenderDistance != Config.Client.Graphics.Quality.lodChunkRenderDistance.get() * LodUtil.CHUNK_WIDTH)
		{
			if (!this.ClientRenderStateRef.compareAndSet(clientRenderState, null))
			{
				return; //If we fail, we'll just wait for the next tick
			}
			
			IClientLevelWrapper levelWrapper = clientRenderState.clientLevel;
			clientRenderState.closeAsync().join(); //TODO: Make it async.
			clientRenderState = new ClientRenderState(this, levelWrapper);
			if (!this.ClientRenderStateRef.compareAndSet(null, clientRenderState))
			{
				//FIXME: How to handle this?
				LOGGER.warn("Failed to set render state due to concurrency after changing view distance");
				clientRenderState.closeAsync();
				return;
			}
		}
		
		clientRenderState.quadtree.tick(new DhBlockPos2D(MC_CLIENT.getPlayerBlockPos()));
		clientRenderState.renderer.bufferHandler.update();
		
		this.chunkToLodBuilder.tick();
	}
	
	
	
	//========//
	// render //
	//========//
	
	public void startRenderer(IClientLevelWrapper clientLevel)
	{
		LOGGER.info("Starting renderer for "+this);
		ClientRenderState ClientRenderState = new ClientRenderState(this, clientLevel);
		if (!this.ClientRenderStateRef.compareAndSet(null, ClientRenderState))
		{
			LOGGER.warn("Failed to start renderer due to concurrency");
			ClientRenderState.closeAsync();
		}
	}
	
	@Override
	public void render(Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks, IProfilerWrapper profiler)
	{
		ClientRenderState ClientRenderState = this.ClientRenderStateRef.get();
		if (ClientRenderState == null)
		{
			LOGGER.error("Tried to call render() on "+this+" when renderer has not been started!");
			return;
		}
		ClientRenderState.renderer.drawLODs(mcModelViewMatrix, mcProjectionMatrix, partialTicks, profiler);
	}
	
	public void stopRenderer()
	{
		LOGGER.info("Stopping renderer for "+this);
		ClientRenderState ClientRenderState = this.ClientRenderStateRef.get();
		if (ClientRenderState == null)
		{
			LOGGER.warn("Tried to stop renderer for "+this+" when it was not started!");
			return;
		}
		
		// stop the render state
		while (!this.ClientRenderStateRef.compareAndSet(ClientRenderState, null)) // TODO why is there a while loop here?
		{
			ClientRenderState = this.ClientRenderStateRef.get();
			if (ClientRenderState == null)
			{
				return;
			}
		}
		ClientRenderState.closeAsync().join(); //TODO: Make it async.
		
	}
	
	
	
	//================//
	// level handling //
	//================//
	
	@Override
	public int computeBaseColor(DhBlockPos pos, IBiomeWrapper biome, IBlockStateWrapper block)
	{
		IClientLevelWrapper clientLevel = this.getClientLevelWrapper();
		if (clientLevel == null)
		{
			return 0;
		}
		else
		{
			return clientLevel.computeBaseColor(pos, biome, block);
		}
	}
	
	@Override
	public IClientLevelWrapper getClientLevelWrapper()
	{
		ClientRenderState ClientRenderState = this.ClientRenderStateRef.get();
		return ClientRenderState == null ? null : ClientRenderState.clientLevel;
	}
	
	@Override
	public ILevelWrapper getLevelWrapper() { return this.clientLevel; }
	
	@Override
	public int getMinY() { return this.clientLevel.getMinHeight(); }
	
	
	
	//===============//
	// data handling //
	//===============//
	
	@Override
	public void updateChunkAsync(IChunkWrapper chunk)
	{
		CompletableFuture<ChunkSizedFullDataSource> future = this.chunkToLodBuilder.tryGenerateData(chunk);
		if (future != null)
		{
			future.thenAccept(this::saveWrites);
		}
	}
	private void saveWrites(ChunkSizedFullDataSource data)
	{
		ClientRenderState ClientRenderState = this.ClientRenderStateRef.get();
		DhLodPos pos = data.getBBoxLodPos().convertToDetailLevel(FullDataSource.SECTION_SIZE_OFFSET);
		if (ClientRenderState != null)
		{
			ClientRenderState.renderSourceFileHandler.write(new DhSectionPos(pos.detailLevel, pos.x, pos.z), data);
		}
		else
		{
			this.fullDataFileHandler.write(new DhSectionPos(pos.detailLevel, pos.x, pos.z), data);
		}
	}
	
	@Override
	public CompletableFuture<Void> saveAsync()
	{
		ClientRenderState ClientRenderState = this.ClientRenderStateRef.get();
		if (ClientRenderState != null)
		{
			return ClientRenderState.renderSourceFileHandler.flushAndSave().thenCombine(this.fullDataFileHandler.flushAndSave(), (voidA, voidB) -> null);
		}
		else
		{
			return this.fullDataFileHandler.flushAndSave();
		}
	}
	
	
	
	@Override
	public void close()
	{
		ClientRenderState ClientRenderState = this.ClientRenderStateRef.get();
		if (ClientRenderState != null)
		{
			while (!this.ClientRenderStateRef.compareAndSet(ClientRenderState, null))
			{
				ClientRenderState = this.ClientRenderStateRef.get();
				if (ClientRenderState == null)
				{
					return;
				}
			}
			ClientRenderState.closeAsync().join(); //TODO: Make this async.
		}
		
		LOGGER.info("Closed DHLevel for "+this.clientLevel);
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
