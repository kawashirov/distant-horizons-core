package com.seibel.lod.core.handlers.dependencyInjection;

import com.seibel.lod.core.api.external.coreInterfaces.ICoreDhApiOverrideable;

import java.util.ArrayList;

/**
 * Contains a list of overrides and their priorities.
 * 
 * @author James Seibel 
 * @version 2022-9-5
 */
public class OverridePriorityListContainer implements IBindable
{
	/** Sorted highest priority to lowest */
	private final ArrayList<OverridePriorityPair> overridePairList = new ArrayList<>();
	
	
	/** Doesn't do any validation */
	public void addOverride(ICoreDhApiOverrideable override)
	{
		OverridePriorityPair priorityPair = new OverridePriorityPair(override, override.getPriority());
		this.overridePairList.add(priorityPair);
		
		sortList();
	}
	
	/** @return true if the override was removed from the list, false otherwise. */
	public boolean removeOverride(ICoreDhApiOverrideable override)
	{
		if (this.overridePairList.contains(override))
		{
			this.overridePairList.remove(override);
			sortList();
			
			return true;
		}
		else
		{
			return false;
		}
	}
	
	
	// getters //
	
	public ICoreDhApiOverrideable getOverrideWithLowestPriority()
	{
		if (this.overridePairList.size() == 0)
		{
			return null;
		}
		else
		{
			// last item should have the highest priority
			return this.overridePairList.get(this.overridePairList.size() - 1).override;
		}
	}
	public ICoreDhApiOverrideable getOverrideWithHighestPriority()
	{
		if (this.overridePairList.get(0) != null)
		{
			return this.overridePairList.get(0).override;
		}
		else
		{
			return null;
		}
	}
	public ICoreDhApiOverrideable getCoreOverride()
	{
		int lastIndex = this.overridePairList.size() - 1;
		if (this.overridePairList.get(lastIndex) != null && this.overridePairList.get(lastIndex).priority == OverrideInjector.CORE_PRIORITY)
		{
			return this.overridePairList.get(lastIndex).override;
		}
		else
		{
			return null;
		}
	}
	/** Returns null if no override with the given priority is found */
	public ICoreDhApiOverrideable getOverrideWithPriority(int priority)
	{
		for (OverridePriorityPair pair : this.overridePairList)
		{
			if (pair.priority == priority)
			{
				return pair.override;
			}
		}
		
		return null;
	}
	
	
	// utils //
	
	/** sort the list so the lowest priority item is first in the list */
	private void sortList() { this.overridePairList.sort((x,y) -> Integer.compare(x.priority, y.priority)); }
	
	
	
	private class OverridePriorityPair
	{
		public final ICoreDhApiOverrideable override;
		public int priority;
		
		public OverridePriorityPair(ICoreDhApiOverrideable newOverride, int newPriority)
		{
			this.override = newOverride;
			this.priority = newPriority;
		}
	}
}
