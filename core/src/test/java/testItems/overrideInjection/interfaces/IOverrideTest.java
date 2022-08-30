package testItems.overrideInjection.interfaces;

import com.seibel.lod.core.api.external.items.interfaces.override.IDhApiOverrideable;

/**
 * Dummy override test interface for dependency unit tests.
 *
 * @author James Seibel
 * @version 2022-7-19
 */
public interface IOverrideTest extends IDhApiOverrideable
{
	public int getValue();
	
}
