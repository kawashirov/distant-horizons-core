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
 
package com.seibel.lod.core.pos;

import com.seibel.lod.core.util.LodUtil;
import com.seibel.lod.core.util.MathUtil;

import java.util.Objects;

public class Pos2D
{
	public static final Pos2D ZERO = new Pos2D(0, 0);
	public final int x;
	public final int y;
	
	
	
	public Pos2D(int x, int y)
	{
		this.x = x;
		this.y = y;
	}
	
	
	
	public Pos2D add(Pos2D other) { return new Pos2D(this.x + other.x, this.y + other.y); }
	public Pos2D subtract(Pos2D other) { return new Pos2D(this.x - other.x, this.y - other.y); }
	public Pos2D subtract(int value) { return new Pos2D(this.x - value, this.y - value); }
	
	public double dist(Pos2D other) { return Math.sqrt(Math.pow(this.x - other.x, 2) + Math.pow(this.y - other.y, 2)); }
	public long distSquared(Pos2D other) { return MathUtil.pow2((long) this.x - other.x) + MathUtil.pow2((long) this.y - other.y); }
	public int chebyshevDist(Pos2D other) { return Math.max(Math.abs(this.x - other.x), Math.abs(this.y - other.y)); }
	public int manhattanDist(Pos2D o) { return Math.abs(this.x - o.x) + Math.abs(this.y - o.y); }
	
	
	
	public int hashCode() { return Objects.hash(this.x, this.y); }
	
	public String toString() { return "[" + this.x + ", " + this.y + "]"; }
	
	public boolean equals(Object otherObj)
	{
		if (otherObj == this)
			return true;
		if (otherObj instanceof Pos2D)
		{
			Pos2D otherPos = (Pos2D) otherObj;
			return this.x == otherPos.x && this.y == otherPos.y;
		}
		return false;
	}
	
}
