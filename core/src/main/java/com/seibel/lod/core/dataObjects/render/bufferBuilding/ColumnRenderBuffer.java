package com.seibel.lod.core.dataObjects.render.bufferBuilding;

import com.seibel.lod.core.pos.DhBlockPos2D;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.render.renderer.DebugRenderer;
import com.seibel.lod.core.render.renderer.IDebugRenderable;
import com.seibel.lod.core.render.renderer.LodRenderer;
import com.seibel.lod.core.render.AbstractRenderBuffer;
import com.seibel.lod.core.config.Config;
import com.seibel.lod.api.enums.config.EGpuUploadMethod;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhBlockPos;
import com.seibel.lod.core.render.glObject.GLProxy;
import com.seibel.lod.core.render.glObject.buffer.GLVertexBuffer;
import com.seibel.lod.core.util.*;
import com.seibel.lod.core.util.objects.StatsMap;
import org.apache.logging.log4j.Logger;

import java.awt.*;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.concurrent.*;

import static com.seibel.lod.core.render.glObject.GLProxy.GL_LOGGER;

/**
 * Java representation of one or more OpenGL buffers for rendering.
 * 
 * @see ColumnRenderBufferBuilder
 */
public class ColumnRenderBuffer extends AbstractRenderBuffer implements IDebugRenderable
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	private static final long MAX_BUFFER_UPLOAD_TIMEOUT_NANOSECONDS = 1_000_000;
	
	
	public final DhBlockPos pos;
	
	public boolean buffersUploaded = false;
	
	private GLVertexBuffer[] vbos;
    private GLVertexBuffer[] vbosTransparent;
	private boolean closed = false;

	private final DhSectionPos debugPos;
	
	
	//==============//
	// constructors //
	//==============//
	
	public ColumnRenderBuffer(DhBlockPos pos, DhSectionPos debugPos)
	{
		this.pos = pos;
		this.debugPos = debugPos;
		vbos = new GLVertexBuffer[0];
		vbosTransparent = new GLVertexBuffer[0];
		DebugRenderer.register(this);
	}

	public void debugRender(DebugRenderer r)
	{
		if (closed || vbos == null) {
			return;
		}
		Color c = Color.green;
		r.renderBox(debugPos, 128, 128, 0.05f, c);
	}
	
	
	
	
	
	//==================//
	// buffer uploading //
	//==================//
	
	public void uploadBuffer(LodQuadBuilder builder, EGpuUploadMethod method) throws InterruptedException
	{
		if (method.useEarlyMapping)
		{
			this.uploadBuffersMapped(builder, method);
		}
		else
		{
			this.uploadBuffersDirect(builder, method);
		}
		
		this.buffersUploaded = true;
	}
	
	private void uploadBuffersMapped(LodQuadBuilder builder, EGpuUploadMethod method)
	{
		// opaque vbos //
		
		this.vbos = ColumnRenderBufferBuilder.resizeBuffer(this.vbos, builder.getCurrentNeededOpaqueVertexBufferCount());
		for (int i = 0; i < this.vbos.length; i++)
		{
			if (this.vbos[i] == null)
			{
				this.vbos[i] = new GLVertexBuffer(method.useBufferStorage);
			}
		}
		LodQuadBuilder.BufferFiller func = builder.makeOpaqueBufferFiller(method);
		for (GLVertexBuffer vbo : this.vbos)
		{
			func.fill(vbo);
		}
		
		
		// transparent vbos //
		
		this.vbosTransparent = ColumnRenderBufferBuilder.resizeBuffer(this.vbosTransparent, builder.getCurrentNeededTransparentVertexBufferCount());
		for (int i = 0; i < this.vbosTransparent.length; i++)
		{
			if (this.vbosTransparent[i] == null)
			{
				this.vbosTransparent[i] = new GLVertexBuffer(method.useBufferStorage);
			}
		}
		LodQuadBuilder.BufferFiller transparentFillerFunc = builder.makeTransparentBufferFiller(method);
		for (GLVertexBuffer vbo : this.vbosTransparent)
		{
			transparentFillerFunc.fill(vbo);
		}
	}
	
	private void uploadBuffersDirect(LodQuadBuilder builder, EGpuUploadMethod method) throws InterruptedException
	{
		this.vbos = ColumnRenderBufferBuilder.resizeBuffer(this.vbos, builder.getCurrentNeededOpaqueVertexBufferCount());
		uploadBuffersDirect(this.vbos, builder.makeOpaqueVertexBuffers(), method);
		
		this.vbosTransparent = ColumnRenderBufferBuilder.resizeBuffer(this.vbosTransparent, builder.getCurrentNeededTransparentVertexBufferCount());
		uploadBuffersDirect(this.vbosTransparent, builder.makeTransparentVertexBuffers(), method);
	}
	private static void uploadBuffersDirect(GLVertexBuffer[] vbos, Iterator<ByteBuffer> iter, EGpuUploadMethod method) throws InterruptedException
	{
		long remainingNS = 0;
		long BPerNS = Config.Client.Advanced.GpuBuffers.gpuUploadPerMegabyteInMilliseconds.get();
		int vboIndex = 0;
		while (iter.hasNext())
		{
			if (vboIndex >= vbos.length)
			{
				throw new RuntimeException("Too many vertex buffers!!");
			}
			
			ByteBuffer bb = iter.next();
			GLVertexBuffer vbo = ColumnRenderBufferBuilder.getOrMakeBuffer(vbos, vboIndex++, method.useBufferStorage);
			int size = bb.limit() - bb.position();
			
			try
			{
				vbo.bind();
				vbo.uploadBuffer(bb, size / LodUtil.LOD_VERTEX_FORMAT.getByteSize(), method, FULL_SIZED_BUFFER);
			}
			catch (Exception e)
			{
				vbos[vboIndex - 1] = null;
				vbo.close();
				LOGGER.error("Failed to upload buffer: ", e);
			}
			
			if (BPerNS <= 0)
			{
				continue;
			}
			
			// upload buffers over an extended period of time
			// to hopefully prevent stuttering.
			remainingNS += size * BPerNS;
			if (remainingNS >= TimeUnit.NANOSECONDS.convert(1000 / 60, TimeUnit.MILLISECONDS))
			{
				if (remainingNS > MAX_BUFFER_UPLOAD_TIMEOUT_NANOSECONDS)
				{
					remainingNS = MAX_BUFFER_UPLOAD_TIMEOUT_NANOSECONDS;
				}
				
				Thread.sleep(remainingNS / 1000000, (int) (remainingNS % 1000000));
				remainingNS = 0;
			}
		}
		
		if (vboIndex < vbos.length)
		{
			throw new RuntimeException("Too few vertex buffers!!");
		}
	}
	
	
	
	
	
	//========//
	// render //
	//========//
	
    @Override
	public boolean renderOpaque(LodRenderer renderContext)
	{
		boolean hasRendered = false;
		renderContext.setupOffset(this.pos);
		for (GLVertexBuffer vbo : this.vbos)
		{
			if (vbo == null)
			{
				continue;
			}
			
			if (vbo.getVertexCount() == 0)
			{
				continue;
			}
			
			hasRendered = true;
			renderContext.drawVbo(vbo);
			//LodRenderer.tickLogger.info("Vertex buffer: {}", vbo);
		}
		return hasRendered;
	}
	
    @Override
	public boolean renderTransparent(LodRenderer renderContext)
	{
		boolean hasRendered = false;
		
		renderContext.setupOffset(this.pos);
		for (GLVertexBuffer vbo : this.vbosTransparent)
		{
			if (vbo == null)
			{
				continue;
			}
			
			if (vbo.getVertexCount() == 0)
			{
				continue;
			}
			
			hasRendered = true;
			renderContext.drawVbo(vbo);
			//LodRenderer.tickLogger.info("Vertex buffer: {}", vbo);
		}
		
		return hasRendered;
	}
	
	
	
	//==============//
	// misc methods //
	//==============//
	
    @Override
    public void debugDumpStats(StatsMap statsMap)
	{
		statsMap.incStat("RenderBuffers");
		statsMap.incStat("SimpleRenderBuffers");
		for (GLVertexBuffer vertexBuffer : vbos)
		{
			if (vertexBuffer != null)
			{
				statsMap.incStat("VBOs");
				if (vertexBuffer.getSize() == FULL_SIZED_BUFFER)
				{
					statsMap.incStat("FullsizedVBOs");
				}
				
				if (vertexBuffer.getSize() == 0)
				{
					GL_LOGGER.warn("VBO with size 0");
				}
				statsMap.incBytesStat("TotalUsage", vertexBuffer.getSize());
			}
		}
	}
	
    @Override
    public void close()
	{
        if (this.closed)
		{
			return;
		}
		this.closed = true;
		this.buffersUploaded = false;
		
        GLProxy.getInstance().recordOpenGlCall(() ->
		{
			for (GLVertexBuffer buffer : this.vbos)
			{
				if (buffer != null)
				{
					buffer.destroy(false);
				}
			}
			
			for (GLVertexBuffer buffer : this.vbosTransparent)
			{
				if (buffer != null)
				{
					buffer.destroy(false);
				}
			}
        });
    }
}
