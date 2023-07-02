package com.seibel.distanthorizons.core.level;

import com.seibel.distanthorizons.core.wrapperInterfaces.world.IClientLevelWrapper;

/**
 * Enhances an IClientLevelWrapper with server provided world information.
 */
public interface IServerEnhancedClientLevel extends IClientLevelWrapper
{
    /**
     * Returns the world key, which is used to select the correct folder on the client.
     * @return
     */
    String getServerWorldKey();
}
