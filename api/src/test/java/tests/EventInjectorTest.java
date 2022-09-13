package tests;

import org.junit.Assert;
import testItems.events.abstractObjects.DhApiTestEvent;
import testItems.events.objects.DhTestEvent;
import testItems.events.objects.DhTestEventAlt;

import java.util.ArrayList;


/**
 * @author James Seibel
 * @version 2022-9-11
 */
public class EventInjectorTest
{
	
//	//@Test
//	public void testEventDependencies() // this also tests list dependencies since there can be more than one event handler bound per event
//	{
//		// Injector setup
//		DhApiEventInjector TEST_EVENT_HANDLER = new DhApiEventInjector();
//
//
//		// pre-dependency setup
//		Assert.assertNull("Nothing should have been bound.", TEST_EVENT_HANDLER.get(DhApiTestEvent.class));
//
//
//		// dependency setup
//		TEST_EVENT_HANDLER.bind(DhApiTestEvent.class, new DhTestEvent());
//		TEST_EVENT_HANDLER.bind(DhApiTestEvent.class, new DhTestEventAlt());
//		TEST_EVENT_HANDLER.runDelayedSetup();
//
//
//		// get first
//		CoreDhApiTestEvent afterRenderEvent = TEST_EVENT_HANDLER.get(CoreDhApiTestEvent.class);
//		Assert.assertNotNull("Event not bound.", afterRenderEvent);
//
//
//		// get list
//		ArrayList<CoreDhApiTestEvent> afterRenderEventList = TEST_EVENT_HANDLER.getAll(CoreDhApiTestEvent.class);
//		Assert.assertEquals("Bound list doesn't contain the correct number of items.", 2, afterRenderEventList.size());
//		// object one
//		Assert.assertNotNull("Event not bound.", afterRenderEventList.get(0));
//		Assert.assertEquals("First event object setup incorrectly.", null, afterRenderEventList.get(0).getTestValue());
//		// object two
//		Assert.assertNotNull("Event not bound.", afterRenderEventList.get(1));
//		Assert.assertEquals("First event object setup incorrectly.", null, afterRenderEventList.get(1).getTestValue());
//
//
//		// event firing
//		Assert.assertEquals("fireAllEvents canceled returned canceled incorrectly.", true, TEST_EVENT_HANDLER.fireAllEvents(CoreDhApiTestEvent.class, true));
//		// object one
//		Assert.assertEquals("Event not fired for first object.", true, afterRenderEventList.get(0).getTestValue());
//		// object two
//		Assert.assertEquals("Event not fired for second object.", true, afterRenderEventList.get(1).getTestValue());
//
//
//		// unbind
//		CoreDhApiTestEvent unboundEvent = afterRenderEventList.get(0);
//		Assert.assertTrue("Unbind should've removed item.", TEST_EVENT_HANDLER.unbind(CoreDhApiTestEvent.class, DhTestEvent.class));
//		Assert.assertFalse("Unbind should've already removed item.", TEST_EVENT_HANDLER.unbind(CoreDhApiTestEvent.class, DhTestEvent.class));
//
//		// check unbinding
//		afterRenderEventList = TEST_EVENT_HANDLER.getAll(CoreDhApiTestEvent.class);
//		Assert.assertEquals("Unbound list doesn't contain the correct number of items.", 1, afterRenderEventList.size());
//		Assert.assertNotNull("Unbinding removed all items.", afterRenderEventList.get(0));
//
//
//		// check unbound event firing
//		Assert.assertEquals("fireAllEvents canceled returned canceled incorrectly.", false, TEST_EVENT_HANDLER.fireAllEvents(CoreDhApiTestEvent.class, false));
//		// remaining event
//		Assert.assertEquals("Event not fired for remaining object.", false, ((DhTestEventAlt) afterRenderEventList.get(0)).eventFiredValue);
//		// unbound event
//		Assert.assertEquals("Event fired for unbound object.", true, unboundEvent.getTestValue());
//
//	}
	
}



