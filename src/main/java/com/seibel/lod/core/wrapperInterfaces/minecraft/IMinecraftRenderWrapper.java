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

package com.seibel.lod.core.wrapperInterfaces.minecraft;

import java.awt.Color;
import java.util.HashSet;

import com.seibel.lod.core.handlers.dependencyInjection.IBindable;
import com.seibel.lod.core.handlers.dependencyInjection.ModAccessorHandler;
import com.seibel.lod.core.handlers.dependencyInjection.SingletonHandler;
import com.seibel.lod.core.objects.DHBlockPos;
import com.seibel.lod.core.objects.DHChunkPos;
import com.seibel.lod.core.objects.math.Mat4f;
import com.seibel.lod.core.objects.math.Vec3d;
import com.seibel.lod.core.objects.math.Vec3f;
import com.seibel.lod.core.wrapperInterfaces.IVersionConstants;
import com.seibel.lod.core.wrapperInterfaces.IWrapperFactory;
import com.seibel.lod.core.wrapperInterfaces.misc.ILightMapWrapper;
import com.seibel.lod.core.wrapperInterfaces.modAccessor.ISodiumAccessor;
import com.seibel.lod.core.wrapperInterfaces.world.IWorldWrapper;

/**
 * Contains everything related to
 * rendering in Minecraft.
 * 
 * @author James Seibel
 * @version 3-5-2022
 */
public interface IMinecraftRenderWrapper extends IBindable
{
	Vec3f getLookAtVector();
	
	DHBlockPos getCameraBlockPosition();
	
	boolean playerHasBlindnessEffect();
	
	Vec3d getCameraExactPosition();
	
	Mat4f getDefaultProjectionMatrix(float partialTicks);
	
	double getGamma();
	
	Color getFogColor(float partialTicks);
	
	default Color getSpecialFogColor(float partialTicks) {return getFogColor(partialTicks);}

	boolean isFogStateSpecial();
	
	Color getSkyColor();
	
	double getFov(float partialTicks);
	
	/** Measured in chunks */
	int getRenderDistance();
	
	int getScreenWidth();
	int getScreenHeight();

	int getTargetFrameBuffer();
	int getTargetFrameBufferViewportWidth();
	int getTargetFrameBufferViewportHeight();
	
	/**
	 * This method returns the ChunkPos of all chunks that Minecraft
	 * is going to render this frame.
	 * <br>
	 * If not implemented this calls {@link #getMaximumRenderedChunks()}.
	 */
	default HashSet<DHChunkPos> getVanillaRenderedChunks()
	{
		ISodiumAccessor sodium = ModAccessorHandler.get(ISodiumAccessor.class);
		return sodium==null ? getMaximumRenderedChunks() : sodium.getNormalRenderedChunks();
	}

	static boolean correctedCheckRadius(int dx, int dz, int radius2Mul4) {
		dx = dx*2;// + (dx < 0 ? -1 : 1);
		dz = dz*2;// + (dz < 0 ? -1 : 1);
		return (dx*dx + dz*dz <= radius2Mul4);
	}

	/**
	 * <strong>Doesn't need to be implemented.</strong> <br>
	 * Returns every chunk position within the vanilla render distance.
	 */
	default HashSet<DHChunkPos> getMaximumRenderedChunks()
	{
		IMinecraftClientWrapper mcWrapper = SingletonHandler.get(IMinecraftClientWrapper.class);
		IWrapperFactory factory = SingletonHandler.get(IWrapperFactory.class);
		IVersionConstants versionConstants = SingletonHandler.get(IVersionConstants.class);
		IMinecraftClientWrapper minecraft = SingletonHandler.get(IMinecraftClientWrapper.class);
		IWorldWrapper clientWorld = minecraft.getWrappedClientWorld();

		int chunkDist = this.getRenderDistance() + 1; // For some reason having '+1' is actually closer to real value
		
		DHChunkPos centerChunkPos = mcWrapper.getPlayerChunkPos();
		int centerChunkX = centerChunkPos.getX();
		int centerChunkZ = centerChunkPos.getZ();
		int chunkDist2Mul4 = chunkDist*chunkDist*4;
		
		// add every position within render distance
		HashSet<DHChunkPos> renderedPos = new HashSet<DHChunkPos>();
		for (int deltaChunkX = -chunkDist; deltaChunkX <= chunkDist; deltaChunkX++)
		{
			for(int deltaChunkZ = -chunkDist; deltaChunkZ <= chunkDist; deltaChunkZ++)
			{
				if (!versionConstants.isVanillaRenderedChunkSquare() &&
						!correctedCheckRadius(deltaChunkX,deltaChunkZ,chunkDist2Mul4)) {
					continue;
				}
				if (!clientWorld.hasChunkLoaded(centerChunkX + deltaChunkX, centerChunkZ + deltaChunkZ)) continue;
				renderedPos.add(new DHChunkPos(centerChunkX + deltaChunkX, centerChunkZ + deltaChunkZ));
			}
		}
		return renderedPos;	
	}

	ILightMapWrapper getLightmapWrapper();
	
	// Try and disable vanilla fog. Return true if successful, or false if not able to.
	boolean tryDisableVanillaFog();

	
}
