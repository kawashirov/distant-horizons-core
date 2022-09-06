package com.seibel.lod.core.api.external.coreInterfaces;

import com.seibel.lod.core.handlers.dependencyInjection.IBindable;
import com.seibel.lod.core.handlers.dependencyInjection.OverrideInjector;

/**
 * Implemented by all DhApi objects that can be overridden.
 *
 * @author James Seibel
 * @version 2022-9-5
 */
public interface ICoreDhApiOverrideable extends IBindable
{
	/**
	 * Returns when this Override should be used. <br>
	 * For most developers this can be left at the default.
	 */
	default int getPriority() { return OverrideInjector.DEFAULT_NON_CORE_OVERRIDE_PRIORITY; }
	
}
