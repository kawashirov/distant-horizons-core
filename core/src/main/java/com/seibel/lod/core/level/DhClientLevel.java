package com.seibel.lod.core.level;

import com.seibel.lod.core.dataObjects.fullData.sources.ChunkSizedFullDataSource;
import com.seibel.lod.core.dataObjects.fullData.sources.FullDataSource;
import com.seibel.lod.core.dataObjects.transformers.ChunkToLodBuilder;
import com.seibel.lod.core.file.fullDatafile.IFullDataSourceProvider;
import com.seibel.lod.core.file.renderfile.RenderSourceFileHandler;
import com.seibel.lod.core.level.states.ClientRenderState;
import com.seibel.lod.core.pos.DhLodPos;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.render.LodQuadTree;
import com.seibel.lod.core.util.FileScanUtil;
import com.seibel.lod.core.file.fullDatafile.RemoteFullDataFileHandler;
import com.seibel.lod.core.pos.DhBlockPos2D;
import com.seibel.lod.core.render.RenderBufferHandler;
import com.seibel.lod.core.file.structure.ClientOnlySaveStructure;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhBlockPos;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.math.Mat4f;
import com.seibel.lod.core.render.renderer.LodRenderer;
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
	public final RemoteFullDataFileHandler dataFileHandler;
	public final ChunkToLodBuilder chunkToLodBuilder = new ChunkToLodBuilder();;
	public final IClientLevelWrapper level;
	
	public final AtomicReference<ClientRenderState> ClientRenderStateRef = new AtomicReference<>();
	
	
	
	public DhClientLevel(ClientOnlySaveStructure saveStructure, IClientLevelWrapper level)
	{
		this.saveStructure = saveStructure;
		
		saveStructure.getDataFolder(level).mkdirs();
		saveStructure.getRenderCacheFolder(level).mkdirs();
		this.dataFileHandler = new RemoteFullDataFileHandler(this, saveStructure.getDataFolder(level));
		
		this.level = level;
		FileScanUtil.scanFiles(saveStructure, level, this.dataFileHandler, null);
		LOGGER.info("Started DHLevel for "+level+" with saves at "+ saveStructure);
	}
	
	
	
	@Override
	public void dumpRamUsage()
	{
		//TODO
	}
	
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
	
	@Override
	public int computeBaseColor(DhBlockPos pos, IBiomeWrapper biome, IBlockStateWrapper block) { return 0; /* TODO */ }
	
	@Override
	public IClientLevelWrapper getClientLevelWrapper() { return this.level; }
	
	@Override
	public ILevelWrapper getLevelWrapper() { return this.level; }
	
	@Override
	public IFullDataSourceProvider getFileHandler() { return this.dataFileHandler; }
	
	@Override 
	public void clearRenderDataCache()
	{
		ClientRenderState ClientRenderState = this.ClientRenderStateRef.get();
		if (ClientRenderState != null && ClientRenderState.quadtree != null)
		{
			ClientRenderState.quadtree.clearRenderDataCache();
		}
	}
	
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
			this.dataFileHandler.write(new DhSectionPos(pos.detailLevel, pos.x, pos.z), data);
		}
	}
	
	@Override
	public int getMinY() { return this.level.getMinHeight(); }
	
	@Override
	public CompletableFuture<Void> saveAsync()
	{
		ClientRenderState ClientRenderState = this.ClientRenderStateRef.get();
		if (ClientRenderState != null)
		{
			return ClientRenderState.renderSourceFileHandler.flushAndSave().thenCombine(this.dataFileHandler.flushAndSave(), (voidA, voidB) -> null);
		}
		else
		{
			return this.dataFileHandler.flushAndSave();
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
		
		LOGGER.info("Closed DHLevel for "+this.level);
	}
	
}
