package testItems.singletonInjection.interfaces;

import com.seibel.lod.core.dependencyInjection.IBindable;

/**
 * Dummy test interface for dependency unit tests.
 *
 * @author James Seibel
 * @version 2022-7-16
 */
public interface ISingletonTestTwo extends IBindable
{
	public int getValue();
	
	public int getDependentValue();
}
