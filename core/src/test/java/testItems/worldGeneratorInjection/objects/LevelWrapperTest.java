package testItems.worldGeneratorInjection.objects;

import com.seibel.lod.core.api.external.coreImplementations.interfaces.wrappers.world.ICoreDhApiLevelWrapper;

/**
 * Stub implementation of a Level wrapper for basic unit testing.
 *
 * @author James Seibel
 * @version 2022-9-8
 */
public class LevelWrapperTest implements ICoreDhApiLevelWrapper
{
	@Override
	public Object getWrappedMcObject_UNSAFE() { return null; }
	
	@Override
	public boolean hasCeiling() { return false; }
	
	@Override
	public boolean hasSkyLight() { return false; }
	
	@Override
	public int getHeight() { return 0; }
	
}
