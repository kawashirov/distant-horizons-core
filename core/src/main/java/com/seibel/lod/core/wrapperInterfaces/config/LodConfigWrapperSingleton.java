package com.seibel.lod.core.wrapperInterfaces.config;

import com.seibel.lod.api.items.enums.config.*;
import com.seibel.lod.api.items.enums.rendering.*;
import com.seibel.lod.core.config.Config;

/**
 * Use config getters rather than this
 */
@Deprecated
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
		public final IMultiplayer multiplayer;
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
		public IMultiplayer multiplayer() {
			return multiplayer;
		}

		@Override
		public IAdvanced advanced()
		{
			return advanced;
		}


		@Override
		public boolean getOptionsButton()
		{
			return Config.Client.optionsButton.get();
		}
		@Override
		public void setOptionsButton(boolean newOptionsButton)
		{
			Config.Client.optionsButton.set(newOptionsButton);
		}


		//================//
		// Client Configs //
		//================//
		public Client()
		{
			graphics = new Graphics();
			worldGenerator = new WorldGenerator();
			multiplayer = new Multiplayer();
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
				fogQuality = new FogQuality();
				advancedGraphics = new AdvancedGraphics();
			}


			public static class Quality implements IQuality
			{
				@Override
				public EHorizontalResolution getDrawResolution()
				{
					return Config.Client.Graphics.Quality.drawResolution.get();
				}
				@Override
				public void setDrawResolution(EHorizontalResolution newHorizontalResolution)
				{
					Config.Client.Graphics.Quality.drawResolution.set(newHorizontalResolution);
				}


				@Override
				public int getLodChunkRenderDistance()
				{
					return Config.Client.Graphics.Quality.lodChunkRenderDistance.get();
				}
				@Override
				public void setLodChunkRenderDistance(int newLodChunkRenderDistance)
				{
					Config.Client.Graphics.Quality.lodChunkRenderDistance.set(newLodChunkRenderDistance);
				}


				@Override
				public EVerticalQuality getVerticalQuality()
				{
					return Config.Client.Graphics.Quality.verticalQuality.get();
				}
				@Override
				public void setVerticalQuality(EVerticalQuality newVerticalQuality)
				{
					Config.Client.Graphics.Quality.verticalQuality.set(newVerticalQuality);
				}


				@Override
				public int getHorizontalScale()
				{
					return Config.Client.Graphics.Quality.horizontalScale.get();
				}
				@Override
				public void setHorizontalScale(int newHorizontalScale)
				{
					Config.Client.Graphics.Quality.horizontalScale.set(newHorizontalScale);
				}


				@Override
				public EHorizontalQuality getHorizontalQuality()
				{
					return Config.Client.Graphics.Quality.horizontalQuality.get();
				}
				@Override
				public void setHorizontalQuality(EHorizontalQuality newHorizontalQuality)
				{
					Config.Client.Graphics.Quality.horizontalQuality.set(newHorizontalQuality);
				}

				@Override
				public void setTransparency(ETransparency newTransparency) {
					Config.Client.Graphics.Quality.transparency.set(newTransparency);
				}

				@Override
				public ETransparency getTransparency(){
					return Config.Client.Graphics.Quality.transparency.get();
				}


				@Override
				public int getLodBiomeBlending() {
					return Config.Client.Graphics.Quality.lodBiomeBlending.get();
				}

				@Override
				public void setLodBiomeBlending(int newLodBiomeBlending) {
					Config.Client.Graphics.Quality.lodBiomeBlending.set(newLodBiomeBlending);
				}
			}


			public static class FogQuality implements IFogQuality
			{
				public final IAdvancedFog advancedFog;

				FogQuality()
				{
					advancedFog = new AdvancedFog();
				}

				@Override
				public EFogDistance getFogDistance()
				{
					return Config.Client.Graphics.FogQuality.fogDistance.get();
				}
				@Override
				public void setFogDistance(EFogDistance newFogDistance)
				{
					Config.Client.Graphics.FogQuality.fogDistance.set(newFogDistance);
				}


				@Override
				public EFogDrawMode getFogDrawMode()
				{
					return Config.Client.Graphics.FogQuality.fogDrawMode.get();
				}

				@Override
				public void setFogDrawMode(EFogDrawMode setFogDrawMode)
				{
					Config.Client.Graphics.FogQuality.fogDrawMode.set(setFogDrawMode);
				}


				@Override
				public EFogColorMode getFogColorMode()
				{
					return Config.Client.Graphics.FogQuality.fogColorMode.get();
				}

				@Override
				public void setFogColorMode(EFogColorMode newFogColorMode)
				{
					Config.Client.Graphics.FogQuality.fogColorMode.set(newFogColorMode);
				}


				@Override
				public boolean getDisableVanillaFog()
				{
					return Config.Client.Graphics.FogQuality.disableVanillaFog.get();
				}
				@Override
				public void setDisableVanillaFog(boolean newDisableVanillaFog)
				{
					Config.Client.Graphics.FogQuality.disableVanillaFog.set(newDisableVanillaFog);
				}

				@Override
				public IAdvancedFog advancedFog() {
					return advancedFog;
				}

				public static class AdvancedFog implements IAdvancedFog {
					public final IHeightFog heightFog;

					public AdvancedFog() {
						heightFog = new HeightFog();
					}

					@Override
					public double getFarFogStart() {
						return Config.Client.Graphics.FogQuality.AdvancedFog.farFogStart.get();
					}
					@Override
					public double getFarFogEnd() {
						return Config.Client.Graphics.FogQuality.AdvancedFog.farFogEnd.get();
					}
					@Override
					public double getFarFogMin() {
						return Config.Client.Graphics.FogQuality.AdvancedFog.farFogMin.get();
					}
					@Override
					public double getFarFogMax() {
						return Config.Client.Graphics.FogQuality.AdvancedFog.farFogMax.get();
					}
					@Override
					public EFogFalloff getFarFogType() {
						return Config.Client.Graphics.FogQuality.AdvancedFog.farFogType.get();
					}
					@Override
					public double getFarFogDensity() {
						return Config.Client.Graphics.FogQuality.AdvancedFog.farFogDensity.get();
					}

					@Override
					public void setFarFogStart(double newFarFogStart) {
						Config.Client.Graphics.FogQuality.AdvancedFog.farFogStart.set(newFarFogStart);
					}
					@Override
					public void setFarFogEnd(double newFarFogEnd) {
						Config.Client.Graphics.FogQuality.AdvancedFog.farFogEnd.set(newFarFogEnd);
					}
					@Override
					public void setFarFogMin(double newFarFogMin) {
						Config.Client.Graphics.FogQuality.AdvancedFog.farFogMin.set(newFarFogMin);
					}
					@Override
					public void setFarFogMax(double newFarFogMax) {
						Config.Client.Graphics.FogQuality.AdvancedFog.farFogMax.set(newFarFogMax);
					}
					@Override
					public void setFarFogType(EFogFalloff newFarFogType) {
						Config.Client.Graphics.FogQuality.AdvancedFog.farFogType.set(newFarFogType);
					}
					@Override
					public void setFarFogDensity(double newFarFogDensity) {
						Config.Client.Graphics.FogQuality.AdvancedFog.farFogDensity.set(newFarFogDensity);
					}

					@Override
					public IHeightFog heightFog() {
						return heightFog;
					}

					public static class HeightFog implements IHeightFog {

						@Override
						public EHeightFogMixMode getHeightFogMixMode() {
							return Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogMixMode.get();
						}
						@Override
						public EHeightFogMode getHeightFogMode() {
							return Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogMode.get();
						}
						@Override
						public double getHeightFogHeight() {
							return Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogHeight.get();
						}
						@Override
						public double getHeightFogStart() {
							return Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogStart.get();
						}
						@Override
						public double getHeightFogEnd() {
							return Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogEnd.get();
						}
						@Override
						public double getHeightFogMin() {
							return Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogMin.get();
						}
						@Override
						public double getHeightFogMax() {
							return Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogMax.get();
						}
						@Override
						public EFogFalloff getHeightFogType() {
							return Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogType.get();
						}
						@Override
						public double getHeightFogDensity() {
							return Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogDensity.get();
						}

						@Override
						public void setHeightFogMixMode(EHeightFogMixMode newHeightFogMixMode) {
							Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogMixMode.set(newHeightFogMixMode);
						}
						@Override
						public void setHeightFogMode(EHeightFogMode newHeightFogMode) {
							Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogMode.set(newHeightFogMode);
						}
						@Override
						public void setHeightFogHeight(double newHeightFogHeight) {
							Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogHeight.set(newHeightFogHeight);
						}
						@Override
						public void setHeightFogStart(double newHeightFogStart) {
							Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogStart.set(newHeightFogStart);
						}
						@Override
						public void setHeightFogEnd(double newHeightFogEnd) {
							Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogEnd.set(newHeightFogEnd);
						}
						@Override
						public void setHeightFogMin(double newHeightFogMin) {
							Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogMin.set(newHeightFogMin);
						}
						@Override
						public void setHeightFogMax(double newHeightFogMax) {
							Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogMax.set(newHeightFogMax);
						}
						@Override
						public void setHeightFogType(EFogFalloff newHeightFogType) {
							Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogType.set(newHeightFogType);
						}
						@Override
						public void setHeightFogDensity(double newHeightFogDensity) {
							Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogDensity.set(newHeightFogDensity);
						}
					}
				}

			}


			public static class AdvancedGraphics implements IAdvancedGraphics
			{
				@Override
				public boolean getDisableDirectionalCulling()
				{
					return Config.Client.Graphics.AdvancedGraphics.disableDirectionalCulling.get();
				}
				@Override
				public void setDisableDirectionalCulling(boolean newDisableDirectionalCulling)
				{
					Config.Client.Graphics.AdvancedGraphics.disableDirectionalCulling.set(newDisableDirectionalCulling);
				}


				@Override
				public EVanillaOverdraw getVanillaOverdraw()
				{
					return Config.Client.Graphics.AdvancedGraphics.vanillaOverdraw.get();
				}
				@Override
				public void setVanillaOverdraw(EVanillaOverdraw newVanillaOverdraw)
				{
					Config.Client.Graphics.AdvancedGraphics.vanillaOverdraw.set(newVanillaOverdraw);
				}

				@Override
				public int getOverdrawOffset() {
					return Config.Client.Graphics.AdvancedGraphics.overdrawOffset.get();
				}

				@Override
				public void setOverdrawOffset(int newOverdrawOffset) {
					Config.Client.Graphics.AdvancedGraphics.overdrawOffset.set(newOverdrawOffset);
				}
				/*
				@Override
				public int getBacksideCullingRange()
				{
					return Config.Client.Graphics.AdvancedGraphics.backsideCullingRange;
				}
				@Override
				public void setBacksideCullingRange(int newBacksideCullingRange)
				{
					ConfigGui.editSingleOption.getEntry("client.graphics.advancedGraphics.backsideCullingRange").value = newBacksideCullingRange;
					ConfigGui.editSingleOption.saveOption("client.graphics.advancedGraphics.backsideCullingRange");
				}*/

				@Override
				public boolean getUseExtendedNearClipPlane()
				{
					return Config.Client.Graphics.AdvancedGraphics.useExtendedNearClipPlane.get();
				}
				@Override
				public void setUseExtendedNearClipPlane(boolean newUseExtendedNearClipPlane)
				{
					Config.Client.Graphics.AdvancedGraphics.useExtendedNearClipPlane.set(newUseExtendedNearClipPlane);
				}

				@Override
				public double getBrightnessMultiplier()
				{
					return Config.Client.Graphics.AdvancedGraphics.brightnessMultiplier.get();
				}
				@Override
				public void setBrightnessMultiplier(double newBrightnessMultiplier)
				{
					Config.Client.Graphics.AdvancedGraphics.brightnessMultiplier.set(newBrightnessMultiplier);
				}

				@Override
				public double getSaturationMultiplier()
				{
					return Config.Client.Graphics.AdvancedGraphics.saturationMultiplier.get();
				}
				@Override
				public void setSaturationMultiplier(double newSaturationMultiplier)
				{
					Config.Client.Graphics.AdvancedGraphics.saturationMultiplier.set(newSaturationMultiplier);
				}

				@Override
				public boolean getEnableCaveCulling() {
					return Config.Client.Graphics.AdvancedGraphics.enableCaveCulling.get();
				}

				@Override
				public void setEnableCaveCulling(boolean newEnableCaveCulling) {
					Config.Client.Graphics.AdvancedGraphics.enableCaveCulling.set(newEnableCaveCulling);

				}

				@Override
				public int getCaveCullingHeight() {
					return Config.Client.Graphics.AdvancedGraphics.caveCullingHeight.get();
				}

				@Override
				public void setCaveCullingHeight(int newCaveCullingHeight) {
					Config.Client.Graphics.AdvancedGraphics.caveCullingHeight.set(newCaveCullingHeight);

				}

				@Override
				public int getEarthCurveRatio()
				{
					return Config.Client.Graphics.AdvancedGraphics.earthCurveRatio.get();
				}
				@Override
				public void setEarthCurveRatio(int newEarthCurveRatio)
				{
//					if (newEarthCurveRatio < 50) newEarthCurveRatio = 0; // TODO: Leetom can you please remove this
					Config.Client.Graphics.AdvancedGraphics.earthCurveRatio.set(newEarthCurveRatio);
				}
			}
		}




		//========================//
		// WorldGenerator Configs //
		//========================//
		public static class WorldGenerator implements IWorldGenerator
		{
			@Override
			public EGenerationPriority getGenerationPriority()
			{
				return Config.Client.WorldGenerator.generationPriority.get();
			}
			@Override
			public void setGenerationPriority(EGenerationPriority newGenerationPriority)
			{
				Config.Client.WorldGenerator.generationPriority.set(newGenerationPriority);
			}


			@Override
			public EDistanceGenerationMode getDistanceGenerationMode()
			{
				return Config.Client.WorldGenerator.distanceGenerationMode.get();
			}
			@Override
			public void setDistanceGenerationMode(EDistanceGenerationMode newDistanceGenerationMode)
			{
				Config.Client.WorldGenerator.distanceGenerationMode.set(newDistanceGenerationMode);
			}

			/*
			@Override
			public boolean getAllowUnstableFeatureGeneration()
			{
				return Config.Client.WorldGenerator.allowUnstableFeatureGeneration;
			}
			@Override
			public void setAllowUnstableFeatureGeneration(boolean newAllowUnstableFeatureGeneration)
			{
				ConfigGui.editSingleOption.getEntry("client.worldGenerator.allowUnstableFeatureGeneration").value = newAllowUnstableFeatureGeneration;
				ConfigGui.editSingleOption.saveOption("client.worldGenerator.allowUnstableFeatureGeneration");
			}*/


			@Override
			public EBlocksToAvoid getBlocksToAvoid()
			{
				return Config.Client.WorldGenerator.blocksToAvoid.get();
			}
			@Override
			public void setBlockToAvoid(EBlocksToAvoid newBlockToAvoid)
			{
				Config.Client.WorldGenerator.blocksToAvoid.set(newBlockToAvoid);
			}


			@Override
			public Boolean getTintWithAvoidedBlocks() {
				return Config.Client.WorldGenerator.tintWithAvoidedBlocks.get();
			}
			@Override
			public void setTintWithAvoidedBlocks(Boolean shouldTint) {
				Config.Client.WorldGenerator.tintWithAvoidedBlocks.set(shouldTint);
			}

			@Override
			public boolean getEnableDistantGeneration()
			{
				return Config.Client.WorldGenerator.enableDistantGeneration.get();
			}
			@Override
			public void setEnableDistantGeneration(boolean newEnableDistantGeneration)
			{
				Config.Client.WorldGenerator.enableDistantGeneration.set(newEnableDistantGeneration);
			}
			@Override
			public ELightGenerationMode getLightGenerationMode()
			{
				return Config.Client.WorldGenerator.lightGenerationMode.get();
			}
			@Override
			public void setLightGenerationMode(ELightGenerationMode newLightGenerationMode)
			{
				Config.Client.WorldGenerator.lightGenerationMode.set(newLightGenerationMode);
			}
		}



		//=====================//
		// Multiplayer Configs //
		//=====================//
		public static class Multiplayer implements IMultiplayer
		{
			@Override
			public EServerFolderNameMode getServerFolderNameMode()
			{
				return Config.Client.Multiplayer.serverFolderNameMode.get();
			}
			@Override
			public void setServerFolderNameMode(EServerFolderNameMode newServerFolderNameMode)
			{
				Config.Client.Multiplayer.serverFolderNameMode.set(newServerFolderNameMode);
			}

			@Override
			public double getMultiDimensionRequiredSimilarity()
			{
				return Config.Client.Multiplayer.multiDimensionRequiredSimilarity.get();
			}

			@Override
			public void setMultiDimensionRequiredSimilarity(double newMultiDimensionMinimumSimilarityPercent)
			{
				Config.Client.Multiplayer.multiDimensionRequiredSimilarity.set(newMultiDimensionMinimumSimilarityPercent);
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
				public double getNumberOfWorldGenerationThreads()
				{
					return Config.Client.Advanced.Threading.numberOfWorldGenerationThreads.get();
				}
				@Override
				public void setNumberOfWorldGenerationThreads(double newNumberOfWorldGenerationThreads)
				{
					Config.Client.Advanced.Threading.numberOfWorldGenerationThreads.set(newNumberOfWorldGenerationThreads);
				}


				@Override
				public int getNumberOfBufferBuilderThreads()
				{
					return Config.Client.Advanced.Threading.numberOfBufferBuilderThreads.get();
				}
				@Override
				public void setNumberOfBufferBuilderThreads(int newNumberOfWorldBuilderThreads)
				{
					Config.Client.Advanced.Threading.numberOfBufferBuilderThreads.set(newNumberOfWorldBuilderThreads);
				}
			}




			//===============//
			// Debug Options //
			//===============//
			public static class Debugging implements IDebugging
			{
				public final IDebugSwitch debugSwitch;

				@Override
				public IDebugSwitch debugSwitch()
				{
					return debugSwitch;
				}

				/* RendererType:
				 * DEFAULT
				 * DEBUG
				 * DISABLED
				 * */
				@Override
				public ERendererMode getRendererType() {
					return Config.Client.Advanced.Debugging.rendererMode.get();
				}
				@Override
				public void setRendererType(ERendererMode newRenderType) {
					Config.Client.Advanced.Debugging.rendererMode.set(newRenderType);
				}

				@Override
				public EDebugMode getDebugMode()
				{
					return Config.Client.Advanced.Debugging.debugMode.get();
				}
				@Override
				public void setDebugMode(EDebugMode newDebugMode)
				{
					Config.Client.Advanced.Debugging.debugMode.set(newDebugMode);
				}


				@Override
				public boolean getDebugKeybindingsEnabled()
				{
					return Config.Client.Advanced.Debugging.enableDebugKeybindings.get();
				}
				@Override
				public void setDebugKeybindingsEnabled(boolean newEnableDebugKeybindings)
				{
					Config.Client.Advanced.Debugging.enableDebugKeybindings.set(newEnableDebugKeybindings);
				}

				public Debugging()
				{
					debugSwitch = new DebugSwitch();
				}

				public static class DebugSwitch implements IDebugSwitch {

					/* The logging switches available:
					 * WorldGenEvent
					 * WorldGenPerformance
					 * WorldGenLoadEvent
					 * LodBuilderEvent
					 * RendererBufferEvent
					 * RendererGLEvent
					 * FileReadWriteEvent
					 * FileSubDimEvent
					 * NetworkEvent //NOT IMPL YET
					 */

					@Override
					public ELoggerMode getLogWorldGenEvent() {
						return Config.Client.Advanced.Debugging.DebugSwitch.logWorldGenEvent.get();
					}
					@Override
					public void setLogWorldGenEvent(ELoggerMode newLogWorldGenEvent) {
						Config.Client.Advanced.Debugging.DebugSwitch.logWorldGenEvent.set(newLogWorldGenEvent);
					}

					@Override
					public ELoggerMode getLogWorldGenPerformance() {
						return Config.Client.Advanced.Debugging.DebugSwitch.logWorldGenPerformance.get();
					}
					@Override
					public void setLogWorldGenPerformance(ELoggerMode newLogWorldGenPerformance) {
						Config.Client.Advanced.Debugging.DebugSwitch.logWorldGenPerformance.set(newLogWorldGenPerformance);
					}

					@Override
					public ELoggerMode getLogWorldGenLoadEvent() {
						return Config.Client.Advanced.Debugging.DebugSwitch.logWorldGenLoadEvent.get();
					}
					@Override
					public void setLogWorldGenLoadEvent(ELoggerMode newLogWorldGenLoadEvent) {
						Config.Client.Advanced.Debugging.DebugSwitch.logWorldGenLoadEvent.set(newLogWorldGenLoadEvent);
					}

					@Override
					public ELoggerMode getLogLodBuilderEvent() {
						return Config.Client.Advanced.Debugging.DebugSwitch.logLodBuilderEvent.get();
					}
					@Override
					public void setLogLodBuilderEvent(ELoggerMode newLogLodBuilderEvent) {
						Config.Client.Advanced.Debugging.DebugSwitch.logLodBuilderEvent.set(newLogLodBuilderEvent);
					}

					@Override
					public ELoggerMode getLogRendererBufferEvent() {
						return Config.Client.Advanced.Debugging.DebugSwitch.logRendererBufferEvent.get();
					}
					@Override
					public void setLogRendererBufferEvent(ELoggerMode newLogRendererBufferEvent) {
						Config.Client.Advanced.Debugging.DebugSwitch.logRendererBufferEvent.set(newLogRendererBufferEvent);
					}

					@Override
					public ELoggerMode getLogRendererGLEvent() {
						return Config.Client.Advanced.Debugging.DebugSwitch.logRendererGLEvent.get();
					}
					@Override
					public void setLogRendererGLEvent(ELoggerMode newLogRendererGLEvent) {
						Config.Client.Advanced.Debugging.DebugSwitch.logRendererGLEvent.set(newLogRendererGLEvent);
					}

					@Override
					public ELoggerMode getLogFileReadWriteEvent() {
						return Config.Client.Advanced.Debugging.DebugSwitch.logFileReadWriteEvent.get();
					}
					@Override
					public void setLogFileReadWriteEvent(ELoggerMode newLogFileReadWriteEvent) {
						Config.Client.Advanced.Debugging.DebugSwitch.logFileReadWriteEvent.set(newLogFileReadWriteEvent);
					}

					@Override
					public ELoggerMode getLogFileSubDimEvent() {
						return Config.Client.Advanced.Debugging.DebugSwitch.logFileSubDimEvent.get();
					}
					@Override
					public void setLogFileSubDimEvent(ELoggerMode newLogFileSubDimEvent) {
						Config.Client.Advanced.Debugging.DebugSwitch.logFileSubDimEvent.set(newLogFileSubDimEvent);
					}

					@Override
					public ELoggerMode getLogNetworkEvent() {
						return Config.Client.Advanced.Debugging.DebugSwitch.logNetworkEvent.get();
					}
					@Override
					public void setLogNetworkEvent(ELoggerMode newLogNetworkEvent) {
						Config.Client.Advanced.Debugging.DebugSwitch.logNetworkEvent.set(newLogNetworkEvent);
					}
				}
			}


			public static class Buffers implements IBuffers
			{

				@Override
				public EGpuUploadMethod getGpuUploadMethod()
				{
					return Config.Client.Advanced.Buffers.gpuUploadMethod.get();
				}
				@Override
				public void setGpuUploadMethod(EGpuUploadMethod newDisableVanillaFog)
				{
					Config.Client.Advanced.Buffers.gpuUploadMethod.set(newDisableVanillaFog);
				}


				@Override
				public int getGpuUploadPerMegabyteInMilliseconds()
				{
					return Config.Client.Advanced.Buffers.gpuUploadPerMegabyteInMilliseconds.get();
				}
				@Override
				public void setGpuUploadPerMegabyteInMilliseconds(int newMilliseconds) {
					Config.Client.Advanced.Buffers.gpuUploadPerMegabyteInMilliseconds.set(newMilliseconds);
				}


				@Override
				public EBufferRebuildTimes getRebuildTimes()
				{
					return Config.Client.Advanced.Buffers.rebuildTimes.get();
				}
				@Override
				public void setRebuildTimes(EBufferRebuildTimes newBufferRebuildTimes)
				{
					Config.Client.Advanced.Buffers.rebuildTimes.set(newBufferRebuildTimes);
				}
			}

			@Override
			public boolean getLodOnlyMode() {
				return Config.Client.Advanced.lodOnlyMode.get();
			}

			@Override
			public void setLodOnlyMode(boolean newLodOnlyMode) {
				Config.Client.Advanced.lodOnlyMode.set(newLodOnlyMode);
			}
		}
	}
}
