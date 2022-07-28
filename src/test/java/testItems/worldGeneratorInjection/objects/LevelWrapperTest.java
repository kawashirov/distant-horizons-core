package testItems.worldGeneratorInjection.objects;

import com.seibel.lod.core.api.external.items.enums.worldGeneration.EDhApiLevelType;
import com.seibel.lod.core.api.external.items.interfaces.world.IDhApiDimensionTypeWrapper;
import com.seibel.lod.core.api.external.items.interfaces.world.IDhApiLevelWrapper;

/**
 * Stub implementation of a Level wrapper for basic unit testing.
 *
 * @author James Seibel
 * @version 2022-7-27
 */
public class LevelWrapperTest implements IDhApiLevelWrapper
{
	@Override
	public Object getWrappedMcObject_UNSAFE() { return null; }
	
	@Override
	public IDhApiDimensionTypeWrapper getDimensionType() { return null; }
	
	@Override
	public EDhApiLevelType getLevelType() { return EDhApiLevelType.UNKNOWN; }
	
	@Override
	public boolean hasCeiling() { return false; }
	
	@Override
	public boolean hasSkyLight() { return false; }
	
	@Override
	public int getHeight() { return 0; }
	
	@Override
	public int getSeaLevel() { return 0; }
}
