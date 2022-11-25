package tests;

import com.seibel.lod.api.objects.events.DhApiEventDefinition;
import com.seibel.lod.core.DependencyInjection.ApiEventInjector;
import com.seibel.lod.core.events.ApiEventDefinitionHandler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import testItems.events.abstractObjects.DhApiOneTimeTestEvent;
import testItems.events.objects.DhOneTimeTestEventHandler;
import testItems.events.objects.DhOneTimeTestEventHandlerAlt;
import testItems.events.abstractObjects.DhApiTestEvent;
import testItems.events.objects.DhTestEventHandler;
import testItems.events.objects.DhTestEventHandlerAlt;

import java.util.ArrayList;


/**
 * @author James Seibel
 * @version 2022-11-21
 */
public class EventInjectorTest
{
	@Before
	public void testSetup()
	{
		// reset the injectors and event definitions
		ApiEventInjector.INSTANCE.clear();
		ApiEventDefinitionHandler.INSTANCE.clear();
		
		DhApiTestEvent.firstTimeSetupComplete = false;
		DhApiOneTimeTestEvent.firstTimeSetupComplete = false;
	}
	
	
	
	
	@Test
	public void testGeneralAndRecurringEvents() // this also tests list dependencies since there can be more than one event handler bound per event
	{
		// Injector setup
		ApiEventInjector TEST_EVENT_HANDLER = ApiEventInjector.INSTANCE;
		

		// pre-dependency setup
		Assert.assertNull("Nothing should have been bound.", TEST_EVENT_HANDLER.get(DhApiTestEvent.class));


		// dependency setup
		TEST_EVENT_HANDLER.bind(DhApiTestEvent.class, new DhTestEventHandler());
		TEST_EVENT_HANDLER.bind(DhApiTestEvent.class, new DhTestEventHandlerAlt());
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
		Assert.assertTrue("Unbind should've removed item.", TEST_EVENT_HANDLER.unbind(DhApiTestEvent.class, DhTestEventHandler.class));
		Assert.assertFalse("Unbind should've already removed item.", TEST_EVENT_HANDLER.unbind(DhApiTestEvent.class, DhTestEventHandler.class));

		// check unbinding
		afterRenderEventList = TEST_EVENT_HANDLER.getAll(DhApiTestEvent.class);
		Assert.assertEquals("Unbound list doesn't contain the correct number of items.", 1, afterRenderEventList.size());
		Assert.assertNotNull("Unbinding removed all items.", afterRenderEventList.get(0));


		// check unbound event firing
		Assert.assertEquals("fireAllEvents canceled returned canceled incorrectly.", false, TEST_EVENT_HANDLER.fireAllEvents(DhApiTestEvent.class, false));
		// remaining event
		Assert.assertEquals("Event not fired for remaining object.", false, ((DhTestEventHandlerAlt) afterRenderEventList.get(0)).eventFiredValue);
		// unbound event
		Assert.assertEquals("Event fired for unbound object.", true, unboundEvent.getTestValue());
		
		
		// prevent event handlers from being bound to the wrong event interface
		Assert.assertThrows("Event bound to a non-implementing interface.", IllegalArgumentException.class, () -> { TEST_EVENT_HANDLER.bind(DhApiOneTimeTestEvent.class, new DhTestEventHandler()); });
		Assert.assertThrows("Event bound to a non-implementing interface.", IllegalArgumentException.class, () -> { TEST_EVENT_HANDLER.bind(DhApiTestEvent.class, new DhOneTimeTestEventHandler()); });
		
	}
	
	@Test
	public void testEventDefinition()
	{
		String errorMessagePrefix = "Missing " + DhApiEventDefinition.class.getSimpleName() + " for event class [";
		Assert.assertNotNull(errorMessagePrefix + DhApiTestEvent.class.getSimpleName() + "]", ApiEventDefinitionHandler.getEventDefinition(DhApiTestEvent.class));
		Assert.assertNotNull(errorMessagePrefix + DhApiOneTimeTestEvent.class.getSimpleName() + "]", ApiEventDefinitionHandler.getEventDefinition(DhApiOneTimeTestEvent.class));
	}
	
	@Test
	public void testOneTimeEventFiring()
	{
		// Injector setup
		ApiEventInjector TEST_EVENT_HANDLER = ApiEventInjector.INSTANCE;
		
		
		// pre-dependency setup
		Assert.assertNull("Nothing should have been bound.", TEST_EVENT_HANDLER.get(DhApiOneTimeTestEvent.class));
		
		
		// pre-event fire binding
		TEST_EVENT_HANDLER.bind(DhApiOneTimeTestEvent.class, new DhOneTimeTestEventHandler());
		TEST_EVENT_HANDLER.runDelayedSetup();
		
		
		// pre-bound event firing
		Assert.assertEquals("fireAllEvents canceled returned canceled incorrectly.", true, TEST_EVENT_HANDLER.fireAllEvents(DhApiOneTimeTestEvent.class, true));
		// validate event fired correctly
		ArrayList<DhApiOneTimeTestEvent> oneTimeEventList = TEST_EVENT_HANDLER.getAll(DhApiOneTimeTestEvent.class);
		Assert.assertEquals("Event not fired for pre-fire object.", true, oneTimeEventList.get(0).getTestValue());
		
		
		// post-event fire binding
		// the event should fire instantly
		TEST_EVENT_HANDLER.bind(DhApiOneTimeTestEvent.class, new DhOneTimeTestEventHandlerAlt());
		// validate both events have fired
		oneTimeEventList = TEST_EVENT_HANDLER.getAll(DhApiOneTimeTestEvent.class);
		Assert.assertEquals("Event not fired for pre-fire object.", true, oneTimeEventList.get(0).getTestValue());
		Assert.assertEquals("Event not fired for post-fire object.", true, oneTimeEventList.get(1).getTestValue());
		
		
		
		// recurring event test
		TEST_EVENT_HANDLER.bind(DhApiTestEvent.class, new DhTestEventHandler());
		ArrayList<DhApiTestEvent> recurringEventList = TEST_EVENT_HANDLER.getAll(DhApiTestEvent.class);
		Assert.assertNull("This unrelated recurring event shouldn't have been fired.", recurringEventList.get(0).getTestValue());
		
	}
	
}



