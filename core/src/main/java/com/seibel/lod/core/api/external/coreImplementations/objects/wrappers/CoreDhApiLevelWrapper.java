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

package com.seibel.lod.core.api.external.coreImplementations.objects.wrappers;

import com.seibel.lod.core.api.external.coreImplementations.interfaces.wrappers.world.ICoreDhApiLevelWrapper;
import com.seibel.lod.core.enums.ELevelType;
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
public class CoreDhApiLevelWrapper implements ICoreDhApiLevelWrapper
{
	private final ILevelWrapper levelWrapper;
	private final IDimensionTypeWrapper dimensionTypeWrapper;
	
	
	public CoreDhApiLevelWrapper(ILevelWrapper newLevelWrapper)
	{
		this.levelWrapper = newLevelWrapper;
		this.dimensionTypeWrapper = this.levelWrapper.getDimensionType();
	}
	
	
	public ELevelType getLevelType()
	{
		if (this.levelWrapper.getClass().isAssignableFrom(IClientLevelWrapper.class))
		{
			return ELevelType.CLIENT_LEVEL;
		}
		else if (this.levelWrapper.getClass().isAssignableFrom(IServerLevelWrapper.class))
		{
			return ELevelType.CLIENT_LEVEL;
		}
		else
		{
			// shouldn't normally happen, but just in case
			return ELevelType.UNKNOWN;
		}
	}
	
	public boolean hasCeiling() { return this.levelWrapper.hasCeiling(); }
	
	public boolean hasSkyLight() { return this.levelWrapper.hasSkyLight(); }
	
	public int getHeight() { return this.levelWrapper.getHeight(); }
	
	public int getMinHeight() { return this.levelWrapper.getMinHeight(); }
	
	@Override
	public Object getWrappedMcObject_UNSAFE() { return this.levelWrapper.unwrapLevel(); }
	
}