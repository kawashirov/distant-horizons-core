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

import com.seibel.lod.core.pos.Pos2D;
import com.seibel.lod.core.util.LodUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class MovableGridRingList<T> extends ArrayList<T> implements List<T>
{
	
	private final AtomicReference<Pos2D> pos = new AtomicReference<>();
	
	private final int halfSize;
	private final int size;
	private final ReentrantReadWriteLock moveLock = new ReentrantReadWriteLock();
	
	private Pos2D[] ringIteratorList = null;
	
	
	
	//==============//
	// constructors //
	//==============//
	
	public MovableGridRingList(int halfSize, Pos2D center) { this(halfSize, center.x, center.y); }
	public MovableGridRingList(int halfSize, int centerX, int centerY)
	{
		super((halfSize * 2 + 1) * (halfSize * 2 + 1));
		
		this.size = halfSize * 2 + 1;
		this.halfSize = halfSize;
		this.pos.set(new Pos2D(centerX-halfSize, centerY-halfSize));
		this.clear();
	}
	
	
	/**
	 * TODO: Check if this needs to be synchronized 
	 * <br>
	 * FIXME: Make all usage of this class do stuff relative to the minPos instead of the center
	 */
	private void buildRingIteratorList()
	{
		this.ringIteratorList = null;
		Pos2D[] posArray = new Pos2D[this.size*this.size];
		
		int i = 0;
		for (int xPos = -this.halfSize; xPos <= this.halfSize; xPos++)
		{
			for (int zPos = -this.halfSize; zPos <= this.halfSize; zPos++)
			{
				posArray[i] = new Pos2D(xPos, zPos);
				i++;
			}
		}
		
		// sort the positions from nearest to farthest from the world origin
		Arrays.sort(posArray, (a, b) ->
		{
			long disSqrA = (long) a.x*a.x + (long) a.y*a.y;
			long disSqrB = (long) b.x*b.x + (long) b.y*b.y;
			return Double.compare(disSqrA, disSqrB);
		});
		
		for (int j = 0; j < posArray.length; j++)
		{
			posArray[j] = posArray[j].add(new Pos2D(this.halfSize, this.halfSize));
		}
		for (Pos2D pos2D : posArray)
		{
			LodUtil.assertTrue(pos2D.x >= 0 && pos2D.x < this.size);
			LodUtil.assertTrue(pos2D.y >= 0 && pos2D.y < this.size);
		}
		
		this.ringIteratorList = posArray;
	}
	
	
	
	
	
	@Override
	public void clear() { this.clear(null); }
	/** @param consumer the consumer run on each item before it is removed from the list */
	public void clear(Consumer<? super T> consumer)
	{
		this.moveLock.writeLock().lock();
		try
		{
			if (consumer != null)
			{
				super.forEach((t) ->
				{
					if (t != null)
					{
						consumer.accept(t);
					}
				});
			}
			
			super.clear();
			super.ensureCapacity(this.size * this.size);
			// TODO why are we filling the array will nulls? everything should already be null after the clear
			for (int i = 0; i < this.size * this.size; i++)
			{
				super.add(null);
			}
		}
		finally
		{
			this.moveLock.writeLock().unlock();
		}
	}
	
	public Pos2D getCenter()
	{
		Pos2D bottom = this.pos.get();
		return new Pos2D(bottom.x + this.halfSize, bottom.y + this.halfSize);
	}
	public Pos2D getMinInRange() { return this.pos.get(); }
	public Pos2D getMaxInRange() {
		Pos2D bottom = this.pos.get();
		return new Pos2D(bottom.x + this.size-1, bottom.y + this.size-1);
	}
	public int getSize() { return this.size; }
	public int getHalfSize() { return this.halfSize; }
	
	/**
	 * Warning: Be careful with race conditions!
	 * The grid may move after this query!
	 */
	public boolean inRange(int x, int y)
	{
		Pos2D minPos = this.pos.get();
		return (x>=minPos.x 
				&& x<minPos.x+this.size 
				&& y>=minPos.y 
				&& y<minPos.y+this.size);
	}
	private boolean _inRangeAquired(int x, int y, Pos2D min)
	{
		return (x>=min.x 
				&& x<min.x+this.size 
				&& y>=min.y 
				&& y<min.y+this.size);
	}
	private T _getUnsafe(int x, int y)
	{
		return super.get(Math.floorMod(x, this.size) + Math.floorMod(y, this.size)*this.size);
	}
	private void _setUnsafe(int x, int y, T t)
	{
		super.set(Math.floorMod(x, this.size) + Math.floorMod(y, this.size)*this.size, t);
	}
	private T _swapUnsafe(int x, int y, T t)
	{
		return super.set(Math.floorMod(x, this.size) + Math.floorMod(y, this.size)*this.size, t);
	}

	/** returns null if x,y is outside the grid */
	public T get(int x, int y)
	{
		Pos2D min = pos.get();
		if (!this._inRangeAquired(x, y, min))
		{
			return null;
		}
		
		this.moveLock.readLock().lock();
		try
		{
			Pos2D newMin = pos.get();
			// Use EXECT compare here
			if (min!=newMin)
			{
				if (!_inRangeAquired(x, y, newMin))
				{
					return null;
				}
			}
			return this._getUnsafe(x, y);
		}
		finally
		{
			this.moveLock.readLock().unlock();
		}
	}
	
	/** returns false if x,y is outside the grid */
	public boolean set(int x, int y, T t)
	{
		Pos2D min = this.pos.get();
		if (!this._inRangeAquired(x, y, min))
		{
			return false;
		}
		
		this.moveLock.readLock().lock();
		try
		{
			Pos2D newMin = this.pos.get();
			// Use EXACT compare here
			if (min!=newMin)
			{
				if (!this._inRangeAquired(x, y, newMin))
				{
					return false;
				}
			}
			this._setUnsafe(x, y, t);
			return true;
		}
		finally
		{
			this.moveLock.readLock().unlock();
		}
	}
	
	// return input t if x,y is outside of the grid
	public T swap(int x, int y, T t)
	{
		Pos2D min = this.pos.get();
		if (!this._inRangeAquired(x, y, min))
		{
			return t;
		}
		
		this.moveLock.readLock().lock();
		try
		{
			Pos2D newMin = this.pos.get();
			// Use EXACT compare here
			if (min!=newMin)
			{
				if (!this._inRangeAquired(x, y, newMin))
				{
					return t;
				}
			}
			return this._swapUnsafe(x, y, t);
		}
		finally
		{
			this.moveLock.readLock().unlock();
		}
	}
	
	public T remove(int x, int y) { return this.swap(x, y, null); }
	
	public T get(Pos2D p) { return this.get(p.x, p.y); }
	public boolean set(Pos2D p, T t) { return this.set(p.x, p.y, t); }
	public T swap(Pos2D p, T t) { return this.swap(p.x, p.y, t); }
	public T remove(Pos2D p) { return this.remove(p.x, p.y); }
	
	
	// TODO: implement this
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
	
	/**
	 * return null if x,y is outside of the grid
	 * Otherwise, return the new value (for chaining) 
	 */
	public T setChained(int x, int y, T t) { return this.set(x,y,t) ? t : null; }
	public T setChained(Pos2D p, T t) { return this.setChained(p.x, p.y, t); }
	
	/** Returns true if the grid was successfully moved, false otherwise */
	public boolean move(int newCenterX, int newCenterY) { return this.move(newCenterX, newCenterY, null); }

	public boolean move(int newCenterX, int newCenterY, Consumer<? super T> consumer)
	{
		Pos2D cPos = this.pos.get();
		int newMinX = newCenterX - this.halfSize;
		int newMinY = newCenterY - this.halfSize;
		if (cPos.x == newMinX && cPos.y == newMinY)
		{
			return false;
		}
		
		this.moveLock.writeLock().lock();
		try
		{
			cPos = this.pos.get();
			int deltaX = newMinX - cPos.x;
			int deltaY = newMinY - cPos.y;
			if (deltaX == 0 && deltaY == 0)
			{
				return false;
			}
			
			// if the x or z offset is equal to or greater than
			// the total width, just delete the current data
			// and update the pos
			if (Math.abs(deltaX) >= this.size || Math.abs(deltaY) >= this.size)
			{
				this.clear(consumer);
			}
			else
			{
				for (int x = 0; x < this.size; x++)
				{
					for (int y = 0; y < this.size; y++)
					{
						if (x - deltaX < 0 || y - deltaY < 0 || x - deltaX >= this.size || y - deltaY >= this.size)
						{
							T t = this._swapUnsafe(x + cPos.x, y + cPos.y, null);
							if (t != null && consumer != null)
							{
								consumer.accept(t);
							}
						}
					}
				}
			}
			
			this.pos.set(new Pos2D(newMinX, newMinY));
			return true;
		}
		finally
		{
			this.moveLock.writeLock().unlock();
		}
	}
	
	/**
	 * TODO: Use MutablePos2D in the future
	 * Will pass in null entries
	 */
	public void forEachPos(BiConsumer<? super T, Pos2D> consumer)
	{
		this.moveLock.readLock().lock();
		try
		{
			Pos2D min = this.pos.get();
			for (int x = min.x; x < min.x + this.size; x++)
			{
				for (int y = min.y; y < min.y + this.size; y++)
				{
					T t = this._getUnsafe(x, y);
					consumer.accept(t, new Pos2D(x, y));
				}
			}
		}
		finally
		{
			this.moveLock.readLock().unlock();
		}
	}
	
	/**
	 * TODO: Use MutablePos2D in the future
	 * Will skip null entries
	 */
	public void forEachOrdered(Consumer<? super T> consumer)
	{
		if (this.ringIteratorList == null)
		{
			this.buildRingIteratorList();
		}
		
		this.moveLock.readLock().lock();
		try
		{
			Pos2D min = this.pos.get();
			for (Pos2D offset : this.ringIteratorList)
			{
				T t = this._getUnsafe(min.x + offset.x, min.y + offset.y);
				if (t != null)
				{
					consumer.accept(t);
				}
			}
		}
		finally
		{
			this.moveLock.readLock().unlock();
		}
	}
	
	/**
	 * TODO: Use MutablePos2D in the future
	 * Will pass in null entries
	 */
	public void forEachPosOrdered(BiConsumer<? super T, Pos2D> consumer)
	{
		if (this.ringIteratorList == null)
		{
			this.buildRingIteratorList();
		}
		
		this.moveLock.readLock().lock();
		try
		{
			Pos2D min = this.pos.get();
			for (Pos2D offset : this.ringIteratorList)
			{
				LodUtil.assertTrue(this._inRangeAquired(min.x + offset.x, min.y + offset.y, min));
				T t = this._getUnsafe(min.x + offset.x, min.y + offset.y);
				consumer.accept(t, new Pos2D(min.x + offset.x, min.y + offset.y));
			}
		}
		finally
		{
			this.moveLock.readLock().unlock();
		}
	}



	@Override
	public String toString()
	{
		Pos2D p = this.pos.get();
		return this.getClass().getSimpleName() + "[" + (p.x+this.halfSize) + "," + (p.y+this.halfSize) + "] " + this.size + "*" + this.size + "[" + this.size() + "]";
	}
	
	public String toDetailString()
	{
		StringBuilder str = new StringBuilder("\n");
		int i = 0;
		str.append(this);
		str.append("\n");
		for (T t : this)
		{
			
			str.append(t != null ? t.toString() : "NULL");
			str.append(", ");
			i++;
			if (i % this.size == 0)
			{
				str.append("\n");
			}
		}
		return str.toString();
	}
	
}
