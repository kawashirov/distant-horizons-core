package testItems.dependencyInjection.objects;

import com.seibel.lod.core.handlers.dependencyInjection.IBindable;
import testItems.dependencyInjection.interfaces.ITestOne;
import testItems.dependencyInjection.interfaces.ITestTwo;
import tests.DependencyInjectorTest;

/**
 * Dummy test implementation object for dependency injection unit tests.
 *
 * @author James Seibel
 * @version 2022-7-16
 */
public class ConcreteTestTwo implements ITestTwo, IBindable
{
	private ITestOne testInterOne;
	
	public static int VALUE = 2;
	
	
	@Override
	public void finishDelayedSetup() { testInterOne = DependencyInjectorTest.TEST_SINGLETON_HANDLER.get(ITestOne.class, true); }
	@Override
	public boolean getDelayedSetupComplete() { return testInterOne != null; }
	
	
	
	@Override
	public int getValue() { return VALUE; }
	
	@Override
	public int getDependentValue() { return testInterOne.getValue(); }
	
}
