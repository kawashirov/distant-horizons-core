package com.seibel.lod.core.render;

import com.seibel.lod.core.level.IDhClientLevel;
import com.seibel.lod.core.logging.DhLoggerBuilder;
import com.seibel.lod.core.pos.DhSectionPos;
import com.seibel.lod.core.datatype.render.IRenderSource;
import com.seibel.lod.core.file.renderfile.ILodRenderSourceProvider;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CompletableFuture;

public class LodRenderSection
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
    public final DhSectionPos pos;
	
	// TODO create an enum to represent the section's state instead of using magic numbers in the childCount
	//      states (may not be a complete or correct list): loaded (childCount 4), unloaded (childCount 0), markedForDeletion/markedForFreeing (childCount -1)
	
    /* Following used for LodQuadTree tick() method, and ONLY for that method! */
    // the number of children of this section
    // (Should always be 4 after tick() is done, or 0 only if this is an unloaded node)
    public byte childCount = 0;
	
    private CompletableFuture<IRenderSource> loadFuture;
    private boolean isRenderEnabled = false;
	
	// TODO: Should I provide a way to change the render source?
	private IRenderSource renderSource;
	private ILodRenderSourceProvider renderSourceProvider = null;
	
	
	
	// Create sub region
    public LodRenderSection(DhSectionPos pos) { this.pos = pos; }
	
	
	
	//===========//
	// rendering //
	//===========//
	
    public void enableRender()
	{
        if (this.isRenderEnabled)
		{
			return;
		}
		
		
		this.loadFuture = this.renderSourceProvider.read(this.pos);
		this.isRenderEnabled = true;
    }
    public void disableRender()
	{
        if (!this.isRenderEnabled)
		{
			return;
		}
		
		
        if (this.renderSource != null)
		{
			this.renderSource.disableRender();
			this.renderSource.dispose();
			this.renderSource = null;
        }
		
        if (this.loadFuture != null)
		{
			this.loadFuture.cancel(true);
			this.loadFuture = null;
        }
		
		this.isRenderEnabled = false;
    }
	
	
	
	//==============//
	// LOD provider //
	//==============//
	
	// TODO why does this just set the sourceProvider?
    public void load(ILodRenderSourceProvider renderDataProvider) { this.renderSourceProvider = renderDataProvider; }
    public void reload(ILodRenderSourceProvider renderDataProvider)
	{
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
        if (this.loadFuture != null && this.loadFuture.isDone())
		{
			this.renderSource = this.loadFuture.join();
			this.loadFuture = null;
			
            if (this.isRenderEnabled)
			{
				this.renderSource.enableRender(level, quadTree);
            }
        }
		
        if (this.renderSource != null)
		{
			this.renderSourceProvider.refreshRenderSource(this.renderSource);
        }
    }
	
    public void dispose()
	{
		if (this.renderSource != null)
		{
			this.renderSource.dispose();
		}
		else if (this.loadFuture != null)
		{
			this.loadFuture.cancel(true);
		}
	}

	
	
	//========================//
	// getters and properties //
	//========================//
	
    public boolean shouldRender() { return this.isLoaded() && this.isRenderEnabled; }

    public boolean isLoaded() { return this.renderSource != null; }
	public boolean isLoading() { return this.loadFuture != null; }
	
    //FIXME: Used by RenderBufferHandler
    public int FIXME_BYPASS_DONT_USE_getChildCount() { return this.childCount; }
	
    public boolean isOutdated() { return this.renderSource != null && !this.renderSource.isValid(); }

    public IRenderSource getRenderSource() { return this.renderSource; }
	
	
	
	//==============//
	// base methods //
	//==============//
	
    public String toString() {
        return "LodRenderSection{" +
                "pos=" + this.pos +
                ", childCount=" + this.childCount +
                ", lodRenderSource=" + this.renderSource +
                ", loadFuture=" + this.loadFuture +
                ", isRenderEnabled=" + this.isRenderEnabled +
                '}';
    }
	
}
