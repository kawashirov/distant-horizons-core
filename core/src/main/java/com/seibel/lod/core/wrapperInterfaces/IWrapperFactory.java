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

package com.seibel.lod.core.wrapperInterfaces;

import com.seibel.lod.core.level.IDhLevel;
import com.seibel.lod.core.interfaces.dependencyInjection.IBindable;
import com.seibel.lod.core.wrapperInterfaces.block.IBlockStateWrapper;
import com.seibel.lod.core.wrapperInterfaces.world.IBiomeWrapper;
import com.seibel.lod.core.wrapperInterfaces.worldGeneration.AbstractBatchGenerationEnvionmentWrapper;

import java.io.IOException;

/**
 * This handles creating abstract wrapper objects.
 * 
 * @author James Seibel
 * @version 3-5-2022
 */
public interface IWrapperFactory extends IBindable 
{
	AbstractBatchGenerationEnvionmentWrapper createBatchGenerator(IDhLevel targetLevel);
	IBiomeWrapper deserializeBiomeWrapper(String str) throws IOException;
	IBlockStateWrapper deserializeBlockStateWrapper(String str) throws IOException;
	IBlockStateWrapper getAirBlockStateWrapper();
}
