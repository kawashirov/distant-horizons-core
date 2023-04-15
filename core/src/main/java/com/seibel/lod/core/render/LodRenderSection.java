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

public class LodRenderSection
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
    public final DhSectionPos pos;
	
    private CompletableFuture<ColumnRenderSource> loadFuture;
    private boolean isRenderEnabled = false;
    
	private ColumnRenderSource renderSource;
	private ILodRenderSourceProvider renderSourceProvider = null;
	
	public final AtomicReference<ColumnRenderBuffer> renderBufferRef = new AtomicReference<>();
	
	
	
    public LodRenderSection(DhSectionPos pos) { this.pos = pos; }
	
	
	
	//===========//
	// rendering //
	//===========//
	
    public void loadRenderSource(ILodRenderSourceProvider renderDataProvider)
	{
		this.renderSourceProvider = renderDataProvider;
		if (this.renderSourceProvider == null)
		{
			return;
		}
		
		if (this.renderSource == null && this.loadFuture == null)
		{
			this.loadFuture = this.renderSourceProvider.read(this.pos);
			this.loadFuture.whenComplete((renderSource, ex) -> 
			{
				this.renderSource = renderSource;
				this.loadFuture = null;
			});
		}
    }
	public void enableRendering(IDhClientLevel level)
	{
		this.isRenderEnabled = true;
		
		if (this.renderSource != null)
		{
			this.renderSource.enableRender(level);
		}
	}
	
	
    public void disableAndDisposeRendering()
	{
        if (!this.isRenderEnabled)
		{
			return;
		}
		
		this.disposeRenderData();
		this.isRenderEnabled = false;
    }
	
	
	
	//========================//
	// render source provider //
	//========================//
	
    public void reload(ILodRenderSourceProvider renderDataProvider)
	{
		// don't accidentally enable rendering for a disabled section
		if (!this.isRenderEnabled)
		{
			return;
		}
		
		
		this.renderSourceProvider = renderDataProvider;
		
        if (this.loadFuture != null)
		{
			this.loadFuture.cancel(true);
			this.loadFuture = null;
        }
		
        if (this.renderSource != null)
		{
			this.renderSource.dispose();
			this.renderSource = null;
        }
		
		this.loadFuture = this.renderSourceProvider.read(this.pos);
    }
	
	
	
	//================//
	// update methods //
	//================//
	
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
		
		if (this.loadFuture != null)
		{
			this.loadFuture.cancel(true);
			this.loadFuture = null;
		}
	}

	
	
	//========================//
	// getters and properties //
	//========================//
	
    public boolean shouldRender() { return this.isLoaded() && this.isRenderEnabled; }

    public boolean isRenderingEnabled() { return this.isRenderEnabled; }
    public boolean isLoaded() { return this.renderSource != null; }
	public boolean isLoading() { return this.loadFuture != null; }
    public boolean isOutdated() { return this.renderSource != null && !this.renderSource.isValid(); }
	
    public ColumnRenderSource getRenderSource() { return this.renderSource; }
    public CompletableFuture<ColumnRenderSource> getRenderSourceLoadingFuture() { return this.loadFuture; }
	
	
	//==============//
	// base methods //
	//==============//
	
    public String toString() {
        return "LodRenderSection{" +
                "pos=" + this.pos +
                ", lodRenderSource=" + this.renderSource +
                ", loadFuture=" + this.loadFuture +
                ", isRenderEnabled=" + this.isRenderEnabled +
                '}';
    }
	
}
