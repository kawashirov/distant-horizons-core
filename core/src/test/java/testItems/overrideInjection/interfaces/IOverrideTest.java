package testItems.overrideInjection.interfaces;

import com.seibel.lod.api.items.interfaces.override.IDhApiOverrideable;

/**
 * Dummy override test interface for dependency unit tests.
 *
 * @author James Seibel
 * @version 2022-9-5
 */
public interface IOverrideTest extends IDhApiOverrideable
{
	public int getValue();
	
}
