/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU GPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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
 
package com.seibel.lod.core.enums.rendering;

import java.util.Objects;

public class FogSetting {
    public final double start;
    public final double end;
    public final double min;
    public final double max;
    public final double density;
    public final FogType fogType;

    public FogSetting(double start, double end, double min, double max, double density, FogType fogType) {
        this.start = start;
        this.end = end;
        this.min = min;
        this.max = max;
        this.density = density;
        this.fogType = fogType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FogSetting that = (FogSetting) o;
        return Double.compare(that.start, start) == 0 && Double.compare(that.end, end) == 0 && Double.compare(that.min, min) == 0 && Double.compare(that.max, max) == 0 && Double.compare(that.density, density) == 0 && fogType == that.fogType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end, min, max, density, fogType);
    }

    public enum FogType {
        LINEAR,
        EXPONENTIAL,
        EXPONENTIAL_SQUARED,
        // TEXTURE_BASED, // TODO: Impl this
    }
    

}
