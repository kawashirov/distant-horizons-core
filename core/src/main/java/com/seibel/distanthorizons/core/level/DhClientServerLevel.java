/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2023 James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
	
	private final IServerLevelWrapper serverLevelWrapper;
	
	
	
	public DhClientServerLevel(AbstractSaveStructure saveStructure, IServerLevelWrapper serverLevelWrapper)
	{
		if (saveStructure.getFullDataFolder(serverLevelWrapper).mkdirs())
		{
			LOGGER.warn("unable to create data folder.");
		}
		this.serverLevelWrapper = serverLevelWrapper;
		this.serverside = new ServerLevelModule(this, saveStructure);
		this.clientside = new ClientLevelModule(this);
		LOGGER.info("Started " + DhClientServerLevel.class.getSimpleName() + " for " + serverLevelWrapper + " with saves at " + saveStructure);
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
	public void render(Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks, IProfilerWrapper profiler)
	{
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
		boolean isWorldGenRunning = serverside.worldGenModule.isWorldGenRunning();
		if (shouldDoWorldGen && !isWorldGenRunning)
		{
			// start world gen
			serverside.worldGenModule.startWorldGen(serverside.dataFileHandler, new ServerLevelModule.WorldGenState(this));
		}
		else if (!shouldDoWorldGen && isWorldGenRunning)
		{
			// stop world gen
			serverside.worldGenModule.stopWorldGen(serverside.dataFileHandler);
		}

		if (serverside.worldGenModule.isWorldGenRunning())
		{
			ClientLevelModule.ClientRenderState renderState = clientside.ClientRenderStateRef.get();
			if (renderState != null && renderState.quadtree != null)
			{
				serverside.dataFileHandler.removeGenRequestIf(p -> !renderState.quadtree.isSectionPosInBounds(p));
			}
			serverside.worldGenModule.worldGenTick(new DhBlockPos2D(MC_CLIENT.getPlayerBlockPos()));
		}
	}
	
	//========//
	// render //
	//========//
	
	public void startRenderer(IClientLevelWrapper clientLevel)
	{
		clientside.startRenderer();
	}
	
	public void stopRenderer() { this.clientside.stopRenderer(); }
	
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
	public IClientLevelWrapper getClientLevelWrapper() { return this.serverLevelWrapper.tryGetClientLevelWrapper(); }
	
	@Override
	public void clearRenderCache()
	{
		clientside.clearRenderCache();
	}
	
	@Override
	public IServerLevelWrapper getServerLevelWrapper() { return serverLevelWrapper; }
	@Override
	public ILevelWrapper getLevelWrapper() { return getServerLevelWrapper(); }
	
	@Override
	public IFullDataSourceProvider getFileHandler()
	{
		return serverside.dataFileHandler;
	}
	
	@Override
	public AbstractSaveStructure getSaveStructure()
	{
		return serverside.saveStructure;
	}
	
	@Override
	public void saveWrites(ChunkSizedFullDataAccessor data)
	{
		clientside.saveWrites(data);
	}
	
	@Override
	public int getMinY() { return getLevelWrapper().getMinHeight(); }
	
	@Override
	public CompletableFuture<Void> saveAsync()
	{
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
		LOGGER.info("Closed " + this.getClass().getSimpleName() + " for " + this.getServerLevelWrapper());
	}
	
	@Override
	public void onWorldGenTaskComplete(DhSectionPos pos)
	{
		//if (pos.sectionDetailLevel == DhSectionPos.SECTION_MINIMUM_DETAIL_LEVEL)
		DebugRenderer.makeParticle(
				new DebugRenderer.BoxParticle(
						new DebugRenderer.Box(pos, 128f, 156f, 0.09f, Color.red.darker()),
						0.2, 32f
				)
		);
		clientside.reloadPos(pos);
	}
	
	@Override
	public void dumpRamUsage()
	{
		
	}
	
}
