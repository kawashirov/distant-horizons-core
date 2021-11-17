package com.seibel.lod.wrappers.config;

import com.seibel.lod.api.forge.ForgeConfig;
import com.seibel.lod.core.enums.config.BlockToAvoid;
import com.seibel.lod.core.enums.config.BufferRebuildTimes;
import com.seibel.lod.core.enums.config.DistanceGenerationMode;
import com.seibel.lod.core.enums.config.GenerationPriority;
import com.seibel.lod.core.enums.config.GpuUploadMethod;
import com.seibel.lod.core.enums.config.HorizontalQuality;
import com.seibel.lod.core.enums.config.HorizontalResolution;
import com.seibel.lod.core.enums.config.HorizontalScale;
import com.seibel.lod.core.enums.config.LodTemplate;
import com.seibel.lod.core.enums.config.VanillaOverdraw;
import com.seibel.lod.core.enums.config.VerticalQuality;
import com.seibel.lod.core.enums.rendering.DebugMode;
import com.seibel.lod.core.enums.rendering.FogDistance;
import com.seibel.lod.core.enums.rendering.FogDrawOverride;
import com.seibel.lod.core.wrapperAdapters.config.ILodConfigWrapperSingleton;

/**
 * This holds the config defaults and setters/getters
 * that should be hooked into the host mod loader (Fabric, Forge, etc.).
 * 
 * @author James Seibel
 * @version 11-16-2021
 */
public class LodConfigWrapperSingleton implements ILodConfigWrapperSingleton
{
	public static final LodConfigWrapperSingleton INSTANCE = new LodConfigWrapperSingleton();
	
	
	private static final Client client = new Client();
	@Override
	public IClient client()
	{
		return client;
	}
	
	public static class Client implements IClient
	{
		public final IGraphics graphics;
		public final IWorldGenerator worldGenerator;
		public final IAdvanced advanced;
		

		@Override
		public IGraphics graphics()
		{
			return graphics;
		}
		
		@Override
		public IWorldGenerator worldGenerator()
		{
			return worldGenerator;
		}
		
		@Override
		public IAdvanced advanced()
		{
			return advanced;
		}
		
		
		
		//================//
		// Client Configs //
		//================//
		public Client()
		{
			graphics = new Graphics();
			worldGenerator = new WorldGenerator();
			advanced = new Advanced();
		}
		
		
		//==================//
		// Graphics Configs //
		//==================//
		public static class Graphics implements IGraphics
		{
			public final IQuality quality;
			public final IFogQuality fogQuality;
			public final IAdvancedGraphics advancedGraphics;
			
			

			@Override
			public IQuality quality()
			{
				return quality;
			}

			@Override
			public IFogQuality fogQuality()
			{
				return fogQuality;
			}

			@Override
			public IAdvancedGraphics advancedGraphics()
			{
				return advancedGraphics;
			}
			
			
			Graphics()
			{
				quality = new Quality();
				advancedGraphics = new AdvancedGraphics();
				fogQuality = new FogQuality();
			}
			
			
			public static class Quality implements IQuality
			{
				@Override
				public HorizontalResolution getDrawResolution()
				{
					return ForgeConfig.CLIENT.graphics.qualityOption.drawResolution.get();
				}
				@Override
				public void setDrawResolution(HorizontalResolution newHorizontalResolution)
				{
					ForgeConfig.CLIENT.graphics.qualityOption.drawResolution.set(newHorizontalResolution);
				}
				
				
				@Override
				public int getLodChunkRenderDistance()
				{
					return ForgeConfig.CLIENT.graphics.qualityOption.lodChunkRenderDistance.get();
				}
				@Override
				public void setLodChunkRenderDistance(int newLodChunkRenderDistance)
				{
					ForgeConfig.CLIENT.graphics.qualityOption.lodChunkRenderDistance.set(newLodChunkRenderDistance);
				}
				
				
				@Override
				public VerticalQuality getVerticalQuality()
				{
					return ForgeConfig.CLIENT.graphics.qualityOption.verticalQuality.get();
				}
				@Override
				public void setVerticalQuality(VerticalQuality newVerticalQuality)
				{
					ForgeConfig.CLIENT.graphics.qualityOption.verticalQuality.set(newVerticalQuality);
				}
				
				
				@Override
				public HorizontalScale getHorizontalScale()
				{
					return ForgeConfig.CLIENT.graphics.qualityOption.horizontalScale.get();
				}
				@Override
				public void setHorizontalScale(HorizontalScale newHorizontalScale)
				{
					ForgeConfig.CLIENT.graphics.qualityOption.horizontalScale.set(newHorizontalScale);
				}
				
				
				@Override
				public HorizontalQuality getHorizontalQuality()
				{
					return ForgeConfig.CLIENT.graphics.qualityOption.horizontalQuality.get();
				}
				@Override
				public void setHorizontalQuality(HorizontalQuality newHorizontalQuality)
				{
					ForgeConfig.CLIENT.graphics.qualityOption.horizontalQuality.set(newHorizontalQuality);
				}
			}
			
			
			public static class FogQuality implements IFogQuality
			{
				@Override
				public FogDistance getFogDistance()
				{
					return ForgeConfig.CLIENT.graphics.fogQuality.fogDistance.get();
				}
				@Override
				public void setFogDistance(FogDistance newFogDistance)
				{
					ForgeConfig.CLIENT.graphics.fogQuality.fogDistance.set(newFogDistance);
				}
				
				
				@Override
				public FogDrawOverride getFogDrawOverride()
				{
					return ForgeConfig.CLIENT.graphics.fogQuality.fogDrawOverride.get();
				}
				@Override
				public void setFogDrawOverride(FogDrawOverride newFogDrawOverride)
				{
					ForgeConfig.CLIENT.graphics.fogQuality.fogDrawOverride.set(newFogDrawOverride);
				}
				
				
				@Override
				public boolean getDisableVanillaFog()
				{
					return ForgeConfig.CLIENT.graphics.fogQuality.disableVanillaFog.get();
				}
				@Override
				public void setDisableVanillaFog(boolean newDisableVanillaFog)
				{
					ForgeConfig.CLIENT.graphics.fogQuality.disableVanillaFog.set(newDisableVanillaFog);
				}
			}
			
			
			public static class AdvancedGraphics implements IAdvancedGraphics
			{
				@Override
				public LodTemplate getLodTemplate()
				{
					return ForgeConfig.CLIENT.graphics.advancedGraphicsOption.lodTemplate.get();
				}
				@Override
				public void setLodTemplate(LodTemplate newLodTemplate)
				{
					ForgeConfig.CLIENT.graphics.advancedGraphicsOption.lodTemplate.set(newLodTemplate);
				}
				
				
				@Override
				public boolean getDisableDirectionalCulling()
				{
					return ForgeConfig.CLIENT.graphics.advancedGraphicsOption.disableDirectionalCulling.get();
				}
				@Override
				public void setDisableDirectionalCulling(boolean newDisableDirectionalCulling)
				{
					ForgeConfig.CLIENT.graphics.advancedGraphicsOption.disableDirectionalCulling.set(newDisableDirectionalCulling);
				}
				
				
				@Override
				public boolean getAlwaysDrawAtMaxQuality()
				{
					return ForgeConfig.CLIENT.graphics.advancedGraphicsOption.alwaysDrawAtMaxQuality.get();
				}
				@Override
				public void setAlwaysDrawAtMaxQuality(boolean newAlwaysDrawAtMaxQuality)
				{
					ForgeConfig.CLIENT.graphics.advancedGraphicsOption.alwaysDrawAtMaxQuality.set(newAlwaysDrawAtMaxQuality);
				}
				
				
				@Override
				public VanillaOverdraw getVanillaOverdraw()
				{
					return ForgeConfig.CLIENT.graphics.advancedGraphicsOption.vanillaOverdraw.get();
				}
				@Override
				public void setVanillaOverdraw(VanillaOverdraw newVanillaOverdraw)
				{
					ForgeConfig.CLIENT.graphics.advancedGraphicsOption.vanillaOverdraw.set(newVanillaOverdraw);
				}
				
				
				@Override
				public GpuUploadMethod getGpuUploadMethod()
				{
					return ForgeConfig.CLIENT.graphics.advancedGraphicsOption.gpuUploadMethod.get();
				}
				@Override
				public void setGpuUploadMethod(GpuUploadMethod newDisableVanillaFog)
				{
					ForgeConfig.CLIENT.graphics.advancedGraphicsOption.gpuUploadMethod.set(newDisableVanillaFog);
				}
				
				
				@Override
				public boolean getUseExtendedNearClipPlane()
				{
					return ForgeConfig.CLIENT.graphics.advancedGraphicsOption.useExtendedNearClipPlane.get();
				}
				@Override
				public void setUseExtendedNearClipPlane(boolean newUseExtendedNearClipPlane)
				{
					ForgeConfig.CLIENT.graphics.advancedGraphicsOption.useExtendedNearClipPlane.set(newUseExtendedNearClipPlane);
				}
			}
		}
		
		
		
		
		//========================//
		// WorldGenerator Configs //
		//========================//
		public static class WorldGenerator implements IWorldGenerator
		{
			@Override
			public GenerationPriority getGenerationPriority()
			{
				return ForgeConfig.CLIENT.worldGenerator.generationPriority.get();
			}
			@Override
			public void setGenerationPriority(GenerationPriority newGenerationPriority)
			{
				ForgeConfig.CLIENT.worldGenerator.generationPriority.set(newGenerationPriority);
			}
			
			
			@Override
			public DistanceGenerationMode getDistanceGenerationMode()
			{
				return ForgeConfig.CLIENT.worldGenerator.distanceGenerationMode.get();
			}
			@Override
			public void setDistanceGenerationMode(DistanceGenerationMode newDistanceGenerationMode)
			{
				ForgeConfig.CLIENT.worldGenerator.distanceGenerationMode.set(newDistanceGenerationMode);
			}
			
			
			@Override
			public boolean getAllowUnstableFeatureGeneration()
			{
				return ForgeConfig.CLIENT.worldGenerator.allowUnstableFeatureGeneration.get();
			}
			@Override
			public void setAllowUnstableFeatureGeneration(boolean newAllowUnstableFeatureGeneration)
			{
				ForgeConfig.CLIENT.worldGenerator.allowUnstableFeatureGeneration.set(newAllowUnstableFeatureGeneration);
			}
			
			
			@Override
			public BlockToAvoid getBlockToAvoid()
			{
				return ForgeConfig.CLIENT.worldGenerator.blockToAvoid.get();
			}
			@Override
			public void setBlockToAvoid(BlockToAvoid newBlockToAvoid)
			{
				ForgeConfig.CLIENT.worldGenerator.blockToAvoid.set(newBlockToAvoid);
			}
		}
		
		
		
		
		//============================//
		// AdvancedModOptions Configs //
		//============================//
		public static class Advanced implements IAdvanced
		{
			public final IThreading threading;
			public final IDebugging debugging;
			public final IBuffers buffers;
			
			
			@Override
			public IThreading threading()
			{
				return threading;
			}


			@Override
			public IDebugging debugging()
			{
				return debugging;
			}


			@Override
			public IBuffers buffers()
			{
				return buffers;
			}
			
			
			public Advanced()
			{
				threading = new Threading();
				debugging = new Debugging();
				buffers = new Buffers();
			}
			
			public static class Threading implements IThreading
			{
				@Override
				public int getNumberOfWorldGenerationThreads()
				{
					return ForgeConfig.CLIENT.advancedModOptions.threading.numberOfWorldGenerationThreads.get();
				}
				@Override
				public void setNumberOfWorldGenerationThreads(int newNumberOfWorldGenerationThreads)
				{
					ForgeConfig.CLIENT.advancedModOptions.threading.numberOfWorldGenerationThreads.set(newNumberOfWorldGenerationThreads);
				}
				
				
				@Override
				public int getNumberOfBufferBuilderThreads()
				{
					return ForgeConfig.CLIENT.advancedModOptions.threading.numberOfBufferBuilderThreads.get();
				}
				@Override
				public void setNumberOfBufferBuilderThreads(int newNumberOfWorldBuilderThreads)
				{
					ForgeConfig.CLIENT.advancedModOptions.threading.numberOfBufferBuilderThreads.set(newNumberOfWorldBuilderThreads);
				}
			}
			
			
			
			
			//===============//
			// Debug Options //
			//===============//
			public static class Debugging implements IDebugging
			{
				@Override
				public boolean getDrawLods()
				{
					return ForgeConfig.CLIENT.advancedModOptions.debugging.drawLods.get();
				}
				@Override
				public void setDrawLods(boolean newDrawLods)
				{
					ForgeConfig.CLIENT.advancedModOptions.debugging.drawLods.set(newDrawLods);
				}
				
				
				@Override
				public DebugMode getDebugMode()
				{
					return ForgeConfig.CLIENT.advancedModOptions.debugging.debugMode.get();
				}
				@Override
				public void setDebugMode(DebugMode newDebugMode)
				{
					ForgeConfig.CLIENT.advancedModOptions.debugging.debugMode.set(newDebugMode);
				}
				
				
				@Override
				public boolean getEnableDebugKeybindings()
				{
					return ForgeConfig.CLIENT.advancedModOptions.debugging.enableDebugKeybindings.get();
				}
				@Override
				public void setEnableDebugKeybindings(boolean newEnableDebugKeybindings)
				{
					ForgeConfig.CLIENT.advancedModOptions.debugging.enableDebugKeybindings.set(newEnableDebugKeybindings);
				}
			}
			
			
			public static class Buffers implements IBuffers
			{
				@Override
				public BufferRebuildTimes getRebuildTimes()
				{
					return ForgeConfig.CLIENT.advancedModOptions.buffers.rebuildTimes.get();
				}
				@Override
				public void setRebuildTimes(BufferRebuildTimes newBufferRebuildTimes)
				{
					ForgeConfig.CLIENT.advancedModOptions.buffers.rebuildTimes.set(newBufferRebuildTimes);
				}
			}
		}	
	}	
}
