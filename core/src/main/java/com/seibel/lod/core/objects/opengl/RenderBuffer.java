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
 
package com.seibel.lod.core.objects.opengl;

import java.util.ConcurrentModificationException;

import com.seibel.lod.core.builders.lodBuilding.bufferBuilding.LodQuadBuilder;
import com.seibel.lod.core.enums.config.EGpuUploadMethod;
import com.seibel.lod.core.render.LodRenderProgram;
import com.seibel.lod.core.util.StatsMap;

public abstract class RenderBuffer implements AutoCloseable
{
	
	private enum State {
		None,
		Building,
		Uploading,
		Closed,
	}
	private State owner = State.None;
	private State nextOwner = State.None;
	final private void _lockThread(State newOwner) {
		if (owner != State.None || (nextOwner != State.None && nextOwner != newOwner))
			throw new ConcurrentModificationException("RenderMethod Illegal state!");
		owner = newOwner;
		nextOwner = State.None;
	}
	final private void _unlockThread(State oldOwner) {
		if (owner != oldOwner)
			throw new ConcurrentModificationException("RenderMethod Illegal state!");
		owner = State.None;
	}
	final private void _unlockThreadTo(State oldOwner, State newOwner) {
		if (owner != oldOwner)
			throw new ConcurrentModificationException("RenderMethod Illegal state!");
		owner = State.None;
		nextOwner = newOwner;
	}
	
	final public void build(Runnable r) {
		_lockThread(State.Building);
		try {
			r.run();
		} finally {
			_unlockThread(State.Building);
		}
	}

	/* Return false if current renderMethod is not suited for current builder
	 * This will auto close the object if returning false. */
	final public boolean tryUploadBuffers(LodQuadBuilder builder, EGpuUploadMethod uploadMethod) {
		_lockThread(State.Uploading);
		boolean successful = false;
		try {
			successful = uploadBuffers(builder, uploadMethod);
			return successful;
		} finally {
			if (!successful) {
				_unlockThreadTo(State.Uploading, State.Closed);
				close();
			} else { 
				_unlockThread(State.Uploading);
			}
		}
	}

	// ======================================================================
	// ====================== Methods for implementations ===================
	// ======================================================================
	
	// =========== Called by build starter thread ==========
	
	/* Called on being reused after the object is swapped to the back
	 *  and a new build event is triggered. Used for cleaning up non
	 *  reusable objects sooner.
	 * Note: This is run on BUILDER thread, and does not have access to
	 *  GL Context, Use GLProxy.recordOpenGlCall() to access GL Context
	 *  instead! */
	public void onReuse() {}
	
	// =========== Called by buffer upload thread ==========
	
	/* Return false if current renderMethod is not suited for current builder
	 *  If false, close call will be automatically triggered.
	 *  If true, the object will be used (by first calling the swapBufferToFront())
	 *  on tick render. */
	protected abstract boolean uploadBuffers(LodQuadBuilder builder, EGpuUploadMethod uploadMethod);
	
	// ========== Called by render thread ==========
	
	/* Called on buffer first being used by a render thread. */
	public void onSwapToFront() {}
	
	/* Called on buffer no longer being used. (Life ended)
	 *  Return false if current object cannot be reused.
	 * Note: This should not do too much stuff as it is run on render thread!
	 *  The corresponding cleanups should be done using the onReuse() to prevent
	 *  lag spikes! If you want this buffer to not be reused, but cleanup is
	 *  expensive, use onReuse() instead!
	 * Note 2: This may not be triggered on some situations like renderer being
	 *  terminated, or dimension changed. So implementation should NEVER assume
	 *  that onSwapToFront() will link to a call of onSwapToBack()! */
	public boolean onSwapToBack() {return true;}
	
	/* Called on... well... rendering.
	 * Return false if nothing rendered. (Optional) */
	public abstract boolean render(LodRenderProgram shaderProgram);

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
	
	
}
