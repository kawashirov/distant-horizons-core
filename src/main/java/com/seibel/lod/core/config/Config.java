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


import com.seibel.lod.core.enums.rendering.EFogDrawMode;
import com.seibel.lod.core.enums.rendering.EFogColorMode;
import com.seibel.lod.core.enums.rendering.EFogDistance;
import com.seibel.lod.core.enums.rendering.ERendererMode;
import com.seibel.lod.core.config.types.*;

import com.seibel.lod.core.enums.config.*;
import com.seibel.lod.core.enums.rendering.*;


/**
 * This handles any configuration the user has access to.
 * @author coolGi
 * @version 04-29-2022
 */

public class Config
{
    // CONFIG STRUCTURE
    // 	-> Client
    //		|
    //		|-> Graphics
    //		|		|-> Quality
    //		|		|-> FogQuality
    //		|		|-> AdvancedGraphics
    //		|
    //		|-> World Generation
    //		|
    //		|-> Advanced
    //				|-> Threads
    //				|-> Buffers
    //				|-> Debugging

    // Since the original config system uses forge stuff, that means we have to rewrite the whole config system

    public static ConfigCategory client = new ConfigCategory.Builder().set(Client.class).build();


    public static class Client
    {
        public static ConfigCategory graphics = new ConfigCategory.Builder().set(Graphics.class).build();

        public static ConfigCategory worldGenerator = new ConfigCategory.Builder().set(WorldGenerator.class).build();

        public static ConfigCategory multiplayer = new ConfigCategory.Builder().set(Multiplayer.class).build();

        public static ConfigCategory advanced = new ConfigCategory.Builder().set(Advanced.class).build();

        public static ConfigEntry<Boolean> optionsButton = new ConfigEntry.Builder<Boolean>()
                .set(true)
                .comment("Show the lod button in the options screen next to fov")
                .build();


        public static class Graphics
        {
            public static ConfigCategory quality = new ConfigCategory.Builder().set(Quality.class).build();

            public static ConfigCategory fogQuality = new ConfigCategory.Builder().set(FogQuality.class).build();

            public static ConfigCategory advancedGraphics = new ConfigCategory.Builder().set(AdvancedGraphics.class).build();


            public static class Quality
            {
                public static ConfigEntry<EHorizontalResolution> drawResolution = new ConfigEntry.Builder<EHorizontalResolution>()
                        .set(EHorizontalResolution.BLOCK)
                        .comment(""
                                + "What is the maximum detail fake chunks should be drawn at? \n"
                                + "This setting will only affect closer chunks.\n"
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
                        .build();

                public static ConfigEntry<Integer> lodChunkRenderDistance = new ConfigEntry.Builder<Integer>()
                        .setMinDefaultMax(32, 128, 4096)
                        .comment("The radius of the mod's render distance. (measured in chunks)")
                        .build();

                public static ConfigEntry<EVerticalQuality> verticalQuality = new ConfigEntry.Builder<EVerticalQuality>()
                        .set(EVerticalQuality.MEDIUM)
                        .comment(""
                                + "This indicates how detailed fake chunks will represent \n"
                                + " overhangs, caves, floating islands, ect. \n"
                                + "Higher options will make the world more accurate, but"
                                + " will increase memory and GPU usage. \n"
                                + "\n"
                                + EVerticalQuality.LOW + ": uses at max 2 columns per position. \n"
                                + EVerticalQuality.MEDIUM + ": uses at max 4 columns per position. \n"
                                + EVerticalQuality.HIGH + ": uses at max 8 columns per position. \n"
                                + "\n"
                                + "Lowest Quality: " + EVerticalQuality.LOW + "\n"
                                + "Highest Quality: " + EVerticalQuality.HIGH)
                        .build();

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
                                + "This indicates how quickly fake chunks decrease in quality the further away they are. \n"
                                + "Higher settings will render higher quality fake chunks farther away, \n"
                                + " but will increase memory and GPU usage.")
                        .build();

                public static ConfigEntry<EDropoffQuality> dropoffQuality = new ConfigEntry.Builder<EDropoffQuality>()
                        .set(EDropoffQuality.AUTO)
                        .comment(""
                                + "This determines how lod level drop off will be done. \n"
                                + "\n"
                                + EDropoffQuality.SMOOTH_DROPOFF + ": \n"
                                + "    The lod level is calculated for each point, making the drop off a smooth circle. \n"
                                + EDropoffQuality.PERFORMANCE_FOCUSED + ": \n"
                                + "    One detail level for an entire region. Minimize CPU usage and \n"
                                + "     improve terrain refresh delay, especially for high Lod render distance. \n"
                                + EDropoffQuality.AUTO + ": \n"
                                + "    Use "+ EDropoffQuality.SMOOTH_DROPOFF + " for less then 128 Lod render distance, \n"
                                + "     or "+ EDropoffQuality.PERFORMANCE_FOCUSED +" otherwise.")
                        .build();

                public static ConfigEntry<Integer> lodBiomeBlending = new ConfigEntry.Builder<Integer>()
                        .setMinDefaultMax(0,1,7)
                        .comment(""
                                + "This is the same as vanilla Biome Blending settings for Lod area. \n"
                                + "    Note that anything other than '0' will greatly effect Lod building time \n"
                                + "     and increase triangle count. The cost on chunk generation speed is also \n"
                                + "     quite large if set too high.\n"
                                + "\n"
                                + "    '0' equals to Vanilla Biome Blending of '1x1' or 'OFF', \n"
                                + "    '1' equals to Vanilla Biome Blending of '3x3', \n"
                                + "    '2' equals to Vanilla Biome Blending of '5x5'...")
                        .build();
            }


            public static class FogQuality
            {
                public static ConfigEntry<EFogDistance> fogDistance = new ConfigEntry.Builder<EFogDistance>()
                        .set(EFogDistance.FAR)
                        .comment("At what distance should Fog be drawn on the fake chunks?")
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
                        .build();

                public static ConfigEntry<EFogColorMode> fogColorMode = new ConfigEntry.Builder<EFogColorMode>()
                        .set(EFogColorMode.USE_WORLD_FOG_COLOR)
                        .comment(""
                                + "What color should fog use? \n"
                                + "\n"
                                + EFogColorMode.USE_WORLD_FOG_COLOR + ": Use the world's fog color. \n"
                                + EFogColorMode.USE_SKY_COLOR + ": Use the sky's color. \n"
                                + "\n"
                                + "This setting doesn't affect performance.")
                        .build();

                public static ConfigEntry<Boolean> disableVanillaFog = new ConfigEntry.Builder<Boolean>()
                        .set(true)
                        .comment(""
                                + "If true disable Minecraft's fog. \n"
                                + "\n"
                                + "Experimental! Mod support is not guarantee.")
                        .build();

                public static ConfigCategory advancedFog = new ConfigCategory.Builder().set(AdvancedFog.class).build();


                public static class AdvancedFog {
                    // TODO: Make some of the option here floats rather than doubles (the ClassicConfigGUI dosnt support floats)
                    private static final Double FOG_RANGE_MIN = 0.0;
                    private static final Double FOG_RANGE_MAX = Math.sqrt(2.0);

                    public static ConfigEntry<Double> farFogStart = new ConfigEntry.Builder<Double>()
                            .setMinDefaultMax(FOG_RANGE_MIN, 0.0, FOG_RANGE_MAX)
                            .comment(""
                                    + "Where should the far fog start? \n"
                                    + "\n"
                                    + "    '0.0': Fog start at player's position.\n"
                                    + "    '1.0': The fog-start's circle fit just in the lod render distance square.\n"
                                    + "    '1.414': The lod render distance square fit just in the fog-start's circle.")
                            .build();

                    public static ConfigEntry<Double> farFogEnd = new ConfigEntry.Builder<Double>()
                            .setMinDefaultMax(FOG_RANGE_MIN, 1.0, FOG_RANGE_MAX)
                            .comment(""
                                    + "Where should the far fog end? \n"
                                    + "\n"
                                    + "    '0.0': Fog end at player's position.\n"
                                    + "    '1.0': The fog-end's circle fit just in the lod render distance square.\n"
                                    + "    '1.414': The lod render distance square fit just in the fog-end's circle.")
                            .build();

                    public static ConfigEntry<Double> farFogMin = new ConfigEntry.Builder<Double>()
                            .setMinDefaultMax(-5.0,0.0, FOG_RANGE_MAX)
                            .comment(""
                                    + "What is the minimum fog thickness? \n"
                                    + "\n"
                                    + "    '0.0': No fog at all.\n"
                                    + "    '1.0': Fully fog color.")
                            .build();

                    public static ConfigEntry<Double> farFogMax = new ConfigEntry.Builder<Double>()
                            .setMinDefaultMax(FOG_RANGE_MIN, 1.0, 5.0)
                            .comment(""
                                    + "What is the maximum fog thickness? \n"
                                    + "\n"
                                    + "    '0.0': No fog at all.\n"
                                    + "    '1.0': Fully fog color.")
                            .build();

                    public static ConfigEntry<EFogFalloff> farFogType = new ConfigEntry.Builder<EFogFalloff>()
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
                            .comment("What is the fog density?")
                            .build();

                    public static ConfigCategory heightFog = new ConfigCategory.Builder().set(HeightFog.class).build();


                    public static class HeightFog {
                        public static ConfigEntry<EHeightFogMixMode> heightFogMixMode = new ConfigEntry.Builder<EHeightFogMixMode>()
                                .set(EHeightFogMixMode.BASIC)
                                .comment(""
                                        + "How the height should effect the fog thickness combined with the normal function? \n"
                                        + "\n"
                                        + EHeightFogMixMode.BASIC + ": No special height fog effect. Fog is calculated based on camera distance \n"
                                        + EHeightFogMixMode.IGNORE_HEIGHT + ": Ignore height completely. Fog is calculated based on horizontal distance \n"
                                        + EHeightFogMixMode.ADDITION + ": heightFog + farFog \n"
                                        + EHeightFogMixMode.MAX + ": max(heightFog, farFog) \n"
                                        + EHeightFogMixMode.MULTIPLY + ": heightFog * farFog \n"
                                        + EHeightFogMixMode.INVERSE_MULTIPLY + ": 1 - (1-heightFog) * (1-farFog) \n"
                                        + EHeightFogMixMode.LIMITED_ADDITION + ": farFog + max(farFog, heightFog) \n"
                                        + EHeightFogMixMode.MULTIPLY_ADDITION + ": farFog + farFog * heightFog \n"
                                        + EHeightFogMixMode.INVERSE_MULTIPLY_ADDITION + ": farFog + 1 - (1-heightFog) * (1-farFog) \n"
                                        + EHeightFogMixMode.AVERAGE + ": farFog*0.5 + heightFog*0.5 \n"
                                        + "\n"
                                        + "Note that for 'BASIC' mode and 'IGNORE_HEIGHT' mode, fog settings for height fog has no effect.")
                                .build();

                        public static ConfigEntry<EHeightFogMode> heightFogMode = new ConfigEntry.Builder<EHeightFogMode>()
                                .set(EHeightFogMode.ABOVE_AND_BELOW_CAMERA)
                                .comment(""
                                        + "Where should the height fog be located? \n"
                                        + "\n"
                                        + EHeightFogMode.ABOVE_CAMERA + ": Height fog starts from camera to the sky \n"
                                        + EHeightFogMode.BELOW_CAMERA + ": Height fog starts from camera to the void \n"
                                        + EHeightFogMode.ABOVE_AND_BELOW_CAMERA + ": Height fog starts from camera to both the sky and the void \n"
                                        + EHeightFogMode.ABOVE_SET_HEIGHT + ": Height fog starts from a set height to the sky \n"
                                        + EHeightFogMode.BELOW_SET_HEIGHT + ": Height fog starts from a set height to the void \n"
                                        + EHeightFogMode.ABOVE_AND_BELOW_SET_HEIGHT + ": Height fog starts from a set height to both the sky and the void")
                                .build();

                        public static ConfigEntry<Double> heightFogHeight = new ConfigEntry.Builder<Double>()
                                .setMinDefaultMax(-4096.0, 70.0, 4096.0)
                                .comment("If the height fog is calculated around a set height, what is that height position?")
                                .build();

                        public static ConfigEntry<Double> heightFogStart = new ConfigEntry.Builder<Double>()
                                .setMinDefaultMax(FOG_RANGE_MIN, 0.0, FOG_RANGE_MAX)
                                .comment(""
                                        + "How far the start of height fog should offset? \n"
                                        + "\n"
                                        + "    '0.0': Fog start with no offset.\n"
                                        + "    '1.0': Fog start with offset of the entire world's height. (Include depth)")
                                .build();

                        public static ConfigEntry<Double> heightFogEnd = new ConfigEntry.Builder<Double>()
                                .setMinDefaultMax(FOG_RANGE_MIN, 1.0, FOG_RANGE_MAX)
                                .comment(""
                                        + "How far the end of height fog should offset? \n"
                                        + "\n"
                                        + "    '0.0': Fog end with no offset.\n"
                                        + "    '1.0': Fog end with offset of the entire world's height. (Include depth)")
                                .build();

                        public static ConfigEntry<Double> heightFogMin = new ConfigEntry.Builder<Double>()
                                .setMinDefaultMax(-5.0, 0.0, FOG_RANGE_MAX)
                                .comment(""
                                        + "What is the minimum fog thickness? \n"
                                        + "\n"
                                        + "    '0.0': No fog at all.\n"
                                        + "    '1.0': Fully fog color.")
                                .build();

                        public static ConfigEntry<Double> heightFogMax = new ConfigEntry.Builder<Double>()
                                .setMinDefaultMax(FOG_RANGE_MIN, 1.0, 5.0)
                                .comment(""
                                        + "What is the maximum fog thickness? \n"
                                        + "\n"
                                        + "    '0.0': No fog at all.\n"
                                        + "    '1.0': Fully fog color.")
                                .build();

                        public static ConfigEntry<EFogFalloff> heightFogType = new ConfigEntry.Builder<EFogFalloff>()
                                .set(EFogFalloff.EXPONENTIAL_SQUARED)
                                .comment(""
                                        + "How the fog thickness should be calculated from height? \n"
                                        + "\n"
                                        + EFogFalloff.LINEAR + ": Linear based on height (will ignore 'density')\n"
                                        + EFogFalloff.EXPONENTIAL + ": 1/(e^(height*density)) \n"
                                        + EFogFalloff.EXPONENTIAL_SQUARED + ": 1/(e^((height*density)^2)")
                                .build();

                        public static ConfigEntry<Double> heightFogDensity = new ConfigEntry.Builder<Double>()
                                .setMinDefaultMax(0.01, 2.5, 50.0)
                                .comment("What is the fog density?")
                                .build();

                    }
                }
            }


            public static class AdvancedGraphics
            {
                public static ConfigEntry<Boolean> disableDirectionalCulling = new ConfigEntry.Builder<Boolean>()
                        .set(false)
                        .comment(""
                                + "If false fake chunks behind the player's camera \n"
                                + " aren't drawn, increasing GPU performance. \n"
                                + "\n"
                                + "If true all LODs are drawn, even those behind \n"
                                + " the player's camera, decreasing GPU performance. \n"
                                + "\n"
                                + "Disable this if you see LODs disappearing at the corners of your vision.")
                        .build();

                public static ConfigEntry<EVanillaOverdraw> vanillaOverdraw = new ConfigEntry.Builder<EVanillaOverdraw>()
                        .set(EVanillaOverdraw.DYNAMIC)
                        .comment(""
                                + "How often should LODs be drawn on top of regular chunks? \n"
                                + "HALF and ALWAYS will prevent holes in the world, \n"
                                + " but may look odd for transparent blocks or in caves. \n"
                                + "\n"
                                + EVanillaOverdraw.NEVER + ": \n"
                                + "    LODs won't render on top of vanilla chunks. Use Overdraw offset to change the border offset. \n"
                                + EVanillaOverdraw.DYNAMIC + ": \n"
                                + "    LODs will render on top of distant vanilla chunks to hide delayed loading. \n"
                                + "    Will dynamically decide the border offset based on vanilla render distance. \n"
                                + EVanillaOverdraw.ALWAYS + ": \n"
                                + "    LODs will render on all vanilla chunks preventing all holes in the world.")
                        .setPerformance(ConfigEntryPerformance.NONE)
                        .build();

                public static ConfigEntry<Integer> overdrawOffset = new ConfigEntry.Builder<Integer>()
                        .setMinDefaultMax(-16, 0, 16)
                        .comment(""
                                + "If on Vanilla Overdraw mode of NEVER, how much should should the border be offset? \n"
                                + "\n"
                                + " '1': The start of lods will be shifted inwards by 1 chunk, causing 1 chunk of overdraw. \n"
                                + " '-1': The start fo lods will be shifted outwards by 1 chunk, causing 1 chunk of gap. \n"
                                + "\n"
                                + "This setting can be used to deal with gaps due to our vanilla rendered chunk \n"
                                + " detection not being perfect.")
                        .build();

                public static ConfigEntry<Boolean> useExtendedNearClipPlane = new ConfigEntry.Builder<Boolean>()
                        .set(true)
                        .comment(""
                                + "Will prevent some overdraw issues, but may cause nearby fake chunks to render incorrectly \n"
                                + " especially when in/near an ocean.")
                        .setPerformance(ConfigEntryPerformance.NONE)
                        .build();

                public static ConfigEntry<Double> brightnessMultiplier = new ConfigEntry.Builder<Double>() // TODO: Make this a float (the ClassicConfigGUI dosnt support floats)
                        .set(1.0)
                        .comment(""
                                + "How bright fake chunk colors are. \n"
                                + "\n"
                                + " 0 = black \n"
                                + " 1 = normal \n"
                                + " 2 = near white")
                        .build();

                public static ConfigEntry<Double> saturationMultiplier = new ConfigEntry.Builder<Double>() // TODO: Make this a float (the ClassicConfigGUI dosnt support floats)
                        .set(1.0)
                        .comment(""
                                + "How saturated fake chunk colors are. \n"
                                + "\n"
                                + " 0 = black and white \n"
                                + " 1 = normal \n"
                                + " 2 = very saturated")
                        .build();

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
                        .comment("At what Y value should cave culling start?")
                        .build();

                public static ConfigEntry<Integer> earthCurveRatio = new ConfigEntry.Builder<Integer>()
                        .setMinDefaultMax(0,0,5000)
                        .comment(""
                                + "This is the earth size ratio when applying the curvature shader effect. \n"
                                + "\n"
                                + "NOTE: This feature is just for fun and is VERY experimental! \n"
                                + "Please don't report any issues related to this feature. \n"
                                + "\n"
                                + " 0 = flat/disabled \n"
                                + " 1 = 1 to 1 (6,371,000 blocks) \n"
                                + " 100 = 1 to 100 (63,710 blocks) \n"
                                + " 10000 = 1 to 10000 (637.1 blocks) \n"
                                + "\n"
                                + "NOTE: Due to current limitations, the min value is 50 \n"
                                + " and the max value is 5000. Any values outside this range \n"
                                + " will be set to 0(disabled).")
                        .build();

				/*
				@ConfigAnnotations.FileComment
				public static String _backsideCullingRange = IAdvancedGraphics.VANILLA_CULLING_RANGE_DESC;
				@ConfigAnnotations.Entry(minValue = 0, maxValue = 512)
				public static int backsideCullingRange = IAdvancedGraphics.VANILLA_CULLING_RANGE_MIN_DEFAULT_MAX.defaultValue;
				*/
            }
        }


        public static class WorldGenerator
        {
            public static ConfigEntry<Boolean> enableDistantGeneration = new ConfigEntry.Builder<Boolean>()
                    .set(true)
                    .comment(""
                            + "Whether to enable Distant chunks generator? \n"
                            + "\n"
                            + "Turning this on allows Distant Horizons to make lods for chunks \n"
                            + " that are outside of vanilla view distance. \n"
                            + "\n"
                            + "Note that in server, distant generation is always off.")
                    .build();

            public static ConfigEntry<EDistanceGenerationMode> distanceGenerationMode = new ConfigEntry.Builder<EDistanceGenerationMode>()
                    .set(EDistanceGenerationMode.FEATURES)
                    .comment(""
                            + "How detailed should fake chunks be generated outside the vanilla render distance? \n"
                            + "\n"
                            + "The times are the amount of time it took one of the developer's PC to generate \n"
                            + " one chunk in Minecraft 1.16.5 and may be inaccurate for different Minecraft versions. \n"
                            + "They are included to give a rough estimate as to how the different options \n"
                            + " may perform in comparison to each other. \n"
                            + "(Note that all modes will load in already existing chunks) \n"
                            + "\n"
                            + EDistanceGenerationMode.NONE + " \n"
                            + "Only run the Generator to load in already existing chunks. \n"
                            + "\n"
                            + EDistanceGenerationMode.BIOME_ONLY + " \n"
                            + "Only generate the biomes and use the biome's \n"
                            + " grass color, water color, or snow color. \n"
                            + "Doesn't generate height, everything is shown at sea level. \n"
                            + " - Fastest (2-5 ms) \n"
                            + "\n"
                            + EDistanceGenerationMode.BIOME_ONLY_SIMULATE_HEIGHT + " \n"
                            + "Same as " + EDistanceGenerationMode.BIOME_ONLY + ", except instead \n"
                            + " of always using sea level as the LOD height \n"
                            + " different biome types (mountain, ocean, forest, etc.) \n"
                            + " use predetermined heights to simulate having height data. \n"
                            + " - Fastest (2-5 ms) \n"
                            + "\n"
                            + EDistanceGenerationMode.SURFACE + " \n"
                            + "Generate the world surface, \n"
                            + " this does NOT include trees, \n"
                            + " or structures. \n"
                            + " - Faster (10-20 ms) \n"
                            + "\n"
                            + EDistanceGenerationMode.FEATURES + " \n"
                            + "Generate everything except structures. \n"
                            + "WARNING: This may cause world generation bugs or instability! \n"
                            + " - Fast (15-20 ms) \n"
                            + "\n"
                            + EDistanceGenerationMode.FULL + " \n"
                            + "Ask the local server to generate/load each chunk. \n"
                            + "This will show player made structures, which can \n"
                            + " be useful if you are adding the mod to a pre-existing world. \n"
                            + "This is the most compatible, but causes server/simulation lag. \n"
                            + " - Slow (15-50 ms, with spikes up to 200 ms) \n"
                            + "\n"
                            + "The multithreaded options may increase CPU load significantly (while generating) \n"
                            + " depending on how many world generation threads you have allocated.")
                    .build();

            public static ConfigEntry<ELightGenerationMode> lightGenerationMode = new ConfigEntry.Builder<ELightGenerationMode>()
                    .set(ELightGenerationMode.FANCY)
                    .comment(""
                            + "How should block and sky lights be processed for distant generation? \n"
                            + "\n"
                            + "Note that this include already existing chunks since vanilla \n"
                            + " does not store sky light values to save file. \n"
                            + "\n"
                            + ELightGenerationMode.FAST + ": Use height map to fake the light values. \n"
                            + ELightGenerationMode.FANCY + ": Use actaul light engines to generate proper values. \n"
                            + "\n"
                            + "This will effect generation speed, but not the rendering performance.")
                    .build();

            public static ConfigEntry<EGenerationPriority> generationPriority = new ConfigEntry.Builder<EGenerationPriority>()
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
                    .set(EServerFolderNameMode.AUTO)
                    .comment(""
                            + " What multiplayer save folders should be named. \n"
                            + "\n"
                            + " " + EServerFolderNameMode.AUTO + ": " + EServerFolderNameMode.NAME_IP + " for LAN connections, " + EServerFolderNameMode.NAME_IP_PORT + " for all others. \n"
                            + " " + EServerFolderNameMode.NAME_ONLY + ": Example: \"Minecraft Server\" \n"
                            + " " + EServerFolderNameMode.NAME_IP + ": Example: \"Minecraft Server IP 192.168.1.40\" \n"
                            + " " + EServerFolderNameMode.NAME_IP_PORT + ": Example: \"Minecraft Server IP 192.168.1.40:25565\""
                            + " " + EServerFolderNameMode.NAME_IP_PORT_MC_VERSION + ": Example: \"Minecraft Server IP 192.168.1.40:25565 GameVersion 1.16.5\"")
                    .build();

            public static ConfigEntry<Double> multiDimensionRequiredSimilarity = new ConfigEntry.Builder<Double>()
                    .setMinDefaultMax(0.0, 0.0, 1.0)
                    .comment(""
                            + "When matching worlds of the same dimension type the \n"
                            + " tested chunks must be at least this percent the same \n"
                            + " in order to be considered the same world. \n"
                            + "\n"
                            + "Note: If you use portals to enter a dimension at two \n"
                            + " different locations this system may think it is two different worlds. \n"
                            + "\n"
                            + " 1.0 (100%) the chunks must be identical. \n"
                            + " 0.5 (50%)  the chunks must be half the same. \n"
                            + " 0.0 (0%)   disables multi-dimension support, \n"
                            + "            only one world will be used per dimension.")
                    .build();

        }


        public static class Advanced
        {
            public static ConfigCategory threading = new ConfigCategory.Builder().set(Threading.class).build();

            public static ConfigCategory debugging = new ConfigCategory.Builder().set(Debugging.class).build();

            public static ConfigCategory buffers = new ConfigCategory.Builder().set(Buffers.class).build();

            public static ConfigEntry<Boolean> lodOnlyMode = new ConfigEntry.Builder<Boolean>()
                    .set(false)
                    .comment(""
                            + " Due to some demand for playing without vanilla terrain, \n"
                            + " we decided to add this mode for fun. \n"
                            + "\n"
                            + " NOTE: Do not report any issues when this mode is on! \n"
                            + "    This setting is only for fun, and mod \n"
                            + "    compatibility is not guaranteed.")
                    .build();


            public static class Threading
            {
                public static final ConfigEntry<Double> numberOfWorldGenerationThreads = new ConfigEntry.Builder<Double>()
                        .setMinDefaultMax(0.1,
                                (double) Math.min(Runtime.getRuntime().availableProcessors()/2, 4),
                                (double) Runtime.getRuntime().availableProcessors())
                        .comment(""
                                + " How many threads should be used when generating fake \n"
                                + " chunks outside the normal render distance? \n"
                                + "\n"
                                + " If it's less than 1, it will be treated as a percentage \n"
                                + " of time single thread can run before going to idle. \n"
                                + "\n"
                                + " If you experience stuttering when generating distant LODs, \n"
                                + " decrease  this number. If you want to increase LOD \n"
                                + " generation speed, increase this number. \n"
                                + "\n"
                                + " This and the number of buffer builder threads are independent, \n"
                                + " so if they add up to more threads than your CPU has cores, \n"
                                + " that shouldn't cause an issue.")
                        .build();
    
                /** Returns the number of threads that can be used to generate terrain */
                public static int getWorldGenerationThreadPoolSize()
                {
                    Double objValue = numberOfWorldGenerationThreads.get();
                    double value = objValue;
                    //double value = numberOfWorldGenerationThreads.get();
                    if (value < 1.0)
                    {
                        return 1;
                    }
                    else
                    {
                        return (int) value;
                    }
                }
                /** Returns how often world generator threads should run as a number between 0.0 and 1.0 */
                public static double getWorldGenerationPartialRunTime()
                {
                    return numberOfWorldGenerationThreads.get() > 1 ?
                            1.0 : numberOfWorldGenerationThreads.get();
                }
    
    
                public static ConfigEntry<Integer> numberOfBufferBuilderThreads = new ConfigEntry.Builder<Integer>()
                        .setMinDefaultMax(1,
                                Math.min(Runtime.getRuntime().availableProcessors()/2, 2),
                                Runtime.getRuntime().availableProcessors())
                        .comment(""
                                + "How many threads are used when building vertex buffers? \n"
                                + " (The things sent to your GPU to draw the fake chunks). \n"
                                + "\n"
                                + "If you experience high CPU usage when NOT generating distant \n"
                                + " fake chunks, lower this number. A higher number will make fake\n"
                                + " fake chunks' transition faster when moving around the world. \n"
                                + "\n"
                                + "This and the number of world generator threads are independent, \n"
                                + " so if they add up to more threads than your CPU has cores, \n"
                                + " that shouldn't cause an issue. \n"
                                + "\n"
                                + "The maximum value is the number of logical processors on your CPU.")
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
                                + EDebugMode.SHOW_WIREFRAME + ": Fake chunks will be drawn as wireframes. \n"
                                + EDebugMode.SHOW_DETAIL + ": Fake chunks color will be based on their detail level. \n"
                                + EDebugMode.SHOW_DETAIL_WIREFRAME + ": Fake chunks color will be based on their detail level, drawn as a wireframe. \n"
                                + EDebugMode.SHOW_GENMODE + ": Fake chunks color will be based on their distant generation mode. \n"
                                + EDebugMode.SHOW_GENMODE_WIREFRAME + ": Fake chunks color will be based on their distant generation mode, drawn as a wireframe. \n"
                                + EDebugMode.SHOW_OVERLAPPING_QUADS + ": Fake chunks will be drawn with total white, but overlapping quads will be drawn with red. \n"
                                + EDebugMode.SHOW_OVERLAPPING_QUADS_WIREFRAME + ": Fake chunks will be drawn with total white, \n"
                                + " but overlapping quads will be drawn with red, drawn as a wireframe.")
                        .build();

                public static ConfigEntry<Boolean> enableDebugKeybindings = new ConfigEntry.Builder<Boolean>()
                        .set(false)
                        .comment(""
                                + "If true the F8 key can be used to cycle through the different debug modes. \n"
                                + " and the F6 key can be used to enable and disable LOD rendering.")
                        .build();

                public static ConfigCategory debugSwitch = new ConfigCategory.Builder().set(DebugSwitch.class).build();


                public static class DebugSwitch {
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
            }


            public static class Buffers
            {
                public static ConfigEntry<EGpuUploadMethod> gpuUploadMethod = new ConfigEntry.Builder<EGpuUploadMethod>()
                        .set(EGpuUploadMethod.AUTO)
                        .comment(""
                                + "What method should be used to upload geometry to the GPU? \n"
                                + "\n"
                                + EGpuUploadMethod.AUTO + ": Picks the best option based on the GPU you have. \n"
                                + EGpuUploadMethod.BUFFER_STORAGE + ": Default for NVIDIA if OpenGL 4.5 is supported. \n"
                                + "    Fast rendering, no stuttering. \n"
                                + EGpuUploadMethod.SUB_DATA + ": Backup option for NVIDIA. \n"
                                + "    Fast rendering but may stutter when uploading. \n"
                                + EGpuUploadMethod.BUFFER_MAPPING + ": Slow rendering but won't stutter when uploading. Possibly the best option for integrated GPUs. \n"
                                + "    Default option for AMD/Intel. \n"
                                + "    May end up storing buffers in System memory. \n"
                                + "    Fast rendering if in GPU memory, slow if in system memory, \n"
                                + "     but won't stutter when uploading.  \n"
                                + EGpuUploadMethod.DATA + ": Fast rendering but will stutter when uploading. \n"
                                + "    Backup option for AMD/Intel. \n"
                                + "    Fast rendering but may stutter when uploading. \n"
                                + "\n"
                                + "If you don't see any difference when changing these settings, or the world looks corrupted: \n"
                                + "Restart the game to clear the old buffers.")
                        .build();

                public static ConfigEntry<Integer> gpuUploadPerMegabyteInMilliseconds = new ConfigEntry.Builder<Integer>()
                        .setMinDefaultMax(0, 0, 50)
                        .comment(""
                                + "How long should a buffer wait per Megabyte of data uploaded?\n"
                                + "Helpful resource for frame times: https://fpstoms.com \n"
                                + "\n"
                                + "Longer times may reduce stuttering but will make fake chunks \n"
                                + " transition and load slower. Change this to [0] for no timeout.\n"
                                + "\n"
                                + "NOTE:\n"
                                + "Before changing this config, try changing \"GPU Upload methods\"\n"
                                + " and determined the best method for your hardware first.")
                        .build();

                public static ConfigEntry<EBufferRebuildTimes> rebuildTimes = new ConfigEntry.Builder<EBufferRebuildTimes>()
                        .set(EBufferRebuildTimes.NORMAL)
                        .comment(""
                                + "How frequently should vertex buffers (geometry) be rebuilt and sent to the GPU? \n"
                                + "Higher settings may cause stuttering, but will prevent holes in the world")
                        .build();
            }
        }
    }
}
