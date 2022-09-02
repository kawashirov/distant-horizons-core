package com.seibel.lod.core.wrapperInterfaces.modAccessor;

import java.awt.*;

public interface IBCLibAccessor extends IModAccessor {
    /** Sets the BCLib custom fog renderer */
    void setRenderCustomFog(boolean newValue);

    /** Gets the BCLib fog color */
    Color getFogColor();
}
