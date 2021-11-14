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

package com.seibel.lod.render;

import java.util.HashSet;

import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.NVFogDistance;

import com.seibel.lod.builders.bufferBuilding.LodBufferBuilderFactory;
import com.seibel.lod.builders.bufferBuilding.LodBufferBuilderFactory.VertexBuffersAndOffset;
import com.seibel.lod.config.LodConfig;
import com.seibel.lod.enums.config.GpuUploadMethod;
import com.seibel.lod.enums.rendering.DebugMode;
import com.seibel.lod.enums.rendering.FogDistance;
import com.seibel.lod.enums.rendering.FogDrawOverride;
import com.seibel.lod.enums.rendering.FogQuality;
import com.seibel.lod.handlers.ReflectionHandler;
import com.seibel.lod.lodApi.ApiShared;
import com.seibel.lod.objects.lod.LodDimension;
import com.seibel.lod.objects.lod.RegionPos;
import com.seibel.lod.objects.math.Mat4f;
import com.seibel.lod.objects.opengl.LodVertexBuffer;
import com.seibel.lod.objects.rending.NearFarFogSettings;
import com.seibel.lod.proxy.GlProxy;
import com.seibel.lod.render.shader.LodShaderProgram;
import com.seibel.lod.util.DetailDistanceUtil;
import com.seibel.lod.util.LevelPosUtil;
import com.seibel.lod.util.LodUtil;
import com.seibel.lod.wrappers.McObjectConverter;
import com.seibel.lod.wrappers.MinecraftWrapper;
import com.seibel.lod.wrappers.block.BlockPosWrapper;
import com.seibel.lod.wrappers.chunk.ChunkPosWrapper;

import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.potion.Effects;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

/**
 * This is where all the magic happens. <br>
 * This is where LODs are draw to the world.
 * 
 * @author James Seibel
 * @version 11-8-2021
 */
public class LodRenderer
{
	/**
	 * this is the light used when rendering the LODs,
	 * it should be something different from what is used by Minecraft
	 */
	private static final int LOD_GL_LIGHT_NUMBER = GL15.GL_LIGHT2;
	
	/**
	 * If true the LODs colors will be replaced with
	 * a checkerboard, this can be used for debugging.
	 */
	public DebugMode previousDebugMode = DebugMode.OFF;
	
	private final MinecraftWrapper mc;
	private final GameRenderer gameRender;
	private IProfiler profiler;
	private int farPlaneBlockDistance;
	
	
	/** This is used to generate the buildable buffers */
	private final LodBufferBuilderFactory lodBufferBuilderFactory;
	
	/** Each VertexBuffer represents 1 region */
	private LodVertexBuffer[][][] vbos;
	/**
	 * the OpenGL IDs for the vbos of the same indices.
	 * These have to be separate because we can't override the
	 * buffers in the VBOs (and we don't want to)
	 */
	private int[][][] storageBufferIds;
	
	private ChunkPosWrapper vbosCenter = new ChunkPosWrapper(0, 0);
	
	
	/** This is used to determine if the LODs should be regenerated */
	private int[] previousPos = new int[] { 0, 0, 0 };
	
	// these variables are used to determine if the buffers should be rebuilt
	private float prevSkyBrightness = 0;
	private double prevBrightness = 0;
	private int prevRenderDistance = 0;
	private long prevPlayerPosTime = 0;
	private long prevVanillaChunkTime = 0;
	private long prevChunkTime = 0;
	
	
	/** This is used to determine if the LODs should be regenerated */
	private FogDistance prevFogDistance = FogDistance.NEAR_AND_FAR;
	
	/**
	 * if this is true the LOD buffers should be regenerated,
	 * provided they aren't already being regenerated.
	 */
	private volatile boolean partialRegen = false;
	private volatile boolean fullRegen = true;
	
	/**
	 * This HashSet contains every chunk that Vanilla Minecraft
	 * is going to render
	 */
	public boolean[][] vanillaRenderedChunks;
	public boolean vanillaRenderedChunksChanged;
	public boolean vanillaRenderedChunksEmptySkip = false;
	public int vanillaBlockRenderedDistance;
	
	
	
	
	public LodRenderer(LodBufferBuilderFactory newLodNodeBufferBuilder)
	{
		mc = MinecraftWrapper.INSTANCE;
		gameRender = mc.getGameRenderer();
		
		lodBufferBuilderFactory = newLodNodeBufferBuilder;
	}
	
	
	
	
	
	
	
	/**
	 * Besides drawing the LODs this method also starts
	 * the async process of generating the Buffers that hold those LODs.
	 * @param lodDim The dimension to draw, if null doesn't replace the current dimension.
	 * @param mcModelViewMatrix This matrix stack should come straight from MC's renderChunkLayer (or future equivalent) method
	 * @param mcProjectionMatrix 
	 * @param partialTicks how far into the current tick this method was called.
	 */
	public void drawLODs(LodDimension lodDim, Mat4f mcModelViewMatrix, Mat4f mcProjectionMatrix, float partialTicks, IProfiler newProfiler)
	{
		//=================================//
		// determine if LODs should render //
		//=================================//
		
		if (lodDim == null)
		{
			// if there aren't any loaded LodChunks
			// don't try drawing anything
			return;
		}
		
		if (mc.getPlayer().getActiveEffectsMap().get(Effects.BLINDNESS) != null)
		{
			// if the player is blind don't render LODs,
			// and don't change minecraft's fog
			// which blindness relies on.
			return;
		}
		
		
		
		
		
		// TODO move the buffer regeneration logic into its own class (probably called in the client proxy instead)
		// starting here...
		determineIfLodsShouldRegenerate(lodDim, partialTicks);
		
		//=================//
		// create the LODs //
		//=================//
		
		// only regenerate the LODs if:
		// 1. we want to regenerate LODs
		// 2. we aren't already regenerating the LODs
		// 3. we aren't waiting for the build and draw buffers to swap
		//		(this is to prevent thread conflicts)
		if ((partialRegen || fullRegen) && !lodBufferBuilderFactory.generatingBuffers && !lodBufferBuilderFactory.newBuffersAvailable())
		{
			// generate the LODs on a separate thread to prevent stuttering or freezing
			lodBufferBuilderFactory.generateLodBuffersAsync(this, lodDim, mc.getPlayerBlockPos(), true);
			
			// the regen process has been started,
			// it will be done when lodBufferBuilder.newBuffersAvailable()
			// is true
			fullRegen = false;
			partialRegen = false;
		}
		
		// TODO move the buffer regeneration logic into its own class (probably called in the client proxy instead)
		// ...ending here
		
		if (lodBufferBuilderFactory.newBuffersAvailable())
		{
			swapBuffers();
		}
		
		
		
		
		
		//===============//
		// initial setup //
		//===============//
		
		profiler = newProfiler;
		profiler.push("LOD setup");
		
		GlProxy glProxy = GlProxy.getInstance();
		
		
		
		// set the required open GL settings
		
		if (LodConfig.CLIENT.advancedModOptions.debugging.debugMode.get() == DebugMode.SHOW_DETAIL_WIREFRAME)
			GL15.glPolygonMode(GL15.GL_FRONT_AND_BACK, GL15.GL_LINE);
		else
			GL15.glPolygonMode(GL15.GL_FRONT_AND_BACK, GL15.GL_FILL);
		
		GL15.glEnable(GL15.GL_CULL_FACE);
		GL15.glEnable(GL15.GL_DEPTH_TEST);
		
		// enable transparent rendering
		GL15.glBlendFunc(GL15.GL_SRC_ALPHA, GL15.GL_ONE_MINUS_SRC_ALPHA);
		GL15.glEnable(GL15.GL_BLEND);
		
		// get MC's shader program
		int currentProgram = GL20.glGetInteger(GL20.GL_CURRENT_PROGRAM);
		
		
		Mat4f modelViewMatrix = offsetTheModelViewMatrix(mcModelViewMatrix, partialTicks);
		vanillaBlockRenderedDistance = mc.getRenderDistance() * LodUtil.CHUNK_WIDTH;
		// required for setupFog and setupProjectionMatrix
		if (mc.getClientWorld().dimensionType().hasCeiling())
			farPlaneBlockDistance = Math.min(LodConfig.CLIENT.graphics.qualityOption.lodChunkRenderDistance.get(), LodUtil.CEILED_DIMENSION_MAX_RENDER_DISTANCE) * LodUtil.CHUNK_WIDTH;
		else
			farPlaneBlockDistance = LodConfig.CLIENT.graphics.qualityOption.lodChunkRenderDistance.get() * LodUtil.CHUNK_WIDTH;
		
		
		Mat4f projectionMatrix = createProjectionMatrix(mcProjectionMatrix, vanillaBlockRenderedDistance, partialTicks);
		
		
		// commented out until we can add shaders to handle lighting
		//setupLighting(lodDim, partialTicks);
		
		
//		// determine the current fog settings, so they can be
//		// reset after drawing the LODs
//		float defaultFogStartDist = GL15.glGetFloat(GL15.GL_FOG_START);
//		float defaultFogEndDist = GL15.glGetFloat(GL15.GL_FOG_END);
//		int defaultFogMode = GL15.glGetInteger(GL15.GL_FOG_MODE);
//		int defaultFogDistance = glProxy.fancyFogAvailable ? GL15.glGetInteger(NVFogDistance.GL_FOG_DISTANCE_MODE_NV) : -1;
		
		//ShaderInstance mcShader = RenderSystem.getShader();
		
//		NearFarFogSettings fogSettings = determineFogSettings();
		
		
		
		
		
		//===========//
		// rendering //
		//===========//
		
		profiler.popPush("LOD draw");
		
		if (vbos != null)
		{
			ActiveRenderInfo camera = mc.getGameRenderer().getMainCamera();
			Vector3f cameraDir = camera.getLookVector();
			
			// TODO re-enable once rendering is totally working
			boolean cullingDisabled = true; //LodConfig.CLIENT.graphics.advancedGraphicsOption.disableDirectionalCulling.get();
			boolean renderBufferStorage = LodConfig.CLIENT.graphics.advancedGraphicsOption.gpuUploadMethod.get() == GpuUploadMethod.BUFFER_STORAGE && glProxy.bufferStorageSupported;
			
			// used to determine what type of fog to render
			int halfWidth = vbos.length / 2;
			int quarterWidth = vbos.length / 4;
			
			// where the center of the built buffers is (needed when culling regions)
			RegionPos vboCenterRegionPos = new RegionPos(vbosCenter);
			
			
			
			
			// can be used when testing shaders
			//glProxy.createShaderProgram();
			
			
			LodShaderProgram shaderProgram = glProxy.lodShaderProgram;
			shaderProgram.use();
			
			
			// determine the VertexArrayObject's element positions
	        int posAttrib = shaderProgram.getAttributeLocation("vPosition");
	        shaderProgram.enableVertexAttribute(posAttrib);
	        int colAttrib = shaderProgram.getAttributeLocation("color");
	        shaderProgram.enableVertexAttribute(colAttrib);
			
	        
	        
	        // upload the required uniforms
	        int mvmUniform = shaderProgram.getUniformLocation("modelViewMatrix");
	        shaderProgram.setUniform(mvmUniform, modelViewMatrix);
			int projUniform = shaderProgram.getUniformLocation("projectionMatrix");
			shaderProgram.setUniform(projUniform, projectionMatrix);
	        
			
			// render each of the buffers
			for (int x = 0; x < vbos.length; x++)
			{
				for (int z = 0; z < vbos.length; z++)
				{
					RegionPos vboPos = new RegionPos(
							x + vboCenterRegionPos.x - (lodDim.getWidth() / 2),
							z + vboCenterRegionPos.z - (lodDim.getWidth() / 2));
					
					if (cullingDisabled || RenderUtil.isRegionInViewFrustum(camera.getBlockPosition(), cameraDir, vboPos.blockPos()))
					{
						// TODO add fog to the fragment shader
//						if ((x > halfWidth - quarterWidth && x < halfWidth + quarterWidth) 
//							&& (z > halfWidth - quarterWidth && z < halfWidth + quarterWidth))
//							setupFog(fogSettings.near.distance, fogSettings.near.quality);
//						else
//							setupFog(fogSettings.far.distance, fogSettings.far.quality);
						
//						if (storageBufferIds != null && renderBufferStorage)
//							for (int i = 0; i < storageBufferIds[x][z].length; i++)
//								drawArrays(storageBufferIds[x][z][i], vbos[x][z][i].vertexCount, posAttrib, colAttrib);
//						else
							for (int i = 0; i < vbos[x][z].length; i++)
								drawArrays(vbos[x][z][i].id, vbos[x][z][i].vertexCount, posAttrib, colAttrib);
					}
				}
			}
	        
	        
	        GL20.glDisableVertexAttribArray(posAttrib);
	        GL20.glDisableVertexAttribArray(colAttrib);
		}
		
		
		
		
		
		//=========//
		// cleanup //
		//=========//
		
		profiler.popPush("LOD cleanup");
		
		GL15.glPolygonMode(GL15.GL_FRONT_AND_BACK, GL15.GL_FILL);
		GL15.glDisable(GL15.GL_BLEND); // TODO: what should this be reset to?
		
		GL20.glUseProgram(currentProgram);
		//RenderSystem.setShader(() -> mcShader);
		
		// clear the depth buffer so everything drawn is drawn
		// over the LODs
		GL15.glClear(GL15.GL_DEPTH_BUFFER_BIT);
		
		
		// end of internal LOD profiling
		profiler.pop();
	}
	
	/** This is where the actual drawing happens. */
	private void drawArrays(int glBufferId, int vertexCount, int posAttrib, int colAttrib)
	{
		if (glBufferId == 0)
			return;
		
		// can be used to check for OpenGL errors
//		int error = GL15.glGetError();
//		ClientProxy.LOGGER.info(Integer.toHexString(error));
		
		
        // bind the buffer we are going to draw
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, glBufferId);
		GL30.glBindVertexArray(GlProxy.getInstance().vertexArrayObjectId);
        
		// let OpenGL know how our buffer is set up
		int vertexByteCount = (Float.BYTES * 3) + (Byte.BYTES * 4);
        GL20.glEnableVertexAttribArray(posAttrib);
        GL20.glVertexAttribPointer(posAttrib, 3, GL15.GL_FLOAT, false, vertexByteCount, 0);
        GL20.glEnableVertexAttribArray(colAttrib);
        GL20.glVertexAttribPointer(colAttrib, 4, GL15.GL_UNSIGNED_BYTE, true, vertexByteCount, Float.BYTES * 3);
        
        // draw the LODs
		GL30.glDrawArrays(GL30.GL_TRIANGLES, 0, vertexCount);
		
		
		GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0);
		GL30.glBindVertexArray(0);
		
		GL20.glDisableVertexAttribArray(posAttrib);
		GL20.glDisableVertexAttribArray(colAttrib);
	}
	
	
	
	
	
	//=================//
	// Setup Functions //
	//=================//
	
	private void setupFog(FogDistance fogDistance, FogQuality fogQuality)
	{
		if (fogQuality == FogQuality.OFF)
		{
			GL15.glDisable(GL15.GL_FOG);
			return;
		}
		
		if (fogDistance == FogDistance.NEAR_AND_FAR)
		{
			throw new IllegalArgumentException("setupFog doesn't accept the NEAR_AND_FAR fog distance.");
		}
		
		// determine the fog distance mode to use
		int glFogDistanceMode;
		if (fogQuality == FogQuality.FANCY)
		{
			// fancy fog (fragment distance based fog)
			glFogDistanceMode = NVFogDistance.GL_EYE_RADIAL_NV;
		}
		else
		{
			// fast fog (frustum distance based fog)
			glFogDistanceMode = NVFogDistance.GL_EYE_PLANE_ABSOLUTE_NV;
		}
		
		// the multipliers are percentages
		// of the regular view distance.
		if (fogDistance == FogDistance.FAR)
		{
			// the reason that I wrote fogEnd then fogStart backwards
			// is because we are using fog backwards to how
			// it is normally used, with it hiding near objects
			// instead of far objects.
			
			if (fogQuality == FogQuality.FANCY)
			{
				// for more realistic fog when using FAR
				if (LodConfig.CLIENT.graphics.fogQualityOption.fogDistance.get() == FogDistance.NEAR_AND_FAR)
					GL15.glFogf(GL15.GL_FOG_START, farPlaneBlockDistance * 1.6f * 0.9f);
				else
					GL15.glFogf(GL15.GL_FOG_START, Math.min(vanillaBlockRenderedDistance * 1.5f, farPlaneBlockDistance * 0.9f * 1.6f));
				GL15.glFogf(GL15.GL_FOG_END, farPlaneBlockDistance * 1.6f);
			}
			else if (fogQuality == FogQuality.FAST)
			{
				// for the far fog of the normal chunks
				// to start right where the LODs' end use:
				// end = 0.8f, start = 1.5f
				GL15.glFogf(GL15.GL_FOG_START, farPlaneBlockDistance * 0.75f);
				GL15.glFogf(GL15.GL_FOG_END, farPlaneBlockDistance * 1.0f);
			}
		}
		else if (fogDistance == FogDistance.NEAR)
		{
			if (fogQuality == FogQuality.FANCY)
			{
				GL15.glFogf(GL15.GL_FOG_END, vanillaBlockRenderedDistance * 1.41f);
				GL15.glFogf(GL15.GL_FOG_START, vanillaBlockRenderedDistance * 1.6f);
			}
			else if (fogQuality == FogQuality.FAST)
			{
				GL15.glFogf(GL15.GL_FOG_END, vanillaBlockRenderedDistance * 1.0f);
				GL15.glFogf(GL15.GL_FOG_START, vanillaBlockRenderedDistance * 1.5f);
			}
		}
		
		GL15.glEnable(GL15.GL_FOG);
		GL15.glFogi(GL15.GL_FOG_MODE, GL15.GL_LINEAR);
		
		if (GlProxy.getInstance().fancyFogAvailable)
			GL15.glFogi(NVFogDistance.GL_FOG_DISTANCE_MODE_NV, glFogDistanceMode);
	}
	
	/** 
	 * Revert any changes that were made to the fog
	 * and sets up the fog for Minecraft.
	 */
	private void cleanupFog(NearFarFogSettings fogSettings,
			float defaultFogStartDist, float defaultFogEndDist,
			int defaultFogMode, int defaultFogDistance)
	{
		GL15.glFogf(GL15.GL_FOG_START, defaultFogStartDist);
		GL15.glFogf(GL15.GL_FOG_END, defaultFogEndDist);
		GL15.glFogi(GL15.GL_FOG_MODE, defaultFogMode);
		
		// this setting is only valid if the GPU supports fancy fog
		if (GlProxy.getInstance().fancyFogAvailable)
			GL15.glFogi(NVFogDistance.GL_FOG_DISTANCE_MODE_NV, defaultFogDistance);
		
		// disable fog if Minecraft wasn't rendering fog
		// or we want it disabled
		if (!fogSettings.vanillaIsRenderingFog
			|| LodConfig.CLIENT.graphics.fogQualityOption.disableVanillaFog.get())
		{
			// Make fog render a infinite distance away.
			// This doesn't technically disable Minecraft's fog
			// so performance will probably be the same regardless, unlike
			// Optifine's no fog setting.
			
			// we can't disable minecraft's fog outright because by default
			// minecraft will re-enable the fog after our code
			
			GL15.glFogf(GL15.GL_FOG_START, 0.0F);
			GL15.glFogf(GL15.GL_FOG_END, Float.MAX_VALUE);
			GL15.glFogf(GL15.GL_FOG_DENSITY, Float.MAX_VALUE);
		}
	}
	
	
	/**
	 * Translate the camera relative to the LodDimension's center,
	 * this is done since all LOD buffers are created in world space
	 * instead of object space.
	 * (since AxisAlignedBoundingBoxes (LODs) use doubles and thus have a higher
	 * accuracy vs the model view matrix, which only uses floats)
	 */
	private Mat4f offsetTheModelViewMatrix(Mat4f mcModelViewMatrix, float partialTicks)
	{
		// get all relevant camera info
		ActiveRenderInfo camera = mc.getGameRenderer().getMainCamera();
		Vector3d projectedView = camera.getPosition();
		
		// translate the camera relative to the regions' center
		// (AxisAlignedBoundingBoxes (LODs) use doubles and thus have a higher
		// accuracy vs the model view matrix, which only uses floats)
		BlockPosWrapper bufferPos = vbosCenter.getWorldPosition();
		double xDiff = projectedView.x - bufferPos.getX();
		double zDiff = projectedView.z - bufferPos.getZ();
		mcModelViewMatrix.multiplyTranslationMatrix(-xDiff, -projectedView.y, -zDiff);
		
		return mcModelViewMatrix;
	}
		
	/**
	 * create and return a new projection matrix based on MC's projection matrix
	 * @param currentProjectionMatrix this is Minecraft's current projection matrix
	 * @param vanillaBlockRenderedDistance Minecraft's vanilla far plane distance
	 * @param partialTicks how many ticks into the frame we are
	 */
	private Mat4f createProjectionMatrix(Mat4f currentProjectionMatrix, float vanillaBlockRenderedDistance, float partialTicks)
	{
		// create the new projection matrix
		
		Mat4f lodProj = Mat4f.perspective(
				getFov(partialTicks, true),
				(float) this.mc.getWindow().getScreenWidth() / (float) this.mc.getWindow().getScreenHeight(),
				LodConfig.CLIENT.graphics.advancedGraphicsOption.useExtendedNearClipPlane.get() ? vanillaBlockRenderedDistance / 5 : 1,
				farPlaneBlockDistance * LodUtil.CHUNK_WIDTH / 2);
				
		
		// get Minecraft's un-edited projection matrix
		// (this is before it is zoomed, distorted, etc.)
		Mat4f defaultMcProj = McObjectConverter.Convert(mc.getGameRenderer().getProjectionMatrix(mc.getGameRenderer().getMainCamera(), partialTicks, true));
		// true here means use "use fov setting" (probably)
		
		// this logic strips away the defaultMcProj matrix, so we
		// can get the distortionMatrix, which represents all
		// transformations, zooming, distortions, etc. done
		// to Minecraft's Projection matrix
		Mat4f defaultMcProjInv = defaultMcProj.copy();
		defaultMcProjInv.invert();
		
		Mat4f distortionMatrix = defaultMcProjInv.copy();
		distortionMatrix.multiply(currentProjectionMatrix);
		
		
		// edit the lod projection to match Minecraft's
		// (so the LODs line up with the real world)
		lodProj.multiply(distortionMatrix);
		
		return lodProj;
	}
	
	///** setup the lighting to be used for the LODs */
	/*private void setupLighting(LodDimension lodDimension, float partialTicks)
	{
		// Determine if the player has night vision
		boolean playerHasNightVision = false;
		if (this.mc.getPlayer() != null)
		{
			Iterator<EffectInstance> iterator = this.mc.getPlayer().getActiveEffects().iterator();
			while (iterator.hasNext())
			{
				EffectInstance instance = iterator.next();
				if (instance.getEffect() == Effects.NIGHT_VISION)
				{
					playerHasNightVision = true;
					break;
				}
			}
		}

		float sunBrightness = lodDimension.dimension.hasSkyLight() ? mc.getSkyDarken(partialTicks) : 0.2f;
		sunBrightness = playerHasNightVision ? 1.0f : sunBrightness;
		float gamma = (float) mc.getOptions().gamma - 0.0f;
		float dayEffect = (sunBrightness - 0.2f) * 1.25f;
		float lightStrength = (gamma * 0.34f - 0.01f) * (1.0f - dayEffect) + dayEffect - 0.20f; //gamma * 0.2980392157f + 0.1647058824f
		float blueLightStrength = (gamma * 0.44f + 0.12f) * (1.0f - dayEffect) + dayEffect - 0.20f; //gamma * 0.4235294118f + 0.2784313725f

		float[] lightAmbient = {lightStrength, lightStrength, blueLightStrength, 1.0f};


		// can be used for debugging
		//		if (partialTicks < 0.005)
		//			ClientProxy.LOGGER.debug(lightStrength);

		ByteBuffer temp = ByteBuffer.allocateDirect(16);
		temp.order(ByteOrder.nativeOrder());
		GL15.glLightfv(LOD_GL_LIGHT_NUMBER, GL15.GL_AMBIENT, (FloatBuffer) temp.asFloatBuffer().put(lightAmbient).flip());
		GL15.glEnable(LOD_GL_LIGHT_NUMBER); // Enable the above lighting

		RenderSystem.enableLighting();
	}*/
	
	/** Create all buffers that will be used. */
	public void setupBuffers(LodDimension lodDim)
	{
		lodBufferBuilderFactory.setupBuffers(lodDim);
	}
	
	
	//======================//
	// Other Misc Functions //
	//======================//
	
	/**
	 * If this is called then the next time "drawLODs" is called
	 * the LODs will be regenerated; the same as if the player moved.
	 */
	public void regenerateLODsNextFrame()
	{
		fullRegen = true;
	}
	
	
	/**
	 * Replace the current Vertex Buffers with the newly
	 * created buffers from the lodBufferBuilder. <br><br>
	 * <p>
	 * For some reason this has to be called after the frame has been rendered,
	 * otherwise visual stuttering/rubber banding may happen. I'm not sure why...
	 */
	private void swapBuffers()
	{
		// replace the drawable buffers with
		// the newly created buffers from the lodBufferBuilder
		VertexBuffersAndOffset result = lodBufferBuilderFactory.getVertexBuffers();
		vbos = result.vbos;
		storageBufferIds = result.storageBufferIds;
		vbosCenter = result.drawableCenterChunkPos;
	}
	
	/** Calls the BufferBuilder's destroyBuffers method. */
	public void destroyBuffers()
	{
		lodBufferBuilderFactory.destroyBuffers();
	}
	
	// TODO move this into the MC wrapper
	private double getFov(float partialTicks, boolean useFovSetting)
	{
		return mc.getGameRenderer().getFov(mc.getGameRenderer().getMainCamera(), partialTicks, useFovSetting);
	}
	
	
	/** Return what fog settings should be used when rendering. */
	private NearFarFogSettings determineFogSettings()
	{
		NearFarFogSettings fogSettings = new NearFarFogSettings();
		
		
		FogQuality quality = ReflectionHandler.INSTANCE.getFogQuality();
		FogDrawOverride override = LodConfig.CLIENT.graphics.fogQualityOption.fogDrawOverride.get();
		
		
		fogSettings.vanillaIsRenderingFog = quality != FogQuality.OFF;
		
		
		// use any fog overrides the user may have set
		switch (override)
		{
		case FANCY:
			quality = FogQuality.FANCY;
			break;
		
		case NO_FOG:
			quality = FogQuality.OFF;
			break;
		
		case FAST:
			quality = FogQuality.FAST;
			break;
		
		case OPTIFINE_SETTING:
			// don't override anything
			break;
		}
		
		
		// only use fancy fog if the user's GPU can deliver
		if (!GlProxy.getInstance().fancyFogAvailable && quality == FogQuality.FANCY)
		{
			quality = FogQuality.FAST;
		}
		
		
		// how different distances are drawn depends on the quality set
		switch (quality)
		{
		case FANCY:
			fogSettings.near.quality = FogQuality.FANCY;
			fogSettings.far.quality = FogQuality.FANCY;
			
			switch (LodConfig.CLIENT.graphics.fogQualityOption.fogDistance.get())
			{
			case NEAR_AND_FAR:
				fogSettings.near.distance = FogDistance.NEAR;
				fogSettings.far.distance = FogDistance.FAR;
				break;
			
			case NEAR:
				fogSettings.near.distance = FogDistance.NEAR;
				fogSettings.far.distance = FogDistance.NEAR;
				break;
			
			case FAR:
				fogSettings.near.distance = FogDistance.FAR;
				fogSettings.far.distance = FogDistance.FAR;
				break;
			}
			break;
		
		case FAST:
			fogSettings.near.quality = FogQuality.FAST;
			fogSettings.far.quality = FogQuality.FAST;
			
			// fast fog setting should only have one type of
			// fog, since the LODs are separated into a near
			// and far portion; and fast fog is rendered from the
			// frustrum's perspective instead of the camera
			switch (LodConfig.CLIENT.graphics.fogQualityOption.fogDistance.get())
			{
			case NEAR_AND_FAR:
			case NEAR:
				fogSettings.near.distance = FogDistance.NEAR;
				fogSettings.far.distance = FogDistance.NEAR;
				break;
			
			case FAR:
				fogSettings.near.distance = FogDistance.FAR;
				fogSettings.far.distance = FogDistance.FAR;
				break;
			}
			break;
		
		case OFF:
			fogSettings.near.quality = FogQuality.OFF;
			fogSettings.far.quality = FogQuality.OFF;
			break;
		}
		return fogSettings;
	}
	
	
	/** Determines if the LODs should have a fullRegen or partialRegen */
	private void determineIfLodsShouldRegenerate(LodDimension lodDim, float partialTicks)
	{
		short chunkRenderDistance = (short) mc.getRenderDistance();
		int vanillaRenderedChunksWidth = chunkRenderDistance * 2 + 2;
		
		//=============//
		// full regens //
		//=============//
		
		// check if the view distance changed
		if (ApiShared.previousLodRenderDistance != LodConfig.CLIENT.graphics.qualityOption.lodChunkRenderDistance.get()
					|| chunkRenderDistance != prevRenderDistance
					|| prevFogDistance != LodConfig.CLIENT.graphics.fogQualityOption.fogDistance.get())
		{
			
			vanillaRenderedChunks = new boolean[vanillaRenderedChunksWidth][vanillaRenderedChunksWidth];
			DetailDistanceUtil.updateSettings();
			fullRegen = true;
			previousPos = LevelPosUtil.createLevelPos((byte) 4, mc.getPlayerChunkPos().getZ(), mc.getPlayerChunkPos().getZ());
			prevFogDistance = LodConfig.CLIENT.graphics.fogQualityOption.fogDistance.get();
			prevRenderDistance = chunkRenderDistance;
		}
		
		// did the user change the debug setting?
		if (LodConfig.CLIENT.advancedModOptions.debugging.debugMode.get() != previousDebugMode)
		{
			previousDebugMode = LodConfig.CLIENT.advancedModOptions.debugging.debugMode.get();
			fullRegen = true;
		}
		
		
		long newTime = System.currentTimeMillis();
		
		// check if the player has moved
		if (newTime - prevPlayerPosTime > LodConfig.CLIENT.advancedModOptions.buffers.rebuildTimes.get().playerMoveTimeout)
		{
			if (LevelPosUtil.getDetailLevel(previousPos) == 0
						|| mc.getPlayerChunkPos().getX() != LevelPosUtil.getPosX(previousPos)
						|| mc.getPlayerChunkPos().getZ() != LevelPosUtil.getPosZ(previousPos))
			{
				vanillaRenderedChunks = new boolean[vanillaRenderedChunksWidth][vanillaRenderedChunksWidth];
				fullRegen = true;
				previousPos = LevelPosUtil.createLevelPos((byte) 4, mc.getPlayerChunkPos().getX(), mc.getPlayerChunkPos().getZ());
			}
			prevPlayerPosTime = newTime;
		}
		
		
		
		// determine how far the lighting has to 
		// change in order to rebuild the buffers
		
		// the max brightness is 1 and the minimum is 0.2
		float skyBrightness = lodDim.dimension.hasSkyLight() ? mc.getSkyDarken(partialTicks) : 0.2f;
		float minLightingDifference;
		switch (LodConfig.CLIENT.advancedModOptions.buffers.rebuildTimes.get())
		{
		case FREQUENT:
			minLightingDifference = 0.025f;
			break;
		case NORMAL:
			minLightingDifference = 0.05f;
			break;
		default:
		case RARE:
			minLightingDifference = 0.1f;
			break;
		}
		
		// check if the lighting changed
		if (Math.abs(skyBrightness - prevSkyBrightness) > minLightingDifference
					// make sure the lighting gets to the max/minimum value
					// (just in case the minLightingDifference is too large to notice the change)
					|| (skyBrightness == 1.0f && prevSkyBrightness != 1.0f) // noon
					|| (skyBrightness == 0.2f && prevSkyBrightness != 0.2f) // midnight
					|| mc.getOptions().gamma != prevBrightness)
		{
			fullRegen = true;
			prevBrightness = mc.getOptions().gamma;
			prevSkyBrightness = skyBrightness;
		}
		
		/*if (lightMap != lastLightMap)
		{
			fullRegen = true;
			lastLightMap = lightMap;
		}*/
		
		//================//
		// partial regens //
		//================//
		
		
		// check if the vanilla rendered chunks changed
		if (newTime - prevVanillaChunkTime > LodConfig.CLIENT.advancedModOptions.buffers.rebuildTimes.get().renderedChunkTimeout)
		{
			if (vanillaRenderedChunksChanged)
			{
				partialRegen = true;
				vanillaRenderedChunksChanged = false;
			}
			prevVanillaChunkTime = newTime;
		}
		
		
		// check if there is any newly generated terrain to show
		if (newTime - prevChunkTime > LodConfig.CLIENT.advancedModOptions.buffers.rebuildTimes.get().chunkChangeTimeout)
		{
			if (lodDim.regenDimensionBuffers)
			{
				partialRegen = true;
				lodDim.regenDimensionBuffers = false;
			}
			prevChunkTime = newTime;
		}
		
		
		
		//==============//
		// LOD skipping //
		//==============//
		
		// determine which LODs should not be rendered close to the player
		HashSet<ChunkPos> chunkPosToSkip = LodUtil.getNearbyLodChunkPosToSkip(lodDim, mc.getPlayerBlockPos());
		int xIndex;
		int zIndex;
		for (ChunkPos pos : chunkPosToSkip)
		{
			vanillaRenderedChunksEmptySkip = false;
			
			xIndex = (pos.x - mc.getPlayerChunkPos().getX()) + (chunkRenderDistance + 1);
			zIndex = (pos.z - mc.getPlayerChunkPos().getZ()) + (chunkRenderDistance + 1);
			
			// sometimes we are given chunks that are outside the render distance,
			// This prevents index out of bounds exceptions
			if (xIndex >= 0 && zIndex >= 0
						&& xIndex < vanillaRenderedChunks.length
						&& zIndex < vanillaRenderedChunks.length)
			{
				if (!vanillaRenderedChunks[xIndex][zIndex])
				{
					vanillaRenderedChunks[xIndex][zIndex] = true;
					vanillaRenderedChunksChanged = true;
					lodDim.markRegionBufferToRegen(pos.getRegionX(), pos.getRegionZ());
				}
			}
		}
		
		
		// if the player is high enough, draw all LODs
		if (chunkPosToSkip.isEmpty() && mc.getPlayer().position().y > 256 && !vanillaRenderedChunksEmptySkip)
		{
			vanillaRenderedChunks = new boolean[vanillaRenderedChunksWidth][vanillaRenderedChunksWidth];
			vanillaRenderedChunksChanged = true;
			vanillaRenderedChunksEmptySkip = true;
		}
		
		vanillaRenderedChunks = new boolean[vanillaRenderedChunksWidth][vanillaRenderedChunksWidth];
	}
	
}
