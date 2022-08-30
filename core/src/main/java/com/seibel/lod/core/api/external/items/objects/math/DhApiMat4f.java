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

package com.seibel.lod.core.api.external.items.objects.math;

/**
 * A simple way to store a 4x4 array
 * of floats without having to worry
 * about remembering which array is columns
 * and which one is rows.
 * <br>
 * Based on Minecraft 1.16's implementation
 * of a 4x4 matrix.
 * 
 * @author James Seibel
 * @version 2022-8-21
 */
public class DhApiMat4f
{
	private float m00;
	private float m01;
	private float m02;
	private float m03;
	private float m10;
	private float m11;
	private float m12;
	private float m13;
	private float m20;
	private float m21;
	private float m22;
	private float m23;
	private float m30;
	private float m31;
	private float m32;
	private float m33;
	
	
	public DhApiMat4f()
	{
		
	}
	
	public DhApiMat4f(DhApiMat4f sourceMatrix)
	{
		this.m00 = sourceMatrix.m00;
		this.m01 = sourceMatrix.m01;
		this.m02 = sourceMatrix.m02;
		this.m03 = sourceMatrix.m03;
		this.m10 = sourceMatrix.m10;
		this.m11 = sourceMatrix.m11;
		this.m12 = sourceMatrix.m12;
		this.m13 = sourceMatrix.m13;
		this.m20 = sourceMatrix.m20;
		this.m21 = sourceMatrix.m21;
		this.m22 = sourceMatrix.m22;
		this.m23 = sourceMatrix.m23;
		this.m30 = sourceMatrix.m30;
		this.m31 = sourceMatrix.m31;
		this.m32 = sourceMatrix.m32;
		this.m33 = sourceMatrix.m33;
	}
	
	public DhApiMat4f(float[] values)
	{
		m00 = values[0];
		m01 = values[1];
		m02 = values[2];
		m03 = values[3];
		m10 = values[4];
		m11 = values[5];
		m12 = values[6];
		m13 = values[7];
		m20 = values[8];
		m21 = values[9];
		m22 = values[10];
		m23 = values[11];
		m30 = values[12];
		m31 = values[13];
		m32 = values[14];
		m33 = values[15];
	}
	
	
	
	/** Returns the values of this matrix in row major order (AKA rows then columns) */
	private float[] getValuesAsArray()
	{
		return new float[] {
				this.m00,
				this.m01,
				this.m02,
				this.m03,
				
				this.m10,
				this.m11,
				this.m12,
				this.m13,
				
				this.m20,
				this.m21,
				this.m22,
				this.m23,
				
				this.m30,
				this.m31,
				this.m32,
				this.m33,
		};
	}
	
	
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		else if (obj != null && this.getClass() == obj.getClass())
		{
			DhApiMat4f otherMatrix = (DhApiMat4f) obj;
			return Float.compare(otherMatrix.m00, this.m00) == 0
					&& Float.compare(otherMatrix.m01, this.m01) == 0
					&& Float.compare(otherMatrix.m02, this.m02) == 0
					&& Float.compare(otherMatrix.m03, this.m03) == 0
					&& Float.compare(otherMatrix.m10, this.m10) == 0
					&& Float.compare(otherMatrix.m11, this.m11) == 0
					&& Float.compare(otherMatrix.m12, this.m12) == 0
					&& Float.compare(otherMatrix.m13, this.m13) == 0
					&& Float.compare(otherMatrix.m20, this.m20) == 0
					&& Float.compare(otherMatrix.m21, this.m21) == 0
					&& Float.compare(otherMatrix.m22, this.m22) == 0
					&& Float.compare(otherMatrix.m23, this.m23) == 0
					&& Float.compare(otherMatrix.m30, this.m30) == 0
					&& Float.compare(otherMatrix.m31, this.m31) == 0
					&& Float.compare(otherMatrix.m32, this.m32) == 0
					&& Float.compare(otherMatrix.m33, this.m33) == 0;
		}
		else
		{
			return false;
		}
	}
	
	@Override
	public int hashCode()
	{
		int i = this.m00 != 0.0F ? Float.floatToIntBits(this.m00) : 0;
		i = 31 * i + (this.m01 != 0.0F ? Float.floatToIntBits(this.m01) : 0);
		i = 31 * i + (this.m02 != 0.0F ? Float.floatToIntBits(this.m02) : 0);
		i = 31 * i + (this.m03 != 0.0F ? Float.floatToIntBits(this.m03) : 0);
		i = 31 * i + (this.m10 != 0.0F ? Float.floatToIntBits(this.m10) : 0);
		i = 31 * i + (this.m11 != 0.0F ? Float.floatToIntBits(this.m11) : 0);
		i = 31 * i + (this.m12 != 0.0F ? Float.floatToIntBits(this.m12) : 0);
		i = 31 * i + (this.m13 != 0.0F ? Float.floatToIntBits(this.m13) : 0);
		i = 31 * i + (this.m20 != 0.0F ? Float.floatToIntBits(this.m20) : 0);
		i = 31 * i + (this.m21 != 0.0F ? Float.floatToIntBits(this.m21) : 0);
		i = 31 * i + (this.m22 != 0.0F ? Float.floatToIntBits(this.m22) : 0);
		i = 31 * i + (this.m23 != 0.0F ? Float.floatToIntBits(this.m23) : 0);
		i = 31 * i + (this.m30 != 0.0F ? Float.floatToIntBits(this.m30) : 0);
		i = 31 * i + (this.m31 != 0.0F ? Float.floatToIntBits(this.m31) : 0);
		i = 31 * i + (this.m32 != 0.0F ? Float.floatToIntBits(this.m32) : 0);
		return 31 * i + (this.m33 != 0.0F ? Float.floatToIntBits(this.m33) : 0);
	}
	
	
	@Override
	public String toString()
	{
		return "Matrix4f:\n" +
				this.m00 + " " + this.m01 + " " + this.m02 + " " + this.m03 + "\n" +
				this.m10 + " " + this.m11 + " " + this.m12 + " " + this.m13 + "\n" +
				this.m20 + " " + this.m21 + " " + this.m22 + " " + this.m23 + "\n" +
				this.m30 + " " + this.m31 + " " + this.m32 + " " + this.m33 + "\n";
	}
	
	
}
