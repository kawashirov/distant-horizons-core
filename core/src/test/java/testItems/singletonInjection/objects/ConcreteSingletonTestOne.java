package testItems.singletonInjection.objects;

import com.seibel.lod.core.handlers.dependencyInjection.DependencyInjector;
import com.seibel.lod.core.handlers.dependencyInjection.IBindable;
import testItems.singletonInjection.interfaces.ISingletonTestOne;
import testItems.singletonInjection.interfaces.ISingletonTestTwo;
import tests.DependencyInjectorTest;

/**
 * Dummy test implementation object for dependency injection unit tests.
 *
 * @author James Seibel
 * @version 2022-7-16
 */
public class ConcreteSingletonTestOne implements ISingletonTestOne, IBindable
{
	private final DependencyInjector<IBindable> dependencyInjector;
	private ISingletonTestTwo testInterTwo;
	
	public static int VALUE = 1;
	
	
	public ConcreteSingletonTestOne(DependencyInjector<IBindable> newDependencyInjector)
	{
		dependencyInjector = newDependencyInjector;
	}
	
	
	@Override
	public void finishDelayedSetup() { testInterTwo = dependencyInjector.get(ISingletonTestTwo.class, true); }
	@Override
	public boolean getDelayedSetupComplete() { return testInterTwo != null; }
	
	
	@Override
	public int getValue() { return VALUE; }
	
	@Override
	public int getDependentValue() { return testInterTwo.getValue(); }
	
}
