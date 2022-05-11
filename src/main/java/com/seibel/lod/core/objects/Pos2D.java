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
 
package com.seibel.lod.core.objects;

import com.seibel.lod.core.util.LodUtil;

public class Pos2D {
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
        return LodUtil.pow2((long)x - other.x) + LodUtil.pow2((long)y - other.y);
    }
}
