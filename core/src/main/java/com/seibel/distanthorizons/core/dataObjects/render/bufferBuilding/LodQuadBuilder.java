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

package com.seibel.distanthorizons.core.dataObjects.render.bufferBuilding;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

import com.seibel.distanthorizons.core.enums.EDhDirection;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.pos.Pos2D;
import com.seibel.distanthorizons.core.render.AbstractRenderBuffer;
import com.seibel.distanthorizons.core.render.glObject.buffer.GLVertexBuffer;
import com.seibel.distanthorizons.core.util.ColorUtil;
import com.seibel.distanthorizons.api.enums.config.EGpuUploadMethod;
import com.seibel.distanthorizons.coreapi.util.MathUtil;
import org.apache.logging.log4j.Logger;

//TODO: Recheck this class for refactoring

/**
 * Used to create the quads before they are converted to render-able buffers. <br><br>
 *
 * Note: the magic number 6 you see throughout this method represents the number of sides on a cube.
 */
public class LodQuadBuilder
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	public final boolean skipQuadsWithZeroSkylight;
	public final short skyLightCullingBelow;
	
	@SuppressWarnings("unchecked")
	private final ArrayList<BufferQuad>[] opaqueQuads = (ArrayList<BufferQuad>[]) new ArrayList[6];
	@SuppressWarnings("unchecked")
	private final ArrayList<BufferQuad>[] transparentQuads = (ArrayList<BufferQuad>[]) new ArrayList[6];
	private final boolean doTransparency;
	
	/** Used to turn transparent LODs above the void opaque to prevent seeing the void. */
	private final HashMap<Pos2D, Short> minOpaqueHeightByRelativePos = new HashMap<>(64 * 64); // the 64*64 capacity was the smallest James saw the builder work with, so it should be a good starting point
	/** See {@link LodQuadBuilder#minOpaqueHeightByRelativePos} */
	private final HashMap<Pos2D, Short> minTransparentHeightByRelativePos = new HashMap<>(64 * 64);
	
	
	public static final int[][][] DIRECTION_VERTEX_IBO_QUAD = new int[][][]
			{
					// X,Z //
					{ // UP
							{1, 0}, // 0
							{1, 1}, // 1
							{0, 1}, // 2
							{0, 0}, // 3
					},
					{ // DOWN
							{0, 0}, // 0
							{0, 1}, // 1
							{1, 1}, // 2
							{1, 0}, // 3
					},
					
					// X,Y //
					{ // NORTH
							{0, 0}, // 0
							{0, 1}, // 1
							{1, 1}, // 2
							
							{1, 0}, // 3
					},
					{ // SOUTH
							{1, 0}, // 0
							{1, 1}, // 1
							{0, 1}, // 2
							
							{0, 0}, // 3
					},
					
					// Z,Y //
					{ // WEST
							{0, 0}, // 0
							{1, 0}, // 1
							{1, 1}, // 2
							
							{0, 1}, // 3
					},
					{ // EAST
							{0, 1}, // 0
							{1, 1}, // 1
							{1, 0}, // 2
							
							{0, 0}, // 3
					},
			};
	
	private int premergeCount = 0;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public LodQuadBuilder(boolean enableSkylightCulling, short skyLightCullingBelow, boolean doTransparency)
	{
		this.doTransparency = doTransparency;
		for (int i = 0; i < 6; i++)
		{
			this.opaqueQuads[i] = new ArrayList<>();
			this.transparentQuads[i] = new ArrayList<>();
		}
		
		this.skipQuadsWithZeroSkylight = enableSkylightCulling;
		this.skyLightCullingBelow = skyLightCullingBelow;
		
	}
	
	
	
	//===========//
	// add quads //
	//===========//
	
	public void addQuadAdj(
			EDhDirection dir, short x, short y, short z,
			short widthEastWest, short widthNorthSouthOrUpDown,
			int color, byte skyLight, byte blockLight)
	{
		if (dir == EDhDirection.DOWN)
		{
			throw new IllegalArgumentException("addQuadAdj() is only for adj direction! Not UP or Down!");
		}
		
		if (this.skipQuadsWithZeroSkylight && skyLight == 0 && y + widthNorthSouthOrUpDown < this.skyLightCullingBelow)
		{
			return;
		}
		
		BufferQuad quad = new BufferQuad(x, y, z, widthEastWest, widthNorthSouthOrUpDown, color, skyLight, blockLight, dir);
		ArrayList<BufferQuad> quadList = (this.doTransparency && ColorUtil.getAlpha(color) < 255) ? this.transparentQuads[dir.ordinal()] : this.opaqueQuads[dir.ordinal()];
		if (!quadList.isEmpty() &&
				(
						quadList.get(quadList.size() - 1).tryMerge(quad, BufferMergeDirectionEnum.EastWest)
								|| quadList.get(quadList.size() - 1).tryMerge(quad, BufferMergeDirectionEnum.NorthSouthOrUpDown))
		)
		{
			this.premergeCount++;
			return;
		}
		
		quadList.add(quad);
	}
	
	// XZ
	public void addQuadUp(short x, short y, short z, short widthEastWest, short widthNorthSouthOrUpDown, int color, byte skylight, byte blocklight) // TODO argument names are wrong
	{
		// cave culling
		if (this.skipQuadsWithZeroSkylight && skylight == 0 && y < this.skyLightCullingBelow)
		{
			return;
		}
		
		BufferQuad quad = new BufferQuad(x, y, z, widthEastWest, widthNorthSouthOrUpDown, color, skylight, blocklight, EDhDirection.UP);
		boolean isTransparent = (this.doTransparency && ColorUtil.getAlpha(color) < 255);
		ArrayList<BufferQuad> quadList = isTransparent ? this.transparentQuads[EDhDirection.UP.ordinal()] : this.opaqueQuads[EDhDirection.UP.ordinal()];
		
		
		// update the minimum relative height for this quad's positions
		for (int xRel = x; xRel < (x + widthEastWest); xRel++)
		{
			for (int zRel = z; zRel < (z + widthNorthSouthOrUpDown); zRel++)
			{
				Pos2D relPos = new Pos2D(xRel, zRel);
				
				HashMap<Pos2D, Short> minHeightByRelativePos = isTransparent ? this.minTransparentHeightByRelativePos : this.minOpaqueHeightByRelativePos;
				Short currentHeight = minHeightByRelativePos.get(relPos);
				// the default height is MAX_VALUE to preserve the comparison logic later
				currentHeight = (currentHeight == null) ? Short.MAX_VALUE : currentHeight;
				
				minHeightByRelativePos.put(relPos, (short) Math.min(currentHeight, quad.y));
			}
		}
		
		
		// attempt to merge this quad with adjacent ones
		if (!quadList.isEmpty() &&
				(
						quadList.get(quadList.size() - 1).tryMerge(quad, BufferMergeDirectionEnum.EastWest)
								|| quadList.get(quadList.size() - 1).tryMerge(quad, BufferMergeDirectionEnum.NorthSouthOrUpDown))
		)
		{
			this.premergeCount++;
			return;
		}
		
		quadList.add(quad);
	}
	
	public void addQuadDown(short x, short y, short z, short width, short wz, int color, byte skylight, byte blocklight)
	{
		if (skipQuadsWithZeroSkylight && skylight == 0 && y < skyLightCullingBelow)
			return;
		BufferQuad quad = new BufferQuad(x, y, z, width, wz, color, skylight, blocklight, EDhDirection.DOWN);
		ArrayList<BufferQuad> qs = (doTransparency && ColorUtil.getAlpha(color) < 255)
				? transparentQuads[EDhDirection.DOWN.ordinal()] : opaqueQuads[EDhDirection.DOWN.ordinal()];
		if (!qs.isEmpty() &&
				(qs.get(qs.size() - 1).tryMerge(quad, BufferMergeDirectionEnum.EastWest)
						|| qs.get(qs.size() - 1).tryMerge(quad, BufferMergeDirectionEnum.NorthSouthOrUpDown))
		)
		{
			premergeCount++;
			return;
		}
		qs.add(quad);
	}
	
	
	
	//==============//
	// add vertices //
	//==============//
	
	private static void putQuad(ByteBuffer bb, BufferQuad quad)
	{
		int[][] quadBase = DIRECTION_VERTEX_IBO_QUAD[quad.direction.ordinal()];
		short widthEastWest = quad.widthEastWest;
		short widthNorthSouth = quad.widthNorthSouthOrUpDown;
		EDhDirection.Axis axis = quad.direction.getAxis();
		for (int i = 0; i < quadBase.length; i++)
		{
			short dx, dy, dz;
			int mx, my, mz;
			switch (axis)
			{
				case X: // ZY
					dx = 0;
					dy = quadBase[i][1] == 1 ? widthNorthSouth : 0;
					dz = quadBase[i][0] == 1 ? widthEastWest : 0;
					mx = 0;
					my = quadBase[i][1] == 1 ? 1 : -1;
					mz = quadBase[i][0] == 1 ? 1 : -1;
					break;
				case Y: // XZ
					dx = quadBase[i][0] == 1 ? widthEastWest : 0;
					dy = 0;
					dz = quadBase[i][1] == 1 ? widthNorthSouth : 0;
					mx = quadBase[i][0] == 1 ? 1 : -1;
					my = 0;
					mz = quadBase[i][1] == 1 ? 1 : -1;
					break;
				case Z: // XY
					dx = quadBase[i][0] == 1 ? widthEastWest : 0;
					dy = quadBase[i][1] == 1 ? widthNorthSouth : 0;
					dz = 0;
					mx = quadBase[i][0] == 1 ? 1 : -1;
					my = quadBase[i][1] == 1 ? 1 : -1;
					mz = 0;
					break;
				default:
					throw new IllegalArgumentException("Invalid Axis enum: " + axis);
			}
			putVertex(bb, (short) (quad.x + dx), (short) (quad.y + dy), (short) (quad.z + dz),
					quad.hasError ? ColorUtil.RED : quad.color, // TODO add debug config that allows toggling this
					quad.hasError ? 15 : quad.skyLight,
					quad.hasError ? 15 : quad.blockLight,
					mx, my, mz);
		}
	}
	
	private static void putVertex(ByteBuffer bb, short x, short y, short z, int color, byte skylight, byte blocklight, int mx, int my, int mz)
	{
		skylight %= 16;
		blocklight %= 16;
		
		bb.putShort(x);
		bb.putShort(y);
		bb.putShort(z);
		
		short meta = 0;
		meta |= (skylight | (blocklight << 4));
		byte mirco = 0;
		// mirco offset which is a xyz 2bit value
		// 0b00 = no offset
		// 0b01 = positive offset
		// 0b11 = negative offset
		// format is: 0b00zzyyxx
		if (mx != 0) mirco |= mx > 0 ? 0b01 : 0b11;
		if (my != 0) mirco |= my > 0 ? 0b0100 : 0b1100;
		if (mz != 0) mirco |= mz > 0 ? 0b010000 : 0b110000;
		meta |= mirco << 8;
		
		bb.putShort(meta);
		byte r = (byte) ColorUtil.getRed(color);
		byte g = (byte) ColorUtil.getGreen(color);
		byte b = (byte) ColorUtil.getBlue(color);
		byte a = (byte) ColorUtil.getAlpha(color);
		bb.put(r);
		bb.put(g);
		bb.put(b);
		bb.put(a);
	}
	
	
	
	//=================//
	// data finalizing //
	//=================//
	
	/** runs any final data cleanup, merging, etc. */
	public void finalizeData()
	{
		this.mergeQuads();
		if (this.doTransparency)
		{
			this.fixTransparencyOverVoid(); // should happen after merging
		}
	}
	
	/** Uses Greedy meshing to merge this builder's Quads. */
	public void mergeQuads()
	{
		long mergeCount = 0;
		long preQuadsCount = this.getCurrentOpaqueQuadsCount() + this.getCurrentTransparentQuadsCount();
		if (preQuadsCount <= 1)
		{
			return;
		}
		
		for (int directionIndex = 0; directionIndex < 6; directionIndex++)
		{
			mergeCount += mergeQuadsInternal(this.opaqueQuads, directionIndex, BufferMergeDirectionEnum.EastWest);
			if (this.doTransparency)
			{
				mergeCount += mergeQuadsInternal(this.transparentQuads, directionIndex, BufferMergeDirectionEnum.EastWest);
			}
			
			
			// only run the second merge if the face is the top or bottom
			if (directionIndex == EDhDirection.UP.ordinal() || directionIndex == EDhDirection.DOWN.ordinal())
			{
				mergeCount += mergeQuadsInternal(this.opaqueQuads, directionIndex, BufferMergeDirectionEnum.NorthSouthOrUpDown);
				if (this.doTransparency)
				{
					mergeCount += mergeQuadsInternal(this.transparentQuads, directionIndex, BufferMergeDirectionEnum.NorthSouthOrUpDown);
				}
			}
		}
		
		long postQuadsCount = this.getCurrentOpaqueQuadsCount() + this.getCurrentTransparentQuadsCount();
		//if (mergeCount != 0)
		LOGGER.debug("Merged {}/{}({}) quads", mergeCount, preQuadsCount, mergeCount / (double) preQuadsCount);
	}
	
	/** Merges all of this builder's quads for the given directionIndex (up, down, left, etc.) in the given direction */
	private static long mergeQuadsInternal(ArrayList<BufferQuad>[] list, int directionIndex, BufferMergeDirectionEnum mergeDirection)
	{
		if (list[directionIndex].size() <= 1)
			return 0;
		
		list[directionIndex].sort((objOne, objTwo) -> objOne.compare(objTwo, mergeDirection));
		
		long mergeCount = 0;
		ListIterator<BufferQuad> iter = list[directionIndex].listIterator();
		BufferQuad currentQuad = iter.next();
		while (iter.hasNext())
		{
			BufferQuad nextQuad = iter.next();
			
			if (currentQuad.tryMerge(nextQuad, mergeDirection))
			{
				// merge successful, attempt to merge the next quad
				mergeCount++;
				iter.set(null);
			}
			else
			{
				// merge fail, move on to the next quad
				currentQuad = nextQuad;
			}
		}
		list[directionIndex].removeIf(Objects::isNull);
		return mergeCount;
	}
	
	
	/** makes any transparent LODs that are over the void opaque to prevent seeing the void through them. */
	public void fixTransparencyOverVoid()
	{
		// make transparent LODs opaque if they are over the void
		ListIterator<BufferQuad> iter = this.transparentQuads[EDhDirection.UP.ordinal()].listIterator();
		if (iter.hasNext())
		{
			BufferQuad currentQuad = iter.next();
			while (iter.hasNext())
			{
				Pos2D relPos = new Pos2D(currentQuad.x, currentQuad.z);
				
				Short minOpaqueHeight = this.minOpaqueHeightByRelativePos.get(relPos);
				minOpaqueHeight = (minOpaqueHeight == null) ? Short.MAX_VALUE : minOpaqueHeight;
				
				Short minTransHeight = this.minTransparentHeightByRelativePos.get(relPos);
				minTransHeight = (minTransHeight == null) ? Short.MAX_VALUE : minTransHeight;
				
				
				if (currentQuad.y < minOpaqueHeight && currentQuad.y == minTransHeight)
				{
					// transparent quad is at the bottom, make it opaque to prevent seeing through the world
					currentQuad.color = ColorUtil.setAlpha(currentQuad.color, 255);
					
					// move the now-opaque quad into the opaque list (if not done the quads may render on top of other transparent quads)
					iter.remove();
					this.opaqueQuads[EDhDirection.UP.ordinal()].add(currentQuad);
				}
				
				currentQuad = iter.next();
			}
		}
	}
	
	
	
	//==============//
	// buffer setup //
	//==============//
	
	public Iterator<ByteBuffer> makeOpaqueVertexBuffers()
	{
		return new Iterator<ByteBuffer>()
		{
			final ByteBuffer bb = ByteBuffer.allocateDirect(AbstractRenderBuffer.FULL_SIZED_BUFFER)
					.order(ByteOrder.nativeOrder());
			int dir = skipEmpty(0);
			int quad = 0;
			
			private int skipEmpty(int d)
			{
				while (d < 6 && opaqueQuads[d].isEmpty())
				{
					d++;
				}
				return d;
			}
			
			@Override
			public boolean hasNext()
			{
				return dir < 6;
			}
			
			@Override
			public ByteBuffer next()
			{
				if (dir >= 6)
				{
					return null;
				}
				bb.clear();
				bb.limit(AbstractRenderBuffer.FULL_SIZED_BUFFER);
				while (bb.hasRemaining() && dir < 6)
				{
					writeData();
				}
				bb.limit(bb.position());
				bb.rewind();
				return bb;
			}
			
			private void writeData()
			{
				int i = quad;
				for (; i < opaqueQuads[dir].size(); i++)
				{
					if (!bb.hasRemaining())
					{
						break;
					}
					putQuad(bb, opaqueQuads[dir].get(i));
				}
				
				if (i >= opaqueQuads[dir].size())
				{
					quad = 0;
					dir++;
					dir = skipEmpty(dir);
				}
				else
				{
					quad = i;
				}
			}
		};
	}
	
	public Iterator<ByteBuffer> makeTransparentVertexBuffers()
	{
		return new Iterator<ByteBuffer>()
		{
			final ByteBuffer bb = ByteBuffer.allocateDirect(AbstractRenderBuffer.FULL_SIZED_BUFFER)
					.order(ByteOrder.nativeOrder());
			int directionIndex = this.skipEmptyDirectionIndices(0);
			int quad = 0;
			
			private int skipEmptyDirectionIndices(int directionIndex)
			{
				while (directionIndex < 6 &&
						(LodQuadBuilder.this.transparentQuads[directionIndex] == null
								|| LodQuadBuilder.this.transparentQuads[directionIndex].isEmpty()))
				{
					directionIndex++;
				}
				
				return directionIndex;
			}
			
			@Override
			public boolean hasNext() { return this.directionIndex < 6; }
			
			@Override
			public ByteBuffer next()
			{
				if (this.directionIndex >= 6)
				{
					return null;
				}
				
				this.bb.clear();
				this.bb.limit(AbstractRenderBuffer.FULL_SIZED_BUFFER);
				while (this.bb.hasRemaining() && this.directionIndex < 6)
				{
					this.writeData();
				}
				this.bb.limit(this.bb.position());
				this.bb.rewind();
				return this.bb;
			}
			
			private void writeData()
			{
				int i = this.quad;
				for (; i < LodQuadBuilder.this.transparentQuads[this.directionIndex].size(); i++)
				{
					if (!this.bb.hasRemaining())
					{
						break;
					}
					putQuad(this.bb, LodQuadBuilder.this.transparentQuads[this.directionIndex].get(i));
				}
				
				if (i >= LodQuadBuilder.this.transparentQuads[this.directionIndex].size())
				{
					this.quad = 0;
					this.directionIndex++;
					this.directionIndex = this.skipEmptyDirectionIndices(this.directionIndex);
				}
				else
				{
					this.quad = i;
				}
			}
		};
	}
	public interface BufferFiller
	{
		/** If true: more data needs to be filled */
		boolean fill(GLVertexBuffer vbo);
		
	}
	
	public BufferFiller makeOpaqueBufferFiller(EGpuUploadMethod method)
	{
		return new BufferFiller()
		{
			int dir = 0;
			int quad = 0;
			
			public boolean fill(GLVertexBuffer vbo)
			{
				if (dir >= 6)
				{
					vbo.setVertexCount(0);
					return false;
				}
				
				int numOfQuads = _countRemainingQuads();
				if (numOfQuads > AbstractRenderBuffer.MAX_QUADS_PER_BUFFER)
					numOfQuads = AbstractRenderBuffer.MAX_QUADS_PER_BUFFER;
				if (numOfQuads == 0)
				{
					vbo.setVertexCount(0);
					return false;
				}
				ByteBuffer bb = vbo.mapBuffer(numOfQuads * AbstractRenderBuffer.QUADS_BYTE_SIZE, method,
						AbstractRenderBuffer.FULL_SIZED_BUFFER);
				if (bb == null)
					throw new NullPointerException("mapBuffer returned null");
				bb.clear();
				bb.limit(numOfQuads * AbstractRenderBuffer.QUADS_BYTE_SIZE);
				while (bb.hasRemaining() && dir < 6)
				{
					writeData(bb);
				}
				bb.rewind();
				vbo.unmapBuffer();
				vbo.setVertexCount(numOfQuads * 4);
				return dir < 6;
			}
			
			private int _countRemainingQuads()
			{
				int a = opaqueQuads[dir].size() - quad;
				for (int i = dir + 1; i < opaqueQuads.length; i++)
				{
					a += opaqueQuads[i].size();
				}
				return a;
			}
			
			private void writeData(ByteBuffer bb)
			{
				int startQ = quad;
				
				int i = startQ;
				for (i = startQ; i < opaqueQuads[dir].size(); i++)
				{
					if (!bb.hasRemaining())
					{
						break;
					}
					putQuad(bb, opaqueQuads[dir].get(i));
				}
				
				if (i >= opaqueQuads[dir].size())
				{
					quad = 0;
					dir++;
					while (dir < 6 && opaqueQuads[dir].isEmpty())
					{
						dir++;
					}
				}
				else
				{
					quad = i;
				}
			}
		};
	}
	
	public BufferFiller makeTransparentBufferFiller(EGpuUploadMethod method)
	{
		return new BufferFiller()
		{
			int dir = 0;
			int quad = 0;
			
			public boolean fill(GLVertexBuffer vbo)
			{
				if (dir >= 6)
				{
					vbo.setVertexCount(0);
					return false;
				}
				
				int numOfQuads = _countRemainingQuads();
				if (numOfQuads > AbstractRenderBuffer.MAX_QUADS_PER_BUFFER)
					numOfQuads = AbstractRenderBuffer.MAX_QUADS_PER_BUFFER;
				if (numOfQuads == 0)
				{
					vbo.setVertexCount(0);
					return false;
				}
				ByteBuffer bb = vbo.mapBuffer(numOfQuads * AbstractRenderBuffer.QUADS_BYTE_SIZE, method,
						AbstractRenderBuffer.FULL_SIZED_BUFFER);
				if (bb == null)
					throw new NullPointerException("mapBuffer returned null");
				bb.clear();
				bb.limit(numOfQuads * AbstractRenderBuffer.QUADS_BYTE_SIZE);
				while (bb.hasRemaining() && dir < 6)
				{
					writeData(bb);
				}
				bb.rewind();
				vbo.unmapBuffer();
				vbo.setVertexCount(numOfQuads * 4);
				return dir < 6;
			}
			
			private int _countRemainingQuads()
			{
				int a = transparentQuads[dir].size() - quad;
				for (int i = dir + 1; i < transparentQuads.length; i++)
				{
					a += transparentQuads[i].size();
				}
				return a;
			}
			
			private void writeData(ByteBuffer bb)
			{
				int startQ = quad;
				
				int i = startQ;
				for (i = startQ; i < transparentQuads[dir].size(); i++)
				{
					if (!bb.hasRemaining())
					{
						break;
					}
					putQuad(bb, transparentQuads[dir].get(i));
				}
				
				if (i >= transparentQuads[dir].size())
				{
					quad = 0;
					dir++;
					while (dir < 6 && transparentQuads[dir].isEmpty())
					{
						dir++;
					}
				}
				else
				{
					quad = i;
				}
			}
		};
	}
	
	
	
	//=========//
	// getters //
	//=========//
	
	public int getCurrentOpaqueQuadsCount()
	{
		int i = 0;
		for (ArrayList<BufferQuad> quadList : this.opaqueQuads)
		{
			i += quadList.size();
		}
		
		return i;
	}
	public int getCurrentTransparentQuadsCount()
	{
		if (!this.doTransparency)
		{
			return 0;
		}
		
		int i = 0;
		for (ArrayList<BufferQuad> quadList : this.transparentQuads)
		{
			i += quadList.size();
		}
		
		return i;
	}
	
	/** Returns how many GpuBuffers will be needed to render opaque quads in this builder. */
	public int getCurrentNeededOpaqueVertexBufferCount() { return MathUtil.ceilDiv(this.getCurrentOpaqueQuadsCount(), AbstractRenderBuffer.MAX_QUADS_PER_BUFFER); }
	/** Returns how many GpuBuffers will be needed to render transparent quads in this builder. */
	public int getCurrentNeededTransparentVertexBufferCount()
	{
		if (!this.doTransparency)
		{
			return 0;
		}
		
		return MathUtil.ceilDiv(this.getCurrentTransparentQuadsCount(), AbstractRenderBuffer.MAX_QUADS_PER_BUFFER);
	}
	
}
