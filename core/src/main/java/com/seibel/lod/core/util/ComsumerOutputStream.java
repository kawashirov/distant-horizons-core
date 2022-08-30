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
 
package com.seibel.lod.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

public final class ComsumerOutputStream extends OutputStream
{
	final Consumer<String> func;
	private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	
	public ComsumerOutputStream(Consumer<String> func) {
		this.func = func;
	}
	@Override
	public void write(int b)
	{
		buffer.write(b);
	}
	
	@Override
	public void write(byte[] b) throws IOException {
		buffer.write(b);
	}
	@Override
	public void write(byte[] b,
	         int off,
	         int len) {
		buffer.write(b, off, len);
	}
	
	@Override
	public void flush()
	{
		String str = buffer.toString();
		buffer.reset();
		func.accept(str);
	}
	
	@Override
	public void close() throws IOException
	{
		buffer.close();
	}
}
