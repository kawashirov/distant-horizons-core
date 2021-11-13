package com.seibel.lod.objects.opengl;

import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

/**
 * A (almost) exact copy of Minecraft's
 * VertexFormat class, several methods
 * were commented out since we didn't need them.
 * 
 * @author James Seibel
 * @version 11-13-2021
 */
public class LodVertexFormat
{
	private final ImmutableList<LodVertexFormatElement> elements;
	private final IntList offsets = new IntArrayList();
	private final int vertexSize;
	
	public LodVertexFormat(ImmutableList<LodVertexFormatElement> elementList)
	{
		this.elements = elementList;
		int i = 0;
		
		for (LodVertexFormatElement LodVertexFormatElement : elementList)
		{
			this.offsets.add(i);
			i += LodVertexFormatElement.getByteSize();
		}
		
		this.vertexSize = i;
	}
	
	public int getIntegerSize()
	{
		return this.getVertexSize() / 4;
	}
	
	public int getVertexSize()
	{
		return this.vertexSize;
	}
	
	public ImmutableList<LodVertexFormatElement> getElements()
	{
		return this.elements;
	}
	

	// Forge added method
	public int getOffset(int index)
	{
		return offsets.getInt(index);
	}
		


	@Override
	public String toString()
	{
		return "format: " + this.elements.size() + " elements: " + this.elements.stream().map(Object::toString).collect(Collectors.joining(" "));
	}
	
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		else if (obj != null && this.getClass() == obj.getClass())
		{
			LodVertexFormat vertexformat = (LodVertexFormat) obj;
			return this.vertexSize != vertexformat.vertexSize ? false : this.elements.equals(vertexformat.elements);
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public int hashCode()
	{
		return this.elements.hashCode();
	}
	
	
	
	
	
	

	/* not currently needed setupBufferState()
	public void setupBufferState(long p_227892_1_)
	{
		if (!RenderSystem.isOnRenderThread())
		{
			RenderSystem.recordRenderCall(() ->
			{
				this.setupBufferState(p_227892_1_);
			});
		}
		else
		{
			int i = this.getVertexSize();
			List<LodVertexFormatElement> list = this.getElements();
			
			for (int j = 0; j < list.size(); ++j)
			{
				list.get(j).setupBufferState(p_227892_1_ + this.offsets.getInt(j), i);
			}
			
		}
	}
	*/
	
	/* not currently needed clearBufferState()
	public void clearBufferState()
	{
		if (!RenderSystem.isOnRenderThread())
		{
			RenderSystem.recordRenderCall(this::clearBufferState);
		}
		else
		{
			for (LodVertexFormatElement LodVertexFormatElement : this.getElements())
			{
				LodVertexFormatElement.clearBufferState();
			}
			
		}
	}
	*/
	
	
	/* not currently needed has Position/Normal/Color/UV
	public boolean hasPosition()
	{
		return elements.stream().anyMatch(e -> e.getUsage() == LodVertexFormatElement.Usage.POSITION);
	}
	
	public boolean hasNormal()
	{
		return elements.stream().anyMatch(e -> e.getUsage() == LodVertexFormatElement.Usage.NORMAL);
	}
	
	public boolean hasColor()
	{
		return elements.stream().anyMatch(e -> e.getUsage() == LodVertexFormatElement.Usage.COLOR);
	}
	
	public boolean hasUV(int which)
	{
		return elements.stream().anyMatch(e -> e.getUsage() == LodVertexFormatElement.Usage.UV && e.getIndex() == which);
	}
	*/
	
}