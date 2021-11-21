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

package com.seibel.lod.forge.wrappers;

import java.nio.FloatBuffer;

import com.seibel.lod.core.enums.LodDirection;
import com.seibel.lod.core.objects.math.Mat4f;

import net.minecraft.util.Direction;
import net.minecraft.util.math.vector.Matrix4f;

/**
 * This class converts to and from Minecraft objects (Ex: Matrix4f)
 * and objects we created (Ex: Mat4f).
 * 
 * @author James Seibel
 * @version 11-20-2021
 */
public class McObjectConverter
{
	/** 4x4 float matrix converter */
	public static Mat4f Convert(Matrix4f mcMatrix)
	{
		FloatBuffer buffer = FloatBuffer.allocate(16);
		mcMatrix.store(buffer);
		Mat4f matrix = new Mat4f(buffer);
		matrix.transpose();
		return matrix;
	}


	public static Direction Convert(LodDirection lodDirection)
	{
		return Direction.byName(lodDirection.name());
	}
	
	
}
