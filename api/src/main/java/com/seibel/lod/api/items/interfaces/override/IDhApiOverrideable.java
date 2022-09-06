package com.seibel.lod.api.items.interfaces.override;

import com.seibel.lod.core.api.external.coreImplementations.interfaces.override.ICoreDhApiOverrideable;
import com.seibel.lod.core.api.external.items.enums.override.EDhApiOverridePriority;
import com.seibel.lod.core.handlers.dependencyInjection.IBindable;

/**
 * Implemented by all DhApi objects that can be overridden.
 *
 * @author James Seibel
 * @version 2022-9-5
 */
public interface IDhApiOverrideable extends ICoreDhApiOverrideable, IBindable
{
	/**
	 * Returns when this Override should be used. <br>
	 * For most developers this can be left at the default.
	 */
	@Override
	default EDhApiOverridePriority getPriority() { return EDhApiOverridePriority.PRIMARY; }
	
}
