/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Lesser General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Lesser General Public License for more details.
 *
 *    You should have received a copy of the GNU Lesser General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.distanthorizons.api.objects.events;

/**
 * The event definition includes meta information about how the event will behave.
 * 
 * @author James Seibel
 * @version 2022-11-20
 */
public class DhApiEventDefinition
{
	/** True if the event can be canceled. */
	public final boolean isCancelable;
	
	/**
	 * True if the event will only ever be fired once. <Br>
	 * An example of this would be initial setup methods, DH won't run its initial setup more than once. <br><br>
	 *
	 * If a handler is bound for a one time event after the event has been fired, the handler will be immediately fired.
	 */
	public final boolean isOneTimeEvent;
	
	
	
	public DhApiEventDefinition(boolean isCancelable, boolean isOneTimeEvent)
	{
		this.isCancelable = isCancelable;
		this.isOneTimeEvent = isOneTimeEvent;
	}
	
}
