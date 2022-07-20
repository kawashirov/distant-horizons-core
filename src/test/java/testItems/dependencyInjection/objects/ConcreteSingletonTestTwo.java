package testItems.dependencyInjection.objects;

import com.seibel.lod.core.handlers.dependencyInjection.IBindable;
import testItems.dependencyInjection.interfaces.ISingletonTestOne;
import testItems.dependencyInjection.interfaces.ISingletonTestTwo;
import tests.DependencyInjectorTest;

/**
 * Dummy test implementation object for dependency injection unit tests.
 *
 * @author James Seibel
 * @version 2022-7-16
 */
public class ConcreteSingletonTestTwo implements ISingletonTestTwo, IBindable
{
	private ISingletonTestOne testInterOne;
	
	public static int VALUE = 2;
	
	
	@Override
	public void finishDelayedSetup() { testInterOne = DependencyInjectorTest.TEST_SINGLETON_HANDLER.get(ISingletonTestOne.class, true); }
	@Override
	public boolean getDelayedSetupComplete() { return testInterOne != null; }
	
	
	
	@Override
	public int getValue() { return VALUE; }
	
	@Override
	public int getDependentValue() { return testInterOne.getValue(); }
	
}
