/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2023 James Seibel
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

package com.seibel.distanthorizons.core.wrapperInterfaces.modAccessor;

import com.seibel.distanthorizons.api.enums.rendering.EFogDrawMode;
import com.seibel.distanthorizons.core.dependencyInjection.SingletonInjector;
import com.seibel.distanthorizons.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;

import java.lang.reflect.Field;

/**
 * Contains any shared code between Optifine for Forge (official Optifine)
 * and Optifine on Fabric (unofficial ports).
 *
 * @author James Seibel
 * @version 2022-11-24
 */
public abstract class AbstractOptifineAccessor implements IOptifineAccessor
{
	public Field ofFogField = null;
	public Object mcOptionsObject = null;
	
	
	
	//=======//
	// setup //
	//=======//
	
	public void finishDelayedSetup()
	{
		this.mcOptionsObject = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class).getOptionsObject();
		this.ofFogField = getOptifineFogField();
	}
	
	/** Returns null if Optifine isn't installed. */
	public static Field getOptifineFogField()
	{
		Object mcOptions = SingletonInjector.INSTANCE.get(IMinecraftClientWrapper.class).getOptionsObject();
		
		// try and find the ofFogType variable in gameSettings
		for (Field field : mcOptions.getClass().getDeclaredFields())
		{
			if (field.getName().equals("ofFogType"))
			{
				return field;
			}
		}
		return null;
	}
	
	public static boolean optifinePresent() { return getOptifineFogField() != null; }
	
	
	
	//===================//
	// interface methods //
	//===================//
	
	@Override
	public EFogDrawMode getFogDrawMode()
	{
		if (this.ofFogField == null)
		{
			// either optifine isn't installed,
			// the variable name was changed, or
			// the setup method wasn't called yet.
			return EFogDrawMode.FOG_ENABLED;
		}
		
		int returnNum = 0;
		
		try
		{
			returnNum = (int) this.ofFogField.get(this.mcOptionsObject);
		}
		catch (IllegalArgumentException | IllegalAccessException e)
		{
			e.printStackTrace();
		}
		
		switch (returnNum)
		{
			default:
			case 0: // optifine's "default" option,
				// it should never be used, so default to fog Enabled
				
				// normal options
			case 1: // fast
			case 2: // fancy
				return EFogDrawMode.FOG_ENABLED;
			case 3: // off
				return EFogDrawMode.FOG_DISABLED;
		}
	}
	
	@Override
	public double getRenderResolutionMultiplier()
	{
		/*
		 * TODO remove comment when done, this is just here as reference
		 * Returns the percentage multiplier of the screen's current resolution. <br>
		 * 1.0 = 100% <br>
		 * 1.5 = 150% <br>
		 */
		
		// TODO
		return 1.0;
	}
	
}
