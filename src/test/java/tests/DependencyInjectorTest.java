package tests;

import com.seibel.lod.core.handlers.dependencyInjection.DependencyInjector;
import com.seibel.lod.core.handlers.dependencyInjection.DhApiEventInjector;
import com.seibel.lod.core.handlers.dependencyInjection.IBindable;

import org.junit.Assert;
import org.junit.Test;
import testItems.dependencyInjection.implementations.DhTestEvent;
import testItems.dependencyInjection.implementations.DhTestEventAlt;
import testItems.dependencyInjection.interfaces.ISingletonTestOne;
import testItems.dependencyInjection.interfaces.ISingletonTestTwo;
import testItems.dependencyInjection.objects.ConcreteSingletonTestBoth;
import testItems.dependencyInjection.objects.ConcreteSingletonTestOne;
import testItems.dependencyInjection.objects.ConcreteSingletonTestTwo;
import testItems.dependencyInjection.objects.DhApiTestEvent;

import java.util.ArrayList;


/**
 * @author James Seibel
 * @version 2022-7-19
 */
public class DependencyInjectorTest
{
	
	@Test
	public void testSingleImplementationSingleton()
	{
		// clear the previous dependencies and only allow single dependencies
		DependencyInjector<IBindable> TEST_SINGLETON_HANDLER = new DependencyInjector<>(IBindable.class, false);
		
		
		// pre-setup
		Assert.assertNull(ISingletonTestOne.class.getSimpleName() + " should not have been bound.", TEST_SINGLETON_HANDLER.get(ISingletonTestOne.class));
		
		
		// dependency setup
		TEST_SINGLETON_HANDLER.bind(ISingletonTestOne.class, new ConcreteSingletonTestOne());
		TEST_SINGLETON_HANDLER.bind(ISingletonTestTwo.class, new ConcreteSingletonTestTwo());
		
		TEST_SINGLETON_HANDLER.runDelayedSetup();
		
		
		// basic dependencies
		ISingletonTestOne testInterOne = TEST_SINGLETON_HANDLER.get(ISingletonTestOne.class);
		Assert.assertNotNull(ISingletonTestOne.class.getSimpleName() + " not bound.", testInterOne);
		Assert.assertEquals(ISingletonTestOne.class.getSimpleName() + " incorrect value.", testInterOne.getValue(), ConcreteSingletonTestOne.VALUE);
		
		ISingletonTestTwo testInterTwo = TEST_SINGLETON_HANDLER.get(ISingletonTestTwo.class);
		Assert.assertNotNull(ISingletonTestTwo.class.getSimpleName() + " not bound.", testInterTwo);
		Assert.assertEquals(ISingletonTestTwo.class.getSimpleName() + " incorrect value.", testInterTwo.getValue(), ConcreteSingletonTestTwo.VALUE);
		
		
		// circular dependencies (if this throws an exception the dependency isn't set up)
		Assert.assertEquals(ISingletonTestOne.class.getSimpleName() + " incorrect value.", testInterOne.getDependentValue(), ConcreteSingletonTestTwo.VALUE);
		Assert.assertEquals(ISingletonTestTwo.class.getSimpleName() + " incorrect value.", testInterTwo.getDependentValue(), ConcreteSingletonTestOne.VALUE);
		
	}
	
	@Test
	public void testMultipleImplementationSingleton()
	{
		// clear the previous dependencies and only allow single dependencies
		DependencyInjector<IBindable> TEST_SINGLETON_HANDLER = new DependencyInjector<IBindable>(IBindable.class, false);
		
		
		// pre-setup
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
		// clear the previous dependencies and only allow single dependencies
		DependencyInjector<IBindable> TEST_SINGLETON_HANDLER = new DependencyInjector<>(IBindable.class, false);
		// setup the list (event) dependency handler
		DhApiEventInjector TEST_EVENT_HANDLER = new DhApiEventInjector();
		
		
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



