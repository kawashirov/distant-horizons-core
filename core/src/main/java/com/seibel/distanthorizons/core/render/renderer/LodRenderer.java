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

package com.seibel.distanthorizons.core.render.renderer;

import com.seibel.distanthorizons.core.config.Config;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.logging.ConfigBasedLogger;
import com.seibel.distanthorizons.core.logging.ConfigBasedSpamLogger;
import com.seibel.distanthorizons.core.pos.DhBlockPos;
import com.seibel.distanthorizons.core.render.AbstractRenderBuffer;
import com.seibel.distanthorizons.core.render.RenderBufferHandler;
import com.seibel.distanthorizons.core.render.glObject.GLProxy;
import com.seibel.distanthorizons.core.render.glObject.GLState;
import com.seibel.distanthorizons.core.render.glObject.buffer.GLVertexBuffer;
import com.seibel.distanthorizons.core.render.glObject.buffer.QuadElementBuffer;
import com.seibel.distanthorizons.core.render.renderer.shaders.SSAOShader;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.RenderUtil;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftRenderWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IProfilerWrapper;
import com.seibel.distanthorizons.core.wrapperInterfaces.misc.ILightMapWrapper;
import com.seibel.distanthorizons.api.enums.rendering.EDebugRendering;
import com.seibel.distanthorizons.api.enums.rendering.EFogColorMode;
import com.seibel.distanthorizons.core.render.fog.LodFogConfig;
import com.seibel.distanthorizons.coreapi.util.math.Mat4f;
import com.seibel.distanthorizons.coreapi.util.math.Vec3d;
import com.seibel.distanthorizons.coreapi.util.math.Vec3f;
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
 * @version 2022-8-21
 */
public class LodRenderer
{
	public static final ConfigBasedLogger EVENT_LOGGER = new ConfigBasedLogger(LogManager.getLogger(LodRenderer.class),
			() -> Config.Client.Advanced.Logging.logRendererBufferEvent.get());

	public static ConfigBasedSpamLogger tickLogger = new ConfigBasedSpamLogger(LogManager.getLogger(LodRenderer.class),
			() -> Config.Client.Advanced.Logging.logRendererBufferEvent.get(),1);
	public static final boolean ENABLE_DRAW_LAG_SPIKE_LOGGING = false;
	public static final boolean ENABLE_DUMP_GL_STATE = true;
	public static final long DRAW_LAG_SPIKE_THRESHOLD_NS = TimeUnit.NANOSECONDS.convert(20, TimeUnit.MILLISECONDS);

	public static final boolean ENABLE_IBO = true;

	// TODO make these private, the LOD Builder can get these variables from the config itself
	public static boolean transparencyEnabled = true;
	public static boolean fakeOceanFloor = true;

	public void setupOffset(DhBlockPos pos) {
		Vec3d cam = MC_RENDER.getCameraExactPosition();
		shaderProgram.setModelPos(new Vec3f((float) (pos.x - cam.x), (float) (pos.y - cam.y), (float) (pos.z - cam.z)));
	}

	public void drawVbo(GLVertexBuffer vbo) {
		vbo.bind();
		shaderProgram.bindVertexBuffer(vbo.getId());
		GL32.glDrawElements(GL32.GL_TRIANGLES, (vbo.getVertexCount()/4)*6,
				quadIBO.getType(), 0);
		vbo.unbind();
	}
	public Vec3f getLookVector() {
		return MC_RENDER.getLookAtVector();
	}


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
	private static final IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
	private static final IMinecraftRenderWrapper MC_RENDER = SingletonInjector.INSTANCE.get(IMinecraftRenderWrapper.class);

	public EDebugRendering previousDebugMode = null;
	public final RenderBufferHandler bufferHandler;

	// The shader program
	LodRenderProgram shaderProgram = null;
	public QuadElementBuffer quadIBO = null;
	public boolean isSetupComplete = false;

	public LodRenderer(RenderBufferHandler bufferHandler)
	{
		this.bufferHandler = bufferHandler;
	}

	private boolean closeCalled = false;
	
	public void close()
	{
		if (this.closeCalled)
		{
			EVENT_LOGGER.warn("close() called twice!");
			return;
		}
		
		EVENT_LOGGER.info("Shutting down "+LodRenderer.class.getSimpleName()+"...");
		
		this.closeCalled = true;
		GLProxy.getInstance().recordOpenGlCall(this::cleanup);
		this.bufferHandler.close();
		
		EVENT_LOGGER.info("Finished shutting down "+LodRenderer.class.getSimpleName());
	}

	public void drawLODs(Mat4f baseModelViewMatrix, Mat4f baseProjectionMatrix, float partialTicks, IProfilerWrapper profiler)
	{
		if (closeCalled) {
			EVENT_LOGGER.error("drawLODs() called after close()!");
			return;
		}

		// get MC's shader program
		// Save all MC render state
		LagSpikeCatcher drawSaveGLState = new LagSpikeCatcher();
		GLState currentState = new GLState();
		if (ENABLE_DUMP_GL_STATE) {
			tickLogger.debug("Saving GL state: {}", currentState);
		}
		drawSaveGLState.end("drawSaveGLState");

		GLProxy glProxy = GLProxy.getInstance();
		if (Config.Client.Advanced.Graphics.Fog.disableVanillaFog.get())
			MC_RENDER.tryDisableVanillaFog();
		
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
		boolean renderWireframe = Config.Client.Advanced.Debugging.renderWireframe.get();
		if (renderWireframe) 
		{
			GL32.glPolygonMode(GL32.GL_FRONT_AND_BACK, GL32.GL_LINE);
			//GL32.glDisable(GL32.GL_CULL_FACE);
		}
		else 
		{
			GL32.glPolygonMode(GL32.GL_FRONT_AND_BACK, GL32.GL_FILL);
			GL32.glEnable(GL32.GL_CULL_FACE);
		}
		GL32.glEnable(GL32.GL_DEPTH_TEST);
		// GL32.glDisable(GL32.GL_DEPTH_TEST);
		GL32.glDepthFunc(GL32.GL_LESS);



		transparencyEnabled = Config.Client.Advanced.Graphics.Quality.transparency.get().transparencyEnabled;
		fakeOceanFloor = Config.Client.Advanced.Graphics.Quality.transparency.get().fakeTransparencyEnabled;

		GL32.glDisable(GL32.GL_BLEND); // We render opaque first, then transparent
		GL32.glDepthMask(true);
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
		int vanillaBlockRenderedDistance = MC_RENDER.getRenderDistance() * LodUtil.CHUNK_WIDTH;
		Mat4f modelViewProjectionMatrix = RenderUtil.createCombinedModelViewProjectionMatrix(baseProjectionMatrix, baseModelViewMatrix, partialTicks);
		
		/*---------Fill uniform data--------*/
		// Fill the uniform data. Note: GL33.GL_TEXTURE0 == texture bindpoint 0
		shaderProgram.fillUniformData(modelViewProjectionMatrix,
				MC_RENDER.isFogStateSpecial() ? getSpecialFogColor(partialTicks) : getFogColor(partialTicks),
				0, MC.getWrappedClientWorld().getHeight(), MC.getWrappedClientWorld().getMinHeight(), RenderUtil.getFarClipPlaneDistanceInBlocks(),
				vanillaBlockRenderedDistance, MC_RENDER.isFogStateSpecial());

		// Note: Since lightmapTexture is changing every frame, it's faster to recreate it than to reuse the old one.
		ILightMapWrapper lightmap = MC_RENDER.getLightmapWrapper();
		lightmap.bind();
		if (ENABLE_IBO) quadIBO.bind();
		//lightmapTexture.fillData(MC_RENDER.getLightmapTextureWidth(), MC_RENDER.getLightmapTextureHeight(), MC_RENDER.getLightmapPixels());
		//GL32.glEnable( GL32.GL_POLYGON_OFFSET_FILL );
		//GL32.glPolygonOffset( 1f, 1f );

		bufferHandler.buildRenderList(this.getLookVector());

		//===========//
		// rendering //
		//===========//
		profiler.popPush("LOD draw");
		LagSpikeCatcher draw = new LagSpikeCatcher();
		
		Vec3d cameraPos = MC_RENDER.getCameraExactPosition();
		DhBlockPos cameraBlockPos = MC_RENDER.getCameraBlockPosition();
		Vec3f cameraDir = MC_RENDER.getLookAtVector();
		
		//TODO: Directional culling
		bufferHandler.renderOpaque(this);

		if (Config.Client.Advanced.Graphics.Quality.ssao.get()) {
			SSAOShader.INSTANCE.render(partialTicks);
		}

		//======================//
		// render transparency //
		//======================//
		if (LodRenderer.transparencyEnabled) {
			GL32.glEnable(GL32.GL_BLEND);
			GL32.glBlendFunc(GL32.GL_SRC_ALPHA, GL32.GL_ONE_MINUS_SRC_ALPHA);
			GL32.glDepthMask(false); // This so that even on incorrect sorting of transparent blocks, it still mostly looks correct
			bufferHandler.renderTransparent(this);
			GL32.glDepthMask(true); // Apparently the depth mask state is stored in the FBO, so glState fails to restore it...
		}
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
		DebugRenderer.INSTANCE.render(modelViewProjectionMatrix);
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
			quadIBO.reserve(AbstractRenderBuffer.MAX_QUADS_PER_BUFFER);
		}
		EVENT_LOGGER.info("Renderer setup complete");
	}

	private Color getFogColor(float partialTicks)
	{
		Color fogColor;
		
		if (Config.Client.Advanced.Graphics.Fog.colorMode.get() == EFogColorMode.USE_SKY_COLOR)
			fogColor = MC_RENDER.getSkyColor();
		else
			fogColor = MC_RENDER.getFogColor(partialTicks);
		
		return fogColor;
	}
	private Color getSpecialFogColor(float partialTicks)
	{
		return MC_RENDER.getSpecialFogColor(partialTicks);
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
