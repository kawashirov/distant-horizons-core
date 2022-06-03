/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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

package com.seibel.lod.core.render;

import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.builders.lodBuilding.bufferBuilding.LodBufferBuilderFactory;
import com.seibel.lod.core.config.types.ConfigEntry;
import com.seibel.lod.core.enums.rendering.DebugMode;
import com.seibel.lod.core.api.external.apiObjects.enums.DhApiFogColorMode;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.logging.ConfigBasedLogger;
import com.seibel.lod.core.logging.ConfigBasedSpamLogger;
import com.seibel.lod.core.objects.DHBlockPos;
import com.seibel.lod.core.objects.a7.DHLevel;
import com.seibel.lod.core.objects.a7.render.RenderBufferHandler;
import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.objects.math.Vec3d;
import com.seibel.lod.core.objects.math.Vec3f;
import com.seibel.lod.core.render.objects.GLState;
import com.seibel.lod.core.render.objects.QuadElementBuffer;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.lod.core.wrapperInterfaces.misc.ILightMapWrapper;
import org.apache.logging.log4j.LogManager;
import org.lwjgl.opengl.GL32;

import java.awt.*;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * This is where all the magic happens. <br>
 * This is where LODs are draw to the world.
 * 
 * @author James Seibel
 * @version 12-12-2021
 */
public class a7LodRenderer
{
	public static final ConfigBasedLogger EVENT_LOGGER = new ConfigBasedLogger(LogManager.getLogger(a7LodRenderer.class),
			() -> Config.Client.Advanced.Debugging.DebugSwitch.logRendererBufferEvent.get());

	public static ConfigBasedSpamLogger tickLogger = new ConfigBasedSpamLogger(LogManager.getLogger(a7LodRenderer.class),
			() -> Config.Client.Advanced.Debugging.DebugSwitch.logRendererBufferEvent.get(),1);
	public static final boolean ENABLE_DRAW_LAG_SPIKE_LOGGING = false;
	public static final boolean ENABLE_DUMP_GL_STATE = true;
	public static final long DRAW_LAG_SPIKE_THRESHOLD_NS = TimeUnit.NANOSECONDS.convert(20, TimeUnit.MILLISECONDS);

	public static final boolean ENABLE_IBO = true;
	public static class LagSpikeCatcher {
		long timer = System.nanoTime();
		public LagSpikeCatcher() {}
		public void end(String source) {
			if (!ENABLE_DRAW_LAG_SPIKE_LOGGING) return;
			timer = System.nanoTime() - timer;
			if (timer> DRAW_LAG_SPIKE_THRESHOLD_NS) { //4 ms
				EVENT_LOGGER.debug("NOTE: "+source+" took "+Duration.ofNanos(timer)+"!");
			}

		}
	}
	private static final IMinecraftClientWrapper MC = SingletonHandler.get(IMinecraftClientWrapper.class);
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonHandler.get(IMinecraftRenderWrapper.class);

	public DebugMode previousDebugMode = null;
	public final DHLevel level;

	// The shader program
	LodRenderProgram shaderProgram = null;
	public QuadElementBuffer quadIBO = null;
	public boolean isSetupComplete = false;

	public a7LodRenderer(DHLevel level)
	{
		this.level = level;
	}

	public void close() {
		cleanup();
	}

	public void drawLODs(Mat4f baseModelViewMatrix, Mat4f baseProjectionMatrix, float partialTicks, IProfilerWrapper profiler)
	{
		//=================================//
		// determine if LODs should render //
		//=================================//
		if (MC_RENDER.playerHasBlindnessEffect())
		{
			// if the player is blind, don't render LODs,
			// and don't change minecraft's fog
			// which blindness relies on.
			return;
		}
		if (MC_RENDER.getLightmapWrapper() == null)
			return;

		// get MC's shader program
		// Save all MC render state
		LagSpikeCatcher drawSaveGLState = new LagSpikeCatcher();
		GLState currentState = new GLState();
		if (ENABLE_DUMP_GL_STATE) {
			tickLogger.debug("Saving GL state: {}", currentState);
		}
		drawSaveGLState.end("drawSaveGLState");

		GLProxy glProxy = GLProxy.getInstance();
		if (Config.Client.Graphics.FogQuality.disableVanillaFog.get())
			MC_RENDER.tryDisableVanillaFog();

		// The Buffer manager
		RenderBufferHandler bufferHandler = level.renderBufferHandler;
		
		//===================//
		// draw params setup //
		//===================//
		
		profiler.push("LOD draw setup");
		/*---------Set GL State--------*/
		// Make sure to unbind current VBO so we don't mess up vanilla settings
		//GL32.glBindFramebuffer(GL32.GL_FRAMEBUFFER, MC_RENDER.getTargetFrameBuffer());
		GL32.glViewport(0,0, MC_RENDER.getTargetFrameBufferViewportWidth(), MC_RENDER.getTargetFrameBufferViewportHeight());
		GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, 0);
		// set the required open GL settings
		ConfigEntry<DebugMode> debugModeConfig = Config.Client.Advanced.Debugging.debugMode;
		if (debugModeConfig.get() == DebugMode.SHOW_DETAIL_WIREFRAME
			|| debugModeConfig.get() == DebugMode.SHOW_GENMODE_WIREFRAME
			|| debugModeConfig.get() == DebugMode.SHOW_WIREFRAME
			|| debugModeConfig.get() == DebugMode.SHOW_OVERLAPPING_QUADS_WIREFRAME) {
			GL32.glPolygonMode(GL32.GL_FRONT_AND_BACK, GL32.GL_LINE);
			//GL32.glDisable(GL32.GL_CULL_FACE);
		}
		else {
			GL32.glPolygonMode(GL32.GL_FRONT_AND_BACK, GL32.GL_FILL);
			GL32.glEnable(GL32.GL_CULL_FACE);
		}
		GL32.glEnable(GL32.GL_DEPTH_TEST);
		// GL32.glDisable(GL32.GL_DEPTH_TEST);
		GL32.glDepthFunc(GL32.GL_LESS);
		// TODO: enable for transparent rendering
		// GL32.glBlendFunc(GL32.GL_SRC_ALPHA, GL32.GL_ONE_MINUS_SRC_ALPHA);
		// GL32.glEnable(GL32.GL_BLEND);
		GL32.glDisable(GL32.GL_BLEND);
		GL32.glClear(GL32.GL_DEPTH_BUFFER_BIT);

		/*---------Bind required objects--------*/
		// Setup LodRenderProgram and the LightmapTexture if it has not yet been done
		// also binds LightmapTexture, VAO, and ShaderProgram
		if (!isSetupComplete) {
			setup();
		} else {
			LodFogConfig newConfig = shaderProgram.isShaderUsable();
			if (newConfig != null) {
				shaderProgram.free();
				shaderProgram = new LodRenderProgram(newConfig);
			}
			shaderProgram.bind();
		}
		GL32.glActiveTexture(GL32.GL_TEXTURE0);
		//LightmapTexture lightmapTexture = new LightmapTexture();
		
		/*---------Get required data--------*/
		// Get the matrixs for rendering
		int vanillaBlockRenderedDistance = MC_RENDER.getRenderDistance() * LodUtil.CHUNK_WIDTH;
		int lodChunkDist = Config.Client.Graphics.Quality.lodChunkRenderDistance.get();
		int farPlaneBlockDistance;
		// required for setupFog and setupProjectionMatrix
		if (MC.getWrappedClientWorld().getDimensionType().hasCeiling())
			farPlaneBlockDistance = Math.min(lodChunkDist, LodUtil.CEILED_DIMENSION_MAX_RENDER_DISTANCE) * LodUtil.CHUNK_WIDTH;
		else
			farPlaneBlockDistance = lodChunkDist * LodUtil.CHUNK_WIDTH;

		Mat4f combinedMatrix = createCombinedMatrix(baseProjectionMatrix, baseModelViewMatrix,
				vanillaBlockRenderedDistance, farPlaneBlockDistance, partialTicks);
		
		/*---------Fill uniform data--------*/
		// Fill the uniform data. Note: GL33.GL_TEXTURE0 == texture bindpoint 0
		shaderProgram.fillUniformData(combinedMatrix,
				MC_RENDER.isFogStateSpecial() ? getSpecialFogColor(partialTicks) : getFogColor(partialTicks),
				0, MC.getWrappedClientWorld().getHeight(), MC.getWrappedClientWorld().getMinHeight(), farPlaneBlockDistance,
				vanillaBlockRenderedDistance, MC_RENDER.isFogStateSpecial());

		// Note: Since lightmapTexture is changing every frame, it's faster to recreate it than to reuse the old one.
		ILightMapWrapper lightmap = MC_RENDER.getLightmapWrapper();
		lightmap.bind();
		if (ENABLE_IBO) quadIBO.bind();
		//lightmapTexture.fillData(MC_RENDER.getLightmapTextureWidth(), MC_RENDER.getLightmapTextureHeight(), MC_RENDER.getLightmapPixels());
		//GL32.glEnable( GL32.GL_POLYGON_OFFSET_FILL );
		//GL32.glPolygonOffset( 1f, 1f );

		//===========//
		// rendering //
		//===========//
		profiler.popPush("LOD draw");
		LagSpikeCatcher draw = new LagSpikeCatcher();

		boolean cullingDisabled = Config.Client.Graphics.AdvancedGraphics.disableDirectionalCulling.get();
		Vec3d cameraPos = MC_RENDER.getCameraExactPosition();
		DHBlockPos cameraBlockPos = MC_RENDER.getCameraBlockPosition();
		Vec3f cameraDir = MC_RENDER.getLookAtVector();
		int drawCount = 0;

		//TODO: Directional culling
		bufferHandler.render(shaderProgram);

		//if (drawCall==0)
		//	tickLogger.info("DrawCall Count: {}", drawCount);
		
		//================//
		// render cleanup //
		//================//
		draw.end("LodDraw");
		profiler.popPush("LOD cleanup");
		LagSpikeCatcher drawCleanup = new LagSpikeCatcher();
		lightmap.unbind();
		if (ENABLE_IBO) quadIBO.unbind();
		
		GL32.glBindBuffer(GL32.GL_ARRAY_BUFFER, 0);

		shaderProgram.unbind();
		//lightmapTexture.free();
		GL32.glClear(GL32.GL_DEPTH_BUFFER_BIT);

		currentState.restore();
		drawCleanup.end("LodDrawCleanup");

		// end of internal LOD profiling
		profiler.pop();
		tickLogger.incLogTries();
	}
	
	//=================//
	// Setup Functions //
	//=================//
	
	/** Setup all render objects - REQUIRES to be in render thread */
	private void setup() {
		if (isSetupComplete) {
			EVENT_LOGGER.warn("Renderer setup called but it has already completed setup!");
			return;
		}
		if (!GLProxy.hasInstance()) {
			EVENT_LOGGER.warn("Renderer setup called but GLProxy has not yet been setup!");
			return;
		}

		EVENT_LOGGER.info("Setting up renderer");
		isSetupComplete = true;
		shaderProgram = new LodRenderProgram(LodFogConfig.generateFogConfig());
		if (ENABLE_IBO) {
			quadIBO = new QuadElementBuffer();
			quadIBO.reserve(LodBufferBuilderFactory.MAX_QUADS_PER_BUFFER);
		}
		EVENT_LOGGER.info("Renderer setup complete");
	}

	private Color getFogColor(float partialTicks)
	{
		Color fogColor;
		
		if (Config.Client.Graphics.FogQuality.fogColorMode.get() == DhApiFogColorMode.USE_SKY_COLOR)
			fogColor = MC_RENDER.getSkyColor();
		else
			fogColor = MC_RENDER.getFogColor(partialTicks);
		
		return fogColor;
	}
	private Color getSpecialFogColor(float partialTicks)
	{
		return MC_RENDER.getSpecialFogColor(partialTicks);
	}

	private static float calculateNearClipPlane(float distance, float partialTicks) {
		double fov = MC_RENDER.getFov(partialTicks);
		double aspectRatio = (double)MC_RENDER.getScreenWidth()/MC_RENDER.getScreenHeight();
		return (float) (distance
						/ Math.sqrt(1d + LodUtil.pow2(Math.tan(fov/180d*Math.PI/2d))
						* (LodUtil.pow2(aspectRatio) + 1d)));
	}

	/**
	 * create and return a new projection matrix based on MC's projection matrix
	 * @param projMat this is Minecraft's current projection matrix
	 * @param modelMat this is Minecraft's current model matrix
	 * @param vanillaBlockRenderedDistance Minecraft's vanilla far plane distance
	 */
	private static Mat4f createCombinedMatrix(Mat4f projMat, Mat4f modelMat, float vanillaBlockRenderedDistance,
											  int farPlaneBlockDistance, float partialTicks)
	{
		//Create a copy of the current matrix, so the current matrix isn't modified.
		Mat4f lodProj = projMat.copy();

		float nearClipPlane;
		if (Config.Client.Advanced.lodOnlyMode.get()) {
			nearClipPlane = 0.1f;
		} else if (Config.Client.Graphics.AdvancedGraphics.useExtendedNearClipPlane.get()) {
			nearClipPlane = Math.min((vanillaBlockRenderedDistance-16f),8f*16f);
		} else {
			nearClipPlane = 16f;
		}

		//Set new far and near clip plane values.
		lodProj.setClipPlanes(
				calculateNearClipPlane(nearClipPlane, partialTicks),
				(float)((farPlaneBlockDistance+LodUtil.REGION_WIDTH) * Math.sqrt(2)));

		lodProj.multiply(modelMat);

		return lodProj;
	}
	
	//======================//
	// Cleanup Functions    //
	//======================//

	/** cleanup and free all render objects. REQUIRES to be in render thread
	 *  (Many objects are Native, outside of JVM, and need manual cleanup)  */ 
	private void cleanup() {
		if (!isSetupComplete) {
			EVENT_LOGGER.warn("Renderer cleanup called but Renderer has not completed setup!");
			return;
		}
		if (!GLProxy.hasInstance()) {
			EVENT_LOGGER.warn("Renderer Cleanup called but the GLProxy has never been inited!");
			return;
		}
		isSetupComplete = false;
		EVENT_LOGGER.info("Renderer Cleanup Started");
		shaderProgram.free();
		if (quadIBO != null) quadIBO.destroy(false);
		EVENT_LOGGER.info("Renderer Cleanup Complete");
	}
}
