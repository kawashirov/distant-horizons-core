package testItems.overrideInjection.interfaces;

import com.seibel.lod.core.api.external.coreImplementations.interfaces.override.ICoreDhApiOverrideable;

/**
 * Dummy override test interface for dependency unit tests.
 *
 * @author James Seibel
 * @version 2022-9-5
 */
public interface IOverrideTest extends ICoreDhApiOverrideable
{
	public int getValue();
	
}
