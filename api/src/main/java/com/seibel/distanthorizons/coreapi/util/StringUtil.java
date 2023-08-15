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

package com.seibel.distanthorizons.coreapi.util;

/**
 * Miscellaneous string helper functions.
 *
 * @author James Seibel
 * @version 2022-7-19
 */
public class StringUtil
{
	/**
	 * Returns the n-th index of the given string. <br> <br>
	 *
	 * Original source: https://stackoverflow.com/questions/3976616/how-to-find-nth-occurrence-of-character-in-a-string
	 */
	public static int nthIndexOf(String str, String substr, int n)
	{
		int pos = str.indexOf(substr);
		while (--n > 0 && pos != -1)
		{
			pos = str.indexOf(substr, pos + 1);
		}
		return pos;
	}
	
	/** Combines each item in the given list together separated by the given delimiter. */
	public static <T> String join(String delimiter, Iterable<T> list)
	{
		StringBuilder stringBuilder = new StringBuilder();
		
		boolean firstItem = true;
		for (T item : list)
		{
			if (!firstItem)
			{
				stringBuilder.append(delimiter);
			}
			
			stringBuilder.append(item);
			firstItem = false;
		}
		
		return stringBuilder.toString();
	}
	
	
}
