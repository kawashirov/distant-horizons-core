package com.seibel.lod.core.datatype.full;

import com.seibel.lod.core.pos.DhSectionPos;

public class SampledDataSource extends FullDataSource {
    private boolean[] isGenerated;

    protected SampledDataSource(DhSectionPos sectionPos) {
        super(sectionPos);
    }

}
