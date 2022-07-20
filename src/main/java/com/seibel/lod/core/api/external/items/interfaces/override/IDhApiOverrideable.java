package com.seibel.lod.core.api.external.items.interfaces.override;

import com.seibel.lod.core.api.external.items.enums.override.EDhApiOverridePriority;
import com.seibel.lod.core.handlers.dependencyInjection.IBindable;

/**
 * Implemented by all DhApi objects that can be overridden.
 *
 * @author James Seibel
 * @version 2022-7-18
 */
public interface IDhApiOverrideable extends IBindable
{
	/**
	 * Returns when this Override should be used. <br>
	 * For most developers this can be left at the default.
	 */
	default EDhApiOverridePriority getOverrideType() { return EDhApiOverridePriority.PRIMARY; }
	
}
