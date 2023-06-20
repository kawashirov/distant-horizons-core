package com.seibel.distanthorizons.core.level;

import com.seibel.distanthorizons.core.dataObjects.fullData.accessor.ChunkSizedFullDataAccessor;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.file.fullDatafile.IFullDataSourceProvider;
import com.seibel.distanthorizons.core.render.renderer.DebugRenderer;
import com.seibel.distanthorizons.core.file.structure.AbstractSaveStructure;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.DhBlockPos;
import com.seibel.distanthorizons.core.pos.DhBlockPos2D;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.world.IServerLevelWrapper;
import com.seibel.distanthorizons.coreapi.util.math.Mat4f;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

/** The level used on a singleplayer world */
public class DhClientServerLevel extends DhLevel implements IDhClientLevel, IDhServerLevel
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	private static final IMinecraftClientWrapper MC_CLIENT = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);

	public final ServerLevelModule serverside;
	public final ClientLevelModule clientside;

	public IClientLevelWrapper clientLevelWrapper;
	
	public DhClientServerLevel(AbstractSaveStructure saveStructure, IServerLevelWrapper serverLevelWrapper)
	{
		if (saveStructure.getFullDataFolder(serverLevelWrapper).mkdirs())
		{
			LOGGER.warn("unable to create data folder.");
		}
		serverside = new ServerLevelModule(this, serverLevelWrapper, saveStructure);
		clientside = new ClientLevelModule(this);
		LOGGER.info("Started "+DhClientServerLevel.class.getSimpleName()+" for "+ serverLevelWrapper +" with saves at "+saveStructure);
	}

	//==============//
	// tick methods //
	//==============//
	
	@Override
	public void clientTick()
	{
		clientside.clientTick();
	}

	@Override
	public void render(Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks, IProfilerWrapper profiler) {
		clientside.render(mcModelViewMatrix, mcProjectionMatrix, partialTicks, profiler);
	}

	@Override
	public void serverTick()
	{
		chunkToLodBuilder.tick();
	}
	
	@Override
	public void doWorldGen()
	{
		serverside.worldGeneratorEnabledConfig.pollNewValue();
		boolean shouldDoWorldGen = serverside.worldGeneratorEnabledConfig.get() && clientside.isRendering();
		boolean isWorldGenRunning = serverside.isWorldGenRunning();
		if (shouldDoWorldGen && !isWorldGenRunning)
		{
			// start world gen
			serverside.startWorldGen();
		}
		else if (!shouldDoWorldGen && isWorldGenRunning)
		{
			// stop world gen
			serverside.stopWorldGen();
		}

		if (serverside.isWorldGenRunning())
		{
			serverside.worldGenTick(new DhBlockPos2D(MC_CLIENT.getPlayerBlockPos()));
		}
	}

	//========//
	// render //
	//========//
	
	public void startRenderer(IClientLevelWrapper clientLevel)
	{
		clientside.startRenderer();
	}
	
	public void stopRenderer()
	{
		clientside.stopRenderer();
		clientLevelWrapper = null;
	}

	//================//
	// level handling //
	//================//
	
	@Override //FIXME this can fail if the clientLevel isn't available yet, maybe in that case we could return -1 and handle it upstream?
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
	public IClientLevelWrapper getClientLevelWrapper() { return serverside.levelWrapper.tryGetClientLevelWrapper(); }

	@Override
	public void clearRenderCache() {
		clientside.clearRenderCache();
	}

	@Override
	public IServerLevelWrapper getServerLevelWrapper() { return serverside.levelWrapper; }
	@Override
	public ILevelWrapper getLevelWrapper() { return getServerLevelWrapper(); }

	@Override
	public IFullDataSourceProvider getFileHandler() {
		return serverside.dataFileHandler;
	}

	@Override
	public AbstractSaveStructure getSaveStructure() {
		return serverside.saveStructure;
	}

	@Override
	public void saveWrites(ChunkSizedFullDataAccessor data) {
		clientside.saveWrites(data);
	}

	@Override
	public int getMinY() { return getLevelWrapper().getMinHeight(); }

	@Override
	public CompletableFuture<Void> saveAsync() {
		return CompletableFuture.allOf(clientside.saveAsync(), getFileHandler().flushAndSave());
	}

	//===============//
	// data handling //
	//===============//
	
	@Override
	public void close()
	{
		clientside.close();
		super.close();
		serverside.close();
		LOGGER.info("Closed "+this.getClass().getSimpleName()+" for "+this.getServerLevelWrapper());
	}

	@Override 
	public void onWorldGenTaskComplete(DhSectionPos pos)
	{
		if (pos.sectionDetailLevel == DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL)
			DebugRenderer.makeParticle(
					new DebugRenderer.BoxParticle(
							new DebugRenderer.Box(pos, 0, 256f, 0.09f, Color.red),
							0.5, 512f
					)
			);
		clientside.reloadPos(pos);
	}

	@Override
	public void dumpRamUsage() {

	}
}
