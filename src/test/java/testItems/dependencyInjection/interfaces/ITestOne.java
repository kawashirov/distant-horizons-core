package testItems.dependencyInjection.interfaces;

import com.seibel.lod.core.handlers.dependencyInjection.IBindable;

/**
 * Dummy test interface for dependency unit tests.
 *
 * @author James Seibel
 * @version 2022-7-16
 */
public interface ITestOne extends IBindable
{
	public int getValue();
	
	public int getDependentValue();
}
