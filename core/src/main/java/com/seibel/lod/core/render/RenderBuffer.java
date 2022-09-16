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
 
package com.seibel.lod.core.render;

import com.seibel.lod.core.render.renderer.LodRenderer;
import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.objects.StatsMap;

public abstract class RenderBuffer implements AutoCloseable
{
	// ======================================================================
	// ====================== Methods for implementations ===================
	// ======================================================================

	// ========== Called by render thread ==========
	/* Called on... well... rendering.
	 * Return false if nothing rendered. (Optional) */
	public abstract boolean renderOpaque(LodRenderer renderContext);
	public abstract boolean renderTransparent(LodRenderer renderContext);

	// ========== Called by any thread. (thread safe) ==========
	
	/* Called by anyone. This method is allowed to throw exceptions, but
	 * are never allowed to modify any values. This should behave the same
	 * to other methods as if the method have never been called.
	 * Note: This method is PURELY for debug or stats logging ONLY! */
	public abstract void debugDumpStats(StatsMap statsMap);
	
	// ========= Called only when 1 thread is using it =======
	/* This method is called when object is no longer in use.
	 * Called either after uploadBuffers() returned false (On buffer Upload
	 * thread), or by others when the object is not being used. (not in build,
	 * upload, or render state). */
	public abstract void close();



	public static final int DEFAULT_MEMORY_ALLOCATION = (LodUtil.LOD_VERTEX_FORMAT.getByteSize() * 3) * 8;
	public static final int QUADS_BYTE_SIZE = LodUtil.LOD_VERTEX_FORMAT.getByteSize() * 4;
	public static final int MAX_QUADS_PER_BUFFER = (1024 * 1024 * 1) / QUADS_BYTE_SIZE;
	public static final int FULL_SIZED_BUFFER = MAX_QUADS_PER_BUFFER * QUADS_BYTE_SIZE;


}
