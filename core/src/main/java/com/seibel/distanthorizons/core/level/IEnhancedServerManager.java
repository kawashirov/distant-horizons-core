package com.seibel.distanthorizons.core.level;

import com.seibel.distanthorizons.core.wrapperInterfaces.world.ILevelWrapper;
import com.seibel.distanthorizons.coreapi.interfaces.dependencyInjection.IBindable;

public interface IEnhancedServerManager extends IBindable {
    /**
     * Called when a client level is wrapped by a ServerEnhancedClientLevel, for integration into mod internals.
     * @param clientLevel
     */
    void registerServerEnhancedLevel(IServerEnhancedClientLevel clientLevel);

    /**
     * Returns a new instance of a ServerEnhancedClientLevel.
     * @param level
     * @param worldKey
     * @return
     */
    IServerEnhancedClientLevel getServerEnhancedLevel(ILevelWrapper level, String worldKey);

    /**
     * Sets the LOD engine to use the override wrapper, if the server has communication enabled.
     * @param useOverrideWrapper
     */
    void setUseOverrideWrapper(boolean useOverrideWrapper);
}
