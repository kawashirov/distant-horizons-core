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
public class ConcreteTestOne implements ITestOne, IBindable
{
	private ITestTwo testInterTwo;
	
	public static int VALUE = 1;
	
	
	
	@Override
	public void finishDelayedSetup() { testInterTwo = DependencyInjectorTest.TEST_SINGLETON_HANDLER.get(ITestTwo.class, true); }
	@Override
	public boolean getDelayedSetupComplete() { return testInterTwo != null; }
	
	
	@Override
	public int getValue() { return VALUE; }
	
	@Override
	public int getDependentValue() { return testInterTwo.getValue(); }
	
}
