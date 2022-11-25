package com.seibel.lod.core.wrapperInterfaces.modAccessor;

import com.seibel.lod.api.enums.rendering.EFogDrawMode;
import com.seibel.lod.core.ReflectionHandler;
import com.seibel.lod.core.dependencyInjection.SingletonInjector;
import com.seibel.lod.core.wrapperInterfaces.minecraft.IMinecraftClientWrapper;

import java.lang.reflect.Field;

/**
 * Contains any shared code between Optifine for Forge (official Optifine)
 * and Optifine on Fabric (unofficial ports).
 * 
 * @version 2022-11-24
 * @author James Seibel
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
	
	/** 
	 * Should not be called frequently since this uses reflection calls to determine if Optifine is present. <br>
	 * Use {@link ReflectionHandler#optifinePresent()} instead.
	 */
	public static boolean isOptifinePresent() { return getOptifineFogField() != null; }
	
	
	
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
