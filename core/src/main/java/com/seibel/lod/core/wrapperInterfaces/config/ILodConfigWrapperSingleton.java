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

package com.seibel.lod.core.wrapperInterfaces.config;

import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.enums.rendering.EFogDrawMode;
import com.seibel.lod.core.enums.rendering.EFogColorMode;
import com.seibel.lod.core.enums.rendering.EFogDistance;
import com.seibel.lod.api.items.enums.rendering.ERendererMode;
import com.seibel.lod.core.enums.config.*;
import com.seibel.lod.core.enums.rendering.*;
import com.seibel.lod.core.dependencyInjection.IBindable;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.render.fog.FogSettings;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;

/**
 * Use the real config rather than these getters
 */
@Deprecated
public interface ILodConfigWrapperSingleton extends IBindable
{
	IClient client();

	@Deprecated
	interface IClient
	{
		IGraphics graphics();
		IWorldGenerator worldGenerator();
		IMultiplayer multiplayer();
		IAdvanced advanced();

		boolean getOptionsButton();
		void setOptionsButton(boolean newOptionsButton);
		
		
		//==================//
		// Graphics Configs //
		//==================//
		@Deprecated
		interface IGraphics
		{
			IQuality quality();
			IFogQuality fogQuality();
			IAdvancedGraphics advancedGraphics();

			@Deprecated
			interface IQuality
			{
				EHorizontalResolution getDrawResolution();
				void setDrawResolution(EHorizontalResolution newHorizontalResolution);

				int getLodChunkRenderDistance();
				void setLodChunkRenderDistance(int newLodChunkRenderDistance);

				EVerticalQuality getVerticalQuality();
				void setVerticalQuality(EVerticalQuality newVerticalQuality);

				int getHorizontalScale();
				void setHorizontalScale(int newHorizontalScale);

				EHorizontalQuality getHorizontalQuality();
				void setHorizontalQuality(EHorizontalQuality newHorizontalQuality);

				public void setTransparency(ETransparency newTransparency);

				public ETransparency getTransparency();

				int getLodBiomeBlending();
				void setLodBiomeBlending(int newLodBiomeBlending);
			}
			@Deprecated
			interface IFogQuality
			{
				EFogDistance getFogDistance();
				void setFogDistance(EFogDistance newFogDistance);

				EFogDrawMode getFogDrawMode();
				void setFogDrawMode(EFogDrawMode newFogDrawMode);

				EFogColorMode getFogColorMode();
				void setFogColorMode(EFogColorMode newFogColorMode);

				boolean getDisableVanillaFog();
				void setDisableVanillaFog(boolean newDisableVanillaFog);

				IAdvancedFog advancedFog();
				@Deprecated
				interface IAdvancedFog {
					double getFarFogStart();
					void setFarFogStart(double newFarFogStart);

					double getFarFogEnd();
					void setFarFogEnd(double newFarFogEnd);

					double getFarFogMin();
					void setFarFogMin(double newFarFogMin);

					double getFarFogMax();
					void setFarFogMax(double newFarFogMax);

					EFogFalloff getFarFogType();
					void setFarFogType(EFogFalloff newFarFogType);

					double getFarFogDensity();
					void setFarFogDensity(double newFarFogDensity);

					IHeightFog heightFog();
					@Deprecated
					interface IHeightFog {
						EHeightFogMixMode getHeightFogMixMode();
						void setHeightFogMixMode(EHeightFogMixMode newHeightFogMixMode);

						EHeightFogMode getHeightFogMode();
						void setHeightFogMode(EHeightFogMode newHeightFogMode);

						double getHeightFogHeight();
						void setHeightFogHeight(double newHeightFogHeight);

						double getHeightFogStart();
						void setHeightFogStart(double newHeightFogStart);

						double getHeightFogEnd();
						void setHeightFogEnd(double newHeightFogEnd);

						double getHeightFogMin();
						void setHeightFogMin(double newHeightFogMin);

						double getHeightFogMax();
						void setHeightFogMax(double newHeightFogMax);

						EFogFalloff getHeightFogType();
						void setHeightFogType(EFogFalloff newFarFogType);

						double getHeightFogDensity();
						void setHeightFogDensity(double newHeightFogDensity);

						default FogSettings computeHeightFogSetting() {
							return new FogSettings(
									Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogDensity.get(),
									Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogEnd.get(),
									Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogMin.get(),
									Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogMax.get(),
									Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogDensity.get(),
									Config.Client.Graphics.FogQuality.AdvancedFog.HeightFog.heightFogType.get()
							);
						}
					}
					default FogSettings computeFarFogSetting() {
						return new FogSettings(
								getFarFogStart(),
								getFarFogEnd(),
								getFarFogMin(),
								getFarFogMax(),
								getFarFogDensity(),
								getFarFogType()
						);
					}
				}
			}
			@Deprecated
			interface IAdvancedGraphics
			{
				boolean getDisableDirectionalCulling();
				void setDisableDirectionalCulling(boolean newDisableDirectionalCulling);

				EVanillaOverdraw getVanillaOverdraw();
				void setVanillaOverdraw(EVanillaOverdraw newVanillaOverdraw);

				int getOverdrawOffset();
				void setOverdrawOffset(int newOverdrawOffset);


				/* Disabled for now due to implementation issues.
				MinDefaultMax<Integer> VANILLA_CULLING_RANGE_MIN_DEFAULT_MAX = new MinDefaultMax<Integer>(0, 32, 512);
				String VANILLA_CULLING_RANGE_DESC = ""
						+ " This indicates the minimum range where back sides of blocks start get get culled. \n"
						+ " Higher settings will make terrain look good when looking backwards \n"
						+ " when changing speeds quickly, but will increase upload times and GPU usage.";
				int getBacksideCullingRange();
				void setBacksideCullingRange(int newBacksideCullingRange);*/

				boolean getUseExtendedNearClipPlane();
				void setUseExtendedNearClipPlane(boolean newUseExtendedNearClipPlane);

				double getBrightnessMultiplier();
				void setBrightnessMultiplier(double newBrightnessMultiplier);

				double getSaturationMultiplier();
				void setSaturationMultiplier(double newSaturationMultiplier);

				boolean getEnableCaveCulling();
				void setEnableCaveCulling(boolean newEnableCaveCulling);

				int getCaveCullingHeight();
				void setCaveCullingHeight(int newCaveCullingHeight);

				int getEarthCurveRatio();
				void setEarthCurveRatio(int newEarthCurveRatio);

			}
		}




		//=====================//
		// Multiplayer Configs //
		//=====================//
		@Deprecated
		interface IMultiplayer
		{
			EServerFolderNameMode getServerFolderNameMode();
			void setServerFolderNameMode(EServerFolderNameMode newServerFolderNameMode);

			double getMultiDimensionRequiredSimilarity();
			void setMultiDimensionRequiredSimilarity(double newMultiDimensionMinimumSimilarityPercent);
		}


		
		
		
		//========================//
		// WorldGenerator Configs //
		//========================//
		@Deprecated
		interface IWorldGenerator
		{
			boolean getEnableDistantGeneration();
			void setEnableDistantGeneration(boolean newEnableDistantGeneration);

			EDistanceGenerationMode getDistanceGenerationMode();
			void setDistanceGenerationMode(EDistanceGenerationMode newDistanceGenerationMode);

			ELightGenerationMode getLightGenerationMode();
			void setLightGenerationMode(ELightGenerationMode newLightGenerationMode);

			EGenerationPriority getGenerationPriority();
			void setGenerationPriority(EGenerationPriority newGenerationPriority);
			
			default EGenerationPriority getResolvedGenerationPriority() {
				EGenerationPriority priority = Config.Client.WorldGenerator.generationPriority.get();
				IMinecraftClientWrapper MC = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class);
				if (priority == EGenerationPriority.AUTO)
					priority = MC.hasSinglePlayerServer() ? EGenerationPriority.FAR_FIRST : EGenerationPriority.BALANCED;
				return priority;
			}

			EBlocksToAvoid getBlocksToAvoid();
			void setBlockToAvoid(EBlocksToAvoid newBlockToAvoid);

			Boolean getTintWithAvoidedBlocks();
			void setTintWithAvoidedBlocks(Boolean shouldTint);
		}
		
		
		
		
		//============================//
		// AdvancedModOptions Configs //
		//============================//
		@Deprecated
		interface IAdvanced
		{
			IThreading threading();
			IDebugging debugging();
			IBuffers buffers();

			@Deprecated
			interface IThreading
			{
				double getNumberOfWorldGenerationThreads();
				void setNumberOfWorldGenerationThreads(double newNumberOfWorldGenerationThreads);
				default int _getWorldGenerationThreadPoolSize()
				{
					return Config.Client.Advanced.Threading.numberOfWorldGenerationThreads.get()<1 ? 1 :
							(int) Math.ceil(Config.Client.Advanced.Threading.numberOfWorldGenerationThreads.get());
				}
				default double _getWorldGenerationPartialRunTime()
				{
					return Config.Client.Advanced.Threading.numberOfWorldGenerationThreads.get()>1 ? 1.0 : Config.Client.Advanced.Threading.numberOfWorldGenerationThreads.get();
				}

				int getNumberOfBufferBuilderThreads();
				void setNumberOfBufferBuilderThreads(int newNumberOfWorldBuilderThreads);
			}
			@Deprecated
			interface IDebugging
			{
				ERendererMode getRendererType();
				void setRendererType(ERendererMode newRendererType);

				EDebugMode getDebugMode();
				void setDebugMode(EDebugMode newDebugMode);

				boolean getDebugKeybindingsEnabled();
				void setDebugKeybindingsEnabled(boolean newEnableDebugKeybindings);

				IDebugSwitch debugSwitch();
				@Deprecated
				interface IDebugSwitch
				{
					/*
					 * The logging switches available:
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

					ELoggerMode getLogWorldGenEvent();
					void setLogWorldGenEvent(ELoggerMode newLogWorldGenEvent);

					ELoggerMode getLogWorldGenPerformance();
					void setLogWorldGenPerformance(ELoggerMode newLogWorldGenPerformance);

					ELoggerMode getLogWorldGenLoadEvent();
					void setLogWorldGenLoadEvent(ELoggerMode newLogWorldGenLoadEvent);

					ELoggerMode getLogLodBuilderEvent();
					void setLogLodBuilderEvent(ELoggerMode newLogLodBuilderEvent);

					ELoggerMode getLogRendererBufferEvent();
					void setLogRendererBufferEvent(ELoggerMode newLogRendererBufferEvent);

					ELoggerMode getLogRendererGLEvent();
					void setLogRendererGLEvent(ELoggerMode newLogRendererGLEvent);

					ELoggerMode getLogFileReadWriteEvent();
					void setLogFileReadWriteEvent(ELoggerMode newLogFileReadWriteEvent);

					ELoggerMode getLogFileSubDimEvent();
					void setLogFileSubDimEvent(ELoggerMode newLogFileSubDimEvent);

					ELoggerMode getLogNetworkEvent();
					void setLogNetworkEvent(ELoggerMode newLogNetworkEvent);
				}
			}
			@Deprecated
			interface IBuffers
			{
				EGpuUploadMethod getGpuUploadMethod();
				void setGpuUploadMethod(EGpuUploadMethod newGpuUploadMethod);

				int getGpuUploadPerMegabyteInMilliseconds();
				void setGpuUploadPerMegabyteInMilliseconds(int newMilliseconds);

				EBufferRebuildTimes getRebuildTimes();
				void setRebuildTimes(EBufferRebuildTimes newBufferRebuildTimes);
			}

			boolean getLodOnlyMode();
			void setLodOnlyMode(boolean newLodOnlyMode);
		}
	}
	
}
