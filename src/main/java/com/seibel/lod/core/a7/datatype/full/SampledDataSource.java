package com.seibel.lod.core.a7.datatype.full;

import com.seibel.lod.core.a7.pos.DhSectionPos;

public class SampledDataSource extends FullDataSource {
    private boolean[] isGenerated;

    protected SampledDataSource(DhSectionPos sectionPos) {
        super(sectionPos);
    }

}
