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
 
package com.seibel.lod.core.wrapperInterfaces.block;

import com.seibel.lod.core.enums.ELodDirection;
import com.seibel.lod.core.enums.config.EBlocksToAvoid;
import com.seibel.lod.core.objects.DHBlockPos;
import com.seibel.lod.core.wrapperInterfaces.chunk.IChunkWrapper;


public abstract class IBlockDetailWrapper
{
	// Note: ALL value should be lazily-calculated
	
	// Note: This should be lazily-calculated if block needs tinting to be resolved
	public abstract int getAndResolveFaceColor(ELodDirection dir, IChunkWrapper chunk, DHBlockPos blockPos);
	public abstract boolean hasFaceCullingFor(ELodDirection dir);
	public abstract boolean hasNoCollision();
	public abstract boolean noFaceIsFullFace();
	
	public abstract String serialize();
	
	protected abstract boolean isSame(IBlockDetailWrapper iBlockDetail);
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof IBlockDetailWrapper)) return false;
		return isSame((IBlockDetailWrapper)o);
	}
	
	@Override
	public String toString() {
		return serialize();
	}
	
	public boolean shouldRender(EBlocksToAvoid mode)
	{
		return !((mode.noCollision && hasNoCollision()) || (mode.nonFull && noFaceIsFullFace()));
	}
	
	
}
