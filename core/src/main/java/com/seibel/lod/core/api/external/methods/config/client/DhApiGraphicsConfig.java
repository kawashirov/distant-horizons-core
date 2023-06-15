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

package com.seibel.lod.core.api.external.methods.config.client;

import com.seibel.lod.api.enums.config.*;
import com.seibel.lod.api.enums.rendering.ETransparency;
import com.seibel.lod.api.interfaces.config.IDhApiConfigValue;
import com.seibel.lod.api.interfaces.config.client.IDhApiFogConfig;
import com.seibel.lod.api.interfaces.config.client.IDhApiGraphicsConfig;
import com.seibel.lod.api.interfaces.config.client.IDhApiNoiseTextureConfig;
import com.seibel.lod.api.objects.config.DhApiConfigValue;
import com.seibel.lod.api.enums.rendering.ERendererMode;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.core.config.Config.Client.Advanced.Graphics.Quality;
import com.seibel.lod.core.config.Config.Client.Advanced.Debugging;
import com.seibel.lod.core.config.Config.Client.Advanced.Graphics.AdvancedGraphics;

public class DhApiGraphicsConfig implements IDhApiGraphicsConfig
{
	public static DhApiGraphicsConfig INSTANCE = new DhApiGraphicsConfig();
	
	private DhApiGraphicsConfig() { }
	
	
	
	//==============//
	// inner layers //
	//==============//
	
	public IDhApiFogConfig fog() { return DhApiFogConfig.INSTANCE; }
	public IDhApiNoiseTextureConfig noiseTexture() { return DhApiNoiseTextureConfig.INSTANCE; }
	
	
	
	//========================//
	// basic graphic settings //
	//========================//
	
	@Override
	public IDhApiConfigValue<Integer> chunkRenderDistance()
	{ return new DhApiConfigValue<Integer, Integer>(Quality.lodChunkRenderDistance); }
	
	@Override
	public IDhApiConfigValue<Boolean> renderingEnabled()
	{ return new DhApiConfigValue<Boolean, Boolean>(Config.Client.quickEnableRendering); }
	
	@Override
	public IDhApiConfigValue<ERendererMode> renderingMode()
	{ return new DhApiConfigValue<ERendererMode, ERendererMode>(Debugging.rendererMode); }
	
	
	
	//==================//
	// graphic settings //
	//==================//
	
	@Override
	public IDhApiConfigValue<EMaxHorizontalResolution> maxHorizontalResolution()
	{ return new DhApiConfigValue<EMaxHorizontalResolution, EMaxHorizontalResolution>(Quality.maxHorizontalResolution); }
	
	@Override
	public IDhApiConfigValue<EVerticalQuality> verticalQuality()
	{ return new DhApiConfigValue<EVerticalQuality, EVerticalQuality>(Quality.verticalQuality); }
	
	@Override
	public IDhApiConfigValue<EHorizontalQuality> horizontalQuality()
	{ return new DhApiConfigValue<EHorizontalQuality, EHorizontalQuality>(Quality.horizontalQuality); }
	
	@Override
	public IDhApiConfigValue<Boolean> ambientOcclusion()
	{ return new DhApiConfigValue<Boolean, Boolean>(Quality.ssao); }
	
	@Override
	public IDhApiConfigValue<ETransparency> transparency()
	{ return new DhApiConfigValue<ETransparency, ETransparency>(Quality.transparency); }
	
	@Override
	public IDhApiConfigValue<EBlocksToAvoid> blocksToAvoid()
	{ return new DhApiConfigValue<EBlocksToAvoid, EBlocksToAvoid>(Quality.blocksToIgnore); }
	
	@Override
	public IDhApiConfigValue<Boolean> tintWithAvoidedBlocks()
	{ return new DhApiConfigValue<Boolean, Boolean>(Quality.tintWithAvoidedBlocks); }
	
	// TODO re-implement
//	@Override
//	public IDhApiConfigValue<Integer> getBiomeBlending()
//	{ return new DhApiConfigValue<Integer, Integer>(Quality.lodBiomeBlending); }
	
	
	
	//===========================//
	// advanced graphic settings //
	//===========================//
	
//	@Override
//	public IDhApiConfigValue<Boolean> getDisableDirectionalCulling()
//	{ return new DhApiConfigValue<Boolean, Boolean>(AdvancedGraphics.disableDirectionalCulling); }
	
	@Override
	public IDhApiConfigValue<EOverdrawPrevention> overdrawPrevention()
	{ return new DhApiConfigValue<EOverdrawPrevention, EOverdrawPrevention>(AdvancedGraphics.overdrawPrevention); }
	
	@Override
	public IDhApiConfigValue<Double> brightnessMultiplier()
	{ return new DhApiConfigValue<Double, Double>(AdvancedGraphics.brightnessMultiplier); }
	
	@Override
	public IDhApiConfigValue<Double> saturationMultiplier()
	{ return new DhApiConfigValue<Double, Double>(AdvancedGraphics.saturationMultiplier); }
	
	@Override
	public IDhApiConfigValue<Boolean> caveCullingEnabled()
	{ return new DhApiConfigValue<Boolean, Boolean>(AdvancedGraphics.enableCaveCulling); }
	
	@Override
	public IDhApiConfigValue<Integer> caveCullingHeight()
	{ return new DhApiConfigValue<Integer, Integer>(AdvancedGraphics.caveCullingHeight); }
	
	@Override
	public IDhApiConfigValue<Integer> earthCurvatureRatio()
	{ return new DhApiConfigValue<Integer, Integer>(AdvancedGraphics.earthCurveRatio); }
	
	@Override
	public IDhApiConfigValue<Boolean> lodOnlyMode()
	{ return new DhApiConfigValue<Boolean, Boolean>(Debugging.lodOnlyMode); }
	
	@Override
	public IDhApiConfigValue<Double> lodBias()
	{ return new DhApiConfigValue<Double, Double>(AdvancedGraphics.lodBias); }
	
	
	
}
