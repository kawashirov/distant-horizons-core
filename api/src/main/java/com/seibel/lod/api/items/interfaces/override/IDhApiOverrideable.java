package com.seibel.lod.api.items.interfaces.override;

import com.seibel.lod.core.api.external.coreImplementations.interfaces.override.ICoreDhApiOverrideable;
import com.seibel.lod.core.handlers.dependencyInjection.IBindable;
import com.seibel.lod.core.handlers.dependencyInjection.OverrideInjector;

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
	default int getPriority() { return OverrideInjector.DEFAULT_NON_CORE_OVERRIDE_PRIORITY; }
	
}
