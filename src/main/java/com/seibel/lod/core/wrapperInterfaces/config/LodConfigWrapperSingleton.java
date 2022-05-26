package com.seibel.lod.core.wrapperInterfaces.config;

import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.enums.config.*;
import com.seibel.lod.core.enums.rendering.*;

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
				public HorizontalResolution getDrawResolution()
				{
					return Config.Client.Graphics.Quality.drawResolution.get();
				}
				@Override
				public void setDrawResolution(HorizontalResolution newHorizontalResolution)
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
				public VerticalQuality getVerticalQuality()
				{
					return Config.Client.Graphics.Quality.verticalQuality.get();
				}
				@Override
				public void setVerticalQuality(VerticalQuality newVerticalQuality)
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
				public HorizontalQuality getHorizontalQuality()
				{
					return Config.Client.Graphics.Quality.horizontalQuality.get();
				}
				@Override
				public void setHorizontalQuality(HorizontalQuality newHorizontalQuality)
				{
					Config.Client.Graphics.Quality.horizontalQuality.set(newHorizontalQuality);
				}

				@Override
				public DropoffQuality getDropoffQuality() {
					return Config.Client.Graphics.Quality.dropoffQuality.get();
				}
				@Override
				public void setDropoffQuality(DropoffQuality newDropoffQuality) {
					Config.Client.Graphics.Quality.dropoffQuality.set(newDropoffQuality);
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
				public FogDistance getFogDistance()
				{
					return Config.Client.Graphics.FogQuality.fogDistance.get();
				}
				@Override
				public void setFogDistance(FogDistance newFogDistance)
				{
					Config.Client.Graphics.FogQuality.fogDistance.set(newFogDistance);
				}


				@Override
				public FogDrawMode getFogDrawMode()
				{
					return Config.Client.Graphics.FogQuality.fogDrawMode.get();
				}

				@Override
				public void setFogDrawMode(FogDrawMode setFogDrawMode)
				{
					Config.Client.Graphics.FogQuality.fogDrawMode.set(setFogDrawMode);
				}


				@Override
				public FogColorMode getFogColorMode()
				{
					return Config.Client.Graphics.FogQuality.fogColorMode.get();
				}

				@Override
				public void setFogColorMode(FogColorMode newFogColorMode)
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
					public FogSetting.FogType getFarFogType() {
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
					public void setFarFogType(FogSetting.FogType newFarFogType) {
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
						public HeightFogMixMode getHeightFogMixMode() {
							return Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogMixMode.get();
						}
						@Override
						public HeightFogMode getHeightFogMode() {
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
						public FogSetting.FogType getHeightFogType() {
							return Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogType.get();
						}
						@Override
						public double getHeightFogDensity() {
							return Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogDensity.get();
						}

						@Override
						public void setHeightFogMixMode(HeightFogMixMode newHeightFogMixMode) {
							Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogMixMode.set(newHeightFogMixMode);
						}
						@Override
						public void setHeightFogMode(HeightFogMode newHeightFogMode) {
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
						public void setHeightFogType(FogSetting.FogType newHeightFogType) {
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
				public VanillaOverdraw getVanillaOverdraw()
				{
					return Config.Client.Graphics.AdvancedGraphics.vanillaOverdraw.get();
				}
				@Override
				public void setVanillaOverdraw(VanillaOverdraw newVanillaOverdraw)
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
			public GenerationPriority getGenerationPriority()
			{
				return Config.Client.WorldGenerator.generationPriority.get();
			}
			@Override
			public void setGenerationPriority(GenerationPriority newGenerationPriority)
			{
				Config.Client.WorldGenerator.generationPriority.set(newGenerationPriority);
			}


			@Override
			public DistanceGenerationMode getDistanceGenerationMode()
			{
				return Config.Client.WorldGenerator.distanceGenerationMode.get();
			}
			@Override
			public void setDistanceGenerationMode(DistanceGenerationMode newDistanceGenerationMode)
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
			public BlocksToAvoid getBlocksToAvoid()
			{
				return Config.Client.WorldGenerator.blocksToAvoid.get();
			}
			@Override
			public void setBlockToAvoid(BlocksToAvoid newBlockToAvoid)
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
			public LightGenerationMode getLightGenerationMode()
			{
				return Config.Client.WorldGenerator.lightGenerationMode.get();
			}
			@Override
			public void setLightGenerationMode(LightGenerationMode newLightGenerationMode)
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
			public ServerFolderNameMode getServerFolderNameMode()
			{
				return Config.Client.Multiplayer.serverFolderNameMode.get();
			}
			@Override
			public void setServerFolderNameMode(ServerFolderNameMode newServerFolderNameMode)
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
				public RendererType getRendererType() {
					return Config.Client.Advanced.Debugging.rendererType.get();
				}
				@Override
				public void setRendererType(RendererType newRenderType) {
					Config.Client.Advanced.Debugging.rendererType.set(newRenderType);
				}

				@Override
				public DebugMode getDebugMode()
				{
					return Config.Client.Advanced.Debugging.debugMode.get();
				}
				@Override
				public void setDebugMode(DebugMode newDebugMode)
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
					public LoggerMode getLogWorldGenEvent() {
						return Config.Client.Advanced.Debugging.DebugSwitch.logWorldGenEvent.get();
					}
					@Override
					public void setLogWorldGenEvent(LoggerMode newLogWorldGenEvent) {
						Config.Client.Advanced.Debugging.DebugSwitch.logWorldGenEvent.set(newLogWorldGenEvent);
					}

					@Override
					public LoggerMode getLogWorldGenPerformance() {
						return Config.Client.Advanced.Debugging.DebugSwitch.logWorldGenPerformance.get();
					}
					@Override
					public void setLogWorldGenPerformance(LoggerMode newLogWorldGenPerformance) {
						Config.Client.Advanced.Debugging.DebugSwitch.logWorldGenPerformance.set(newLogWorldGenPerformance);
					}

					@Override
					public LoggerMode getLogWorldGenLoadEvent() {
						return Config.Client.Advanced.Debugging.DebugSwitch.logWorldGenLoadEvent.get();
					}
					@Override
					public void setLogWorldGenLoadEvent(LoggerMode newLogWorldGenLoadEvent) {
						Config.Client.Advanced.Debugging.DebugSwitch.logWorldGenLoadEvent.set(newLogWorldGenLoadEvent);
					}

					@Override
					public LoggerMode getLogLodBuilderEvent() {
						return Config.Client.Advanced.Debugging.DebugSwitch.logLodBuilderEvent.get();
					}
					@Override
					public void setLogLodBuilderEvent(LoggerMode newLogLodBuilderEvent) {
						Config.Client.Advanced.Debugging.DebugSwitch.logLodBuilderEvent.set(newLogLodBuilderEvent);
					}

					@Override
					public LoggerMode getLogRendererBufferEvent() {
						return Config.Client.Advanced.Debugging.DebugSwitch.logRendererBufferEvent.get();
					}
					@Override
					public void setLogRendererBufferEvent(LoggerMode newLogRendererBufferEvent) {
						Config.Client.Advanced.Debugging.DebugSwitch.logRendererBufferEvent.set(newLogRendererBufferEvent);
					}

					@Override
					public LoggerMode getLogRendererGLEvent() {
						return Config.Client.Advanced.Debugging.DebugSwitch.logRendererGLEvent.get();
					}
					@Override
					public void setLogRendererGLEvent(LoggerMode newLogRendererGLEvent) {
						Config.Client.Advanced.Debugging.DebugSwitch.logRendererGLEvent.set(newLogRendererGLEvent);
					}

					@Override
					public LoggerMode getLogFileReadWriteEvent() {
						return Config.Client.Advanced.Debugging.DebugSwitch.logFileReadWriteEvent.get();
					}
					@Override
					public void setLogFileReadWriteEvent(LoggerMode newLogFileReadWriteEvent) {
						Config.Client.Advanced.Debugging.DebugSwitch.logFileReadWriteEvent.set(newLogFileReadWriteEvent);
					}

					@Override
					public LoggerMode getLogFileSubDimEvent() {
						return Config.Client.Advanced.Debugging.DebugSwitch.logFileSubDimEvent.get();
					}
					@Override
					public void setLogFileSubDimEvent(LoggerMode newLogFileSubDimEvent) {
						Config.Client.Advanced.Debugging.DebugSwitch.logFileSubDimEvent.set(newLogFileSubDimEvent);
					}

					@Override
					public LoggerMode getLogNetworkEvent() {
						return Config.Client.Advanced.Debugging.DebugSwitch.logNetworkEvent.get();
					}
					@Override
					public void setLogNetworkEvent(LoggerMode newLogNetworkEvent) {
						Config.Client.Advanced.Debugging.DebugSwitch.logNetworkEvent.set(newLogNetworkEvent);
					}
				}
			}


			public static class Buffers implements IBuffers
			{

				@Override
				public GpuUploadMethod getGpuUploadMethod()
				{
					return Config.Client.Advanced.Buffers.gpuUploadMethod.get();
				}
				@Override
				public void setGpuUploadMethod(GpuUploadMethod newDisableVanillaFog)
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
				public BufferRebuildTimes getRebuildTimes()
				{
					return Config.Client.Advanced.Buffers.rebuildTimes.get();
				}
				@Override
				public void setRebuildTimes(BufferRebuildTimes newBufferRebuildTimes)
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
