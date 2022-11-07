package com.seibel.lod.core.pos;

import com.seibel.lod.core.enums.ELodDirection;
import com.seibel.lod.core.util.BitShiftUtil;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.MathUtil;

import java.util.function.Consumer;

/**
 * The position object used to define LOD objects in the quad trees.
 *
 * @author Leetom
 * @version 2022-11-6
 */
public class DhSectionPos
{
	public final byte sectionDetail;
	
	/** in sectionDetail level grid */
	public final int sectionX;
	/** in sectionDetail level grid */
	public final int sectionZ;
	
	
	
	public DhSectionPos(byte sectionDetail, int sectionX, int sectionZ)
	{
		this.sectionDetail = sectionDetail;
		this.sectionX = sectionX;
		this.sectionZ = sectionZ;
	}
	
	
	
	/** Returns the center for the highest detail level (0) */
	public DhLodPos getCenter() { return this.getCenter((byte) 0); }
	public DhLodPos getCenter(byte returnDetailLevel)
	{
		LodUtil.assertTrue(returnDetailLevel <= this.sectionDetail, "returnDetailLevel must be less than sectionDetail");
		
		if (returnDetailLevel == this.sectionDetail)
			return new DhLodPos(this.sectionDetail, this.sectionX, this.sectionZ);
		
		byte offset = (byte) (this.sectionDetail - returnDetailLevel);
		return new DhLodPos(returnDetailLevel,
				(this.sectionX * BitShiftUtil.powerOfTwo(offset)) + BitShiftUtil.powerOfTwo(offset - 1),
				(this.sectionZ * BitShiftUtil.powerOfTwo(offset)) + BitShiftUtil.powerOfTwo(offset - 1));
	}
	
	/** @return the corner with the smallest X and Z coordinate */
	public DhLodPos getCorner() { return this.getCorner((byte) (this.sectionDetail - 1)); }
	/** @return the corner with the smallest X and Z coordinate */
	public DhLodPos getCorner(byte returnDetailLevel)
	{
		LodUtil.assertTrue(returnDetailLevel <= this.sectionDetail, "returnDetailLevel must be less than sectionDetail");
		byte offset = (byte) (this.sectionDetail - returnDetailLevel);
		return new DhLodPos(returnDetailLevel,
				this.sectionX * BitShiftUtil.powerOfTwo(offset),
				this.sectionZ * BitShiftUtil.powerOfTwo(offset));
	}
	
	public DhLodUnit getWidth() { return this.getWidth(this.sectionDetail);  }
	public DhLodUnit getWidth(byte returnDetailLevel)
	{
		LodUtil.assertTrue(returnDetailLevel <= this.sectionDetail, "returnDetailLevel must be less than sectionDetail");
		byte offset = (byte) (this.sectionDetail - returnDetailLevel);
		return new DhLodUnit(this.sectionDetail, BitShiftUtil.powerOfTwo(offset));
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
	public DhSectionPos getChildByIndex(int child0to3) throws IllegalArgumentException, IllegalStateException
	{
		if (child0to3 < 0 || child0to3 > 3)
			throw new IllegalArgumentException("child0to3 must be between 0 and 3");
		if (this.sectionDetail <= 0)
			throw new IllegalStateException("section detail must be greater than 0");
		
		return new DhSectionPos((byte) (this.sectionDetail - 1),
				this.sectionX * 2 + (child0to3 & 1),
				this.sectionZ * 2 + BitShiftUtil.half(child0to3 & 2));
	}
	/** Returns this position's child index in its parent */
	public int getChildIndexOfParent() { return (this.sectionX & 1) + BitShiftUtil.square(this.sectionZ & 1); }
	
	/** Applies the given consumer to all 4 of this position's children. */
	public void forEachChild(Consumer<DhSectionPos> callback)
	{
		for (int i = 0; i < 4; i++)
		{
			callback.accept(this.getChildByIndex(i));
		}
	}
	
	public DhSectionPos getParentPos() { return new DhSectionPos((byte) (this.sectionDetail + 1), BitShiftUtil.half(this.sectionX), BitShiftUtil.half(this.sectionZ)); }
	
	public DhSectionPos getAdjacentPos(ELodDirection dir)
	{
		return new DhSectionPos(this.sectionDetail,
				this.sectionX + dir.getNormal().x,
				this.sectionZ + dir.getNormal().z);
	}
	
	public DhLodPos getSectionBBoxPos() { return new DhLodPos(this.sectionDetail, this.sectionX, this.sectionZ); }
	
	/** NOTE: This does not consider yOffset! */
	public boolean overlaps(DhSectionPos other) { return this.getSectionBBoxPos().overlaps(other.getSectionBBoxPos()); }
	
	/** Serialize() is different from toString() as it must NEVER be changed, and should be in a short format */
	public String serialize() { return "[" + this.sectionDetail + ',' + this.sectionX + ',' + this.sectionZ + ']'; }
	
	
	
	@Override
	public String toString() { return "{" + this.sectionDetail + "*" + this.sectionX + "," + this.sectionZ + "}"; }
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null || this.getClass() != obj.getClass())
			return false;
		
		DhSectionPos that = (DhSectionPos) obj;
		return this.sectionDetail == that.sectionDetail &&
				this.sectionX == that.sectionX &&
				this.sectionZ == that.sectionZ;
	}
	
	@Override
	public int hashCode()
	{
		return Integer.hashCode(this.sectionDetail) ^ // XOR
				Integer.hashCode(this.sectionX) ^ // XOR
				Integer.hashCode(this.sectionZ);
	}
	
}
