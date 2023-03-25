package com.seibel.lod.core.render;

import com.seibel.lod.core.dataObjects.render.ColumnRenderSource;
import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.file.renderfile.ILodRenderSourceProvider;
import com.seibel.lod.core.util.objects.quadTree.QuadNode;
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
	
	public final AtomicReference<AbstractRenderBuffer> abstractRenderBufferRef = new AtomicReference<>();
	
	
	
    public LodRenderSection(DhSectionPos pos) { this.pos = pos; }
	
	
	
	//===========//
	// rendering //
	//===========//
	
    public void loadRenderSourceAndEnableRendering(ILodRenderSourceProvider renderDataProvider)
	{
        if (this.isRenderEnabled)
		{
			return;
		}
		
		this.renderSourceProvider = renderDataProvider;
		if (this.renderSourceProvider == null)
		{
			return;
		}
		
		if (this.renderSource == null)
		{
			this.loadFuture = this.renderSourceProvider.read(this.pos);
		}
		this.isRenderEnabled = true;
    }
    public void disableRender()
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
	
    public void tick(LodQuadTree quadTree, IDhClientLevel level)
	{
		// get the renderSource if it has finished loading
        if (this.loadFuture != null && this.loadFuture.isDone())
		{
			this.renderSource = this.loadFuture.join();
			this.loadFuture = null;
			
            if (this.isRenderEnabled)
			{
				this.renderSource.enableRender(level, quadTree);
            }
        }
    }
	
    public void disposeRenderData()
	{
		if (this.renderSource != null)
		{
			this.renderSource.disableRender();
			this.renderSource.dispose();
			this.renderSource = null;
		}
		
		if (this.abstractRenderBufferRef.get() != null)
		{
			this.abstractRenderBufferRef.get().close();
			this.abstractRenderBufferRef.set(null);
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
