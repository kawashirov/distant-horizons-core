package com.seibel.lod.core.pos;

import com.seibel.lod.core.util.BitShiftUtil;
import com.seibel.lod.core.util.LodUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * A MC world position that is relative to a given detail level.
 * 
 * @author Leetom
 * @version 2022-11-6
 */
public class DhLodPos implements Comparable<DhLodPos>
{
	public final byte detailLevel;
	public final int x;
	public final int z;
	
	
	
	public DhLodPos(byte detailLevel, int x, int z)
	{
		this.detailLevel = detailLevel;
		this.x = x;
		this.z = z;
	}
	
	
	
    public DhLodUnit getX() { return new DhLodUnit(this.detailLevel, this.x); }
    public DhLodUnit getZ() { return new DhLodUnit(this.detailLevel, this.z); }
	
    public int getBlockWidth() { return this.getBlockWidth(this.detailLevel); }
	public int getBlockWidth(byte detailLevel)
	{
		LodUtil.assertTrue(detailLevel <= this.detailLevel);
		return BitShiftUtil.powerOfTwo(this.detailLevel - detailLevel);
	}
	
	public DhBlockPos2D getCenter() 
	{ 
		return new DhBlockPos2D(
				this.getX().toBlockWidth() + BitShiftUtil.half(this.getBlockWidth()),
				this.getZ().toBlockWidth() + BitShiftUtil.half(this.getBlockWidth())); 
	}
	public DhBlockPos2D getCorner() { return new DhBlockPos2D(this.getX().toBlockWidth(), this.getZ().toBlockWidth()); }
	
	public DhLodPos getCorner(byte newDetail)
	{
		LodUtil.assertTrue(newDetail <= this.detailLevel);
		return new DhLodPos(newDetail,
				this.x * BitShiftUtil.powerOfTwo(this.detailLevel - newDetail), 
				this.z * BitShiftUtil.powerOfTwo(this.detailLevel - newDetail));
	}
	
	public DhLodPos convertUpwardsTo(byte newDetail)
	{
		LodUtil.assertTrue(newDetail >= this.detailLevel);
		return new DhLodPos(newDetail, 
				Math.floorDiv(this.x, BitShiftUtil.powerOfTwo(newDetail - this.detailLevel)), 
				Math.floorDiv(this.z, BitShiftUtil.powerOfTwo(newDetail - this.detailLevel)));
	}
	
	/**
	 * Returns the DhLodPos 1 detail level lower <br><br>
	 * 
	 * Relative child positions returned for each index: <br>
	 * 0 = (0,0) <br>
	 * 1 = (1,0) <br>
	 * 2 = (0,1) <br>
	 * 3 = (1,1) <br>
	 * 
	 * @param child0to3 must be an int between 0 and 3
	 */
	public DhLodPos getChildByIndex(int child0to3) throws IllegalArgumentException, IllegalStateException
	{
		if (child0to3 < 0 || child0to3 > 3)
			throw new IllegalArgumentException("child0to3 must be between 0 and 3");
		if (this.detailLevel <= 0)
			throw new IllegalStateException("detailLevel must be greater than 0");
		
		return new DhLodPos((byte) (this.detailLevel - 1),
				this.x * 2 + (child0to3 & 1),
				this.z * 2 + BitShiftUtil.half(child0to3 & 2));
	}
	/** Returns this position's child index in its parent */
	public int getChildIndexOfParent() { return (this.x & 1) + BitShiftUtil.square(this.z & 1); }
	
	public boolean overlaps(DhLodPos other)
	{
		if (this.equals(other))
			return true;
		if (this.detailLevel == other.detailLevel)
			return false;
		
		if (this.detailLevel > other.detailLevel)
		{
			return this.equals(other.convertUpwardsTo(this.detailLevel));
		}
		else
		{
			return other.equals(this.convertUpwardsTo(other.detailLevel));
		}
	}
	
	/** Only valid for DhLodUnits for an equal or greater detail level */
	public DhLodPos addLodUnit(DhLodUnit width)
	{
		if (width.detailLevel < this.detailLevel)
			throw new IllegalArgumentException("add called with width.detailLevel < pos detail");
		
		return new DhLodPos(this.detailLevel, 
				x + width.createFromDetailLevel(this.detailLevel).numberOfLodSectionsWide, 
				z + width.createFromDetailLevel(this.detailLevel).numberOfLodSectionsWide);
	}
	
	/** Equivalent to adding a DhLodUnit with the same detail level as this DhLodPos */
	public DhLodPos addOffset(int xOffset, int zOffset) { return new DhLodPos(this.detailLevel, this.x + xOffset, this.z + zOffset); }
	
	
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		else if (obj == null || this.getClass() != obj.getClass())
		{
			return false;
		}
		else
		{
			DhLodPos otherPos = (DhLodPos) obj;
			return this.detailLevel == otherPos.detailLevel && this.x == otherPos.x && this.z == otherPos.z;	
		}
	}
	
    @Override
	public int hashCode() { return Objects.hash(detailLevel, x, z); }
	
    @Override
    public int compareTo(@NotNull DhLodPos obj) 
	{
		if (this.detailLevel != obj.detailLevel)
		{
			return Integer.compare(this.detailLevel, obj.detailLevel);
		}
		else if (this.x != obj.x)
		{
			return Integer.compare(this.x, obj.x);
		}
		else
		{
			return Integer.compare(this.z, obj.z);
		}
	}
	
	@Override
	public String toString() { return "[" + this.detailLevel + "*" + this.x + "," + this.z + "]"; }
	
}
