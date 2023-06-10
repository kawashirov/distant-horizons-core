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

package com.seibel.lod.core.config;


import com.seibel.lod.api.enums.config.*;
import com.seibel.lod.api.enums.config.quickOptions.EQuickQuality;
import com.seibel.lod.api.enums.config.quickOptions.EQuickThread;
import com.seibel.lod.api.enums.rendering.*;
import com.seibel.lod.api.enums.worldGeneration.EDhApiDistantGeneratorMode;
import com.seibel.lod.core.config.eventHandlers.RenderCacheConfigEventHandler;
import com.seibel.lod.core.config.types.*;
import com.seibel.lod.core.config.types.enums.*;
import com.seibel.lod.coreapi.ModInfo;

import java.util.*;


/**
 * This handles any configuration the user has access to.
 * 
 * @author coolGi
 * @version 2023-6-7
 */

public class Config
{
	// TODO update this diagram
    // CONFIG STRUCTURE
    // 	-> Client
    //		|
    //		|-> Graphics
    //		|		|-> Quality
    //		|		|-> FogQuality
    //		|		|-> AdvancedGraphics
    //      |       		|-> NoiseTextureSettings
    //		|
    //		|-> World Generation
    //		|
    //		|-> Advanced
    //				|-> Threads
    //				|-> GpuBuffers
    //				|-> Debugging

    // Since the original config system uses forge stuff, that means we have to rewrite the whole config system

    public static ConfigCategory client = new ConfigCategory.Builder().set(Client.class).build();


    public static class Client
    {
		// TODO modify debugging.rendererMode
		public static ConfigEntry<Boolean> quickEnableRendering = new ConfigEntry.Builder<Boolean>()
				.set(true)
				.comment(""
						+ "If true, Distant Horizons will render LODs beyond the vanilla render distance."
						+ "")
				.setAppearance(ConfigEntryAppearance.ONLY_IN_GUI) // TODO set when the game boots
				//.addListener(null) // TODO add listener
				.build();
		
		public static ConfigLinkedEntry quickLodChunkRenderDistance = new ConfigLinkedEntry(Advanced.Graphics.Quality.lodChunkRenderDistance);
		
		public static ConfigEntry<EQuickQuality> quickQualitySetting = new ConfigEntry.Builder<EQuickQuality>()
				.set(EQuickQuality.MEDIUM)
				.comment(""
						+ "Changing this setting will modify a number of different settings that will change the \n"
						+ "visual fidelity of the rendered LODs.\n"
						+ "\n"
						+ "Higher settings will improve the graphical quality while increasing GPU and memory use.\n"
						+ "")
				.setAppearance(ConfigEntryAppearance.ONLY_IN_GUI) // TODO set when the game boots
				//.addListener(null) // TODO add listener
				.build();
		
		public static ConfigEntry<EQuickThread> quickThreadSetting = new ConfigEntry.Builder<EQuickThread>()
				.set(EQuickThread.BALANCED)
				.comment(""
						+ "Changing this setting will modify a number of different settings that will change \n"
						+ "the load that Distant Horizons is allowed to put on your CPU. \n"
						+ "\n"
						+ "Higher options will improve LOD generation and loading speed, \n"
						+ "but will increase CPU load and may introduce stuttering.\n"
						+ "")
				.setAppearance(ConfigEntryAppearance.ONLY_IN_GUI) // TODO set when the game boots
				//.addListener(null) // TODO add listener
				.build();
		
		public static ConfigLinkedEntry quickEnableWorldGenerator = new ConfigLinkedEntry(Advanced.WorldGenerator.enableDistantGeneration);
		
		public static ConfigEntry<Boolean> optionsButton = new ConfigEntry.Builder<Boolean>()
				.set(true)
				.comment("" +
    				"Should Distant Horizon's config button appear in the options screen next to fov slider?")
				.build();
		
		
		
		public static ConfigCategory advanced = new ConfigCategory.Builder().set(Advanced.class).build();
		public static ConfigCategory resetSettingsConfirmation = new ConfigCategory.Builder().set(ResetConfirmation.class).build();
		
		public static class Advanced
		{
			public static ConfigCategory graphics = new ConfigCategory.Builder().set(Graphics.class).build();
			public static ConfigCategory worldGenerator = new ConfigCategory.Builder().set(WorldGenerator.class).build();
			public static ConfigCategory multiplayer = new ConfigCategory.Builder().set(Multiplayer.class).build();
			public static ConfigCategory threading = new ConfigCategory.Builder().set(MultiThreading.class).build();
			public static ConfigCategory buffers = new ConfigCategory.Builder().set(GpuBuffers.class).build();
			public static ConfigCategory autoUpdater = new ConfigCategory.Builder().set(AutoUpdater.class).build();
			
			public static ConfigCategory logging = new ConfigCategory.Builder().set(Logging.class).build();
			public static ConfigCategory debugging = new ConfigCategory.Builder().set(Debugging.class).build();
			
			
			public static class Graphics
			{
				public static ConfigCategory quality = new ConfigCategory.Builder().set(Quality.class).build();
				public static ConfigCategory fog = new ConfigCategory.Builder().set(Fog.class).build();
				public static ConfigCategory noiseTextureSettings = new ConfigCategory.Builder().set(NoiseTextureSettings.class).build();
				public static ConfigCategory advancedGraphics = new ConfigCategory.Builder().set(AdvancedGraphics.class).build();
				
				
				public static class Quality
				{
					public static ConfigEntry<EHorizontalResolution> drawResolution = new ConfigEntry.Builder<EHorizontalResolution>()
							.set(EHorizontalResolution.BLOCK)
							.comment(""
									+ "What is the maximum detail LODs should be drawn at? \n"
									+ "Higher settings will increase memory and GPU usage. \n"
									+ "\n"
									+ EHorizontalResolution.CHUNK + ": render 1 LOD for each Chunk. \n"
									+ EHorizontalResolution.HALF_CHUNK + ": render 4 LODs for each Chunk. \n"
									+ EHorizontalResolution.FOUR_BLOCKS + ": render 16 LODs for each Chunk. \n"
									+ EHorizontalResolution.TWO_BLOCKS + ": render 64 LODs for each Chunk. \n"
									+ EHorizontalResolution.BLOCK + ": render 256 LODs for each Chunk (width of one block). \n"
									+ "\n"
									+ "Lowest Quality: " + EHorizontalResolution.CHUNK + "\n"
									+ "Highest Quality: " + EHorizontalResolution.BLOCK)
							.addListener(RenderCacheConfigEventHandler.INSTANCE)
							.setPerformance(ConfigEntryPerformance.MEDIUM)
							.build();
					
					public static ConfigEntry<Integer> lodChunkRenderDistance = new ConfigEntry.Builder<Integer>()
							.setMinDefaultMax(32, 128, 4096)
							.comment("The radius of the mod's render distance. (measured in chunks)")
							.setPerformance(ConfigEntryPerformance.HIGH)
							.build();
					
					public static ConfigEntry<EVerticalQuality> verticalQuality = new ConfigEntry.Builder<EVerticalQuality>()
							.set(EVerticalQuality.MEDIUM)
							.comment(""
									+ "This indicates how well LODs will represent \n"
									+ "overhangs, caves, floating islands, etc. \n"
									+ "Higher options will make the world more accurate, but"
									+ "will increase memory and GPU usage. \n"
									+ "\n"
									+ "Lowest Quality: " + EVerticalQuality.HEIGHT_MAP + "\n"
									+ "Highest Quality: " + EVerticalQuality.ULTRA)
							.setPerformance(ConfigEntryPerformance.VERY_HIGH)
							.addListener(RenderCacheConfigEventHandler.INSTANCE)
							.build();
					
					// TODO merge with horizontal quality
					public static ConfigEntry<Integer> horizontalScale = new ConfigEntry.Builder<Integer>()
							.setMinDefaultMax(2, 12, 64)
							.comment(""
									+ "This indicates how quickly fake chunks decrease in quality the further away they are. \n"
									+ "Higher settings will render higher quality fake chunks farther away, \n"
									+ " but will increase memory and GPU usage.")
							.build();
					
					public static ConfigEntry<EHorizontalQuality> horizontalQuality = new ConfigEntry.Builder<EHorizontalQuality>()
							.set(EHorizontalQuality.MEDIUM)
							.comment(""
									+ "This indicates how quickly LODs decrease in quality the further away they are. \n"
									+ "Higher settings will render higher quality fake chunks farther away, \n"
									+ "but will increase memory and GPU usage.")
							.setPerformance(ConfigEntryPerformance.MEDIUM)
							.build();
					
					public static ConfigEntry<ETransparency> transparency = new ConfigEntry.Builder<ETransparency>()
							.set(ETransparency.COMPLETE)
							.comment(""
									+ "How should LOD transparency be handled. \n"
									+ "\n"
									+ ETransparency.COMPLETE + ": LODs will render transparent. \n"
									+ ETransparency.FAKE + ": LODs will be opaque, but shaded to match the blocks underneath. \n"
									+ ETransparency.DISABLED + ": LODs will be opaque. \n"
									+ "")
							.setPerformance(ConfigEntryPerformance.MEDIUM)
							.build();
					
					// TODO fixme
//					public static ConfigEntry<Integer> lodBiomeBlending = new ConfigEntry.Builder<Integer>()
//							.setMinDefaultMax(0,1,7)
//							.comment(""
//									+ "This is the same as vanilla Biome Blending settings for Lod area. \n"
//									+ "    Note that anything other than '0' will greatly effect Lod building time \n"
//									+ "     and increase triangle count. The cost on chunk generation speed is also \n"
//									+ "     quite large if set too high.\n"
//									+ "\n"
//									+ "    '0' equals to Vanilla Biome Blending of '1x1' or 'OFF', \n"
//									+ "    '1' equals to Vanilla Biome Blending of '3x3', \n"
//									+ "    '2' equals to Vanilla Biome Blending of '5x5'...")
//							.build();
				}
				
				public static class Fog
				{
					public static ConfigEntry<EFogDistance> fogDistance = new ConfigEntry.Builder<EFogDistance>()
							.set(EFogDistance.FAR)
							.comment("At what distance should Fog be drawn on the LODs?")
							.setPerformance(ConfigEntryPerformance.NONE)
							.build();
					
					public static ConfigEntry<EFogDrawMode> fogDrawMode = new ConfigEntry.Builder<EFogDrawMode>()
							.set(EFogDrawMode.FOG_ENABLED)
							.comment(""
									+ "When should fog be drawn? \n"
									+ "\n"
									+ EFogDrawMode.USE_OPTIFINE_SETTING + ": Use whatever Fog setting Optifine is using.\n"
									+ "If Optifine isn't installed this defaults to " + EFogDrawMode.FOG_ENABLED + ". \n"
									+ EFogDrawMode.FOG_ENABLED + ": Never draw fog on the LODs \n"
									+ EFogDrawMode.FOG_DISABLED + ": Always draw fast fog on the LODs \n"
									+ "\n"
									+ "Disabling fog will improve GPU performance.")
							.setPerformance(ConfigEntryPerformance.VERY_LOW)
							.build();
					
					public static ConfigEntry<EFogColorMode> fogColorMode = new ConfigEntry.Builder<EFogColorMode>()
							.set(EFogColorMode.USE_WORLD_FOG_COLOR)
							.comment(""
									+ "What color should fog use? \n"
									+ "\n"
									+ EFogColorMode.USE_WORLD_FOG_COLOR + ": Use the world's fog color. \n"
									+ EFogColorMode.USE_SKY_COLOR + ": Use the sky's color.")
							.setPerformance(ConfigEntryPerformance.NONE)
							.build();
					
					public static ConfigEntry<Boolean> disableVanillaFog = new ConfigEntry.Builder<Boolean>()
							.set(true)
							.comment(""
									+ "Should Minecraft's fog be disabled? \n"
									+ "\n"
									+ "Note: Other mods may conflict with this setting.")
							.build();
					
					
					
					public static ConfigCategory advancedFog = new ConfigCategory.Builder().set(AdvancedFog.class).build();
					
					
					public static class AdvancedFog
					{
						private static final Double FOG_RANGE_MIN = 0.0;
						private static final Double FOG_RANGE_MAX = Math.sqrt(2.0);
						
						public static ConfigEntry<Double> farFogStart = new ConfigEntry.Builder<Double>()
								.setMinDefaultMax(FOG_RANGE_MIN, 0.0, FOG_RANGE_MAX)
								.comment(""
										+ "At what distance should the far fog start? \n"
										+ "\n"
										+ "0.0: Fog starts at the player's position. \n"
										+ "1.0: Fog starts at the closest edge of the vanilla render distance. \n"
										+ "1.414: Fog starts at the corner of the vanilla render distance.")
								.build();
						
						public static ConfigEntry<Double> farFogEnd = new ConfigEntry.Builder<Double>()
								.setMinDefaultMax(FOG_RANGE_MIN, 1.0, FOG_RANGE_MAX)
								.comment(""
										+ "Where should the far fog end? \n"
										+ "\n"
										+ "0.0: Fog ends at player's position.\n"
										+ "1.0: Fog ends at the closest edge of the vanilla render distance. \n"
										+ "1.414: Fog ends at the corner of the vanilla render distance.")
								.build();
						
						public static ConfigEntry<Double> farFogMin = new ConfigEntry.Builder<Double>()
								.setMinDefaultMax(-5.0,0.0, FOG_RANGE_MAX)
								.comment(""
										+ "What is the minimum fog thickness? \n"
										+ "\n"
										+ "0.0: No fog. \n"
										+ "1.0: Fully opaque fog.")
								.build();
						
						public static ConfigEntry<Double> farFogMax = new ConfigEntry.Builder<Double>()
								.setMinDefaultMax(FOG_RANGE_MIN, 1.0, 5.0)
								.comment(""
										+ "What is the maximum fog thickness? \n"
										+ "\n"
										+ "0.0: No fog. \n"
										+ "1.0: Fully opaque fog.")
								.build();
						
						public static ConfigEntry<EFogFalloff> farFogFalloff = new ConfigEntry.Builder<EFogFalloff>()
								.set(EFogFalloff.EXPONENTIAL_SQUARED)
								.comment(""
										+ "How should the fog thickness should be calculated? \n"
										+ "\n"
										+ EFogFalloff.LINEAR + ": Linear based on distance (will ignore 'density')\n"
										+ EFogFalloff.EXPONENTIAL + ": 1/(e^(distance*density)) \n"
										+ EFogFalloff.EXPONENTIAL_SQUARED + ": 1/(e^((distance*density)^2)")
								.build();
						
						public static ConfigEntry<Double> farFogDensity = new ConfigEntry.Builder<Double>()
								.setMinDefaultMax(0.01,2.5, 50.0)
								.comment(""
										+ "Used in conjunction with the Fog Falloff.")
								.build();
						
						
						
						public static ConfigCategory heightFog = new ConfigCategory.Builder().set(HeightFog.class).build();
						
						
						public static class HeightFog
						{
							public static ConfigUIComment heightFogConfigScreenNote = new ConfigUIComment();
							
							public static ConfigEntry<EHeightFogMixMode> heightFogMixMode = new ConfigEntry.Builder<EHeightFogMixMode>()
									.set(EHeightFogMixMode.BASIC)
									.comment(""
											+ "How should height effect the fog thickness? \n"
											+ "Note: height fog is combined with the other fog settings. \n"
											+ "\n"
											+ EHeightFogMixMode.BASIC + ": No special height fog effect. Fog is calculated based on camera distance \n"
											+ EHeightFogMixMode.IGNORE_HEIGHT + ": Ignore height completely. Fog is only calculated with horizontal distance \n"
											+ EHeightFogMixMode.ADDITION + ": heightFog + farFog \n"
											+ EHeightFogMixMode.MAX + ": max(heightFog, farFog) \n"
											+ EHeightFogMixMode.MULTIPLY + ": heightFog * farFog \n"
											+ EHeightFogMixMode.INVERSE_MULTIPLY + ": 1 - (1-heightFog) * (1-farFog) \n"
											+ EHeightFogMixMode.LIMITED_ADDITION + ": farFog + max(farFog, heightFog) \n"
											+ EHeightFogMixMode.MULTIPLY_ADDITION + ": farFog + farFog * heightFog \n"
											+ EHeightFogMixMode.INVERSE_MULTIPLY_ADDITION + ": farFog + 1 - (1-heightFog) * (1-farFog) \n"
											+ EHeightFogMixMode.AVERAGE + ": farFog*0.5 + heightFog*0.5 \n"
											+ "\n"
											+ "Note: height fog settings are ignored if '"+EHeightFogMixMode.BASIC+"' or '"+EHeightFogMixMode.IGNORE_HEIGHT+"' are selected.")
									.build();
							
							public static ConfigEntry<EHeightFogMode> heightFogMode = new ConfigEntry.Builder<EHeightFogMode>()
									.set(EHeightFogMode.ABOVE_AND_BELOW_CAMERA)
									.comment(""
											+ "Where should the height fog start? \n"
											+ "\n"
											+ EHeightFogMode.ABOVE_CAMERA + ": Height fog starts at the camera and goes towards the sky \n"
											+ EHeightFogMode.BELOW_CAMERA + ": Height fog starts at the camera and goes towards the void \n"
											+ EHeightFogMode.ABOVE_AND_BELOW_CAMERA + ": Height fog starts from the camera to goes towards both the sky and void \n"
											+ EHeightFogMode.ABOVE_SET_HEIGHT + ": Height fog starts from a set height and goes towards the sky \n"
											+ EHeightFogMode.BELOW_SET_HEIGHT + ": Height fog starts from a set height and goes towards the void \n"
											+ EHeightFogMode.ABOVE_AND_BELOW_SET_HEIGHT + ": Height fog starts from a set height and goes towards both the sky and void")
									.build();
							
							public static ConfigEntry<Double> heightFogHeight = new ConfigEntry.Builder<Double>()
									.setMinDefaultMax(-4096.0, 70.0, 4096.0)
									.comment("If the height fog is calculated around a set height, what is that height position?")
									.build();
							
							public static ConfigEntry<Double> heightFogStart = new ConfigEntry.Builder<Double>()
									.setMinDefaultMax(FOG_RANGE_MIN, 0.0, FOG_RANGE_MAX)
									.comment(""
											+ "Should the start of the height fog be offset? \n"
											+ "\n"
											+ "0.0: Fog start with no offset.\n"
											+ "1.0: Fog start with offset of the entire world's height. (Includes depth)")
									.build();
							
							public static ConfigEntry<Double> heightFogEnd = new ConfigEntry.Builder<Double>()
									.setMinDefaultMax(FOG_RANGE_MIN, 1.0, FOG_RANGE_MAX)
									.comment(""
											+ "Should the end of the height fog be offset? \n"
											+ "\n"
											+ "0.0: Fog end with no offset.\n"
											+ "1.0: Fog end with offset of the entire world's height. (Include depth)")
									.build();
							
							public static ConfigEntry<Double> heightFogMin = new ConfigEntry.Builder<Double>()
									.setMinDefaultMax(-5.0, 0.0, FOG_RANGE_MAX)
									.comment(""
											+ "What is the minimum fog thickness? \n"
											+ "\n"
											+ "0.0: No fog. \n"
											+ "1.0: Fully opaque fog.")
									.build();
							
							public static ConfigEntry<Double> heightFogMax = new ConfigEntry.Builder<Double>()
									.setMinDefaultMax(FOG_RANGE_MIN, 1.0, 5.0)
									.comment(""
											+ "What is the maximum fog thickness? \n"
											+ "\n"
											+ "0.0: No fog. \n"
											+ "1.0: Fully opaque fog.")
									.build();
							
							public static ConfigEntry<EFogFalloff> heightFogFalloff = new ConfigEntry.Builder<EFogFalloff>()
									.set(EFogFalloff.EXPONENTIAL_SQUARED)
									.comment(""
											+ "How should the height fog thickness should be calculated? \n"
											+ "\n"
											+ EFogFalloff.LINEAR + ": Linear based on height (will ignore 'density')\n"
											+ EFogFalloff.EXPONENTIAL + ": 1/(e^(height*density)) \n"
											+ EFogFalloff.EXPONENTIAL_SQUARED + ": 1/(e^((height*density)^2)")
									.build();
							
							public static ConfigEntry<Double> heightFogDensity = new ConfigEntry.Builder<Double>()
									.setMinDefaultMax(0.01, 2.5, 50.0)
									.comment("What is the height fog's density?")
									.build();
							
						}
					}
				}
				
				public static class NoiseTextureSettings
				{
					public static ConfigEntry<Boolean> noiseEnable = new ConfigEntry.Builder<Boolean>()
							.set(true)
							.comment(""
									+ "Should a noise texture be applied to LODs? \n"
									+"\n"
									+ "This is done to simulate textures and make the LODs appear more detailed. \n"
									+ "")
							.build();
					
					public static ConfigEntry<Integer> noiseSteps = new ConfigEntry.Builder<Integer>()
							.setMinDefaultMax(0, 4, null)
							.comment(""
									+ "How many steps of noise should be applied to LODs?")
							.build();
					
					public static ConfigEntry<Double> noiseIntensity = new ConfigEntry.Builder<Double>()    // TODO: Make this a float (the ClassicConfigGUI doesn't support floats)
							.setMinDefaultMax(0d, 10d, 100d)                    // TODO: Once this becomes a float make it 0-1 instead of 0-100 (I did this cus doubles only allow 2 decimal places)
							.comment(""
									+ "How intense should the noise should be?")
							.build();
					
					public static ConfigEntry<Double> noiseDropoff = new ConfigEntry.Builder<Double>()    // TODO: Make this a float (the ClassicConfigGUI doesn't support floats)
							.setMinDefaultMax(0d, 3d, null)
							.comment(""
									+ "How far should the noise texture render before it fades away? \n"
									+ "\n"
									+ "0.0 - the noise texture will render the entire LOD render distance. \n"
									+ "3.0 - the noise texture will fade away at 1/3 of the LOD render distance. \n"
									+ "")
							.build();
				}
				
				public static class AdvancedGraphics
				{
					// TODO re-implement
//					public static ConfigEntry<Boolean> disableDirectionalCulling = new ConfigEntry.Builder<Boolean>()
//							.set(false)
//							.comment(""
//									+ "If false fake chunks behind the player's camera \n"
//									+ "aren't drawn, increasing GPU performance. \n"
//									+ "\n"
//									+ "If true all LODs are drawn, even those behind \n"
//									+ "the player's camera, decreasing GPU performance. \n"
//									+ "\n"
//									+ "Disable this if you see LODs disappearing at the corners of your vision.")
//							.build();
					
					// TODO replace with better options
					public static ConfigEntry<Boolean> useExtendedNearClipPlane = new ConfigEntry.Builder<Boolean>()
							.set(true)
							.comment(""
									+ "Will prevent some overdraw issues, but may cause nearby fake chunks to render incorrectly \n"
									+ " especially when in/near an ocean.")
							.setPerformance(ConfigEntryPerformance.NONE)
							.build();
					
					public static ConfigEntry<Double> brightnessMultiplier = new ConfigEntry.Builder<Double>() // TODO: Make this a float (the ClassicConfigGUI doesnt support floats)
							.set(1.0)
							.comment(""
									+ "How bright LOD colors are. \n"
									+ "\n"
									+ "0 = black \n"
									+ "1 = normal \n"
									+ "2 = near white")
							.build();
					
					public static ConfigEntry<Double> saturationMultiplier = new ConfigEntry.Builder<Double>() // TODO: Make this a float (the ClassicConfigGUI doesnt support floats)
							.set(1.0)
							.comment(""
									+ "How saturated LOD colors are. \n"
									+ "\n"
									+ "0 = black and white \n"
									+ "1 = normal \n"
									+ "2 = very saturated")
							.build();
					
					// TODO replace with better options
					public static ConfigEntry<Boolean> enableCaveCulling = new ConfigEntry.Builder<Boolean>()
							.set(true)
							.comment(""
									+ "If enabled caves will be culled \n"
									+ "\n"
									+ "NOTE: This feature is under development and \n"
									+ " it is VERY experimental! Please don't report \n"
									+ " any issues related to this feature. \n"
									+ "\n"
									+ "Additional Info: Currently this cull all faces \n"
									+ " with skylight value of 0 in dimensions that \n"
									+ " does not have a ceiling.")
							.build();
					
					public static ConfigEntry<Integer> caveCullingHeight = new ConfigEntry.Builder<Integer>()
							.setMinDefaultMax(-4096,40,4096)
							.comment("" 
									+ "At what Y value should cave culling start?")
							.build();
					
					public static ConfigEntry<Integer> earthCurveRatio = new ConfigEntry.Builder<Integer>()
							.setMinDefaultMax(0,0,5000)
							.comment(""
									+ "This is the earth size ratio when applying the curvature shader effect. \n"
									+ "Note: Enabling this feature may cause rendering bugs. \n"
									+ "\n"
									+ "0 = flat/disabled \n"
									+ "1 = 1 to 1 (6,371,000 blocks) \n"
									+ "100 = 1 to 100 (63,710 blocks) \n"
									+ "10000 = 1 to 10000 (637.1 blocks) \n"
									+ "\n"
									+ "Note: Due to current limitations, the min value is 50 \n"
									+ "and the max value is 5000. Any values outside this range \n"
									+ "will be set to 0 (disabled).")
							.build();
					
					public static ConfigEntry<Double> lodBias = new ConfigEntry.Builder<Double>()
							.setMinDefaultMax(0d, 0d, null)
							.comment(""
									+ "What the value should vanilla Minecraft's texture LodBias be? \n"
									+ "If set to 0 the mod wont overwrite vanilla's default (which so happens to also be 0)")
							.build();
					
				}
				
			}
			
			public static class WorldGenerator
			{
				public static ConfigEntry<Boolean> enableDistantGeneration = new ConfigEntry.Builder<Boolean>()
						.set(true)
						.comment(""
								+ " Should Distant Horizons slowly generate LODs \n"
								+ " outside the vanilla render distance?\n"
								+ "\n"
								+ " Note: when on a server, distant generation isn't supported \n"
								+ " and will always be disabled.")
						.build();
				
				public static ConfigEntry<EDhApiDistantGeneratorMode> distantGeneratorMode = new ConfigEntry.Builder<EDhApiDistantGeneratorMode>()
						.set(EDhApiDistantGeneratorMode.FEATURES)
						.comment(""
								+ "How detailed should LODs be generated outside the vanilla render distance? \n"
								+ "\n"
								+ EDhApiDistantGeneratorMode.PRE_EXISTING_ONLY + " \n"
								+ "Only create LOD data for already generated chunks. \n"
								+ "\n"
								+ EDhApiDistantGeneratorMode.BIOME_ONLY + " \n"
								+ "Only generate the biomes and use the biome's \n"
								+ "grass color, water color, or snow color. \n"
								+ "Doesn't generate height, everything is shown at sea level. \n"
								+ "- Fastest \n"
								+ "\n"
								+ EDhApiDistantGeneratorMode.BIOME_ONLY_SIMULATE_HEIGHT + " \n"
								+ "Same as " + EDhApiDistantGeneratorMode.BIOME_ONLY + ", except instead \n"
								+ "of always using sea level as the LOD height \n"
								+ "different biome types (mountain, ocean, forest, etc.) \n"
								+ "use predetermined heights to simulate having height data. \n"
								+ "- Fastest \n"
								+ "\n"
								+ EDhApiDistantGeneratorMode.SURFACE + " \n"
								+ "Generate the world surface, \n"
								+ "this does NOT include trees, \n"
								+ "or structures. \n"
								+ "- Faster \n"
								+ "\n"
								+ EDhApiDistantGeneratorMode.FEATURES + " \n"
								+ "Generate everything except structures. \n"
								+ "WARNING: This may cause world generator bugs or instability when paired with certain world generator mods. \n"
								+ "- Fast \n"
								+ "")
							/*
								// FULL isn't currently implemented
								+ "\n"
								+ EDhApiDistantGeneratorMode.FULL + " \n"
								+ "Ask the local server to generate/load each chunk. \n"
								+ "This is the most compatible, but will cause server/simulation lag. \n"
								+ "- Slow (15-50 ms, with spikes up to 200 ms) \n"
							*/
						.build();
				
				public static ConfigEntry<ELightGenerationMode> lightingEngine = new ConfigEntry.Builder<ELightGenerationMode>()
						.set(ELightGenerationMode.DISTANT_HORIZONS)
						.comment(""
								+ " How should distant generation chunk lighting be generated? \n"
								+ "\n"
								+ ELightGenerationMode.DISTANT_HORIZONS + ": Uses Distant Horizons' lighting engine to estimate chunk lighting. \n"
								+ "    Generally lower quality; but more stable for large numbers of world generator threads. \n"
								+ ELightGenerationMode.MINECRAFT + ": Use Minecraft's lighting engine to generate chunk lighting. \n"
								+ "    Generally higher quality; but may crash MC's lighting engine if there is an issue. \n"
								+ "\n"
								+ "This will effect generation speed, but not the rendering performance.")
						.build();
				
				// deprecated and not implemented, can be made public if we ever re-implement it
				@Deprecated
				private static ConfigEntry<EGenerationPriority> generationPriority = new ConfigEntry.Builder<EGenerationPriority>()
						.set(EGenerationPriority.NEAR_FIRST)
						.comment(""
								+ "In what priority should fake chunks be generated outside the vanilla render distance? \n"
								+ "\n"
								+ EGenerationPriority.FAR_FIRST + " \n"
								+ "Fake chunks are generated from lowest to highest detail \n"
								+ " with a priority for far away regions. \n"
								+ "This fills in the world fastest, but you will have large low detail \n"
								+ " blocks for a while while the generation happens. \n"
								+ "\n"
								+ EGenerationPriority.NEAR_FIRST + " \n"
								+ "Fake chunks are generated around the player \n"
								+ " in a spiral, similar to vanilla minecraft. \n"
								+ "Best used when on a server since we can't generate \n"
								+ " fake chunks. \n"
								+ "\n"
								+ EGenerationPriority.BALANCED + " \n"
								+ "A mix between "+ EGenerationPriority.NEAR_FIRST+"and"+ EGenerationPriority.FAR_FIRST+". \n"
								+ "First prioritise completing nearby highest detail chunks, \n"
								+ " then focus on filling in the low detail areas away from the player. \n"
								+ "\n"
								+ EGenerationPriority.AUTO + " \n"
								+ "Uses " + EGenerationPriority.BALANCED + " when on a single player world \n"
								+ " and " + EGenerationPriority.NEAR_FIRST + " when connected to a server.")
						.setPerformance(ConfigEntryPerformance.NONE)
						.build();
				
				// TODO fixme
				public static ConfigEntry<EBlocksToAvoid> blocksToAvoid = new ConfigEntry.Builder<EBlocksToAvoid>()
						.set(EBlocksToAvoid.BOTH)
						.comment(""
								+ "When generating fake chunks, what blocks should be ignored? \n"
								+ "Ignored blocks don't affect the height of the fake chunk, but might affect the color. \n"
								+ "So using " + EBlocksToAvoid.BOTH + " will prevent snow covered blocks from appearing one block too tall, \n"
								+ " but will still show the snow's color.\n"
								+ "\n"
								+ EBlocksToAvoid.NONE + ": Use all blocks when generating fake chunks \n"
								+ EBlocksToAvoid.NON_FULL + ": Only use full blocks when generating fake chunks (ignores slabs, lanterns, torches, tall grass, etc.) \n"
								+ EBlocksToAvoid.NO_COLLISION + ": Only use solid blocks when generating fake chunks (ignores tall grass, torches, etc.) \n"
								+ EBlocksToAvoid.BOTH + ": Only use full solid blocks when generating fake chunks")
						.setPerformance(ConfigEntryPerformance.NONE)
						.build();
				
				// TODO fixme
				public static ConfigEntry<Boolean> tintWithAvoidedBlocks = new ConfigEntry.Builder<Boolean>()
						.set(true)
						.comment(""
								+ "Should the blocks underneath avoided blocks gain the color of the avoided block? \n"
								+ " True: a red flower on grass will tint the grass below it red"
								+ " False: skipped blocks will not change color of surface below them")
						.build();
			}
			
			public static class Multiplayer
			{
				public static ConfigEntry<EServerFolderNameMode> serverFolderNameMode = new ConfigEntry.Builder<EServerFolderNameMode>()
						.set(EServerFolderNameMode.NAME_ONLY)
						.comment(""
								+ "How should multiplayer save folders should be named? \n"
								+ "\n"
								+ EServerFolderNameMode.NAME_ONLY + ": Example: \"Minecraft Server\" \n"
								+ EServerFolderNameMode.NAME_IP + ": Example: \"Minecraft Server IP 192.168.1.40\" \n"
								+ EServerFolderNameMode.NAME_IP_PORT + ": Example: \"Minecraft Server IP 192.168.1.40:25565\""
								+ EServerFolderNameMode.NAME_IP_PORT_MC_VERSION + ": Example: \"Minecraft Server IP 192.168.1.40:25565 GameVersion 1.16.5\"")
						.build();
				
				public static ConfigEntry<Double> multiDimensionRequiredSimilarity = new ConfigEntry.Builder<Double>()
						.setMinDefaultMax(0.0, 0.0, 1.0)
						.comment(""
								+ "AKA: Multiverse support. \n"
								+ "\n"
								+ "When matching levels (dimensions) of the same type (overworld, nether, etc.) the \n"
								+ "loaded chunks must be at least this percent the same \n"
								+ "in order to be considered the same world. \n"
								+ "\n"
								+ "Note: If you use portals to enter a dimension at two \n"
								+ "different locations the system will think the dimension \n"
								+ "it is two different levels. \n"
								+ "\n"
								+ "1.0 (100%) the chunks must be identical. \n"
								+ "0.5 (50%)  the chunks must be half the same. \n"
								+ "0.0 (0%)   disables multi-dimension support, \n"
								+ "            only one world will be used per dimension. \n"
								+ "\n"
								+ "If multiverse support is needed start with a value of 0.2 \n"
								+ "and tweak the sensitivity from there."
								+ "Lower values mean the matching is less strict.\n"
								+ "Higher values mean the matching is more strict.\n"
								+ "")
						.build();
				
			}
			
			public static class MultiThreading
			{
				public static final String THREAD_NOTE = ""
						+ "Multi-threading Note: \n"
						+ "If the total thread count in Distant Horizon's config is more threads than your CPU has cores, \n"
						+ "CPU performance may suffer if Distant Horizons has a lot to load or generate. \n"
						+ "This can be an issue when first loading into a world, when flying, and/or when generating new terrain.";
				
				
				public static final ConfigEntry<Integer> numberOfWorldGenerationThreads = new ConfigEntry.Builder<Integer>()
						.setMinDefaultMax(1,
								Runtime.getRuntime().availableProcessors()/6,
								Runtime.getRuntime().availableProcessors())
						.comment(""
								+ "How many threads should be used when generating LOD \n"
								+ "chunks outside the normal render distance? \n"
								+ "\n"
								+ "If you experience stuttering when generating distant LODs, \n"
								+ "decrease this number. \n"
								+ "If you want to increase LOD \n"
								+ "generation speed, increase this number. \n"
								+ "\n"
								+ THREAD_NOTE)
						.build();
				
				public static ConfigEntry<Integer> numberOfBufferBuilderThreads = new ConfigEntry.Builder<Integer>()
						.setMinDefaultMax(1,
								Runtime.getRuntime().availableProcessors()/4,
								Runtime.getRuntime().availableProcessors())
						.comment(""
								+ "How many threads are used when building geometry data for the GPU? \n"
								+ "\n"
								+ "If you experience high CPU usage when NOT generating distant \n"
								+ "LODs, lower this number. A higher number will make \n"
								+ "LODs' transition faster when moving around the world. \n"
								+ "\n"
								+ THREAD_NOTE)
						.build();
				
				public static final ConfigEntry<Integer> numberOfFileHandlerThreads = new ConfigEntry.Builder<Integer>()
						.setMinDefaultMax(1,
								Runtime.getRuntime().availableProcessors()/8,
								Runtime.getRuntime().availableProcessors())
						.comment(""
								+ "How many threads should be used when reading in LOD data from disk? \n"
								+ "\n"
								+ "Increasing this number will cause LODs to load in faster, \n"
								+ "but may cause lag when loading a new world or when \n"
								+ "quickly flying through existing LODs. \n"
								+ "\n"
								+ THREAD_NOTE)
						.build();
				
				public static final ConfigEntry<Integer> numberOfDataConverterThreads = new ConfigEntry.Builder<Integer>()
						.setMinDefaultMax(1,
								Runtime.getRuntime().availableProcessors()/4,
								Runtime.getRuntime().availableProcessors())
						.comment(""
								+ "How many threads should be used when converting full ID data to render data? \n"
								+ "\n"
								+ "These threads run both when terrain is generated and when\n"
								+ "certain graphics settings are changed. \n"
								+ "\n"
								+ "Generally this number should be equal to the number of world\n"
								+ "generator threads, although these threads shouldn't run as\n"
								+ "often (or as long) as the world generator threads.\n"
								+ "\n"
								+ THREAD_NOTE)
						.build();
				
			}
			
			public static class GpuBuffers
			{
				public static ConfigEntry<EGpuUploadMethod> gpuUploadMethod = new ConfigEntry.Builder<EGpuUploadMethod>()
						.set(EGpuUploadMethod.AUTO)
						.comment(""
								+ "What method should be used to upload geometry to the GPU? \n"
								+ "\n"
								+ EGpuUploadMethod.AUTO + ": Picks the best option based on the GPU you have. \n"
								+ "\n"
								+ EGpuUploadMethod.BUFFER_STORAGE + ": Default if OpenGL 4.5 is supported. \n"
								+ "    Fast rendering, no stuttering. \n"
								+ "\n"
								+ EGpuUploadMethod.SUB_DATA + ": Backup option for NVIDIA. \n"
								+ "    Fast rendering but may stutter when uploading. \n"
								+ "\n"
								+ EGpuUploadMethod.BUFFER_MAPPING + ": Slow rendering but won't stutter when uploading. \n"
								+ "    Generally the best option for integrated GPUs. \n"
								+ "    Default option for AMD/Intel if OpenGL 4.5 isn't supported. \n"
								+ "    May end up storing buffers in System memory. \n"
								+ "    Fast rendering if in GPU memory, slow if in system memory, \n"
								+ "    but won't stutter when uploading.  \n"
								+ "\n"
								+ EGpuUploadMethod.DATA + ": Fast rendering but will stutter when uploading. \n"
								+ "    Backup option for AMD/Intel. \n"
								+ "    Fast rendering but may stutter when uploading. \n"
								+ "\n"
								+ "If you don't see any difference when changing these settings, \n"
								+ "or the world looks corrupted: restart your game."
								+ "")
						.build();
				
				public static ConfigEntry<Integer> gpuUploadPerMegabyteInMilliseconds = new ConfigEntry.Builder<Integer>()
						.setMinDefaultMax(0, 0, 50)
						.comment(""
								+ "How long should a buffer wait per Megabyte of data uploaded? \n"
								+ "Helpful resource for frame times: https://fpstoms.com \n"
								+ "\n"
								+ "Longer times may reduce stuttering but will make fake chunks \n"
								+ "transition and load slower. Change this to [0] for no timeout. \n"
								+ "\n"
								+ "NOTE:\n"
								+ "Before changing this config, try changing the \"GPU Upload method\" first. \n"
								+ "")
						.build();
				
				// deprecated and not implemented, can be made public if we ever re-implement it
				@Deprecated
				private static ConfigEntry<EBufferRebuildTimes> rebuildTimes = new ConfigEntry.Builder<EBufferRebuildTimes>()
						.set(EBufferRebuildTimes.NORMAL)
						.comment(""
								+ "How frequently should vertex buffers (geometry) be rebuilt and sent to the GPU? \n"
								+ "Higher settings may cause stuttering, but will prevent holes in the world")
						.build();
			}
			
			public static class AutoUpdater
			{
				public static ConfigEntry<Boolean> enableAutoUpdater = new ConfigEntry.Builder<Boolean>()
						.set(!ModInfo.IS_DEV_BUILD) // hide the update notification in dev builds 
						.comment(""
								+ "Automatically check for updates on game launch?")
						.build();
				
				public static ConfigEntry<Boolean> enableSilentUpdates = new ConfigEntry.Builder<Boolean>()
						.set(true)
						.comment("" 
								+ "Should Distant Horizons silently, automatically download and install new versions? "
								+ "")
						.build();
			}
			
			public static class Logging
			{
				// TODO add change all option
				// TODO default to error chat and info file
				public static ConfigEntry<ELoggerMode> logWorldGenEvent = new ConfigEntry.Builder<ELoggerMode>()
						.set(ELoggerMode.LOG_WARNING_TO_CHAT_AND_INFO_TO_FILE)
						.comment(""
								+ "If enabled, the mod will log information about the world generation process. \n"
								+ "This can be useful for debugging.")
						.build();
				
				public static ConfigEntry<ELoggerMode> logWorldGenPerformance = new ConfigEntry.Builder<ELoggerMode>()
						.set(ELoggerMode.LOG_WARNING_TO_CHAT_AND_FILE)
						.comment(""
								+ "If enabled, the mod will log performance about the world generation process. \n"
								+ "This can be useful for debugging.")
						.build();
				
				public static ConfigEntry<ELoggerMode> logWorldGenLoadEvent = new ConfigEntry.Builder<ELoggerMode>()
						.set(ELoggerMode.LOG_WARNING_TO_CHAT_AND_FILE)
						.comment(""
								+ "If enabled, the mod will log information about the world generation process. \n"
								+ "This can be useful for debugging.")
						.build();
				
				public static ConfigEntry<ELoggerMode> logLodBuilderEvent = new ConfigEntry.Builder<ELoggerMode>()
						.set(ELoggerMode.LOG_WARNING_TO_CHAT_AND_INFO_TO_FILE)
						.comment(""
								+ "If enabled, the mod will log information about the LOD generation process. \n"
								+ "This can be useful for debugging.")
						.build();
				
				public static ConfigEntry<ELoggerMode> logRendererBufferEvent = new ConfigEntry.Builder<ELoggerMode>()
						.set(ELoggerMode.LOG_WARNING_TO_CHAT_AND_INFO_TO_FILE)
						.comment(""
								+ "If enabled, the mod will log information about the renderer buffer process. \n"
								+ "This can be useful for debugging.")
						.build();
				
				public static ConfigEntry<ELoggerMode> logRendererGLEvent = new ConfigEntry.Builder<ELoggerMode>()
						.set(ELoggerMode.LOG_WARNING_TO_CHAT_AND_INFO_TO_FILE)
						.comment(""
								+ "If enabled, the mod will log information about the renderer OpenGL process. \n"
								+ "This can be useful for debugging.")
						.build();
				
				public static ConfigEntry<ELoggerMode> logFileReadWriteEvent = new ConfigEntry.Builder<ELoggerMode>()
						.set(ELoggerMode.LOG_WARNING_TO_CHAT_AND_INFO_TO_FILE)
						.comment(""
								+ "If enabled, the mod will log information about file read/write operations. \n"
								+ "This can be useful for debugging.")
						.build();
				
				public static ConfigEntry<ELoggerMode> logFileSubDimEvent = new ConfigEntry.Builder<ELoggerMode>()
						.set(ELoggerMode.LOG_WARNING_TO_CHAT_AND_INFO_TO_FILE)
						.comment(""
								+ "If enabled, the mod will log information about file sub-dimension operations. \n"
								+ "This can be useful for debugging.")
						.build();
				
				public static ConfigEntry<ELoggerMode> logNetworkEvent = new ConfigEntry.Builder<ELoggerMode>()
						.set(ELoggerMode.LOG_WARNING_TO_CHAT_AND_INFO_TO_FILE)
						.comment(""
								+ "If enabled, the mod will log information about network operations. \n"
								+ "This can be useful for debugging.")
						.build();
			}
			
			public static class Debugging
			{
				public static ConfigEntry<ERendererMode> rendererMode = new ConfigEntry.Builder<ERendererMode>()
						.set(ERendererMode.DEFAULT)
						.comment(""
								+ "What renderer is active? \n"
								+ "\n"
								+ ERendererMode.DEFAULT + ": Default lod renderer \n"
								+ ERendererMode.DEBUG + ": Debug testing renderer \n"
								+ ERendererMode.DISABLED + ": Disable rendering")
						.build();
				
				public static ConfigEntry<EDebugMode> debugMode = new ConfigEntry.Builder<EDebugMode>()
						.set(EDebugMode.OFF)
						.comment(""
								+ "Should specialized colors/rendering modes be used? \n"
								+ "\n"
								+ EDebugMode.OFF + ": Fake chunks will be drawn with their normal colors. \n"
								+ EDebugMode.SHOW_DETAIL + ": Fake chunks color will be based on their detail level. \n"
								+ EDebugMode.SHOW_GENMODE + ": Fake chunks color will be based on their distant generation mode. \n"
								+ EDebugMode.SHOW_OVERLAPPING_QUADS + ": Fake chunks will be drawn with total white, but overlapping quads will be drawn with red. \n"
								+ "    but overlapping quads will be drawn with red, drawn as a wireframe.")
						.build();
				
				public static ConfigEntry<Boolean> renderWireframe = new ConfigEntry.Builder<Boolean>()
						.set(false)
						.comment(""
								+ "If enabled the LODs will render as wireframe."
								+ "")
						.build();
				
				
				// TODO add LOD-only mode to this
				public static ConfigEntry<Boolean> enableDebugKeybindings = new ConfigEntry.Builder<Boolean>()
						.set(false)
						.comment(""
								+ "If true the F8 key can be used to cycle through the different debug modes. \n"
								+ "and the F6 key can be used to enable and disable LOD rendering.")
						.build();
				
				public static ConfigEntry<Boolean> lodOnlyMode = new ConfigEntry.Builder<Boolean>()
						.set(false)
						.comment(""
								+ "If enabled this will disable (most) vanilla Minecraft rendering. \n"
								+ "\n"
								+ "NOTE: Do not report any issues when this mode is on! \n"
								+ "   This setting is only for fun and debugging. \n"
								+ "   Mod compatibility is not guaranteed.")
						.build();

				public static ConfigEntry<Boolean> debugWireframeRendering = new ConfigEntry.Builder<Boolean>()
						.set(false)
						.comment(""
								+ "If enabled, various wireframes for debugging internal functions will be drawn. \n"
								+ "\n"
								+ "NOTE: There WILL be performance hit! \n"
								+ "   Additionally, only stuff that's loaded after you enable this \n"
								+ "   will render their debug wireframes.")
						.build();
				
				
				// can be set to public inorder to show in the config file and UI
				private static ConfigCategory exampleConfigScreen = new ConfigCategory.Builder()
						.set(ExampleConfigScreen.class)
						.build();
				
				/** This class is used to debug the different features of the config GUI */
				public static class ExampleConfigScreen
				{
					// Defined in the lang, just a note about this screen
					public static ConfigUIComment debugConfigScreenNote = new ConfigUIComment();
					
					public static ConfigEntry<Boolean> boolTest = new ConfigEntry.Builder<Boolean>()
							.set(false)
							.build();
					
					public static ConfigEntry<Byte> byteTest = new ConfigEntry.Builder<Byte>()
							.set((byte) 8)
							.build();
					
					public static ConfigEntry<Integer> intTest = new ConfigEntry.Builder<Integer>()
							.set(69420)
							.build();
					
					public static ConfigEntry<Double> doubleTest = new ConfigEntry.Builder<Double>()
							.set(420.69d)
							.build();
					
					public static ConfigEntry<Short> shortTest = new ConfigEntry.Builder<Short>()
							.set((short) 69)
							.build();
					
					public static ConfigEntry<Long> longTest = new ConfigEntry.Builder<Long>()
							.set(42069L)
							.build();
					
					public static ConfigEntry<Float> floatTest = new ConfigEntry.Builder<Float>()
							.set(0.42069f)
							.build();
					
					public static ConfigEntry<String> stringTest = new ConfigEntry.Builder<String>()
							.set("Test input box")
							.build();
					
					public static ConfigEntry<List<String>> listTest = new ConfigEntry.Builder<List<String>>()
							.set(new ArrayList<String>(Arrays.asList("option 1", "option 2", "option 3")))
							.build();
					
					public static ConfigEntry<Map<String, String>> mapTest = new ConfigEntry.Builder<Map<String, String>>()
							.set(new HashMap<String, String>())
							.build();
					
					public static ConfigCategory categoryTest = new ConfigCategory.Builder().set(CategoryTest.class).build();
					
					public static ConfigEntry<Integer> linkableTest = new ConfigEntry.Builder<Integer>()
							.set(420)
							.build();
					
					
					public static class CategoryTest
					{
						// The name of this can be anything as it will be overwritten by the name of the linked object
						public static ConfigLinkedEntry linkableTest = new ConfigLinkedEntry(ExampleConfigScreen.linkableTest);
					}
				}
				
			}
			
		}
		
		public static class ResetConfirmation
		{
			public static ConfigUIComment resetConfirmationNote = new ConfigUIComment();
			
			public static ConfigEntry<Boolean> resetAllSettings = new ConfigEntry.Builder<Boolean>()
					.set(false)
					.setAppearance(ConfigEntryAppearance.ONLY_IN_GUI)
					//.addListener(null) // TODO add listener
					.build();
			
		}
		
    }
}
