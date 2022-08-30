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
 
package com.seibel.lod.core.objects;

/*
import com.seibel.lod.core.wrapperInterfaces.block.BlockDetail;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class BlockBiomeCouple
{
	public static final ConcurrentMap<BlockDetail, BlockBiomeCouple> noBiomeInstanceCache = new ConcurrentHashMap<>();
	public static ConcurrentMap<IBiomeWrapper, ConcurrentMap<BlockDetail, BlockBiomeCouple>> withBiomeInstanceCache = new ConcurrentHashMap<>();
	
	String blockName;
	String biomeName;
	String coupleName;
	
	IBiomeWrapper biomeColor;
	BlockDetail blockColor;
	
	public static void addBlockBiomeToCache(IBlockColorWrapper blockColor){
	}
	
	public static BlockBiomeCouple getBlockBiomeCouple(IBlockColorWrapper blockColor){
		if(noBiomeInstanceCache.containsKey(blockColor))
		{
			return noBiomeInstanceCache.get(blockColor);
		}
		else
		{
			BlockBiomeCouple couple = new BlockBiomeCouple(blockColor);
			noBiomeInstanceCache.put(blockColor,couple);
			return couple;
		}
	}
	
	public static BlockBiomeCouple getBlockBiomeCouple(IBiomeWrapper biomeColor, IBlockColorWrapper blockColor){
		if(biomeColor == null)
		{
			return getBlockBiomeCouple(blockColor);
		}
		else
		{
			if(withBiomeInstanceCache.containsKey(biomeColor))
			{
				withBiomeInstanceCache.put(biomeColor, new ConcurrentHashMap<>());
			}
			ConcurrentMap<IBlockColorWrapper, BlockBiomeCouple> blockToCoupleMap = withBiomeIstanceCache.get(biomeColor);
			if(blockToCoupleMap.containsKey(blockColor))
			{
				return blockToCoupleMap.get(blockColor);
			}
			else
			{
				BlockBiomeCouple couple = new BlockBiomeCouple(blockColor,biomeColor);
				blockToCoupleMap.put(blockColor,couple);
				return couple;
			}
		}
	}
	
	public BlockBiomeCouple(IBlockColorWrapper blockColor)
	{
		this.biomeColor = null;
		this.blockColor = blockColor;
		biomeName = "";
		blockName = blockColor.getName();
		coupleName = blockName;
	}
	
	public BlockBiomeCouple(IBlockColorWrapper blockColor, IBiomeWrapper biomeColor)
	{
		this.biomeColor = biomeColor;
		this.blockColor = blockColor;
		
		if(biomeColor == null)
			biomeName = biomeColor.getName();
		else
			biomeName = "";
		
		blockName = blockColor.getName();
		
		coupleName = blockName + biomeName;
	}
	
	@Override public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (!(o instanceof BlockBiomeCouple))
			return false;
		BlockBiomeCouple that = (BlockBiomeCouple) o;
		return Objects.equals(blockName, that.blockName) && Objects.equals(biomeName, that.biomeName);
	}
	
	@Override public int hashCode()
	{
		return Objects.hash(blockName, biomeName);
	}
	
	
}
*/