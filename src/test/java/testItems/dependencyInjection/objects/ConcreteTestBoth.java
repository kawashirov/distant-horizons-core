package testItems.dependencyInjection.objects;

import com.seibel.lod.core.handlers.dependencyInjection.IBindable;
import testItems.dependencyInjection.interfaces.ITestOne;
import testItems.dependencyInjection.interfaces.ITestTwo;

/**
 * Dummy test implementation object for dependency injection unit tests.
 *
 * @author James Seibel
 * @version 2022-7-16
 */
public class ConcreteTestBoth implements ITestOne, ITestTwo, IBindable
{
	public static final int VALUE = 3;
	
	@Override
	public void finishDelayedSetup() { }
	
	@Override
	public int getValue()
	{
		return VALUE;
	}
	
	@Override
	public int getDependentValue() { return -1; }
}
