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
 
package com.seibel.lod.core.enums.rendering;

/**
 * Default
 * Debug
 * Disabled
 *
 * @version 2022-6-2
 */
public enum ERendererType
{
    // Reminder:
    // when adding items up the API minor version
    // when removing items up the API major version
    
    DEFAULT,
    DEBUG,
    DISABLED;
    
    
    /** Used by the config GUI to cycle through the available rendering options */
    public static ERendererType next(ERendererType type)
    {
        switch (type)
        {
            case DEFAULT: return DEBUG;
            case DEBUG: return DISABLED;
            default: return DEFAULT;
        }
    }
    
    /** Used by the config GUI to cycle through the available rendering options */
    public static ERendererType previous(ERendererType type)
    {
        switch (type)
        {
            case DEFAULT: return DISABLED;
            case DEBUG: return DEFAULT;
            default: return DEBUG;
        }
    }
    
}
