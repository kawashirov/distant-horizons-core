package testItems.overrideInjection.objects;

import com.seibel.lod.core.api.external.items.enums.override.EDhApiOverridePriority;
import testItems.overrideInjection.interfaces.IOverrideTest;

/**
 * Dummy test implementation object for dependency injection unit tests.
 *
 * @author James Seibel
 * @version 2022-7-19
 */
public class OverrideTestSecondary implements IOverrideTest
{
	public static int VALUE = 2;
	
	
	@Override
	public int getValue() { return VALUE; }
	
	@Override
	public EDhApiOverridePriority getOverrideType() { return EDhApiOverridePriority.SECONDARY; }
	
}
