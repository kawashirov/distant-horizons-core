package com.seibel.lod.core.dataObjects.fullData.sources;

import com.seibel.lod.core.pos.DhSectionPos;

public class SampledFullDataSource extends FullDataSource
{
    private boolean[] isGenerated;

    protected SampledFullDataSource(DhSectionPos sectionPos) { super(sectionPos); }

}
