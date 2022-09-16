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

package com.seibel.lod.core.api.external.coreImplementations.methods.config.client;

import com.seibel.lod.api.items.enums.config.*;
import com.seibel.lod.api.items.interfaces.config.IDhApiConfig;
import com.seibel.lod.api.items.interfaces.config.client.IDhApiGraphicsConfig;
import com.seibel.lod.api.items.objects.config.DhApiConfig;
import com.seibel.lod.core.interfaces.config.converters.RenderModeEnabledConverter;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.api.items.enums.rendering.ERendererMode;
import com.seibel.lod.core.config.Config.Client.Graphics.Quality;
import com.seibel.lod.core.config.Config.Client.Advanced.Debugging;
import com.seibel.lod.core.config.Config.Client.Graphics.AdvancedGraphics;

/**
 * Distant Horizons' graphics/rendering configuration.
 *
 * @author James Seibel
 * @version 2022-9-15
 */
public class DhApiGraphicsConfig implements IDhApiGraphicsConfig
{
	
	//========================//
	// basic graphic settings //
	//========================//
	
	@Override
	public IDhApiConfig<Integer> getChunkRenderDistanceConfig()
	{ return new DhApiConfig<>(Quality.lodChunkRenderDistance); }
	
	@Override
	public IDhApiConfig<Boolean> getRenderingEnabledConfig()
	{ return new DhApiConfig<ERendererMode, Boolean>(Debugging.rendererMode, new RenderModeEnabledConverter()); }
	
	@Override
	public IDhApiConfig<ERendererMode> getRenderingModeConfig()
	{ return new DhApiConfig<>(Debugging.rendererMode); }
	
	
	
	//==================//
	// graphic settings //
	//==================//
	
	@Override
	public IDhApiConfig<EHorizontalResolution> getMaxDetailLevelConfig()
	{ return new DhApiConfig<>(Quality.drawResolution); }
	
	@Override
	public IDhApiConfig<EVerticalQuality> getVerticalQualityConfig()
	{ return new DhApiConfig<>(Quality.verticalQuality); }
	
	@Override
	public IDhApiConfig<EHorizontalQuality> getHorizontalQualityDropoffConfig()
	{ return new DhApiConfig<>(Quality.horizontalQuality); }
	
	@Override
	public IDhApiConfig<Integer> getBiomeBlendingConfig()
	{ return new DhApiConfig<>(Quality.lodBiomeBlending); }
	
	
	
	//===========================//
	// advanced graphic settings //
	//===========================//
	
	@Override
	public IDhApiConfig<Boolean> getDisableDirectionalCullingConfig()
	{ return new DhApiConfig<>(AdvancedGraphics.disableDirectionalCulling); }
	
	@Override
	public IDhApiConfig<EVanillaOverdraw> getVanillaOverdrawConfig()
	{ return new DhApiConfig<>(AdvancedGraphics.vanillaOverdraw); }
	
	@Override
	public IDhApiConfig<Integer> getVanillaOverdrawOffsetConfig()
	{ return new DhApiConfig<>(AdvancedGraphics.overdrawOffset); }
	
	@Override
	public IDhApiConfig<Boolean> getUseExtendedNearClipPlaneConfig()
	{ return new DhApiConfig<>(AdvancedGraphics.useExtendedNearClipPlane); }
	
	@Override
	public IDhApiConfig<Double> getBrightnessMultiplierConfig()
	{ return new DhApiConfig<>(AdvancedGraphics.brightnessMultiplier); }
	
	@Override
	public IDhApiConfig<Double> getSaturationMultiplierConfig()
	{ return new DhApiConfig<>(AdvancedGraphics.saturationMultiplier); }
	
	@Override
	public IDhApiConfig<Boolean> getCaveCullingEnabledConfig()
	{ return new DhApiConfig<>(AdvancedGraphics.enableCaveCulling); }
	
	@Override
	public IDhApiConfig<Integer> getCaveCullingHeightConfig()
	{ return new DhApiConfig<>(AdvancedGraphics.caveCullingHeight); }
	
	@Override
	public IDhApiConfig<Integer> getEarthCurvatureRatioConfig()
	{ return new DhApiConfig<>(AdvancedGraphics.earthCurveRatio); }
	
	@Override
	public IDhApiConfig<Boolean> getEnableLodOnlyModeConfig()
	{ return new DhApiConfig<>(Config.Client.Advanced.lodOnlyMode); }
	
	@Override
	public IDhApiConfig<EBufferRebuildTimes> getGeometryRebuildFrequencyConfig()
	{ return new DhApiConfig<>(Config.Client.Advanced.Buffers.rebuildTimes); }
	
	
	
}
