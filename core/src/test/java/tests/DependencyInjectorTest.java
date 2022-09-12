package tests;

import com.seibel.lod.core.api.external.coreImplementations.interfaces.override.worldGenerator.ICoreDhApiWorldGenerator;
import com.seibel.lod.core.api.external.coreImplementations.interfaces.wrappers.world.ICoreDhApiLevelWrapper;
import com.seibel.lod.core.enums.worldGeneration.EWorldGenThreadMode;
import com.seibel.lod.core.handlers.dependencyInjection.*;

import org.junit.Assert;
import org.junit.Test;
import testItems.overrideInjection.interfaces.IOverrideTest;
import testItems.overrideInjection.objects.OverrideTestAssembly;
import testItems.overrideInjection.objects.OverrideTestCore;
import testItems.overrideInjection.objects.OverrideTestPrimary;
import testItems.singletonInjection.interfaces.ISingletonTestOne;
import testItems.singletonInjection.interfaces.ISingletonTestTwo;
import testItems.singletonInjection.objects.ConcreteSingletonTestBoth;
import testItems.singletonInjection.objects.ConcreteSingletonTestOne;
import testItems.singletonInjection.objects.ConcreteSingletonTestTwo;
import testItems.worldGeneratorInjection.objects.*;


/**
 * @author James Seibel
 * @version 2022-9-11
 */
public class DependencyInjectorTest
{
	
	@Test
	public void testSingleImplementationSingleton()
	{
		// Injector setup
		DependencyInjector<IBindable> TEST_SINGLETON_HANDLER = new DependencyInjector<>(IBindable.class, false);
		
		
		// pre-dependency setup
		Assert.assertNull(ISingletonTestOne.class.getSimpleName() + " should not have been bound.", TEST_SINGLETON_HANDLER.get(ISingletonTestOne.class));
		
		
		// dependency setup
		TEST_SINGLETON_HANDLER.bind(ISingletonTestOne.class, new ConcreteSingletonTestOne(TEST_SINGLETON_HANDLER));
		TEST_SINGLETON_HANDLER.bind(ISingletonTestTwo.class, new ConcreteSingletonTestTwo(TEST_SINGLETON_HANDLER));
		
		TEST_SINGLETON_HANDLER.runDelayedSetup();
		
		
		// basic dependencies
		ISingletonTestOne testInterOne = TEST_SINGLETON_HANDLER.get(ISingletonTestOne.class);
		Assert.assertNotNull(ISingletonTestOne.class.getSimpleName() + " not bound.", testInterOne);
		Assert.assertEquals(ISingletonTestOne.class.getSimpleName() + " incorrect value.", testInterOne.getValue(), ConcreteSingletonTestOne.VALUE);
		Assert.assertEquals(ISingletonTestOne.class.getSimpleName() + " incorrect value.", TEST_SINGLETON_HANDLER.get(ISingletonTestOne.class).getValue(), ConcreteSingletonTestOne.VALUE);
		
		ISingletonTestTwo testInterTwo = TEST_SINGLETON_HANDLER.get(ISingletonTestTwo.class);
		Assert.assertNotNull(ISingletonTestTwo.class.getSimpleName() + " not bound.", testInterTwo);
		Assert.assertEquals(ISingletonTestTwo.class.getSimpleName() + " incorrect value.", testInterTwo.getValue(), ConcreteSingletonTestTwo.VALUE);
		Assert.assertEquals(ISingletonTestTwo.class.getSimpleName() + " incorrect value.", TEST_SINGLETON_HANDLER.get(ISingletonTestTwo.class).getValue(), ConcreteSingletonTestTwo.VALUE);
		
		
		// circular dependencies (if this throws an exception the dependency isn't set up)
		Assert.assertEquals(ISingletonTestOne.class.getSimpleName() + " incorrect value.", testInterOne.getDependentValue(), ConcreteSingletonTestTwo.VALUE);
		Assert.assertEquals(ISingletonTestTwo.class.getSimpleName() + " incorrect value.", testInterTwo.getDependentValue(), ConcreteSingletonTestOne.VALUE);
		
	}
	
	@Test
	public void testMultipleImplementationSingleton()
	{
		// Injector setup
		DependencyInjector<IBindable> TEST_SINGLETON_HANDLER = new DependencyInjector<>(IBindable.class, false);
		
		
		// pre-dependency setup
		Assert.assertNull(ISingletonTestOne.class.getSimpleName() + " should not have been bound.", TEST_SINGLETON_HANDLER.get(ISingletonTestOne.class));
		
		
		// dependency setup
		ConcreteSingletonTestBoth concreteInstance = new ConcreteSingletonTestBoth();
		
		TEST_SINGLETON_HANDLER.bind(ISingletonTestOne.class, concreteInstance);
		TEST_SINGLETON_HANDLER.bind(ISingletonTestTwo.class, concreteInstance);
		
		
		// basic dependencies
		ISingletonTestOne testInterOne = TEST_SINGLETON_HANDLER.get(ISingletonTestOne.class);
		Assert.assertNotNull(ISingletonTestOne.class.getSimpleName() + " not bound.", testInterOne);
		Assert.assertEquals(ISingletonTestOne.class.getSimpleName() + " incorrect value.", testInterOne.getValue(), ConcreteSingletonTestBoth.VALUE);
		
		ISingletonTestTwo testInterTwo = TEST_SINGLETON_HANDLER.get(ISingletonTestTwo.class);
		Assert.assertNotNull(ISingletonTestTwo.class.getSimpleName() + " not bound.", testInterTwo);
		Assert.assertEquals(ISingletonTestTwo.class.getSimpleName() + " incorrect value.", testInterTwo.getValue(), ConcreteSingletonTestBoth.VALUE);
		
	}
	
	@Test
	public void testOverrideInjection()
	{
		OverrideInjector TEST_INJECTOR = new OverrideInjector(OverrideTestAssembly.getPackagePath(2));
		OverrideInjector CORE_INJECTOR = new OverrideInjector();


		// pre-dependency setup
		Assert.assertNull("Nothing should have been bound.", TEST_INJECTOR.get(IOverrideTest.class));
		Assert.assertNull("Nothing should have been bound.", CORE_INJECTOR.get(IOverrideTest.class));


		// variables to use later
		IOverrideTest override;
		OverrideTestCore coreOverride = new OverrideTestCore();
		OverrideTestPrimary primaryOverride = new OverrideTestPrimary();


		// core override binding
		try { TEST_INJECTOR.bind(IOverrideTest.class, coreOverride); } catch (IllegalArgumentException e) { Assert.fail("Core override should be bindable for test package injector."); }

		try
		{
			CORE_INJECTOR.bind(IOverrideTest.class, coreOverride);
			Assert.fail("Core override should not be bindable for core package injector.");
		}
		catch (IllegalArgumentException e) { /* this exception should be thrown */ }


		// core override
		Assert.assertNotNull("Test injector should've bound core override.", TEST_INJECTOR.get(IOverrideTest.class));
		Assert.assertNull("Core injector should not have bound core override.", CORE_INJECTOR.get(IOverrideTest.class));
		// priority gets
		Assert.assertNotNull("Core override should be bound.", TEST_INJECTOR.get(IOverrideTest.class, OverrideInjector.CORE_PRIORITY));
		Assert.assertNull("Non-core override should not be bound yet.", TEST_INJECTOR.get(IOverrideTest.class, OverrideTestPrimary.PRIORITY));
		// standard get
		override = TEST_INJECTOR.get(IOverrideTest.class);
		Assert.assertEquals("Override returned incorrect override type.", override.getPriority(), OverrideInjector.CORE_PRIORITY);
		Assert.assertEquals("Incorrect override object returned.", override.getValue(), OverrideTestCore.VALUE);


		// default override
		TEST_INJECTOR.bind(IOverrideTest.class, primaryOverride);
		// priority gets
		Assert.assertNotNull("Test injector should've bound secondary override.", TEST_INJECTOR.get(IOverrideTest.class));
		Assert.assertNotNull("Core override should be bound.", TEST_INJECTOR.get(IOverrideTest.class, OverrideInjector.CORE_PRIORITY));
		Assert.assertNotNull("Secondary override should be bound.", TEST_INJECTOR.get(IOverrideTest.class, OverrideTestPrimary.PRIORITY));
		// standard get
		override = TEST_INJECTOR.get(IOverrideTest.class);
		Assert.assertEquals("Override returned incorrect override type.", override.getPriority(), OverrideTestPrimary.PRIORITY);
		Assert.assertEquals("Incorrect override object returned.", override.getValue(), OverrideTestPrimary.VALUE);


		// in-line get
		// (make sure the returned type is correct and compiles, the actual value doesn't matter)
		TEST_INJECTOR.get(IOverrideTest.class).getValue();

	}
	
	@Test
	public void testBackupWorldGeneratorInjection()
	{
		WorldGeneratorInjector TEST_INJECTOR = new WorldGeneratorInjector(WorldGeneratorTestAssembly.getPackagePath(2));
		WorldGeneratorInjector CORE_INJECTOR = new WorldGeneratorInjector();


		// pre-dependency setup
		Assert.assertNull("Nothing should have been bound.", TEST_INJECTOR.get());
		Assert.assertNull("Nothing should have been bound.", CORE_INJECTOR.get());


		// variables to use later
		ICoreDhApiWorldGenerator generator;
		WorldGeneratorTestCore coreGenerator = new WorldGeneratorTestCore();
		WorldGeneratorTestSecondary secondaryGenerator = new WorldGeneratorTestSecondary();
		WorldGeneratorTestPrimary primaryGenerator = new WorldGeneratorTestPrimary();


		// core generator binding
		try { TEST_INJECTOR.bind(coreGenerator); } catch (IllegalArgumentException e) { Assert.fail("Core generator should be bindable for test package injector."); }

		try
		{
			CORE_INJECTOR.bind(coreGenerator);
			Assert.fail("Core generator should not be bindable for core package injector.");
		}
		catch (IllegalArgumentException e) { /* this exception should be thrown */ }


		// core override
		Assert.assertNotNull("Test injector should've bound core override.", TEST_INJECTOR.get());
		Assert.assertNull("Core injector should not have bound core override.", CORE_INJECTOR.get());
		// standard get
		generator = TEST_INJECTOR.get();
		Assert.assertEquals("Override returned incorrect override type.", generator.getPriority(), OverrideInjector.CORE_PRIORITY);
		Assert.assertEquals("Incorrect generator returned.", generator.getCoreThreadingMode(), WorldGeneratorTestCore.THREAD_MODE);


		// secondary override
		TEST_INJECTOR.bind(secondaryGenerator);
		// priority gets
		generator = TEST_INJECTOR.get();
		Assert.assertEquals("Override returned incorrect override type.", generator.getPriority(), WorldGeneratorTestSecondary.PRIORITY);
		Assert.assertEquals("Incorrect override object returned.", generator.getCoreThreadingMode(), WorldGeneratorTestSecondary.THREAD_MODE);


		// primary override
		TEST_INJECTOR.bind(primaryGenerator);
		// priority gets
		generator = TEST_INJECTOR.get();
		Assert.assertEquals("Override returned incorrect override type.", generator.getPriority(), WorldGeneratorTestPrimary.PRIORITY);
		Assert.assertEquals("Incorrect override object returned.", generator.getCoreThreadingMode(), WorldGeneratorTestPrimary.THREAD_MODE);



		// in-line get
		// (make sure the returned type is correct and compiles, the actual value doesn't matter)
		EWorldGenThreadMode threadMode = TEST_INJECTOR.get().getCoreThreadingMode();

	}

	@Test
	public void testSpecificLevelWorldGeneratorInjection()
	{
		WorldGeneratorInjector TEST_INJECTOR = new WorldGeneratorInjector(WorldGeneratorTestAssembly.getPackagePath(2));


		// pre-dependency setup
		Assert.assertNull("Nothing should have been bound.", TEST_INJECTOR.get());


		// variables to use later
		ICoreDhApiWorldGenerator generator;
		WorldGeneratorTestCore backupGenerator = new WorldGeneratorTestCore();
		WorldGeneratorTestPrimary levelGenerator = new WorldGeneratorTestPrimary();

		ICoreDhApiLevelWrapper boundLevel = new LevelWrapperTest();
		ICoreDhApiLevelWrapper unboundLevel = new LevelWrapperTest();



		// backup generator binding
		try { TEST_INJECTOR.bind(backupGenerator); } catch (IllegalArgumentException e) { Assert.fail("Core generator should be bindable for test package injector."); }


		// get backup generator
		generator = TEST_INJECTOR.get();
		Assert.assertNotNull("Backup generator not bound.", generator);
		Assert.assertEquals("Incorrect backup generator bound.", generator.getPriority(), OverrideInjector.CORE_PRIORITY);
		Assert.assertEquals("Incorrect backup generator bound.", generator.getCoreThreadingMode(), WorldGeneratorTestCore.THREAD_MODE);


		// bind level specific
		try { TEST_INJECTOR.bind(boundLevel, levelGenerator); } catch (IllegalArgumentException e) { Assert.fail("Core generator should be bindable for test package injector."); }


		// get bound level generator
		generator = TEST_INJECTOR.get(boundLevel);
		Assert.assertNotNull("Level generator not bound.", generator);
		Assert.assertEquals("Incorrect level generator bound.", generator.getPriority(), WorldGeneratorTestPrimary.PRIORITY);
		Assert.assertEquals("Incorrect level generator bound.", generator.getCoreThreadingMode(), WorldGeneratorTestPrimary.THREAD_MODE);

		// get unbound level generator
		generator = TEST_INJECTOR.get(unboundLevel);
		Assert.assertNotNull("Backup level generator not bound.", generator);
		Assert.assertEquals("Incorrect level generator bound.", generator.getPriority(), OverrideInjector.CORE_PRIORITY);
		Assert.assertEquals("Incorrect level generator bound.", generator.getCoreThreadingMode(), WorldGeneratorTestCore.THREAD_MODE);

	}
	
}



