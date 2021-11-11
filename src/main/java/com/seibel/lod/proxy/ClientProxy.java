/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.proxy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL15;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.seibel.lod.builders.bufferBuilding.LodBufferBuilder;
import com.seibel.lod.builders.lodBuilding.LodBuilder;
import com.seibel.lod.builders.worldGeneration.LodGenWorker;
import com.seibel.lod.builders.worldGeneration.LodWorldGenerator;
import com.seibel.lod.config.LodConfig;
import com.seibel.lod.enums.DistanceGenerationMode;
import com.seibel.lod.objects.LodDimension;
import com.seibel.lod.objects.LodWorld;
import com.seibel.lod.objects.RegionPos;
import com.seibel.lod.render.LodRenderer;
import com.seibel.lod.util.DataPointUtil;
import com.seibel.lod.util.DetailDistanceUtil;
import com.seibel.lod.util.LodUtil;
import com.seibel.lod.util.ThreadMapUtil;
import com.seibel.lod.wrappers.MinecraftWrapper;
import com.seibel.lod.wrappers.Chunk.ChunkWrapper;

import net.minecraft.profiler.IProfiler;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * This handles all events sent to the client,
 * and is the starting point for most of the mod.
 * @author James_Seibel
 * @version 11-8-2021
 */
public class ClientProxy
{
	public static final Logger LOGGER = LogManager.getLogger("LOD");
	
	/**
	 * there is some setup that should only happen once,
	 * once this is true that setup has completed
	 */
	private boolean firstTimeSetupComplete = false;
	
	private static final LodWorld lodWorld = new LodWorld();
	private static final LodBuilder lodBuilder = new LodBuilder();
	private static final LodBufferBuilder lodBufferBuilder = new LodBufferBuilder();
	private static LodRenderer renderer = new LodRenderer(lodBufferBuilder);
	private static final LodWorldGenerator lodWorldGenerator = LodWorldGenerator.INSTANCE;
	
	private boolean configOverrideReminderPrinted = false;
	
	private final MinecraftWrapper mc = MinecraftWrapper.INSTANCE;
	
	
	/** This is used to determine if the LODs should be regenerated */
	public static int previousChunkRenderDistance = 0;
	/** This is used to determine if the LODs should be regenerated */
	public static int previousLodRenderDistance = 0;
	
	/**
	 * can be set if we want to recalculate variables related
	 * to the LOD view distance
	 */
	private boolean recalculateWidths = false;
	
	public ClientProxy()
	{
		
	}
	
	
	//==============//
	// render event //
	//==============//
	
	/** Do any setup that is required to draw LODs and then tell the LodRenderer to draw. */
	public void renderLods(MatrixStack mcModelViewMatrix, float partialTicks)
	{
		// comment out when creating a release
		// applyConfigOverrides();
		
		// clear any out of date objects
		mc.clearFrameObjectCache();
		
		try
		{
			// only run the first time setup once
			if (!firstTimeSetupComplete)
				firstFrameSetup();
			
			
			if (mc.getPlayer() == null || lodWorld.getIsWorldNotLoaded())
				return;
			
			LodDimension lodDim = lodWorld.getLodDimension(mc.getCurrentDimension());
			if (lodDim == null)
				return;
			
			DetailDistanceUtil.updateSettings();
			viewDistanceChangedEvent();
			playerMoveEvent(lodDim);
			
			lodDim.cutRegionNodesAsync((int) mc.getPlayer().getX(), (int) mc.getPlayer().getZ());
			lodDim.expandOrLoadRegionsAsync((int) mc.getPlayer().getX(), (int) mc.getPlayer().getZ());
			
			
			// get the default projection matrix, so we can
			// reset it after drawing the LODs
			float[] mcProjMatrixRaw = new float[16];
			GL15.glGetFloatv(GL15.GL_PROJECTION_MATRIX, mcProjMatrixRaw);
			Matrix4f mcProjectionMatrix = new Matrix4f(mcProjMatrixRaw);
			// OpenGl outputs their matrices in col,row form instead of row,col
			// (or maybe vice versa I have no idea :P)
			mcProjectionMatrix.transpose();
			
			// Note to self:
			// if "unspecified" shows up in the pie chart, it is
			// possibly because the amount of time between sections
			// is too small for the profiler to measure
			IProfiler profiler = mc.getProfiler();
			profiler.pop(); // get out of "terrain"
			profiler.push("LOD");
			
			
			renderer.drawLODs(lodDim, mcModelViewMatrix, mcProjectionMatrix, partialTicks, mc.getProfiler());
			
			profiler.pop(); // end LOD
			profiler.push("terrain"); // go back into "terrain"
			
			
			// these can't be set until after the buffers are built (in renderer.drawLODs)
			// otherwise the buffers may be set to the wrong size, or not changed at all
			previousChunkRenderDistance = mc.getRenderDistance();
			previousLodRenderDistance = LodConfig.CLIENT.graphics.qualityOption.lodChunkRenderDistance.get();
		}
		catch (Exception e)
		{
			LOGGER.error("client proxy: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/** used in a development environment to change settings on the fly */
	private void applyConfigOverrides()
	{
		// remind the developer(s) that the config override is active
		if (!configOverrideReminderPrinted)
		{
			// TODO add a send message method to the MC wrapper
//			mc.getPlayer().sendMessage(new StringTextComponent("LOD experimental build 1.5.1"), mc.getPlayer().getUUID());
//			mc.getPlayer().sendMessage(new StringTextComponent("Here be dragons!"), mc.getPlayer().getUUID());
			
			mc.getPlayer().sendMessage(new StringTextComponent("Debug settings enabled!"), mc.getPlayer().getUUID());
			configOverrideReminderPrinted = true;
		}
		
//		LodConfig.CLIENT.graphics.drawResolution.set(HorizontalResolution.BLOCK);
//		LodConfig.CLIENT.worldGenerator.generationResolution.set(HorizontalResolution.BLOCK);
		// requires a world restart?
//		LodConfig.CLIENT.worldGenerator.lodQualityMode.set(VerticalQuality.VOXEL);
		
//		LodConfig.CLIENT.graphics.fogQualityOption.fogDistance.set(FogDistance.FAR);
//		LodConfig.CLIENT.graphics.fogQualityOption.fogDrawOverride.set(FogDrawOverride.FANCY);
//		LodConfig.CLIENT.graphics.fogQualityOption.disableVanillaFog.set(true);
//		LodConfig.CLIENT.graphics.shadingMode.set(ShadingMode.DARKEN_SIDES);
		
//		LodConfig.CLIENT.graphics.advancedGraphicsOption.vanillaOverdraw.set(VanillaOverdraw.DYNAMIC);
		
//		LodConfig.CLIENT.graphics.advancedGraphicsOption.gpuUploadMethod.set(GpuUploadMethod.BUFFER_STORAGE);
		
//		LodConfig.CLIENT.worldGenerator.distanceGenerationMode.set(DistanceGenerationMode.SURFACE);
//		LodConfig.CLIENT.graphics.qualityOption.lodChunkRenderDistance.set(128);
//		LodConfig.CLIENT.worldGenerator.lodDistanceCalculatorType.set(DistanceCalculatorType.LINEAR);
//		LodConfig.CLIENT.worldGenerator.allowUnstableFeatureGeneration.set(false);
		
//		LodConfig.CLIENT.buffers.rebuildTimes.set(BufferRebuildTimes.FREQUENT);
		
		LodConfig.CLIENT.advancedModOptions.debugging.enableDebugKeybindings.set(true);
//		LodConfig.CLIENT.debugging.debugMode.set(DebugMode.SHOW_DETAIL);
	}
	
	
	//==============//
	// forge events //
	//==============//
	
	@SubscribeEvent
	public void serverTickEvent(TickEvent.ServerTickEvent event)
	{
		if (mc.getPlayer() == null || lodWorld.getIsWorldNotLoaded())
			return;
		
		LodDimension lodDim = lodWorld.getLodDimension(mc.getPlayer().level.dimensionType());
		if (lodDim == null)
			return;
		
		lodWorldGenerator.queueGenerationRequests(lodDim, renderer, lodBuilder);
	}
	
	@SubscribeEvent
	public void chunkLoadEvent(ChunkEvent.Load event)
	{
		lodBuilder.generateLodNodeAsync(new ChunkWrapper(event.getChunk()), lodWorld, event.getWorld(), DistanceGenerationMode.SERVER);
	}
	
	@SubscribeEvent
	public void worldSaveEvent(WorldEvent.Save event)
	{
		lodWorld.saveAllDimensions();
	}
	
	/** This is also called when a new dimension loads */
	@SubscribeEvent
	public void worldLoadEvent(WorldEvent.Load event)
	{
		DataPointUtil.worldHeight = event.getWorld().getHeight();
		//LodNodeGenWorker.restartExecutorService();
		//ThreadMapUtil.clearMaps();
		
		// the player just loaded a new world/dimension
		lodWorld.selectWorld(LodUtil.getWorldID(event.getWorld()));
		
		// make sure the correct LODs are being rendered
		// (if this isn't done the previous world's LODs may be drawn)
		renderer.regenerateLODsNextFrame();
	}
	
	@SubscribeEvent
	public void worldUnloadEvent(WorldEvent.Unload event)
	{
		// the player just unloaded a world/dimension
		ThreadMapUtil.clearMaps();
		
		
		if (mc.getConnection().getLevel() == null)
		{
			// the player just left the server
			
			// TODO should "resetMod()" be called here? -James
			
			// if this isn't done unfinished tasks may be left in the queue
			// preventing new LodChunks form being generated
			//LodNodeGenWorker.restartExecutorService(); // TODO why was this commented out? -James
			//ThreadMapUtil.clearMaps();
			
			LodWorldGenerator.INSTANCE.numberOfChunksWaitingToGenerate.set(0);
			lodWorld.deselectWorld();
			
			
			// prevent issues related to the buffer builder
			// breaking when changing worlds.
			renderer.destroyBuffers();
			recalculateWidths = true;
			renderer = new LodRenderer(lodBufferBuilder);
			
			
			// make sure the nulled objects are freed.
			// (this prevents an out of memory error when
			// changing worlds)
			System.gc();
		}
	}
	
	@SubscribeEvent
	public void blockChangeEvent(BlockEvent event)
	{
		if (event.getClass() == BlockEvent.BreakEvent.class ||
				event.getClass() == BlockEvent.EntityPlaceEvent.class ||
				event.getClass() == BlockEvent.EntityMultiPlaceEvent.class ||
				event.getClass() == BlockEvent.FluidPlaceBlockEvent.class ||
				event.getClass() == BlockEvent.PortalSpawnEvent.class)
		{
			// recreate the LOD where the blocks were changed
			lodBuilder.generateLodNodeAsync(new ChunkWrapper(event.getWorld().getChunk(event.getPos())), lodWorld, event.getWorld());
		}
	}
	
	@SubscribeEvent
	public void onKeyInput(InputEvent.KeyInputEvent event)
	{
		if (LodConfig.CLIENT.advancedModOptions.debugging.enableDebugKeybindings.get()
				&& event.getKey() == GLFW.GLFW_KEY_F4 && event.getAction() == GLFW.GLFW_PRESS)
		{
			LodConfig.CLIENT.advancedModOptions.debugging.debugMode.set(LodConfig.CLIENT.advancedModOptions.debugging.debugMode.get().getNext());
		}
		
		if (LodConfig.CLIENT.advancedModOptions.debugging.enableDebugKeybindings.get()
				&& event.getKey() == GLFW.GLFW_KEY_F6 && event.getAction() == GLFW.GLFW_PRESS)
		{
			LodConfig.CLIENT.advancedModOptions.debugging.drawLods.set(!LodConfig.CLIENT.advancedModOptions.debugging.drawLods.get());
		}
	}
	
	
	
	
	//============//
	// LOD events //
	//============//
	
	/** Re-centers the given LodDimension if it needs to be. */
	private void playerMoveEvent(LodDimension lodDim)
	{
		// make sure the dimension is centered
		RegionPos playerRegionPos = new RegionPos(mc.getPlayerBlockPos());
		RegionPos worldRegionOffset = new RegionPos(playerRegionPos.x - lodDim.getCenterRegionPosX(), playerRegionPos.z - lodDim.getCenterRegionPosZ());
		if (worldRegionOffset.x != 0 || worldRegionOffset.z != 0)
		{
			lodWorld.saveAllDimensions();
			lodDim.move(worldRegionOffset);
			//LOGGER.info("offset: " + worldRegionOffset.x + "," + worldRegionOffset.z + "\t center: " + lodDim.getCenterX() + "," + lodDim.getCenterZ());
		}
	}
	
	
	/** Re-sizes all LodDimensions if they need to be. */
	private void viewDistanceChangedEvent()
	{
		// calculate how wide the dimension(s) should be in regions
		int chunksWide;
		if (mc.getClientLevel().dimensionType().hasCeiling())
			chunksWide = Math.min(LodConfig.CLIENT.graphics.qualityOption.lodChunkRenderDistance.get(), LodUtil.CEILED_DIMENSION_MAX_RENDER_DISTANCE) * 2 + 1;
		else
			chunksWide = LodConfig.CLIENT.graphics.qualityOption.lodChunkRenderDistance.get() * 2 + 1;
		
		int newWidth = (int) Math.ceil(chunksWide / (float) LodUtil.REGION_WIDTH_IN_CHUNKS);
		// make sure we have an odd number of regions
		newWidth += (newWidth & 1) == 0 ? 1 : 2;
		
		// do the dimensions need to change in size?
		if (lodBuilder.defaultDimensionWidthInRegions != newWidth || recalculateWidths)
		{
			lodWorld.saveAllDimensions();
			
			// update the dimensions to fit the new width
			lodWorld.resizeDimensionRegionWidth(newWidth);
			lodBuilder.defaultDimensionWidthInRegions = newWidth;
			renderer.setupBuffers(lodWorld.getLodDimension(mc.getClientLevel().dimensionType()));
			
			recalculateWidths = false;
			//LOGGER.info("new dimension width in regions: " + newWidth + "\t potential: " + newWidth );
		}
		DetailDistanceUtil.updateSettings();
	}
	
	
	/** This event is called once during the first frame Minecraft renders in the world. */
	public void firstFrameSetup()
	{
		// make sure the GlProxy is created before the LodBufferBuilder needs it
		GlProxy.getInstance();
		
		firstTimeSetupComplete = true;
	}
	
	/** this method reset some static data every time we change world */
	private void resetMod()
	{
		// TODO when should this be used?
		ThreadMapUtil.clearMaps();
		LodGenWorker.restartExecutorService();
		
	}
	
	
	
	
	//================//
	// public getters //
	//================//
	
	public static LodWorld getLodWorld()
	{
		return lodWorld;
	}
	
	public static LodBuilder getLodBuilder()
	{
		return lodBuilder;
	}
	
	public static LodRenderer getRenderer()
	{
		return renderer;
	}
}
