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

package com.seibel.distanthorizons.core.sql;

import com.seibel.distanthorizons.core.pos.DhSectionPos;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public abstract class AbstractMetaDataRepo extends AbstractDhRepo<MetaDataDto>
{
	public AbstractMetaDataRepo(String databaseType, String databaseLocation) throws SQLException
	{
		super(databaseType, databaseLocation, MetaDataDto.class);
	}
	
	
	
	@Override 
	public String getPrimaryKeyName() { return "DhSectionPos"; }
	
	
	@Override 
	public MetaDataDto convertDictionaryToDto(Map<String, Object> objectMap) throws ClassCastException
	{
		String posString = (String) objectMap.get("DhSectionPos");
		DhSectionPos pos = DhSectionPos.deserialize(posString);
		
		String dataString = (String) objectMap.get("Data");
		int byteLength = dataString.length() - dataString.replace(",", "").length();
		byte[] dataByteArray = new byte[byteLength];
		
		String[] byteStrings = dataString.split(",");
		for (int i = 0; i < byteLength; i++)
		{
			dataByteArray[i] = Byte.parseByte(byteStrings[i]);
		}
		
		
		MetaDataDto metaFile = new MetaDataDto(pos, dataByteArray);
		return metaFile;
	}
	
	@Override 
	public String createSelectPrimaryKeySql(String primaryKey) { return "SELECT * FROM "+this.getTableName()+" WHERE DhSectionPos = '"+primaryKey+"'"; }
	
	@Override 
	public String createInsertSql(MetaDataDto dto)
	{
		String pos = dto.pos.serialize();
		
		StringBuilder dataStringBuilder = new StringBuilder();
		for (byte b : dto.dataArray)
		{
			dataStringBuilder.append(b).append(',');
		}
		String dataString = (dataStringBuilder.length() != 0) ? ("'"+dataStringBuilder.toString()+"'") : "NULL";
		
		return 
			"INSERT INTO "+this.getTableName()+" (DhSectionPos, Data) " +
			"VALUES('"+pos+"',"+dataString+");";
	}
	
	@Override 
	public String createUpdateSql(MetaDataDto dto)
	{
		String pos = dto.pos.serialize();
		
		StringBuilder dataStringBuilder = new StringBuilder();
		for (byte b : dto.dataArray)
		{
			dataStringBuilder.append(b).append(',');
		}
		String dataString = (dataStringBuilder.length() != 0) ? ("'"+dataStringBuilder.toString()+"'") : "NULL";
		
		return
			"UPDATE "+this.getTableName()+" " +
			"SET Data = "+dataString +
			"WHERE DhSectionPos = '"+pos+"'";
	}
	
	
}
