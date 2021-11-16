/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.core.render;

import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.wrappers.block.BlockPosWrapper;
import com.seibel.lod.wrappers.chunk.ChunkPosWrapper;
import com.seibel.lod.wrappers.minecraft.MinecraftWrapper;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3f;

/**
 * This holds miscellaneous helper code
 * to be used in the rendering process.
 * 
 * @author James Seibel
 * @version 10-19-2021
 */
public class RenderUtil
{
	private static final MinecraftWrapper mc = MinecraftWrapper.INSTANCE;
	
	
	/**
	 * Returns if the given ChunkPos is in the loaded area of the world.
	 * @param center the center of the loaded world (probably the player's ChunkPos)
	 */
	public static boolean isChunkPosInLoadedArea(ChunkPosWrapper pos, ChunkPosWrapper center)
	{
		return (pos.getX() >= center.getX() - mc.getRenderDistance()
				&& pos.getX() <= center.getX() + mc.getRenderDistance())
				&&
				(pos.getZ() >= center.getZ() - mc.getRenderDistance()
						&& pos.getZ() <= center.getZ() + mc.getRenderDistance());
	}
	
	/**
	 * Returns if the given coordinate is in the loaded area of the world.
	 * @param centerCoordinate the center of the loaded world
	 */
	public static boolean isCoordinateInLoadedArea(int x, int z, int centerCoordinate)
	{
		return (x >= centerCoordinate - mc.getRenderDistance()
				&& x <= centerCoordinate + mc.getRenderDistance())
				&&
				(z >= centerCoordinate - mc.getRenderDistance()
						&& z <= centerCoordinate + mc.getRenderDistance());
	}
	
	
	/**
	 * Find the coordinates that are in the center half of the given
	 * 2D matrix, starting at (0,0) and going to (2 * lodRadius, 2 * lodRadius).
	 */
	public static boolean isCoordinateInNearFogArea(int i, int j, int lodRadius)
	{
		int halfRadius = lodRadius / 2;
		
		return (i >= lodRadius - halfRadius
				&& i <= lodRadius + halfRadius)
				&&
				(j >= lodRadius - halfRadius
						&& j <= lodRadius + halfRadius);
	}
	
	
	/**
	 * Returns true if one of the region's 4 corners is in front
	 * of the camera.
	 */
	public static boolean isRegionInViewFrustum(BlockPos playerBlockPos, Vector3f cameraDir, BlockPosWrapper vboCenterPos)
	{
		// convert the vbo position into a direction vector
		// starting from the player's position
		Vector3f vboVec = new Vector3f(vboCenterPos.getX(), 0, vboCenterPos.getZ());
		Vector3f playerVec = new Vector3f(playerBlockPos.getX(), playerBlockPos.getY(), playerBlockPos.getZ());
		
		vboVec.sub(playerVec);
		Vector3f vboCenterVec = vboVec;
		
		
		int halfRegionWidth = LodUtil.REGION_WIDTH / 2;
		
		// calculate the 4 corners
		Vector3f vboSeVec = new Vector3f(vboCenterVec.x() + halfRegionWidth, vboCenterVec.y(), vboCenterVec.z() + halfRegionWidth);
		Vector3f vboSwVec = new Vector3f(vboCenterVec.x() - halfRegionWidth, vboCenterVec.y(), vboCenterVec.z() + halfRegionWidth);
		Vector3f vboNwVec = new Vector3f(vboCenterVec.x() - halfRegionWidth, vboCenterVec.y(), vboCenterVec.z() - halfRegionWidth);
		Vector3f vboNeVec = new Vector3f(vboCenterVec.x() + halfRegionWidth, vboCenterVec.y(), vboCenterVec.z() - halfRegionWidth);
		
		// if any corner is visible, this region should be rendered
		return isNormalizedVectorInViewFrustum(vboSeVec, cameraDir) ||
				isNormalizedVectorInViewFrustum(vboSwVec, cameraDir) ||
				isNormalizedVectorInViewFrustum(vboNwVec, cameraDir) ||
				isNormalizedVectorInViewFrustum(vboNeVec, cameraDir);
	}
	
	/**
	 * Currently takes the dot product of the two vectors,
	 * but in the future could do more complicated frustum culling tests.
	 */
	private static boolean isNormalizedVectorInViewFrustum(Vector3f objectVector, Vector3f cameraDir)
	{
		// the -0.1 is to offer a slight buffer, so we are
		// more likely to render LODs and thus, hopefully prevent
		// flickering or odd disappearances
		return objectVector.dot(cameraDir) > -0.1;
	}
}
