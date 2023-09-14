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

package com.seibel.distanthorizons.core.pos;

import com.seibel.distanthorizons.core.enums.EDhDirection;
import com.seibel.distanthorizons.coreapi.util.BitShiftUtil;
import com.seibel.distanthorizons.core.util.LodUtil;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The position object used to define LOD objects in the quad trees. <br><br>
 *
 * A section contains 64 x 64 LOD columns at a given quality.
 * The Section detail level is different from the LOD detail level.
 * For the specifics of how they compare can be viewed in the constants {@link #SECTION_BLOCK_DETAIL_LEVEL},
 * {@link #SECTION_CHUNK_DETAIL_LEVEL}, and {@link #SECTION_REGION_DETAIL_LEVEL}).<br><br>
 *
 * <strong>Why does the smallest render section represent 2x2 MC chunks (section detail level 6)? </strong> <br>
 * A section defines what unit the quad tree works in, because of that we don't want that unit to be too big or too small. <br>
 * <strong>Too small</strong>, and we'll have 1,000s of sections running around, all needing individual files and render buffers.<br>
 * <strong>Too big</strong>, and the LOD dropoff will be very noticeable.<br>
 * With those thoughts in mind we decided on a smallest section size of 32 data points square (IE 2x2 chunks).
 *
 * @author Leetom
 * @version 2022-11-6
 */
public class DhSectionPos
{
	/**
	 * The lowest detail level a Section position can hold.
	 * This section DetailLevel holds 64 x 64 Block level (detail level 0) LODs.
	 */
	public final static byte SECTION_MINIMUM_DETAIL_LEVEL = 6;
	
	public final static byte SECTION_BLOCK_DETAIL_LEVEL = SECTION_MINIMUM_DETAIL_LEVEL + LodUtil.BLOCK_DETAIL_LEVEL;
	public final static byte SECTION_CHUNK_DETAIL_LEVEL = SECTION_MINIMUM_DETAIL_LEVEL + LodUtil.CHUNK_DETAIL_LEVEL;
	public final static byte SECTION_REGION_DETAIL_LEVEL = SECTION_MINIMUM_DETAIL_LEVEL + LodUtil.REGION_DETAIL_LEVEL;
	
	
	public byte sectionDetailLevel;
	
	/** in a sectionDetailLevel grid */
	public int sectionX;
	/** in a sectionDetailLevel grid */
	public int sectionZ;
	
	
	
	//==============//
	// constructors //
	//==============//
	
	public DhSectionPos(byte sectionDetailLevel, int sectionX, int sectionZ)
	{
		this.sectionDetailLevel = sectionDetailLevel;
		this.sectionX = sectionX;
		this.sectionZ = sectionZ;
	}
	
	public DhSectionPos(DhBlockPos blockPos)
	{
		this(LodUtil.BLOCK_DETAIL_LEVEL, blockPos.x, blockPos.z);
		this.convertSelfToDetailLevel(DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL);
	}
	public DhSectionPos(DhBlockPos2D blockPos)
	{
		this(LodUtil.BLOCK_DETAIL_LEVEL, blockPos.x, blockPos.z);
		this.convertSelfToDetailLevel(DhSectionPos.SECTION_BLOCK_DETAIL_LEVEL);
	}
	
	public DhSectionPos(DhChunkPos chunkPos)
	{
		this(LodUtil.CHUNK_DETAIL_LEVEL, chunkPos.x, chunkPos.z);
		this.convertSelfToDetailLevel(DhSectionPos.SECTION_CHUNK_DETAIL_LEVEL);
	}
	
	public DhSectionPos(byte detailLevel, DhLodPos dhLodPos)
	{
		this.sectionDetailLevel = detailLevel;
		this.sectionX = dhLodPos.x;
		this.sectionZ = dhLodPos.z;
	}
	
	
	
	//============//
	// converters //
	//============//
	
	/** uses the absolute detail level aka detail levels like {@link LodUtil#CHUNK_DETAIL_LEVEL} instead of the dhSectionPos detailLevels. */
	public void convertSelfToDetailLevel(byte newDetailLevel)
	{
		// logic originally taken from DhLodPos
		if (newDetailLevel >= this.sectionDetailLevel)
		{
			this.sectionX = Math.floorDiv(this.sectionX, BitShiftUtil.powerOfTwo(newDetailLevel - this.sectionDetailLevel)); 
			this.sectionZ = Math.floorDiv(this.sectionZ, BitShiftUtil.powerOfTwo(newDetailLevel - this.sectionDetailLevel));
		}
		else
		{
			this.sectionX = this.sectionX * BitShiftUtil.powerOfTwo(this.sectionDetailLevel - newDetailLevel);
			this.sectionZ = this.sectionZ * BitShiftUtil.powerOfTwo(this.sectionDetailLevel - newDetailLevel);
		}
		
		this.sectionDetailLevel = newDetailLevel;
	}
	
	/**
	 * uses the absolute detail level aka detail levels like {@link LodUtil#CHUNK_DETAIL_LEVEL} instead of the dhSectionPos detailLevels.
	 *
	 * @return the new position closest to negative infinity with the new detail level
	 */
	public DhSectionPos convertNewToDetailLevel(byte newSectionDetailLevel)
	{
		DhSectionPos newPos = new DhSectionPos(this.sectionDetailLevel, this.sectionX, this.sectionZ);
		newPos.convertSelfToDetailLevel(newSectionDetailLevel);
		
		return newPos;
	}
	
	
	
	//=========//
	// getters //
	//=========//
	
	/** Returns the center for detail level 0 */
	public DhLodPos getCenter() { return this.getCenter((byte) 0); } // TODO why does this use detail level 0 instead of this object's detail level?
	public DhLodPos getCenter(byte returnDetailLevel)
	{
		LodUtil.assertTrue(returnDetailLevel <= this.sectionDetailLevel, "returnDetailLevel must be less than sectionDetail");
		
		if (returnDetailLevel == this.sectionDetailLevel)
		{
			return new DhLodPos(this.sectionDetailLevel, this.sectionX, this.sectionZ);
		}
		
		byte detailLevelOffset = (byte) (this.sectionDetailLevel - returnDetailLevel);
		
		// we can't get the center of the position at block level, only attempt to get the position offset for detail levels above 0 // TODO should this also apply to detail level 1 or is it fine?
		int positionOffset = 0;
		if (this.sectionDetailLevel != 1 || returnDetailLevel != 0)
		{
			positionOffset = BitShiftUtil.powerOfTwo(detailLevelOffset - 1);
		}
		
		return new DhLodPos(returnDetailLevel,
				(this.sectionX * BitShiftUtil.powerOfTwo(detailLevelOffset)) + positionOffset,
				(this.sectionZ * BitShiftUtil.powerOfTwo(detailLevelOffset)) + positionOffset);
	}
	
	/** @return the corner with the smallest X and Z coordinate */
	public DhLodPos getCorner() { return this.getCorner((byte) (this.sectionDetailLevel - 1)); }
	/** @return the corner with the smallest X and Z coordinate */
	public DhLodPos getCorner(byte returnDetailLevel)
	{
		LodUtil.assertTrue(returnDetailLevel <= this.sectionDetailLevel, "returnDetailLevel must be less than sectionDetail");
		
		byte offset = (byte) (this.sectionDetailLevel - returnDetailLevel);
		return new DhLodPos(returnDetailLevel,
				this.sectionX * BitShiftUtil.powerOfTwo(offset),
				this.sectionZ * BitShiftUtil.powerOfTwo(offset));
	}
	
	public DhLodUnit getWidth() { return this.getWidth(this.sectionDetailLevel); } // this always returns 1...
	public DhLodUnit getWidth(byte returnDetailLevel)
	{
		LodUtil.assertTrue(returnDetailLevel <= this.sectionDetailLevel, "returnDetailLevel must be less than sectionDetail");
		byte offset = (byte) (this.sectionDetailLevel - returnDetailLevel);
		return new DhLodUnit(this.sectionDetailLevel, BitShiftUtil.powerOfTwo(offset));
	}
	
	
	
	//==================//
	// parent child pos //
	//==================//
	
	/**
	 * Returns the DhLodPos 1 detail level lower <br><br>
	 *
	 * Relative child positions returned for each index: <br>
	 * 0 = (0,0) - North West <br>
	 * 1 = (1,0) - South West <br>
	 * 2 = (0,1) - North East <br>
	 * 3 = (1,1) - South East <br>
	 *
	 * @param child0to3 must be an int between 0 and 3
	 */
	public DhSectionPos getChildByIndex(int child0to3) throws IllegalArgumentException, IllegalStateException
	{
		if (child0to3 < 0 || child0to3 > 3)
		{
			throw new IllegalArgumentException("child0to3 must be between 0 and 3");
		}
		if (this.sectionDetailLevel <= 0)
		{
			throw new IllegalStateException("section detail must be greater than 0");
		}
		
		return new DhSectionPos((byte) (this.sectionDetailLevel - 1),
				this.sectionX * 2 + (child0to3 & 1),
				this.sectionZ * 2 + BitShiftUtil.half(child0to3 & 2));
	}
	/** Returns this position's child index in its parent */
	public int getChildIndexOfParent() { return (this.sectionX & 1) + BitShiftUtil.square(this.sectionZ & 1); }
	
	public DhSectionPos getParentPos() { return new DhSectionPos((byte) (this.sectionDetailLevel + 1), BitShiftUtil.half(this.sectionX), BitShiftUtil.half(this.sectionZ)); }
	
	
	
	
	public DhSectionPos getAdjacentPos(EDhDirection dir)
	{
		return new DhSectionPos(this.sectionDetailLevel,
				this.sectionX + dir.getNormal().x,
				this.sectionZ + dir.getNormal().z);
	}
	
	public DhLodPos getSectionBBoxPos() { return new DhLodPos(this.sectionDetailLevel, this.sectionX, this.sectionZ); }
	
	
	
	//=============//
	// comparisons //
	//=============//
	
	/** NOTE: This does not consider yOffset! */
	public boolean overlaps(DhSectionPos other) { return this.getSectionBBoxPos().overlapsExactly(other.getSectionBBoxPos()); }
	
	/** NOTE: This does not consider yOffset! */
	public boolean contains(DhSectionPos otherPos)
	{
		DhBlockPos2D thisMinBlockPos = this.getCorner(LodUtil.BLOCK_DETAIL_LEVEL).getCornerBlockPos();
		DhBlockPos2D otherCornerBlockPos = otherPos.getCorner(LodUtil.BLOCK_DETAIL_LEVEL).getCornerBlockPos();
		
		int thisBlockWidth = this.getWidth().toBlockWidth() - 1; // minus 1 to account for zero based positional indexing
		DhBlockPos2D thisMaxBlockPos = new DhBlockPos2D(thisMinBlockPos.x + thisBlockWidth, thisMinBlockPos.z + thisBlockWidth);
		
		return thisMinBlockPos.x <= otherCornerBlockPos.x && otherCornerBlockPos.x <= thisMaxBlockPos.x &&
				thisMinBlockPos.z <= otherCornerBlockPos.z && otherCornerBlockPos.z <= thisMaxBlockPos.z;
	}
	
	
	
	//===========//
	// iterators //
	//===========//
	
	/** Applies the given consumer to all 4 of this position's children. */
	public void forEachChild(Consumer<DhSectionPos> callback)
	{
		for (int i = 0; i < 4; i++)
		{
			callback.accept(this.getChildByIndex(i));
		}
	}
	
	/** Applies the given consumer to all children of the position at the given section detail level. */
	public void forEachChildAtLevel(byte sectionDetailLevel, Consumer<DhSectionPos> callback)
	{
		if (sectionDetailLevel == this.sectionDetailLevel)
		{
			callback.accept(this);
			return;
		}
		
		for (int i = 0; i < 4; i++)
		{
			this.getChildByIndex(i).forEachChildAtLevel(sectionDetailLevel, callback);
		}
	}
	
	
	
	//===============//
	// serialization //
	//===============//
	
	/** Serialize() is different from toString() as it must NEVER be changed, and should be in a short format */
	public String serialize() { return "[" + this.sectionDetailLevel + ',' + this.sectionX + ',' + this.sectionZ + ']'; }
	
	@Nullable
	public static DhSectionPos deserialize(String value)
	{
		if (value.charAt(0) != '[' || value.charAt(value.length() - 1) != ']') return null;
		String[] split = value.substring(1, value.length() - 1).split(",");
		if (split.length != 3) return null;
		return new DhSectionPos(Byte.parseByte(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
		
	}
	
	
	
	//===========//
	// overrides //
	//===========//
	
	@Override
	public String toString() { return "{" + this.sectionDetailLevel + "*" + this.sectionX + "," + this.sectionZ + "}"; }
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null || obj.getClass() != DhSectionPos.class)
		{
			return false;
		}
		
		DhSectionPos that = (DhSectionPos) obj;
		return this.sectionDetailLevel == that.sectionDetailLevel &&
				this.sectionX == that.sectionX &&
				this.sectionZ == that.sectionZ;
	}
	
	@Override
	public int hashCode()
	{
		return Integer.hashCode(this.sectionDetailLevel) ^ // XOR
				Integer.hashCode(this.sectionX) ^ // XOR
				Integer.hashCode(this.sectionZ);
	}
	
}
