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
 
package com.seibel.lod.core.util.gridList;

import java.util.ArrayList;
import java.util.function.Consumer;

/*Layout:
 * 0,1,2,
 * 3,4,5,
 * 6,7,8
 */

@Deprecated // Replace with PosArrayGridList<T>
public class MovableCenteredGridList<T> extends ArrayList<T> {

	private int centerX;
	private int centerY;
	
	public final int gridCentreToEdge;
	public final int gridSize;

	/*
	 * WARNING: Not yet tested if its atomic. (non Thread safe)
	 */
	public MovableCenteredGridList(MovableCenteredGridList<T> other) {
		super(other);
		centerX = other.centerX;
		centerY = other.centerY;
		gridCentreToEdge = other.gridCentreToEdge;
		gridSize = other.gridSize;
	}
	
	public MovableCenteredGridList(int gridCentreToEdge, int centerX, int centerY) {
		super((gridCentreToEdge * 2 + 1) * (gridCentreToEdge * 2 + 1));
		gridSize = gridCentreToEdge * 2 + 1;
		this.gridCentreToEdge = gridCentreToEdge;
		this.centerX = centerX;
		this.centerY = centerY;
		clear();
	}
	
	@Override
	public void clear() {
		super.clear();
		super.ensureCapacity(gridSize*gridSize);
		for (int i=0; i<gridSize*gridSize; i++) {
			super.add(null);
		}
	}
	public void clear(Consumer<? super T> d) {
		super.forEach((t) -> {
			if (t!=null) d.accept(t);
		});
		super.clear();
		super.ensureCapacity(gridSize*gridSize);
		for (int i=0; i<gridSize*gridSize; i++) {
			super.add(null);
		}
	}
	
	public int getCenterX() {return centerX;}
	public int getCenterY() {return centerY;}

	// return null if x,y is outside of the grid
	public T get(int x, int y) {
		x = x-centerX+gridCentreToEdge;
		y = y-centerY+gridCentreToEdge;
		return _getDirect(x,y);
	}
	
	// return false if x,y is outside of the grid
	public boolean set(int x, int y, T t) {
		x = x-centerX+gridCentreToEdge;
		y = y-centerY+gridCentreToEdge;
		return _setDirect(x,y, t);
	}
	
	// return null if x,y is outside of the grid
	// Otherwise, return the new value (for chaining)
	public T setAndGet(int x, int y, T t) {
		x = x-centerX+gridCentreToEdge;
		y = y-centerY+gridCentreToEdge;
		return _setDirect(x,y, t) ? t : null;
	}
	// return null if x,y is outside of the grid
	// Otherwise, return the old value
	public T swap(int x, int y, T t) {
		x = x-centerX+gridCentreToEdge;
		y = y-centerY+gridCentreToEdge;
		return _swapDirect(x,y, t);
	}
	
	public boolean inRange(int x, int y) {
		x = x-centerX+gridCentreToEdge;
		y = y-centerY+gridCentreToEdge;
		return (x>=0 && x<gridSize && y>=0 && y<gridSize);
	} 
	
	private T _getDirect(int x, int y) {
		if (x<0 || x>=gridSize || y<0 || y>=gridSize) return null;
		return super.get(x + y * gridSize);
	}
	private boolean _setDirect(int x, int y, T t) {
		if (x<0 || x>=gridSize || y<0 || y>=gridSize) return false;
		super.set(x + y * gridSize, t);
		return true;
	}
	private T _swapDirect(int x, int y, T t) {
		if (x<0 || x>=gridSize || y<0 || y>=gridSize) return null;
		return super.set(x + y * gridSize, t);
	}
	
	// Return false if haven't changed. Return true if it did
	public boolean move(int newCenterX, int newCenterY) {
		if (centerX == newCenterX && centerY == newCenterY) return false;
		int deltaX = newCenterX - centerX;
		int deltaY = newCenterY - centerY;
		
		// if the x or z offset is equal to or greater than
		// the total width, just delete the current data
		// and update the centerX and/or centerZ
		if (Math.abs(deltaX) >= gridSize || Math.abs(deltaY) >= gridSize)
		{
			clear();
			// update the new center
			centerX = newCenterX;
			centerY = newCenterY;
			return true;
		}
		centerX = newCenterX;
		centerY = newCenterY;
	
		// X
		if (deltaX >= 0 && deltaY >= 0)
		{
			// move everything over to the left-up (as the center moves to the right-down)
			for (int x = 0; x < gridSize; x++)
			{
				for (int y = 0; y < gridSize; y++)
				{
					_setDirect(x, y, _getDirect(x+deltaX, y+deltaY));
				}
			}
		}
		else if (deltaX < 0 && deltaY >= 0)
		{
			// move everything over to the right-up (as the center moves to the left-down)
			for (int x = gridSize - 1; x >= 0; x--)
			{
				for (int y = 0; y < gridSize; y++)
				{
					_setDirect(x, y, _getDirect(x+deltaX, y+deltaY));
				}
			}
		}
		else if (deltaX >= 0 && deltaY < 0)
		{
			// move everything over to the left-down (as the center moves to the right-up)
			for (int x = 0; x < gridSize; x++)
			{
				for (int y = gridSize - 1; y >= 0; y--)
				{
					_setDirect(x, y, _getDirect(x+deltaX, y+deltaY));
				}
			}
		}
		else //if (deltaX < 0 && deltaY < 0)
		{
			// move everything over to the right-down (as the center moves to the left-up)
			for (int x = gridSize - 1; x >= 0; x--)
			{
				for (int y = gridSize - 1; y >= 0; y--)
				{
					_setDirect(x, y, _getDirect(x+deltaX, y+deltaY));
				}
			}
		}
		return true;
	}

	public boolean move(int newCenterX, int newCenterY, Consumer<? super T> d) {
		if (centerX == newCenterX && centerY == newCenterY) return false;
		int deltaX = newCenterX - centerX;
		int deltaY = newCenterY - centerY;
		
		// if the x or z offset is equal to or greater than
		// the total width, just delete the current data
		// and update the centerX and/or centerZ
		if (Math.abs(deltaX) >= gridSize || Math.abs(deltaY) >= gridSize)
		{
			clear(d);
			// update the new center
			centerX = newCenterX;
			centerY = newCenterY;
			return true;
		}
		centerX = newCenterX;
		centerY = newCenterY;
		
		// Dealloc stuff
		for (int x=0; x<gridSize; x++) {
			for (int y=0; y<gridSize; y++) {
				if (x-deltaX<0 || y-deltaY<0 ||
					x-deltaX>=gridSize || y-deltaY>=gridSize) {
					T t = _getDirect(x,y);
					if (t!=null) d.accept(t);
				}
			}
		}
	
		// X
		if (deltaX >= 0 && deltaY >= 0)
		{
			// move everything over to the left-up (as the center moves to the right-down)
			for (int x = 0; x < gridSize; x++)
			{
				for (int y = 0; y < gridSize; y++)
				{
					_setDirect(x, y, _getDirect(x+deltaX, y+deltaY));
				}
			}
		}
		else if (deltaX < 0 && deltaY >= 0)
		{
			// move everything over to the right-up (as the center moves to the left-down)
			for (int x = gridSize - 1; x >= 0; x--)
			{
				for (int y = 0; y < gridSize; y++)
				{
					_setDirect(x, y, _getDirect(x+deltaX, y+deltaY));
				}
			}
		}
		else if (deltaX >= 0 && deltaY < 0)
		{
			// move everything over to the left-down (as the center moves to the right-up)
			for (int x = 0; x < gridSize; x++)
			{
				for (int y = gridSize - 1; y >= 0; y--)
				{
					_setDirect(x, y, _getDirect(x+deltaX, y+deltaY));
				}
			}
		}
		else //if (deltaX < 0 && deltaY < 0)
		{
			// move everything over to the right-down (as the center moves to the left-up)
			for (int x = gridSize - 1; x >= 0; x--)
			{
				for (int y = gridSize - 1; y >= 0; y--)
				{
					_setDirect(x, y, _getDirect(x+deltaX, y+deltaY));
				}
			}
		}
		return true;
	}


	// TODO: This is unused but may be useful later on.
	/*
	public final MovableGridList<T> subGrid(int gridCentreToEdge, int newCenterX, int newCenterY) {
	}*/

	@Override
	public String toString() {
		return "MovableGridList[" + centerX + "," + centerY + "] " + gridSize + "*" + gridSize + "[" + size() + "]";
	}

	public String toDetailString() {
		StringBuilder str = new StringBuilder("\n");
		int i = 0;
		str.append(this);
		str.append("\n");
		for (T t : this) {
			
			str.append(t!=null ? t.toString() : "NULL");
			str.append(", ");
			i++;
			if (i % gridSize == 0) {
				str.append("\n");
			}
		}
		return str.toString();
	}
}
