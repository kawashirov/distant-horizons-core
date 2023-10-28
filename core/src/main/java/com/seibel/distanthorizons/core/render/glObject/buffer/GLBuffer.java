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

package com.seibel.distanthorizons.core.render.glObject.buffer;

import com.seibel.distanthorizons.api.enums.config.EGpuUploadMethod;
import com.seibel.distanthorizons.core.enums.EGLProxyContext;
import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import com.seibel.distanthorizons.core.render.glObject.GLProxy;
import com.seibel.distanthorizons.core.util.LodUtil;
import com.seibel.distanthorizons.core.util.ThreadUtil;
import com.seibel.distanthorizons.core.util.math.UnitBytes;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL32;
import org.lwjgl.opengl.GL44;

import java.lang.invoke.MethodHandles;
import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public class GLBuffer implements AutoCloseable
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());
	
	public static final double BUFFER_EXPANSION_MULTIPLIER = 1.3;
	public static final double BUFFER_SHRINK_TRIGGER = BUFFER_EXPANSION_MULTIPLIER * BUFFER_EXPANSION_MULTIPLIER;
	/** the number of active buffers, can be used for debugging */
	public static AtomicInteger bufferCount = new AtomicInteger(0);
	
	private static final int PHANTOM_REF_CHECK_TIME_IN_MS = 5 * 1000;
	private static final HashMap<Reference<? extends GLBuffer>, Integer> PHANTOM_TO_BUFFER_ID = new HashMap<>();
	private static final HashMap<Integer, Reference<? extends GLBuffer>> BUFFER_ID_TO_PHANTOM = new HashMap<>();
	private static final ReferenceQueue<GLBuffer> PHANTOM_REFERENCE_QUEUE = new ReferenceQueue<>();
	/** TODO we should make a global cleanup thread that handles all phantom references */
	private static final ThreadPoolExecutor CLEANUP_THREAD = ThreadUtil.makeSingleThreadPool("GLBuffer Cleanup");
	
	
	protected int id;
	public final int getId() { return this.id; }
	protected int size = 0;
	public int getSize() { return this.size; }
	protected boolean bufferStorage;
	public final boolean isBufferStorage() { return this.bufferStorage; }
	protected boolean isMapped = false;
	
	
	
	//==============//
	// constructors //
	//==============//
	
	static
	{
		CLEANUP_THREAD.execute(() -> runPhantomReferenceCleanupLoop());
	}
	
	public GLBuffer(boolean isBufferStorage)
	{
		this.create(isBufferStorage);
	}
	
	
	
	//=========//
	// methods //
	//=========//
	
	// Should be override by subclasses
	public int getBufferBindingTarget() { return GL32.GL_COPY_READ_BUFFER; }
	
	public void bind() { GL32.glBindBuffer(this.getBufferBindingTarget(), this.id); }
	public void unbind() { GL32.glBindBuffer(this.getBufferBindingTarget(), 0); }
	
	
	
	//====================//
	// create and destroy //
	//====================//
	
	protected void create(boolean asBufferStorage)
	{
		LodUtil.assertTrue(GLProxy.getInstance().getGlContext() != EGLProxyContext.NONE,
				"Thread ["+Thread.currentThread()+"] tried to create a GLBuffer outside a OpenGL context.");
		
		this.id = GL32.glGenBuffers();
		this.bufferStorage = asBufferStorage;
		bufferCount.getAndIncrement();
		
		PhantomReference<GLBuffer> phantom = new PhantomReference<>(this, PHANTOM_REFERENCE_QUEUE);
		PHANTOM_TO_BUFFER_ID.put(phantom, this.id);
		BUFFER_ID_TO_PHANTOM.put(this.id, phantom);
		
	}
	
	protected void destroy(boolean async)
	{
		if (this.id == 0)
		{
			// the buffer has already been closed
			return;
		}
		
		destroyBufferId(async, this.id);
		
		this.id = 0;
		this.size = 0;
	}
	private static void destroyBufferId(boolean async, int id)
	{
		if (async && GLProxy.getInstance().getGlContext() != EGLProxyContext.PROXY_WORKER)
		{
			GLProxy.getInstance().recordOpenGlCall(() -> destroyBufferId(false, id));
		}
		else
		{
			// remove the phantom reference
			if (BUFFER_ID_TO_PHANTOM.containsKey(id))
			{
				Reference<? extends GLBuffer> phantom = BUFFER_ID_TO_PHANTOM.get(id);
				PHANTOM_TO_BUFFER_ID.remove(phantom);
			}
			
			// destroy the buffer if it exists
			if (GL32.glIsBuffer(id))
			{
				GL32.glDeleteBuffers(id);
				bufferCount.decrementAndGet();
			}
		}
	}
	
	
	
	//==================//
	// buffer uploading //
	//==================//
	
	/** Assumes the GL Context is already bound */
	public void uploadBuffer(ByteBuffer bb, EGpuUploadMethod uploadMethod, int maxExpansionSize, int bufferHint)
	{
		LodUtil.assertTrue(!uploadMethod.useEarlyMapping, "UploadMethod signal that this should use Mapping instead of uploadBuffer!");
		int bbSize = bb.limit() - bb.position();
		LodUtil.assertTrue(bbSize <= maxExpansionSize, "maxExpansionSize is {} but buffer size is {}!", maxExpansionSize, bbSize);
		GLProxy.GL_LOGGER.debug("Uploading buffer with {}.", new UnitBytes(bbSize));
		
		// If size is zero, just ignore it.
		if (bbSize == 0)
		{
			return;
		}
		
		boolean useBuffStorage = uploadMethod.useBufferStorage;
		if (useBuffStorage != this.bufferStorage)
		{
			this.destroy(false);
			this.create(useBuffStorage);
			this.bind();
		}
		
		switch (uploadMethod)
		{
			case AUTO:
				LodUtil.assertNotReach("GpuUploadMethod AUTO must be resolved before call to uploadBuffer()!");
			case BUFFER_STORAGE:
				this.uploadBufferStorage(bb, bufferHint);
				break;
			case DATA:
				this.uploadBufferData(bb, bufferHint);
				break;
			case SUB_DATA:
				this.uploadSubData(bb, maxExpansionSize, bufferHint);
				break;
			default:
				LodUtil.assertNotReach("Unknown GpuUploadMethod!");
		}
	}
	/** Requires the buffer to be bound */
	protected void uploadBufferStorage(ByteBuffer bb, int bufferStorageHint)
	{
		LodUtil.assertTrue(this.bufferStorage, "Buffer is not bufferStorage but its trying to use bufferStorage upload method!");
		int bbSize = bb.limit() - bb.position();
		this.destroy(false);
		this.create(true);
		this.bind();
		GL44.glBufferStorage(this.getBufferBindingTarget(), bb, bufferStorageHint);
		this.size = bbSize;
	}
	/** Requires the buffer to be bound */
	protected void uploadBufferData(ByteBuffer bb, int bufferDataHint)
	{
		LodUtil.assertTrue(!this.bufferStorage, "Buffer is bufferStorage but its trying to use bufferData upload method!");
		int bbSize = bb.limit() - bb.position();
		GL32.glBufferData(this.getBufferBindingTarget(), bb, bufferDataHint);
		this.size = bbSize;
	}
	/** Requires the buffer to be bound */
	protected void uploadSubData(ByteBuffer bb, int maxExpansionSize, int bufferDataHint)
	{
		LodUtil.assertTrue(!this.bufferStorage, "Buffer is bufferStorage but its trying to use subData upload method!");
		int bbSize = bb.limit() - bb.position();
		if (this.size < bbSize || this.size > bbSize * BUFFER_SHRINK_TRIGGER)
		{
			int newSize = (int) (bbSize * BUFFER_EXPANSION_MULTIPLIER);
			if (newSize > maxExpansionSize) newSize = maxExpansionSize;
			GL32.glBufferData(this.getBufferBindingTarget(), newSize, bufferDataHint);
			this.size = newSize;
		}
		GL32.glBufferSubData(this.getBufferBindingTarget(), 0, bb);
	}
	
	
	
	//================//
	// buffer mapping //
	//================//
	
	public ByteBuffer mapBuffer(int targetSize, EGpuUploadMethod uploadMethod, int maxExpensionSize, int bufferHint, int mapFlags)
	{
		LodUtil.assertTrue(targetSize != 0, "MapBuffer targetSize is 0");
		LodUtil.assertTrue(uploadMethod.useEarlyMapping, "Upload method must be one that use early mappings in order to call mapBuffer");
		LodUtil.assertTrue(!this.isMapped, "Buffer is already mapped");
		
		boolean useBuffStorage = uploadMethod.useBufferStorage;
		if (useBuffStorage != this.bufferStorage)
		{
			this.destroy(false);
			this.create(useBuffStorage);
		}
		this.bind();
		ByteBuffer vboBuffer;
		
		if (this.size < targetSize || this.size > targetSize * BUFFER_SHRINK_TRIGGER)
		{
			int newSize = (int) (targetSize * BUFFER_EXPANSION_MULTIPLIER);
			if (newSize > maxExpensionSize) newSize = maxExpensionSize;
			this.size = newSize;
			if (this.bufferStorage)
			{
				GL32.glDeleteBuffers(this.id);
				this.id = GL32.glGenBuffers();
				GL32.glBindBuffer(this.getBufferBindingTarget(), this.id);
				GL32.glBindBuffer(this.getBufferBindingTarget(), this.id);
				GL44.glBufferStorage(this.getBufferBindingTarget(), newSize, bufferHint);
			}
			else
			{
				GL32.glBufferData(GL32.GL_ARRAY_BUFFER, newSize, bufferHint);
			}
		}
		
		vboBuffer = GL32.glMapBufferRange(GL32.GL_ARRAY_BUFFER, 0, targetSize, mapFlags);
		this.isMapped = true;
		return vboBuffer;
	}
	
	// Requires already binded
	public void unmapBuffer()
	{
		LodUtil.assertTrue(this.isMapped, "Buffer is not mapped");
		this.bind();
		GL32.glUnmapBuffer(this.getBufferBindingTarget());
		this.isMapped = false;
	}
	
	
	
	//===========//
	// overrides //
	//===========//
	
	@Override
	public void close() { this.destroy(true); }
	
	@Override
	public String toString()
	{
		return (this.bufferStorage ? "" : "Static-") + getClass().getSimpleName() +
				"[id:" + this.id + ",size:" + this.size + (this.isMapped ? ",MAPPED" : "") + "]";
	}
	
	
	
	//================//
	// static cleanup //
	//================//
	
	private static void runPhantomReferenceCleanupLoop()
	{
		while (true)
		{
			try
			{
				try
				{
					Thread.sleep(PHANTOM_REF_CHECK_TIME_IN_MS);
				}
				catch (InterruptedException ignore) { }
				
				
				Reference<? extends GLBuffer> phantomRef = PHANTOM_REFERENCE_QUEUE.poll();
				while (phantomRef != null)
				{
					// destroy the buffer if it hasn't been cleared yet
					if (PHANTOM_TO_BUFFER_ID.containsKey(phantomRef))
					{
						// remove the phantom reference
						int id = PHANTOM_TO_BUFFER_ID.get(phantomRef);
						PHANTOM_TO_BUFFER_ID.remove(phantomRef);
						BUFFER_ID_TO_PHANTOM.remove(id);
						
						destroyBufferId(true, id);
					}
					
					phantomRef = PHANTOM_REFERENCE_QUEUE.poll();
				}
			}
			catch (Exception e)
			{
				LOGGER.error("Unexpected error in cleanup thread: " + e.getMessage(), e);
			}
		}
	}
	
}
