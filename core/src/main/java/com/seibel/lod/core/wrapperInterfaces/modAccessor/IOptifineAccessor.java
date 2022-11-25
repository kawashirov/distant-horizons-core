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
 
package com.seibel.lod.core.wrapperInterfaces.modAccessor;

import com.seibel.lod.api.enums.rendering.EFogDrawMode;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.pos.DhChunkPos;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;

import java.lang.reflect.Field;
import java.util.HashSet;

public interface IOptifineAccessor extends IModAccessor 
{
	
	/** Can be null */
	HashSet<DhChunkPos> getNormalRenderedChunks();
	
	/** Get what type of fog optifine is currently set to render. */
	EFogDrawMode getFogDrawMode();
	
	/**
	 * Returns the percentage multiplier of the screen's current resolution. <br>
	 * 1.0 = 100% <br>
	 * 1.5 = 150% <br>
	 */
	double getRenderResolutionMultiplier();
	
}
