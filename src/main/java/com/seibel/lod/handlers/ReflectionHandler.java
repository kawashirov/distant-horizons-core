/*
 *    This file is part of the Distant Horizon mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020  James Seibel
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, version 3.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.seibel.lod.handlers;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.seibel.lod.api.lod.ClientApi;
import com.seibel.lod.enums.rendering.FogQuality;
import com.seibel.lod.objects.math.Mat4f;
import com.seibel.lod.wrappers.MinecraftWrapper;

/**
 * This object is used to get variables from methods
 * where they are private. Specifically the fog setting
 * in Optifine.
 * @author James Seibel
 * @version 9 -25-2021
 */
public class ReflectionHandler
{
	public static final ReflectionHandler INSTANCE = new ReflectionHandler();
	
	public Field ofFogField = null;
	public Method vertexBufferUploadMethod = null;
	
	
	
	private ReflectionHandler()
	{
		setupFogField();
	}
	
	
	/**
	 * finds the Optifine fog type field
	 */
	private void setupFogField()
	{
		// get every variable from the entity renderer
		Field[] optionFields = MinecraftWrapper.INSTANCE.getOptions().getClass().getDeclaredFields();
		
		// try and find the ofFogType variable in gameSettings
		for (Field field : optionFields)
		{
			if (field.getName().equals("ofFogType"))
			{
				ofFogField = field;
				return;
			}
		}
		
		// we didn't find the field,
		// either optifine isn't installed, or
		// optifine changed the name of the variable
		ClientApi.LOGGER.info(ReflectionHandler.class.getSimpleName() + ": unable to find the Optifine fog field. If Optifine isn't installed this can be ignored.");
	}
	
	
	/**
	 * Get what type of fog optifine is currently set to render.
	 * @return the fog quality
	 */
	public FogQuality getFogQuality()
	{
		if (ofFogField == null)
		{
			// either optifine isn't installed,
			// the variable name was changed, or
			// the setup method wasn't called yet.
			return FogQuality.FANCY;
		}
		
		int returnNum = 0;
		
		try
		{
			returnNum = (int) ofFogField.get(MinecraftWrapper.INSTANCE.getOptions());
		}
		catch (IllegalArgumentException | IllegalAccessException e)
		{
			e.printStackTrace();
		}
		
		switch (returnNum)
		{
		default:
		case 0:
			// optifine's "default" option,
			// it should never be called in this case
			
			// normal options
		case 1:
			return FogQuality.FAST;
		case 2:
			return FogQuality.FANCY;
		case 3:
			return FogQuality.OFF;
		}
	}
	
	/** Detect if Vivecraft is present using reflection. Attempts to find the "VRRenderer" class. */
	public boolean detectVivecraft()
	{
		try
		{
			Class.forName("org.vivecraft.provider.VRRenderer");
			return true;
		}
		catch (ClassNotFoundException ignored)
		{
			System.out.println("Vivecraft not detected.");
		}
		return false;
	}
	
	/**
	 * Modifies a projection matrix's clip planes.
	 * The projection matrix must be in a column-major format.
	 * 
	 * @param projectionMatrix The projection matrix to be modified.
	 * @param nearClipPlane the new near clip plane value.
	 * @param farClipPlane the new far clip plane value.
	 * @return The modified matrix.
	 */
	public Mat4f Matrix4fModifyClipPlanes(Mat4f projectionMatrix, float nearClipPlane, float farClipPlane)
	{
		// find the matrix values.
		float nearMatrixValue = -((farClipPlane + nearClipPlane) / (farClipPlane - nearClipPlane));
		float farMatrixValue = -((2 * farClipPlane * nearClipPlane) / (farClipPlane - nearClipPlane));
		
		try
		{
			// TODO this was originally created before we had the Mat4f object,
			// so this doesn't need to be done with reflection anymore.
			// And should be moved to the LodRenderer
			
			// get the fields of the projectionMatrix
			Field[] fields = projectionMatrix.getClass().getDeclaredFields();
			// bypass the security protections on the fields that encode near and far plane values.
			fields[10].setAccessible(true);
			fields[11].setAccessible(true);
			// Change the values of the near and far plane.
			fields[10].set(projectionMatrix, nearMatrixValue);
			fields[11].set(projectionMatrix, farMatrixValue);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return projectionMatrix;
	}
}
