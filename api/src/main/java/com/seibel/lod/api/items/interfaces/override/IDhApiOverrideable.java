package com.seibel.lod.api.items.interfaces.override;

import com.seibel.lod.core.interfaces.dependencyInjection.IBindable;
import com.seibel.lod.core.interfaces.dependencyInjection.IOverrideInjector;

/**
 * Implemented by all DhApi objects that can be overridden.
 *
 * @author James Seibel
 * @version 2022-9-5
 */
public interface IDhApiOverrideable extends IBindable
{
	/**
	 * Returns when this Override should be used. <br>
	 * For most developers this can be left at the default.
	 */
	default int getPriority() { return IOverrideInjector.DEFAULT_NON_CORE_OVERRIDE_PRIORITY; }
	
}
