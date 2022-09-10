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
import com.seibel.lod.api.items.interfaces.world.IDhApiDimensionTypeWrapper;
import com.seibel.lod.api.items.interfaces.world.IDhApiLevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IDimensionTypeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IServerLevelWrapper;

/**
 * Can be either a Server or Client level.
 * 
 * @author James Seibel
 * @version 2022-9-10
 */
public class DhApiLevelWrapper implements IDhApiLevelWrapper
{
	private final ILevelWrapper coreLevelWrapper;
	private final IDimensionTypeWrapper coreDimensionTypeWrapper;
	
	
	public DhApiLevelWrapper(ILevelWrapper newLevelWrapper)
	{
		this.coreLevelWrapper = newLevelWrapper;
		this.coreDimensionTypeWrapper = this.coreLevelWrapper.getDimensionType();
	}
	
	
	@Override
	public IDhApiDimensionTypeWrapper getDimensionType() { return new DhApiDimensionTypeWrapper(this.coreDimensionTypeWrapper); }
	
	@Override
	public EDhApiLevelType getLevelType()
	{
		if (this.coreLevelWrapper.getClass().isAssignableFrom(IClientLevelWrapper.class))
		{
			return EDhApiLevelType.CLIENT_LEVEL;
		}
		else if (this.coreLevelWrapper.getClass().isAssignableFrom(IServerLevelWrapper.class))
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
	public boolean hasCeiling() { return this.coreLevelWrapper.hasCeiling(); }
	
	@Override
	public boolean hasSkyLight() { return this.coreLevelWrapper.hasSkyLight(); }
	
	@Override
	public int getHeight() { return this.coreLevelWrapper.getHeight(); }
	
	@Override
	public int getMinHeight() { return this.coreLevelWrapper.getMinHeight(); }
	
	
	@Override
	public Object getWrappedMcObject_UNSAFE() { return this.coreLevelWrapper.unwrapLevel(); }
	
}
