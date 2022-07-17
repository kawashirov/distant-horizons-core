package tests;

import com.seibel.lod.core.handlers.dependencyInjection.DependencyInjector;
import com.seibel.lod.core.handlers.dependencyInjection.DhApiEventInjector;
import com.seibel.lod.core.handlers.dependencyInjection.IBindable;

import org.junit.Assert;
import org.junit.Test;
import testItems.dependencyInjection.implementations.DhTestEvent;
import testItems.dependencyInjection.implementations.DhTestEventAlt;
import testItems.dependencyInjection.interfaces.ITestOne;
import testItems.dependencyInjection.interfaces.ITestTwo;
import testItems.dependencyInjection.objects.ConcreteTestBoth;
import testItems.dependencyInjection.objects.ConcreteTestOne;
import testItems.dependencyInjection.objects.ConcreteTestTwo;
import testItems.dependencyInjection.objects.DhApiTestEvent;

import java.util.ArrayList;


/**
 * @author James Seibel
 * @version 7-16-2022
 */
public class DependencyInjectorTest
{
	public static DependencyInjector<IBindable> TEST_SINGLETON_HANDLER;
	
	public static DhApiEventInjector TEST_EVENT_HANDLER;
	
	@Test
	public void testSingleImplementations()
	{
		// clear the previous dependencies and only allow single dependencies
		TEST_SINGLETON_HANDLER = new DependencyInjector<>(IBindable.class, false);
		
		
		// pre-setup
		Assert.assertNull(ITestOne.class.getSimpleName() + " should not have been bound.", TEST_SINGLETON_HANDLER.get(ITestOne.class));
		
		
		// dependency setup
		TEST_SINGLETON_HANDLER.bind(ITestOne.class, new ConcreteTestOne());
		TEST_SINGLETON_HANDLER.bind(ITestTwo.class, new ConcreteTestTwo());
		
		TEST_SINGLETON_HANDLER.runDelayedSetup();
		
		
		// basic dependencies
		ITestOne testInterOne = TEST_SINGLETON_HANDLER.get(ITestOne.class);
		Assert.assertNotNull(ITestOne.class.getSimpleName() + " not bound.", testInterOne);
		Assert.assertEquals(ITestOne.class.getSimpleName() + " incorrect value.", testInterOne.getValue(), ConcreteTestOne.VALUE);
		
		ITestTwo testInterTwo = TEST_SINGLETON_HANDLER.get(ITestTwo.class);
		Assert.assertNotNull(ITestTwo.class.getSimpleName() + " not bound.", testInterTwo);
		Assert.assertEquals(ITestTwo.class.getSimpleName() + " incorrect value.", testInterTwo.getValue(), ConcreteTestTwo.VALUE);
		
		
		// circular dependencies (if this throws an exception the dependency isn't set up)
		Assert.assertEquals(ITestOne.class.getSimpleName() + " incorrect value.", testInterOne.getDependentValue(), ConcreteTestTwo.VALUE);
		Assert.assertEquals(ITestTwo.class.getSimpleName() + " incorrect value.", testInterTwo.getDependentValue(), ConcreteTestOne.VALUE);
		
	}
	
	@Test
	public void testMultipleImplementations()
	{
		// clear the previous dependencies and only allow single dependencies
		TEST_SINGLETON_HANDLER = new DependencyInjector<IBindable>(IBindable.class, false);
		
		
		// pre-setup
		Assert.assertNull(ITestOne.class.getSimpleName() + " should not have been bound.", TEST_SINGLETON_HANDLER.get(ITestOne.class));
		
		
		// dependency setup
		ConcreteTestBoth concreteInstance = new ConcreteTestBoth();
		
		TEST_SINGLETON_HANDLER.bind(ITestOne.class, concreteInstance);
		TEST_SINGLETON_HANDLER.bind(ITestTwo.class, concreteInstance);
		
		
		// basic dependencies
		ITestOne testInterOne = TEST_SINGLETON_HANDLER.get(ITestOne.class);
		Assert.assertNotNull(ITestOne.class.getSimpleName() + " not bound.", testInterOne);
		Assert.assertEquals(ITestOne.class.getSimpleName() + " incorrect value.", testInterOne.getValue(), ConcreteTestBoth.VALUE);
		
		ITestTwo testInterTwo = TEST_SINGLETON_HANDLER.get(ITestTwo.class);
		Assert.assertNotNull(ITestTwo.class.getSimpleName() + " not bound.", testInterTwo);
		Assert.assertEquals(ITestTwo.class.getSimpleName() + " incorrect value.", testInterTwo.getValue(), ConcreteTestBoth.VALUE);
		
	}
	
	@Test
	public void testEventDependencies() // this also tests list dependencies since there can be more than one event handler bound per event
	{
		// clear the previous dependencies and only allow single dependencies
		TEST_SINGLETON_HANDLER = new DependencyInjector<>(IBindable.class, false);
		// setup the list (event) dependency handler
		TEST_EVENT_HANDLER = new DhApiEventInjector();
		
		
		// pre-setup
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
	
}



