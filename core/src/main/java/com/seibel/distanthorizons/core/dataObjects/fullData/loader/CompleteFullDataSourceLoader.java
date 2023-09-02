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

package com.seibel.distanthorizons.core.dataObjects.fullData.loader;

import com.seibel.distanthorizons.core.dataObjects.fullData.sources.interfaces.IFullDataSource;
import com.seibel.distanthorizons.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.distanthorizons.core.level.IDhLevel;
import com.seibel.distanthorizons.core.util.objects.dataStreams.DhDataInputStream;
import com.seibel.distanthorizons.core.dataObjects.fullData.sources.CompleteFullDataSource;

import java.io.IOException;

public class CompleteFullDataSourceLoader extends AbstractFullDataSourceLoader
{
	public CompleteFullDataSourceLoader()
	{
		super(CompleteFullDataSource.class, CompleteFullDataSource.TYPE_ID, new byte[]{CompleteFullDataSource.DATA_FORMAT_VERSION});
	}
	
	@Override
	public IFullDataSource loadData(FullDataMetaFile dataFile, DhDataInputStream inputStream, IDhLevel level) throws IOException, InterruptedException
	{
		CompleteFullDataSource dataSource = CompleteFullDataSource.createEmpty(dataFile.pos);
		dataSource.populateFromStream(dataFile, inputStream, level);
		return dataSource;
	}
	
}
