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

package com.seibel.lod.api.items.objects.wrappers;

import com.seibel.lod.api.items.enums.worldGeneration.EDhApiLevelType;
import com.seibel.lod.api.items.interfaces.IDhApiUnsafeWrapper;
import com.seibel.lod.api.items.interfaces.world.IDhApiDimensionTypeWrapper;
import com.seibel.lod.api.items.interfaces.world.IDhApiLevelWrapper;
import com.seibel.lod.core.api.external.coreImplementations.interfaces.wrappers.world.ICoreDhApiLevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IDimensionTypeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IServerLevelWrapper;

/**
 * Can be either a Server or Client level.
 * 
 * @author James Seibel
 * @version 2022-9-8
 */
public class DhApiLevelWrapper implements IDhApiLevelWrapper
{
	private final ILevelWrapper levelWrapper;
	private final IDimensionTypeWrapper dimensionTypeWrapper;
	
	
	public DhApiLevelWrapper(ILevelWrapper newLevelWrapper)
	{
		this.levelWrapper = newLevelWrapper;
		this.dimensionTypeWrapper = this.levelWrapper.getDimensionType();
	}
	
	
	@Override
	public IDhApiDimensionTypeWrapper getDimensionType() { return new DhApiDimensionTypeWrapper(this.dimensionTypeWrapper); }
	
	@Override
	public EDhApiLevelType getLevelType()
	{
		if (this.levelWrapper.getClass().isAssignableFrom(IClientLevelWrapper.class))
		{
			return EDhApiLevelType.CLIENT_LEVEL;
		}
		else if (this.levelWrapper.getClass().isAssignableFrom(IServerLevelWrapper.class))
		{
			return EDhApiLevelType.CLIENT_LEVEL;
		}
		else
		{
			// shouldn't normally happen, but just in case
			return EDhApiLevelType.UNKNOWN;
		}
	}
	
	@Override
	public boolean hasCeiling() { return this.levelWrapper.hasCeiling(); }
	
	@Override
	public boolean hasSkyLight() { return this.levelWrapper.hasSkyLight(); }
	
	@Override
	public int getHeight() { return this.levelWrapper.getHeight(); }
	
	@Override
	public int getMinHeight() { return this.levelWrapper.getMinHeight(); }
	
	
	@Override
	public Object getWrappedMcObject_UNSAFE() { return this.levelWrapper.unwrapLevel(); }
	
}
