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

import com.seibel.lod.core.objects.Pos2D;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

public class MovableGridRingList<T> extends ArrayList<T> implements List<T> {
	
	private AtomicReference<Pos2D> pos = new AtomicReference<Pos2D>();
	
	private final int halfSize;
	private final int size;
	private final ReentrantReadWriteLock moveLock = new ReentrantReadWriteLock();

	public MovableGridRingList(int halfSize, int centerX, int centerY) {
		super((halfSize * 2 + 1) * (halfSize * 2 + 1));
		size = halfSize * 2 + 1;
		this.halfSize = halfSize;
		pos.set(new Pos2D(centerX-halfSize, centerY-halfSize));
		clear();
	}
	
	@Override
	public void clear() {
		clear(null);
	}
	
	public void clear(Consumer<? super T> d) {
		moveLock.writeLock().lock();
		try {
			if (d != null) {
				super.forEach((t) -> {
					if (t!=null) d.accept(t);
				});
			}
			super.clear();
			super.ensureCapacity(size*size);
			for (int i=0; i<size*size; i++) {
				super.add(null);
			}
		} finally {
			moveLock.writeLock().unlock();
		}
	}
	
	public Pos2D getCenter() {
		Pos2D bottom = pos.get();
		return new Pos2D(bottom.x+halfSize, bottom.y+halfSize);
	}
	public Pos2D getMinInRange() {
		return pos.get();
	}
	public Pos2D getMaxInRange() {
		Pos2D bottom = pos.get();
		return new Pos2D(bottom.x+size-1, bottom.y+size-1);
	}
	public int getSize() {return size;}
	public int getHalfSize() {return halfSize;}

	// WARNNING! Be careful with race condition!
	// The grid may get moved after this query!
	public boolean inRange(int x, int y) {
		Pos2D min = pos.get();
		return (x>=min.x && x<min.x+size && y>=min.y && y<min.y+size);
	}
	private boolean _inRangeAquired(int x, int y, Pos2D min) {
		return (x>=min.x && x<min.x+size && y>=min.y && y<min.y+size);
	}
	private T _getUnsafe(int x, int y) {
		return super.get(Math.floorMod(x, size) + Math.floorMod(y, size)*size);
	}
	private void _setUnsafe(int x, int y, T t) {
		super.set(Math.floorMod(x, size) + Math.floorMod(y, size)*size, t);
	}
	private T _swapUnsafe(int x, int y, T t) {
		return super.set(Math.floorMod(x, size) + Math.floorMod(y, size)*size, t);
	}

	// return null if x,y is outside of the grid
	public T get(int x, int y) {
		Pos2D min = pos.get();
		if (!_inRangeAquired(x, y, min)) return null;
		moveLock.readLock().lock();
		try {
			Pos2D newMin = pos.get();
			// Use EXECT compare here
			if (min!=newMin)
				if (!_inRangeAquired(x, y, newMin)) return null;
			return _getUnsafe(x, y);
		} finally {
			moveLock.readLock().unlock();
		}
	}
	
	// return false if x,y is outside of the grid
	public boolean set(int x, int y, T t) {
		Pos2D min = pos.get();
		if (!_inRangeAquired(x, y, min)) return false;
		moveLock.readLock().lock();
		try {
			Pos2D newMin = pos.get();
			// Use EXECT compare here
			if (min!=newMin)
				if (!_inRangeAquired(x, y, newMin)) return false;
			_setUnsafe(x, y, t);
			return true;
		} finally {
			moveLock.readLock().unlock();
		}
	}
	
	// return input t if x,y is outside of the grid
	public T swap(int x, int y, T t) {
		Pos2D min = pos.get();
		if (!_inRangeAquired(x, y, min)) return t;
		moveLock.readLock().lock();
		try {
			Pos2D newMin = pos.get();
			// Use EXECT compare here
			if (min!=newMin)
				if (!_inRangeAquired(x, y, newMin)) return t;
			return _swapUnsafe(x, y, t);
		} finally {
			moveLock.readLock().unlock();
		}
	}

	// TODO: Impl this
	/*
	// do a compare and set
	public boolean compareAndSet(int x, int y, T expected, T toBeSet) {
		Pos min = pos.get();
		if (!_inRangeAquired(x, y, min)) return false;
		moveLock.readLock().lock();
		try {
			Pos newMin = pos.get();
			// Use EXECT compare here
			if (min!=newMin)
				if (!_inRangeAquired(x, y, newMin)) return false;
			return _compareAndSetUnsafe(x, y, expected, toBeSet);
		} finally {
			moveLock.readLock().unlock();
		}
	}*/
	
	// return null if x,y is outside of the grid
	// Otherwise, return the new value (for chaining)
	public T setChained(int x, int y, T t) {
		return set(x,y,t) ? t : null;
	}
	
	// Return false if haven't changed. Return true if it did
	public boolean move(int newCenterX, int newCenterY) {
		return move(newCenterX, newCenterY, null);
	}

	public boolean move(int newCenterX, int newCenterY, Consumer<? super T> d) {
		Pos2D cPos = pos.get();
		int newMinX = newCenterX - halfSize;
		int newMinY = newCenterY - halfSize;
		if (cPos.x == newMinX && cPos.y == newMinY)
			return false;
		moveLock.writeLock().lock();
		try {
			cPos = pos.get();
			int deltaX = newMinX - cPos.x;
			int deltaY = newMinY - cPos.y;
			if (deltaX == 0 && deltaY == 0)
				return false;
			// if the x or z offset is equal to or greater than
			// the total width, just delete the current data
			// and update the pos
			if (Math.abs(deltaX) >= size || Math.abs(deltaY) >= size) {
				clear(d);
			} else {
				for (int x = 0; x < size; x++) {
					for (int y = 0; y < size; y++) {
						if (x - deltaX < 0 || y - deltaY < 0 || x - deltaX >= size || y - deltaY >= size) {
							T t = _swapUnsafe(x + cPos.x, y + cPos.y, null);
							if (t != null && d != null)
								d.accept(t);
						}
					}
				}
			}
			pos.set(new Pos2D(newMinX, newMinY));
			return true;
		} finally {
			moveLock.writeLock().unlock();
		}
	}

	@Override
	public String toString() {
		Pos2D p = pos.get();
		return "MovabeGridRingList[" + p.x+halfSize + "," + p.y+halfSize + "] " + size + "*" + size + "[" + size() + "]";
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
			if (i % size == 0) {
				str.append("\n");
			}
		}
		return str.toString();
	}
}
