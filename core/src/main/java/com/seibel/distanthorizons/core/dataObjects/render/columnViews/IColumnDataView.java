/*
 *    This file is part of the Distant Horizons mod
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2023 James Seibel
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

package com.seibel.distanthorizons.core.dataObjects.render.columnViews;

import java.util.Iterator;

public interface IColumnDataView
{
	long get(int index);
	
	int size();
	
	default Iterator<Long> iterator()
	{
		return new Iterator<Long>()
		{
			private int index = 0;
			private final int size = size();
			
			@Override
			public boolean hasNext() { return this.index < this.size; }
			
			@Override
			public Long next() { return get(this.index++); }
			
		};
	}
	
	int verticalSize();
	
	int dataCount();
	
	IColumnDataView subView(int dataIndexStart, int dataCount);
	
	void copyTo(long[] target, int offset, int count);
	
}
