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
public class ConcreteSingletonTestTwo implements ISingletonTestTwo, IBindable
{
	private final DependencyInjector<IBindable> dependencyInjector;
	private ISingletonTestOne testInterOne;
	
	public static int VALUE = 2;
	
	
	public ConcreteSingletonTestTwo(DependencyInjector<IBindable> newDependencyInjector)
	{
		dependencyInjector = newDependencyInjector;
	}
	
	
	@Override
	public void finishDelayedSetup() { testInterOne = dependencyInjector.get(ISingletonTestOne.class, true); }
	@Override
	public boolean getDelayedSetupComplete() { return testInterOne != null; }
	
	
	@Override
	public int getValue() { return VALUE; }
	
	@Override
	public int getDependentValue() { return testInterOne.getValue(); }
	
}
