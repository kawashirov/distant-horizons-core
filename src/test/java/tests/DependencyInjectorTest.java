package tests;

import com.seibel.lod.core.api.external.items.enums.override.EDhApiOverridePriority;
import com.seibel.lod.core.api.external.items.interfaces.override.IDhApiOverrideable;
import com.seibel.lod.core.api.external.items.interfaces.override.IDhApiWorldGenerator;
import com.seibel.lod.core.enums.override.EOverridePriority;
import com.seibel.lod.core.handlers.dependencyInjection.*;

import org.junit.Assert;
import org.junit.Test;
import testItems.eventInjection.objects.DhTestEvent;
import testItems.eventInjection.objects.DhTestEventAlt;
import testItems.overrideInjection.objects.OverrideTestCore;
import testItems.overrideInjection.objects.OverrideTestPrimary;
import testItems.overrideInjection.objects.OverrideTestSecondary;
import testItems.singletonInjection.interfaces.ISingletonTestOne;
import testItems.singletonInjection.interfaces.ISingletonTestTwo;
import testItems.singletonInjection.objects.ConcreteSingletonTestBoth;
import testItems.singletonInjection.objects.ConcreteSingletonTestOne;
import testItems.singletonInjection.objects.ConcreteSingletonTestTwo;
import testItems.eventInjection.abstractObjects.DhApiTestEvent;
import testItems.overrideInjection.interfaces.IOverrideTest;
import testItems.overrideInjection.objects.OverrideTestAssembly;
import testItems.worldGeneratorInjection.objects.WorldGeneratorTestCore;
import testItems.worldGeneratorInjection.objects.WorldGeneratorTestPrimary;
import testItems.worldGeneratorInjection.objects.WorldGeneratorTestSecondary;

import java.util.ArrayList;


/**
 * @author James Seibel
 * @version 2022-7-26
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
	public void testEventDependencies() // this also tests list dependencies since there can be more than one event handler bound per event
	{
		// Injector setup
		DhApiEventInjector TEST_EVENT_HANDLER = new DhApiEventInjector();
		
		
		// pre-dependency setup
		Assert.assertNull("Nothing should have been bound.", TEST_EVENT_HANDLER.get(DhApiTestEvent.class));
		
		
		// dependency setup
		TEST_EVENT_HANDLER.bind(DhApiTestEvent.class, new DhTestEvent());
		TEST_EVENT_HANDLER.bind(DhApiTestEvent.class, new DhTestEventAlt());
		TEST_EVENT_HANDLER.runDelayedSetup();
		
		
		// get first
		DhApiTestEvent afterRenderEvent = TEST_EVENT_HANDLER.get(DhApiTestEvent.class);
		Assert.assertNotNull("Event not bound.", afterRenderEvent);
		
		
		// get list
		ArrayList<DhApiTestEvent> afterRenderEventList = TEST_EVENT_HANDLER.getAll(DhApiTestEvent.class);
		Assert.assertEquals("Bound list doesn't contain the correct number of items.", 2, afterRenderEventList.size());
		// object one
		Assert.assertNotNull("Event not bound.", afterRenderEventList.get(0));
		Assert.assertEquals("First event object setup incorrectly.", null, afterRenderEventList.get(0).getTestValue());
		// object two
		Assert.assertNotNull("Event not bound.", afterRenderEventList.get(1));
		Assert.assertEquals("First event object setup incorrectly.", null, afterRenderEventList.get(1).getTestValue());
		
		
		// event firing
		Assert.assertEquals("fireAllEvents canceled returned canceled incorrectly.", true, TEST_EVENT_HANDLER.fireAllEvents(DhApiTestEvent.class, true));
		// object one
		Assert.assertEquals("Event not fired for first object.", true, afterRenderEventList.get(0).getTestValue());
		// object two
		Assert.assertEquals("Event not fired for second object.", true, afterRenderEventList.get(1).getTestValue());
		
		
		// unbind
		DhApiTestEvent unboundEvent = afterRenderEventList.get(0);
		Assert.assertTrue("Unbind should've removed item.", TEST_EVENT_HANDLER.unbind(DhApiTestEvent.class, DhTestEvent.class));
		Assert.assertFalse("Unbind should've already removed item.", TEST_EVENT_HANDLER.unbind(DhApiTestEvent.class, DhTestEvent.class));
		
		// check unbinding
		afterRenderEventList = TEST_EVENT_HANDLER.getAll(DhApiTestEvent.class);
		Assert.assertEquals("Unbound list doesn't contain the correct number of items.", 1, afterRenderEventList.size());
		Assert.assertNotNull("Unbinding removed all items.", afterRenderEventList.get(0));
		
		
		// check unbound event firing
		Assert.assertEquals("fireAllEvents canceled returned canceled incorrectly.", false, TEST_EVENT_HANDLER.fireAllEvents(DhApiTestEvent.class, false));
		// remaining event
		Assert.assertEquals("Event not fired for remaining object.", false, ((DhTestEventAlt) afterRenderEventList.get(0)).eventFiredValue);
		// unbound event
		Assert.assertEquals("Event fired for unbound object.", true, unboundEvent.getTestValue());
		
	}
	
	@Test
	public void testOverrideInjection()
	{
		OverrideInjector<IDhApiOverrideable> TEST_INJECTOR = new OverrideInjector<>(OverrideTestAssembly.getPackagePath(2));
		OverrideInjector<IDhApiOverrideable> CORE_INJECTOR = new OverrideInjector<>();
		
		
		// pre-dependency setup
		Assert.assertNull("Nothing should have been bound.", TEST_INJECTOR.get(IOverrideTest.class));
		Assert.assertNull("Nothing should have been bound.", CORE_INJECTOR.get(IOverrideTest.class));
		
		
		// variables to use later
		IOverrideTest override;
		OverrideTestCore coreOverride = new OverrideTestCore();
		OverrideTestSecondary secondaryOverride = new OverrideTestSecondary();
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
		Assert.assertNotNull("Core override should be bound.", TEST_INJECTOR.get(IOverrideTest.class, EOverridePriority.CORE));
		Assert.assertNull("Secondary override should not be bound yet.", TEST_INJECTOR.get(IOverrideTest.class, EOverridePriority.SECONDARY));
		Assert.assertNull("Primary override should not be bound yet.", TEST_INJECTOR.get(IOverrideTest.class, EOverridePriority.PRIMARY));
		// standard get
		override = TEST_INJECTOR.get(IOverrideTest.class);
		Assert.assertEquals("Override returned incorrect override type.", override.getOverrideType(), EDhApiOverridePriority.CORE);
		Assert.assertEquals("Incorrect override object returned.", override.getValue(), OverrideTestCore.VALUE);
		
		
		// secondary override
		TEST_INJECTOR.bind(IOverrideTest.class, secondaryOverride);
		// priority gets
		Assert.assertNotNull("Test injector should've bound secondary override.", TEST_INJECTOR.get(IOverrideTest.class));
		Assert.assertNotNull("Core override should be bound.", TEST_INJECTOR.get(IOverrideTest.class, EOverridePriority.CORE));
		Assert.assertNotNull("Secondary override should be bound.", TEST_INJECTOR.get(IOverrideTest.class, EOverridePriority.SECONDARY));
		Assert.assertNull("Primary override should not be bound yet.", TEST_INJECTOR.get(IOverrideTest.class, EOverridePriority.PRIMARY));
		// standard get
		override = TEST_INJECTOR.get(IOverrideTest.class);
		Assert.assertEquals("Override returned incorrect override type.", override.getOverrideType(), EDhApiOverridePriority.SECONDARY);
		Assert.assertEquals("Incorrect override object returned.", override.getValue(), OverrideTestSecondary.VALUE);
		
		
		// primary override
		TEST_INJECTOR.bind(IOverrideTest.class, primaryOverride);
		// priority gets
		Assert.assertNotNull("Test injector should've bound primary override.", TEST_INJECTOR.get(IOverrideTest.class));
		Assert.assertNotNull("Core override should be bound.", TEST_INJECTOR.get(IOverrideTest.class, EOverridePriority.CORE));
		Assert.assertNotNull("Secondary override should be bound.", TEST_INJECTOR.get(IOverrideTest.class, EOverridePriority.SECONDARY));
		Assert.assertNotNull("Primary override should be bound.", TEST_INJECTOR.get(IOverrideTest.class, EOverridePriority.PRIMARY));
		// standard get
		override = TEST_INJECTOR.get(IOverrideTest.class);
		Assert.assertEquals("Override returned incorrect override type.", override.getOverrideType(), EDhApiOverridePriority.PRIMARY);
		Assert.assertEquals("Incorrect override object returned.", override.getValue(), OverrideTestPrimary.VALUE);
		
		
		// in-line get (make sure the gotten type is correct, the actual value doesn't matter)
		Assert.assertNotEquals("Inline get incorrect value.", -1, TEST_INJECTOR.get(IOverrideTest.class).getValue());
		
	}
	
	@Test
	public void testWorldGeneratorInjection()
	{
		WorldGeneratorInjector TEST_INJECTOR = new WorldGeneratorInjector();
		WorldGeneratorInjector CORE_INJECTOR = new WorldGeneratorInjector();
		
		
		// pre-dependency setup
		Assert.assertNull("Nothing should have been bound.", TEST_INJECTOR.get());
		Assert.assertNull("Nothing should have been bound.", CORE_INJECTOR.get());
		
		
		// variables to use later
		IDhApiWorldGenerator generator;
		WorldGeneratorTestCore coreGenerator = new WorldGeneratorTestCore();
		WorldGeneratorTestSecondary secondaryGenerator = new WorldGeneratorTestSecondary();
		WorldGeneratorTestPrimary primaryGenerator = new WorldGeneratorTestPrimary();
		
		
		// TODO need core package overriding
		
//		// core generator binding
//		try { TEST_GENERATOR_INJECTOR.bind(coreGenerator); } catch (IllegalArgumentException e) { Assert.fail("Core generator should be bindable for test package injector."); }
//
//		try
//		{
//			CORE_GENERATOR_INJECTOR.bind(coreGenerator);
//			Assert.fail("Core generator should not be bindable for core package injector.");
//		}
//		catch (IllegalArgumentException e) { /* this exception should be thrown */ }
//
//
//		// core override
//		Assert.assertNotNull("Test injector should've bound core override.", TEST_GENERATOR_INJECTOR.get());
//		Assert.assertNull("Core injector should not have bound core override.", CORE_GENERATOR_INJECTOR.get());
//		// standard get
//		generator = TEST_GENERATOR_INJECTOR.get();
//		Assert.assertEquals("Override returned incorrect override type.", generator.getOverrideType(), EDhApiOverridePriority.CORE);
//		Assert.assertEquals("Incorrect generator returned.", generator.getThreadingMode(), WorldGeneratorTestCore.THREAD_MODE);
		
		
		// TODO not started
		
//		// secondary override
//		TEST_OVERRIDE_INJECTOR.bind(IOverrideTest.class, secondaryOverride);
//		// priority gets
//		Assert.assertNotNull("Test injector should've bound secondary override.", TEST_OVERRIDE_INJECTOR.get(IOverrideTest.class));
//		Assert.assertNotNull("Core override should be bound.", TEST_OVERRIDE_INJECTOR.get(IOverrideTest.class, EOverridePriority.CORE));
//		Assert.assertNotNull("Secondary override should be bound.", TEST_OVERRIDE_INJECTOR.get(IOverrideTest.class, EOverridePriority.SECONDARY));
//		Assert.assertNull("Primary override should not be bound yet.", TEST_OVERRIDE_INJECTOR.get(IOverrideTest.class, EOverridePriority.PRIMARY));
//		// standard get
//		override = TEST_OVERRIDE_INJECTOR.get(IOverrideTest.class);
//		Assert.assertEquals("Override returned incorrect override type.", override.getOverrideType(), EDhApiOverridePriority.SECONDARY);
//		Assert.assertEquals("Incorrect override object returned.", override.getValue(), OverrideTestSecondary.VALUE);
//
//
//		// primary override
//		TEST_OVERRIDE_INJECTOR.bind(IOverrideTest.class, primaryOverride);
//		// priority gets
//		Assert.assertNotNull("Test injector should've bound primary override.", TEST_OVERRIDE_INJECTOR.get(IOverrideTest.class));
//		Assert.assertNotNull("Core override should be bound.", TEST_OVERRIDE_INJECTOR.get(IOverrideTest.class, EOverridePriority.CORE));
//		Assert.assertNotNull("Secondary override should be bound.", TEST_OVERRIDE_INJECTOR.get(IOverrideTest.class, EOverridePriority.SECONDARY));
//		Assert.assertNotNull("Primary override should be bound.", TEST_OVERRIDE_INJECTOR.get(IOverrideTest.class, EOverridePriority.PRIMARY));
//		// standard get
//		override = TEST_OVERRIDE_INJECTOR.get(IOverrideTest.class);
//		Assert.assertEquals("Override returned incorrect override type.", override.getOverrideType(), EDhApiOverridePriority.PRIMARY);
//		Assert.assertEquals("Incorrect override object returned.", override.getValue(), OverrideTestPrimary.VALUE);
//
//
//		// in-line get (make sure the gotten type is correct, the actual value doesn't matter)
//		Assert.assertNotEquals("Inline get incorrect value.", -1, TEST_OVERRIDE_INJECTOR.get(IOverrideTest.class).getValue());
		
	}
	
}



