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

public class Pos2D {
    public static final Pos2D ZERO = new Pos2D(0, 0);
    public final int x;
    public final int y;
    public Pos2D(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Pos2D add(Pos2D other) {
        return new Pos2D(x + other.x, y + other.y);
    }
    public Pos2D subtract(Pos2D other) {
        return new Pos2D(x - other.x, y - other.y);
    }
    public Pos2D subtract(int v) {
        return new Pos2D(x - v, y - v);
    }

    public double dist(Pos2D other) {
        return Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2));
    }
    public long distSquared(Pos2D other) {
        return MathUtil.pow2((long)x - other.x) + MathUtil.pow2((long)y - other.y);
    }
    public int chebyshevDist(Pos2D o) {
        return Math.max(Math.abs(x - o.x), Math.abs(y - o.y));
    }
    public int manhattanDist(Pos2D o) {
        return Math.abs(x - o.x) + Math.abs(y - o.y);
    }
    public int hashCode() {
        return Objects.hash(x, y);
    }
    public String toString() {
        return "[" + x + ", " + y + "]";
    }
    public boolean equals(Object other) {
        if (other == this) return true;
        if (other instanceof Pos2D) {
            Pos2D o = (Pos2D)other;
            return x == o.x && y == o.y;
        }
        return false;
    }
}
