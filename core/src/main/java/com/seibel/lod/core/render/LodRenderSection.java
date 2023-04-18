package com.seibel.lod.core.render;

import com.seibel.lod.core.dataObjects.render.ColumnRenderSource;
import com.seibel.lod.core.dataObjects.render.bufferBuilding.ColumnRenderBuffer;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.file.renderfile.ILodRenderSourceProvider;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A render section represents an area that could be rendered.
 * For more information see {@link LodQuadTree}.
 */
public class LodRenderSection
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	
    public final DhSectionPos pos;
	/** a reference is used so the render buffer can be swapped to and from the buffer builder */
	public final AtomicReference<ColumnRenderBuffer> renderBufferRef = new AtomicReference<>();
	
	private boolean isRenderingEnabled = false;
	
	private ILodRenderSourceProvider renderSourceProvider = null;
    private CompletableFuture<ColumnRenderSource> renderSourceLoadFuture;
	private ColumnRenderSource renderSource;
	
	
	
    public LodRenderSection(DhSectionPos pos) { this.pos = pos; }
	
	
	
	//===========//
	// rendering //
	//===========//
	
	public void enableRendering() { this.isRenderingEnabled = true; }
	public void disableRendering() { this.isRenderingEnabled = false; }
	
	
	
	//=============//
	// render data //
	//=============//
	
	/** does nothing if a render source is already loaded or in the process of loading */
	public void loadRenderSource(ILodRenderSourceProvider renderDataProvider, IDhClientLevel level)
	{
		this.renderSourceProvider = renderDataProvider;
		if (this.renderSourceProvider == null)
		{
			return;
		}
		
		if (this.renderSource == null && this.renderSourceLoadFuture == null)
		{
			this.renderSourceLoadFuture = this.renderSourceProvider.readAsync(this.pos);
			this.renderSourceLoadFuture.whenComplete((renderSource, ex) ->
			{
				this.renderSource = renderSource;
				this.renderSourceLoadFuture = null;
				
				if (this.renderSource != null)
				{
					this.renderSource.allowRendering(level);
				}
			});
		}
	}
	
    public void reload(ILodRenderSourceProvider renderDataProvider)
	{
		// don't accidentally enable rendering for a disabled section
		if (!this.isRenderingEnabled)
		{
			return;
		}
		
		
		this.renderSourceProvider = renderDataProvider;
		
        if (this.renderSourceLoadFuture != null)
		{
			this.renderSourceLoadFuture.cancel(true);
			this.renderSourceLoadFuture = null;
        }
		
        if (this.renderSource != null)
		{
			this.renderSource.dispose();
			this.renderSource = null;
        }
		
		this.renderSourceLoadFuture = this.renderSourceProvider.readAsync(this.pos);
    }
	
	
    public void disposeRenderData()
	{
		if (this.renderSource != null)
		{
			this.renderSource.disableRender();
			this.renderSource.dispose();
			this.renderSource = null;
		}
		
		if (this.renderBufferRef.get() != null)
		{
			this.renderBufferRef.get().close();
			this.renderBufferRef.set(null);
		}
		
		if (this.renderSourceLoadFuture != null)
		{
			this.renderSourceLoadFuture.cancel(true);
			this.renderSourceLoadFuture = null;
		}
	}
	
	
	
	//========================//
	// getters and properties //
	//========================//
	
	/** @return true if this section is loaded and set to render */
    public boolean shouldRender() { return this.isRenderingEnabled && this.isRenderDataLoaded(); }
	/** This can return true before the render data is loaded */
    public boolean isRenderingEnabled() { return this.isRenderingEnabled; }
	
    public ColumnRenderSource getRenderSource() { return this.renderSource; }
	
	public boolean isRenderDataLoaded()
	{
		return this.renderSource != null
				&&
				(
					(
						// if true; either this section represents empty chunks or un-generated chunks. 
						// Either way, there isn't any data to render, but this should be considered "loaded"
						this.renderSource.isEmpty()
					)
					||
					(
						// check if the buffers have been loaded
						this.renderBufferRef.get() != null
						&& this.renderBufferRef.get().areBuffersUploaded()
					)
				);
	}
	
	
	
	//==============//
	// base methods //
	//==============//
	
    public String toString() {
        return "LodRenderSection{" +
                "pos=" + this.pos +
                ", lodRenderSource=" + this.renderSource +
                ", loadFuture=" + this.renderSourceLoadFuture +
                ", isRenderEnabled=" + this.isRenderingEnabled +
                '}';
    }
	
}
