package com.seibel.lod.api.interfaces.override;

import com.seibel.lod.coreapi.interfaces.dependencyInjection.IBindable;
import com.seibel.lod.coreapi.interfaces.dependencyInjection.IOverrideInjector;

/**
 * Implemented by all DhApi objects that can be overridden.
 *
 * @author James Seibel
 * @version 2022-9-5
 */
public interface IDhApiOverrideable extends IBindable
{
	/**
	 * Higher (larger numerical) priorities override lower (smaller numerical) priorities . <br>
	 * For most developers this can be left at the default.
	 */
	default int getPriority() { return IOverrideInjector.DEFAULT_NON_CORE_OVERRIDE_PRIORITY; }
	
}
