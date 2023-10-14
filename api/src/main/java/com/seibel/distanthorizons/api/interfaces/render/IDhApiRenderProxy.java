/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2023 James Seibel
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

package com.seibel.distanthorizons.api.interfaces.render;

import com.seibel.distanthorizons.api.objects.DhApiResult;

/**
 * Used to interact with Distant Horizons' rendering system.
 *
 * @author James Seibel
 * @version 2023-10-13
 * @since API 1.0.0
 */
public interface IDhApiRenderProxy
{
	/**
	 * Forces any cached render data to be deleted and regenerated.
	 * This is generally called whenever resource packs are changed or specific
	 * rendering settings are changed in Distant Horizon's config. <Br><Br>
	 *
	 * If this is called on a dedicated server it won't do anything and will return {@link DhApiResult#success} = false <Br><Br>
	 *
	 * Background: <Br>
	 * Distant Horizons has two different file formats: Full data and Render data. <Br>
	 * - Full data files store the block, biome, etc. information and is the result of loading or generating new chunks. <Br>
	 * - Render data files store LOD colors and are created using the Full data and currently loaded resource packs. <Br>
	 * This is the data cleared by this method.
	 */
	DhApiResult<Boolean> clearRenderDataCache();
	
	/** 
	 * Returns the name of Distant Horizons' FrameBuffer. <br>
	 * Will return {@link DhApiResult#success} = false and {@link DhApiResult#payload} = -1 if the FrameBuffer hasn't been created yet.
	 */
	DhApiResult<Integer> getDhFrameBufferId();
	
	/** 
	 * Sets the FrameBuffer name that Distant Horizons will draw to after it finishes generating a frame texture. <br>
	 * Setting this to -1 will cause Distant Horizons to use Minecraft's FrameBuffer. <br><br>
	 * 
	 * Will return {@link DhApiResult#success} = false given name isn't a valid FrameBuffer.
	 */
	DhApiResult<Void> setTargetFrameBufferId(int frameBufferId);
	/**
	 * Returns the FrameBuffer name that Distant Horizons will draw to after it finishes generating a frame texture. <br>
	 * By default this will be Minecraft's FrameBuffer.
	 */
	DhApiResult<Integer> getTargetFrameBufferId();
	
	/**
	 * Returns the name of Distant Horizons' depth texture. <br>
	 * Will return {@link DhApiResult#success} = false and {@link DhApiResult#payload} = -1 if the texture hasn't been created yet.
	 */
	DhApiResult<Integer> getDhDepthTextureId();
	/**
	 * Returns the name of Distant Horizons' color texture. <br>
	 * Will return {@link DhApiResult#success} = false and {@link DhApiResult#payload} = -1 if the texture hasn't been created yet.
	 */
	DhApiResult<Integer> getDhColorTextureId();
	
}
