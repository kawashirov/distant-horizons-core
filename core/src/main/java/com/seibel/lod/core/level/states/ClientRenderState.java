package com.seibel.lod.core.level.states;

import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.file.renderfile.RenderSourceFileHandler;
import com.seibel.lod.core.level.DhClientServerLevel;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.render.LodQuadTree;
import com.seibel.lod.core.render.RenderBufferHandler;
import com.seibel.lod.core.render.renderer.LodRenderer;
import com.seibel.lod.core.util.FileScanUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IClientLevelWrapper;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;

public class ClientRenderState
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	
	public final IClientLevelWrapper clientLevel;
	public final LodQuadTree quadtree;
	public final RenderSourceFileHandler renderSourceFileHandler;
	public final LodRenderer renderer;
	
	
	
	public ClientRenderState(DhClientServerLevel parent, IClientLevelWrapper clientLevel)
	{
		this.clientLevel = clientLevel;
		this.renderSourceFileHandler = new RenderSourceFileHandler(parent.dataFileHandler, parent, parent.saveStructure.getRenderCacheFolder(parent.serverLevel));
		
		this.quadtree = new LodQuadTree(parent, Config.Client.Graphics.Quality.lodChunkRenderDistance.get() * LodUtil.CHUNK_WIDTH,
				MC_CLIENT.getPlayerBlockPos().x, MC_CLIENT.getPlayerBlockPos().z, this.renderSourceFileHandler);
		
		RenderBufferHandler renderBufferHandler = new RenderBufferHandler(this.quadtree);
		FileScanUtil.scanFiles(parent.saveStructure, parent.serverLevel, null, this.renderSourceFileHandler);
		this.renderer = new LodRenderer(renderBufferHandler);
	}
	
	
	
	public CompletableFuture<Void> closeAsync()
	{
		LOGGER.info("Shutting down "+ClientRenderState.class.getSimpleName()+" async...");
		
		this.renderer.close();
		this.quadtree.close();
		return this.renderSourceFileHandler.flushAndSave();
	}
	
}
