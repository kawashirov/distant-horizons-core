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

package com.seibel.lod.core.api.implementation.wrappers;

import com.seibel.lod.core.api.external.items.enums.worldGeneration.EDhApiLevelType;
import com.seibel.lod.core.api.external.items.interfaces.world.IDhApiDimensionTypeWrapper;
import com.seibel.lod.core.api.external.items.interfaces.world.IDhApiLevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IClientLevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IDimensionTypeWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IServerLevelWrapper;

/**
 * @author James Seibel
 * @version 2022-8-23
 */
public class DhApiDimensionTypeWrapper implements IDhApiDimensionTypeWrapper
{
	private final IDimensionTypeWrapper dimensionTypeWrapper;
	
	
	public DhApiDimensionTypeWrapper(IDimensionTypeWrapper newDimensionTypeWrapper)
	{
		this.dimensionTypeWrapper = newDimensionTypeWrapper;
	}
	
	
	
	@Override
	public String getDimensionName() { return this.dimensionTypeWrapper.getDimensionName(); }
	
	@Override
	public boolean hasCeiling() { return this.dimensionTypeWrapper.hasCeiling(); }
	
	@Override
	public boolean hasSkyLight() { return this.dimensionTypeWrapper.hasSkyLight(); }
	
	
	@Override
	public Object getWrappedMcObject_UNSAFE() { return this.dimensionTypeWrapper.getWrappedMcObject(); }
	
}
