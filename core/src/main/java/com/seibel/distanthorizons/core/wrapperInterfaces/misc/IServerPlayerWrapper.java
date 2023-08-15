package com.seibel.distanthorizons.core.wrapperInterfaces.misc;

import com.seibel.distanthorizons.api.interfaces.IDhApiUnsafeWrapper;

import java.util.UUID;

public interface IServerPlayerWrapper extends IDhApiUnsafeWrapper
{
	UUID getUUID();
	
}
