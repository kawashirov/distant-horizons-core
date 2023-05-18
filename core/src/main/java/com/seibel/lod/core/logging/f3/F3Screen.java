package com.seibel.lod.core.logging.f3;

import com.seibel.lod.coreapi.ModInfo;

import java.io.Closeable;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

public class F3Screen
{
	private static final String[] DEFAULT_STRING = {
			"", // blank line for spacing
			ModInfo.READABLE_NAME + " version: " + ModInfo.VERSION
	};
	private static final LinkedList<Message> SELF_UPDATE_MESSAGE_LIST = new LinkedList<>();
	
	public static void addStringToDisplay(List<String> list)
	{
		list.addAll(Arrays.asList(DEFAULT_STRING));
		Iterator<Message> iterator = SELF_UPDATE_MESSAGE_LIST.iterator();
		while (iterator.hasNext())
		{ 
			Message message = iterator.next();
			if (message == null)
			{
				iterator.remove();
			}
			else
			{
				message.printTo(list);
			}
		}
	}
	
	
	
	//================//
	// helper classes //
	//================//
	
	// we are using Closeable instead of AutoCloseable because the close method should never throw exceptions
	// and because this class shouldn't be used in a try {} block.
	public static abstract class Message implements Closeable
	{
		protected Message() { SELF_UPDATE_MESSAGE_LIST.add(this); }
		
		public abstract void printTo(List<String> output);
		
		@Override 
		public void close() { SELF_UPDATE_MESSAGE_LIST.remove(this); }
	}
	
	public static class StaticMessage extends Message
	{
		private final String[] lines;
		
		public StaticMessage(String... lines) { this.lines = lines; }
		
		@Override
		public void printTo(List<String> output) { output.addAll(Arrays.asList(this.lines)); }
	}
	
	public static class DynamicMessage extends Message
	{
		private final Supplier<String> supplier;
		
		public DynamicMessage(Supplier<String> message) { this.supplier = message; }
		
		public void printTo(List<String> list)
		{
			String message = this.supplier.get();
			if (message != null)
			{
				list.add(message);
			}
		}
	}
	
	public static class MultiDynamicMessage extends Message
	{
		private final Supplier<String>[] supplierList;
		
		@SafeVarargs
		public MultiDynamicMessage(Supplier<String>... suppliers) { this.supplierList = suppliers; }
		
		public void printTo(List<String> list)
		{
			for (Supplier<String> supplier : this.supplierList)
			{
				String message = supplier.get();
				if (message != null)
				{
					list.add(message);
				}
			}
		}
	}
	
	public static class NestedMessage extends Message
	{
		private final Supplier<String[]> supplier;
		
		public NestedMessage(Supplier<String[]> message)
		{
			this.supplier = message;
		}
		
		public void printTo(List<String> list)
		{
			String[] message = this.supplier.get();
			if (message != null)
			{
				list.addAll(Arrays.asList(message));
			}
		}
	}
	
}
