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

package com.seibel.lod.core.datatype.column.accessor;

import com.seibel.lod.core.logging.SpamReducedLogger;
import com.seibel.lod.core.util.ColorUtil;
import com.seibel.lod.core.util.LodUtil;

import java.util.Arrays;


/**
 * A helper class that is used to access the data in a RenderColumn
 * long datapoint.
 */
public class ColumnFormat
{
	/*
	
	|_  |g  |g  |g  |a  |a  |a  |a  |
	|r  |r  |r  |r  |r  |r  |r  |r  |
	|g  |g  |g  |g  |g  |g  |g  |g  |
	|b  |b  |b  |b  |b  |b  |b  |b  |
	
	|h  |h  |h  |h  |h  |h  |h  |h  |
	|h  |h  |h  |h  |d  |d  |d  |d  |
	|d  |d  |d  |d  |d  |d  |d  |d  |
	|bl |bl |bl |bl |sl |sl |sl |sl |
	
	*/
	
    // Reminder: bytes have range of [-128, 127].
    // When converting to or from an int a 128 should be added or removed.
    // If there is a bug with color then it's probably caused by this.
	
	private static final SpamReducedLogger warnLogger = new SpamReducedLogger(1);
	
	
	public final static int EMPTY_DATA = 0;
    public final static int MAX_WORLD_Y_SIZE = 4096;
	
    public final static int ALPHA_DOWNSIZE_SHIFT = 4;
	
	
    public final static int GEN_TYPE_SHIFT = 60;
	
    public final static int COLOR_SHIFT = 32;
    public final static int BLUE_SHIFT = COLOR_SHIFT;
    public final static int GREEN_SHIFT = BLUE_SHIFT + 8;
    public final static int RED_SHIFT = GREEN_SHIFT + 8;
    public final static int ALPHA_SHIFT = RED_SHIFT + 8;
	
    public final static int HEIGHT_SHIFT = 20;
    public final static int DEPTH_SHIFT = 8;
    public final static int BLOCK_LIGHT_SHIFT = 4;
    public final static int SKY_LIGHT_SHIFT = 0;
	
    public final static long ALPHA_MASK = 0xF;
    public final static long RED_MASK = 0xFF;
    public final static long GREEN_MASK = 0xFF;
    public final static long BLUE_MASK = 0xFF;
    public final static long COLOR_MASK = 0xFFFFFF;
    public final static long HEIGHT_MASK = 0xFFF;
    public final static long DEPTH_MASK = 0xFFF;
    public final static long HEIGHT_DEPTH_MASK = 0xFFFFFF;
    public final static long BLOCK_LIGHT_MASK = 0xF;
    public final static long SKY_LIGHT_MASK = 0xF;
    public final static long GEN_TYPE_MASK = 0b111;
    public final static long COMPARE_SHIFT = GEN_TYPE_SHIFT;
	
    public final static long HEIGHT_SHIFTED_MASK = HEIGHT_MASK << HEIGHT_SHIFT;
    public final static long DEPTH_SHIFTED_MASK = DEPTH_MASK << DEPTH_SHIFT;
    public final static long GEN_TYPE_SHIFTED_MASK = GEN_TYPE_MASK << GEN_TYPE_SHIFT;
	
    public final static long VOID_SETTER = HEIGHT_SHIFTED_MASK | DEPTH_SHIFTED_MASK;
	
	
	
	//========================//
	// datapoint manipulation //
	//========================//
	
	public static long createVoidDataPoint(byte generationMode)
	{
		if (generationMode == 0)
		{
			throw new IllegalArgumentException("Trying to create void datapoint with genMode 0, which is NOT allowed in DataPoint version 10!");
		}
		
		return (generationMode & GEN_TYPE_MASK) << GEN_TYPE_SHIFT;
	}

    public static long createDataPoint(int height, int depth, int color, int lightSky, int lightBlock, int generationMode)
	{
		return createDataPoint(
				ColorUtil.getAlpha(color),
				ColorUtil.getRed(color),
				ColorUtil.getGreen(color),
				ColorUtil.getBlue(color),
				height, depth, lightSky, lightBlock, generationMode);
	}
	
	public static long createDataPoint(int height, int depth, int color, int light, int generationMode)
	{
		LodUtil.assertTrue(light >= 0 && light < 256, "Raw Light value must be between 0 and 255!");
		
		return createDataPoint(
				ColorUtil.getAlpha(color),
				ColorUtil.getRed(color),
				ColorUtil.getGreen(color),
				ColorUtil.getBlue(color),
				height, depth, light % 16, light / 16, generationMode);
	}
	
	public static long createDataPoint(int alpha, int red, int green, int blue, int height, int depth, int lightSky, int lightBlock, int generationMode)
	{
		LodUtil.assertTrue(generationMode != 0, "Trying to create datapoint with genMode 0, which is NOT allowed in DataPoint version 10!");
		
		LodUtil.assertTrue(height >= 0 && height < MAX_WORLD_Y_SIZE, "Trying to create datapoint with height["+height+"] out of range!");
		LodUtil.assertTrue(depth >= 0 && depth < MAX_WORLD_Y_SIZE, "Trying to create datapoint with depth["+depth+"] out of range!");
		
		LodUtil.assertTrue(lightSky >= 0 && lightSky < 16, "Trying to create datapoint with lightSky["+lightSky+"] out of range!");
		LodUtil.assertTrue(lightBlock >= 0 && lightBlock < 16, "Trying to create datapoint with lightBlock["+lightBlock+"] out of range!");
		
		LodUtil.assertTrue(alpha >= 0 && alpha < 256, "Trying to create datapoint with alpha["+alpha+"] out of range!");
		LodUtil.assertTrue(red >= 0 && red < 256, "Trying to create datapoint with red["+red+"] out of range!");
		LodUtil.assertTrue(green >= 0 && green < 256, "Trying to create datapoint with green["+green+"] out of range!");
		LodUtil.assertTrue(blue >= 0 && blue < 256, "Trying to create datapoint with blue["+blue+"] out of range!");
		
		LodUtil.assertTrue(generationMode >= 0 && generationMode < 8, "Trying to create datapoint with genMode["+generationMode+"] out of range!");
		LodUtil.assertTrue(depth <= height, "Trying to create datapoint with depth["+depth+"] greater than height["+height+"]!");
		
		return (long) (alpha >>> ALPHA_DOWNSIZE_SHIFT) << ALPHA_SHIFT
				| (red & RED_MASK) << RED_SHIFT
				| (green & GREEN_MASK) << GREEN_SHIFT
				| (blue & BLUE_MASK) << BLUE_SHIFT
				| (height & HEIGHT_MASK) << HEIGHT_SHIFT
				| (depth & DEPTH_MASK) << DEPTH_SHIFT
				| (lightBlock & BLOCK_LIGHT_MASK) << BLOCK_LIGHT_SHIFT
				| (lightSky & SKY_LIGHT_MASK) << SKY_LIGHT_SHIFT
				| (generationMode & GEN_TYPE_MASK) << GEN_TYPE_SHIFT
				;
	}

    public static long shiftHeightAndDepth(long dataPoint, short offset)
	{
		long height = (dataPoint + ((long) offset << HEIGHT_SHIFT)) & HEIGHT_SHIFTED_MASK;
		long depth = (dataPoint + (offset << DEPTH_SHIFT)) & DEPTH_SHIFTED_MASK;
		
		return dataPoint & ~(HEIGHT_SHIFTED_MASK | DEPTH_SHIFTED_MASK) | height | depth;
	}
	
	public static long version9Reorder(long dataPoint)
	{
		/*
		|a  |a  |a  |a  |r  |r  |r  |r   |
		|r  |r  |r  |r  |g  |g  |g  |g  |
		|g  |g  |g  |g  |b  |b  |b  |b  |
		|b  |b  |b  |b  |h  |h  |h  |h  |
		|h  |h  |h  |h  |h  |h  |d  |d  |
		|d  |d  |d  |d  |d  |d  |d  |d  |
		|bl |bl |bl |bl |sl |sl |sl |sl |
		|l  |l  |f  |g  |g  |g  |v  |e  |
		*/
		
		if ((dataPoint & 1) == 0)
		{
			return 0;
		}
		
		long height = (dataPoint >>> 26) & 0x3FF;
		long depth = (dataPoint >>> 16) & 0x3FF;
		
		if (height == depth || (dataPoint & 0b10) == 0b10)
		{
			return createVoidDataPoint((byte) (((dataPoint >>> 2) & 0b111) + 1));
		}
		
		return ((dataPoint >>> 60) & 0xF) << ALPHA_SHIFT
				| ((dataPoint >>> 52) & 0xFF) << RED_SHIFT
				| ((dataPoint >>> 44) & 0xFF) << GREEN_SHIFT
				| ((dataPoint >>> 36) & 0xFF) << BLUE_SHIFT
				| ((dataPoint >>> 26) & 0x3FF) << HEIGHT_SHIFT
				| ((dataPoint >>> 16) & 0x3FF) << DEPTH_SHIFT
				| ((dataPoint >>> 8) & 0xFF) << SKY_LIGHT_SHIFT
				| (((dataPoint >>> 2) & 0xFF) + 1) << GEN_TYPE_SHIFT;
	}
	
    public static short getHeight(long dataPoint) { return (short) ((dataPoint >>> HEIGHT_SHIFT) & HEIGHT_MASK); }
    public static short getDepth(long dataPoint) { return (short) ((dataPoint >>> DEPTH_SHIFT) & DEPTH_MASK); }
	
    public static short getAlpha(long dataPoint) { return (short) ((((dataPoint >>> ALPHA_SHIFT) & ALPHA_MASK) << ALPHA_DOWNSIZE_SHIFT) | 0b1111); }
    public static short getRed(long dataPoint) { return (short) ((dataPoint >>> RED_SHIFT) & RED_MASK); }
    public static short getGreen(long dataPoint) { return (short) ((dataPoint >>> GREEN_SHIFT) & GREEN_MASK); }
    public static short getBlue(long dataPoint) { return (short) ((dataPoint >>> BLUE_SHIFT) & BLUE_MASK); }
	
    public static byte getLightSky(long dataPoint) { return (byte) ((dataPoint >>> SKY_LIGHT_SHIFT) & SKY_LIGHT_MASK); }
    public static byte getLightBlock(long dataPoint) { return (byte) ((dataPoint >>> BLOCK_LIGHT_SHIFT) & BLOCK_LIGHT_MASK); }
	
	public static byte getGenerationMode(long dataPoint)
	{
		byte genMode = (byte) ((dataPoint >>> GEN_TYPE_SHIFT) & GEN_TYPE_MASK);
		if (warnLogger.canMaybeLog() && doesDataPointExist(dataPoint) && genMode == 0)
		{
			warnLogger.warnInc("Existing datapoint with genMode 0 detected! This is invalid in DataPoint version 10!"
					+ " This may be caused by old data that has not been updated correctly.");
			return 1;
		}
		return (genMode == 0) ? 1 : genMode;
	}
	
    public static long overrideGenerationMode(long current, byte b) { return (current & ~GEN_TYPE_SHIFTED_MASK) | ((b & GEN_TYPE_MASK) << GEN_TYPE_SHIFT); }
	
    public static boolean isVoid(long dataPoint) { return (((dataPoint >>> DEPTH_SHIFT) & HEIGHT_DEPTH_MASK) == 0); }
	
    public static boolean doesDataPointExist(long dataPoint) { return dataPoint != 0; }
	
    public static int getColor(long dataPoint)
	{
        long alpha = getAlpha(dataPoint);
        return (int) (((dataPoint >>> COLOR_SHIFT) & COLOR_MASK) | (alpha << (ALPHA_SHIFT - COLOR_SHIFT)));
    }
	
    private static void shrinkArray(short[] array, int packetSize, int start, int length, int arraySize)
	{
        start *= packetSize;
        length *= packetSize;
        arraySize *= packetSize;
        //remove comment to not leave garbage at the end
        //array[start + packetSize + i] = 0;
        if (arraySize - start >= 0) System.arraycopy(array, start + length, array, start, arraySize - start);
    }
	
	private static void extendArray(short[] array, int packetSize, int start, int length, int arraySize)
	{
		start *= packetSize;
		length *= packetSize;
		arraySize *= packetSize;
		for (int i = arraySize - start - 1; i >= 0; i--)
		{
			array[start + length + i] = array[start + i];
			array[start + i] = 0;
		}
	}
	
    /** Return (>0) if dataA should replace dataB, (0) if equal, (<0) if dataB should replace dataA */
    public static int compareDatapointPriority(long dataA, long dataB) { return (int) ((dataA >> COMPARE_SHIFT) - (dataB >> COMPARE_SHIFT)); }
	
	/** This is used to convert a dataPoint to string (useful for the print function) */
	@SuppressWarnings("unused")
	public static String toString(long dataPoint)
	{
		if (!doesDataPointExist(dataPoint))
		{
			return "null";
		}
		else if (isVoid(dataPoint))
		{
			return "void";
		}
		else
		{
			return "H:" + getHeight(dataPoint) +
					" D:" + getDepth(dataPoint) +
					" argb:" + getAlpha(dataPoint) + " " +
					getRed(dataPoint) + " " +
					getBlue(dataPoint) + " " +
					getGreen(dataPoint) +
					" BL/SL:" + getLightBlock(dataPoint) + " " +
					getLightSky(dataPoint) +
					" G:" + getGenerationMode(dataPoint);
		}
	}
	
	
	
	//=================//
	// ColumnArrayView //
	//=================//
	// TODO this should probably be moved
	
	// TODO what is the purpose of these?
	private static final ThreadLocal<int[]> tLocalIndices = new ThreadLocal<>();
	private static final ThreadLocal<boolean[]> tLocalIncreaseIndex = new ThreadLocal<>();
	private static final ThreadLocal<boolean[]> tLocalIndexHandled = new ThreadLocal<>();
	private static final ThreadLocal<short[]> tLocalHeightAndDepth = new ThreadLocal<>();
	private static final ThreadLocal<int[]> tDataIndexCache = new ThreadLocal<>();
	
	
	/**
     * This method merge column of multiple data together
     *
     * @param sourceData one or more columns of data
     * @param output     one column of space for the result to be written to
     */
	public static void mergeMultiData(IColumnDataView sourceData, ColumnArrayView output)
	{
		if (output.dataCount() != 1)
		{
			throw new IllegalArgumentException("output must be only reserved for one datapoint!");
		}
		
		int inputVerticalSize = sourceData.verticalSize();
		int outputVerticalSize = output.verticalSize();
		output.fill(0);
		
		//dataCount indicate how many position we are merging in one position
		int dataCount = sourceData.dataCount();
		
		// We initialize the arrays that are going to be used
		int heightAndDepthLength = (MAX_WORLD_Y_SIZE / 2 + 16) * 2;
		short[] heightAndDepth = tLocalHeightAndDepth.get();
		if (heightAndDepth == null || heightAndDepth.length != heightAndDepthLength)
		{
			heightAndDepth = new short[heightAndDepthLength];
			tLocalHeightAndDepth.set(heightAndDepth);
		}
		
		byte genMode = getGenerationMode(sourceData.get(0));
		if (genMode == 0)
		{
			genMode = 1; // FIXME: Hack to make the version 10 genMode never be 0.
		}
		
		boolean allEmpty = true;
		boolean allVoid = true;
		boolean limited = false;
		boolean allDefault;
		long singleData;
		
		short depth;
		short height;
		int count = 0;
		int i;
		int ii;
		
		int[] indices = tLocalIndices.get();
		if (indices == null || indices.length != dataCount)
		{
			indices = new int[dataCount];
			tLocalIndices.set(indices);
		}
		Arrays.fill(indices, 0);
		
		boolean[] increaseIndex = tLocalIncreaseIndex.get();
		if (increaseIndex == null || increaseIndex.length != dataCount)
		{
			increaseIndex = new boolean[dataCount];
			tLocalIncreaseIndex.set(increaseIndex);
		}
		
		boolean[] indexHandled = tLocalIndexHandled.get();
		if (indexHandled == null || indexHandled.length != dataCount)
		{
			indexHandled = new boolean[dataCount];
			tLocalIndexHandled.set(indexHandled);
		}
		
		long tempData;
		for (int index = 0; index < dataCount; index++)
		{
			tempData = sourceData.get(index * inputVerticalSize);
			allVoid = allVoid && ColumnFormat.isVoid(tempData);
			allEmpty = allEmpty && !ColumnFormat.doesDataPointExist(tempData);
		}
		
		//We check if there is any data that's not empty or void
		if (allEmpty)
		{
			return;
		}
		else if (allVoid)
		{
			output.set(0, createVoidDataPoint(genMode));
			return;
		}
		
		//this check is used only to see if we have checked all the values in the array
		boolean stillHasDataToCheck = true;
		short prevDepth;
		
		while (stillHasDataToCheck)
		{
			Arrays.fill(indexHandled, false);
			boolean connected = true;
			int newHeight = -10000;
			int newDepth = -10000;
			int tempHeight;
			int tempDepth;
			while (connected)
			{
				Arrays.fill(increaseIndex, false);
				for (int index = 0; index < dataCount; index++)
				{
					if (indices[index] < inputVerticalSize)
					{
						tempData = sourceData.get(index * inputVerticalSize + indices[index]);
						if (!ColumnFormat.isVoid(tempData) && ColumnFormat.doesDataPointExist(tempData))
						{
							tempHeight = ColumnFormat.getHeight(tempData);
							tempDepth = ColumnFormat.getDepth(tempData);
							if (tempDepth >= newHeight)
							{
								//First case
								//the column we are checking is higher than the current column
								newDepth = tempDepth;
								newHeight = tempHeight;
								Arrays.fill(increaseIndex, false);
								Arrays.fill(indexHandled, false);
								increaseIndex[index] = true;
								indexHandled[index] = true;
							}
							else if ((tempDepth >= newDepth) && (tempHeight <= newHeight))
							{
								//the column we are checking is contained in the current column
								//we simply increase this index
								increaseIndex[index] = true;
								indexHandled[index] = true;
							}
							else if (tempHeight > newHeight && tempDepth <= newDepth)
							{
								newDepth = tempDepth;
								newHeight = tempHeight;
								increaseIndex[index] = true;
								indexHandled[index] = true;
							}
							else if (tempHeight > newDepth && tempHeight <= newHeight)
							{
								//the column we are checking touches the current column from the bottom
								//for this reason we extend what's below
							
								//We want to avoid to expend this column if it has already been expanded by
								//this index
								if (!indexHandled[index])
								{
									newDepth = tempDepth;
									increaseIndex[index] = true;
									indexHandled[index] = true;
								}
							
							}
							else if (tempDepth < newHeight && tempDepth > newDepth)
							{
								//the column we are checking touches the current column from the top
								//for this reason we extend the top
								newHeight = tempHeight;
								increaseIndex[index] = true;
							}
						}
						else
						{
							indexHandled[index] = true;
						}
					}
				}
				
				//if we added any new data there is a chance that we could add more
				//for this reason we would continue
				//if no data is added than the column hasn't changed.
				//for this reason we can start working on a new column
				connected = false;
				for (int index = 0; index < dataCount; index++)
				{
					if (increaseIndex[index])
					{
						connected = true;
						indices[index]++;
					}
				}
			}
		
			//Now we add the height and depth data we extracted to the heightAndDepth array
			if (newDepth != newHeight)
			{
				if (count != 0)
				{
					prevDepth = heightAndDepth[(count - 1) * 2 + 1];
					if (newHeight > prevDepth)
					{
						newHeight = (short) Math.min(newHeight, prevDepth);
					}
				}
				heightAndDepth[count * 2] = (short) newHeight;
				heightAndDepth[count * 2 + 1] = (short) newDepth;
				count++;
			}
		
			//Here we check the condition that makes the loop continue
			//We stop the loop only if there is no more data to check
			stillHasDataToCheck = false;
			for (int index = 0; index < dataCount; index++)
			{
				if (indices[index] < inputVerticalSize)
				{
					tempData = sourceData.get(index * inputVerticalSize + indices[index]);
					stillHasDataToCheck |= !ColumnFormat.isVoid(tempData) && ColumnFormat.doesDataPointExist(tempData);
				}
			}
		}
	
		//we limit the vertical portion to maxVerticalData
		int j = 0;
		while (count > outputVerticalSize)
		{
			limited = true;
			ii = MAX_WORLD_Y_SIZE;
			for (i = 0; i < count - 1; i++)
			{
				if (heightAndDepth[i * 2 + 1] - heightAndDepth[(i + 1) * 2] <= ii)
				{
					ii = heightAndDepth[i * 2 + 1] - heightAndDepth[(i + 1) * 2];
					j = i;
				}
			}
			
			heightAndDepth[j * 2 + 1] = heightAndDepth[(j + 1) * 2 + 1];
			for (i = j + 1; i < count - 1; i++)
			{
				heightAndDepth[i * 2] = heightAndDepth[(i + 1) * 2];
				heightAndDepth[i * 2 + 1] = heightAndDepth[(i + 1) * 2 + 1];
			}
			
			//System.arraycopy(heightAndDepth, j + 1, heightAndDepth, j, count - j - 1);
			count--;
		}
		//As standard the vertical lods are ordered from top to bottom
	
		if (!limited && dataCount == 1) // This mean source vertSize < output vertSize AND both dataCount == 1
		{
			sourceData.copyTo(output.data, output.offset, output.vertSize);
		}
		else
		{
		
			//We want to efficiently memorize indexes
			int[] dataIndexesCache = tDataIndexCache.get();
			if (dataIndexesCache == null || dataIndexesCache.length != dataCount)
			{
				dataIndexesCache = new int[dataCount];
				tDataIndexCache.set(dataIndexesCache);
			}
			Arrays.fill(dataIndexesCache, 0);
			
			
			//For each lod height-depth value we have found we now want to generate the rest of the data
			//by merging all lods at lower level that are contained inside the new ones
			for (j = 0; j < count; j++)
			{
				//We firstly collect height and depth data
				//this will be added to each realtive long DataPoint
				height = heightAndDepth[j * 2];
				depth = heightAndDepth[j * 2 + 1];
			
				//if both height and depth are at 0 then we finished
				if ((depth == 0 && height == 0) || j >= heightAndDepth.length / 2)
				{
					break;
				}
			
				//We initialize data useful for the merge
				int numberOfChildren = 0;
				allEmpty = true;
				allVoid = true;
			
				//We initialize all the new values that we are going to put in the dataPoint
				int tempAlpha = 0;
				int tempRed = 0;
				int tempGreen = 0;
				int tempBlue = 0;
				int tempLightBlock = 0;
				int tempLightSky = 0;
				long data = 0;
			
				//For each position that we want to merge
				for (int index = 0; index < dataCount; index++)
				{
					//we scan the lods in the position from top to bottom
					while (dataIndexesCache[index] < inputVerticalSize)
					{
						singleData = sourceData.get(index * inputVerticalSize + dataIndexesCache[index]);
						if (doesDataPointExist(singleData) && !isVoid(singleData))
						{
							dataIndexesCache[index]++;
							if ((depth <= getDepth(singleData) && getDepth(singleData) < height)
									|| (depth < getHeight(singleData) && getHeight(singleData) <= height))
							{
								data = singleData;
								break;
							}
						}
						else
						{
							break;
						}
					}
					
					if (!doesDataPointExist(data))
					{
						data = createVoidDataPoint(genMode);
					}
				
					if (doesDataPointExist(data))
					{
						allEmpty = false;
						if (!isVoid(data))
						{
							numberOfChildren++;
							allVoid = false;
							tempAlpha = Math.max(getAlpha(data), tempAlpha);
							tempRed += getRed(data) * getRed(data);
							tempGreen += getGreen(data) * getGreen(data);
							tempBlue += getBlue(data) * getBlue(data);
							tempLightBlock += getLightBlock(data);
							tempLightSky += getLightSky(data);
						}
					}
				}
				
				//we have at least 1 child
				if (dataCount != 1)
				{
					tempRed = tempRed / numberOfChildren;
					tempGreen = tempGreen / numberOfChildren;
					tempBlue = tempBlue / numberOfChildren;
					tempLightBlock = tempLightBlock / numberOfChildren;
					tempLightSky = tempLightSky / numberOfChildren;
				}
				
				//data = createDataPoint(tempAlpha, tempRed, tempGreen, tempBlue, height, depth, tempLightSky, tempLightBlock, tempGenMode, allDefault);
				//if (j > 0 && getColor(data) == getColor(dataPoint[j]))
				//{
				//	add simplification at the end due to color
				//}
				
				output.set(j, createDataPoint(tempAlpha, (int) Math.sqrt(tempRed), (int) Math.sqrt(tempGreen), (int) Math.sqrt(tempBlue), height, depth, tempLightSky, tempLightBlock, genMode));
			
			}
		}
	}
	
}