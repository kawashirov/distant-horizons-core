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

import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiWorldGenerationStep;
import com.seibel.distanthorizons.core.file.metaData.BaseMetaData;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.coreapi.util.StringUtil;

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
		
		// meta data
		int checksum = (Integer) objectMap.get("Checksum");
		long dataVersion = (Long) objectMap.get("DataVersion");
		byte dataDetailLevel = (Byte) objectMap.get("DataDetailLevel");
		String worldGenStepString = (String) objectMap.get("WorldGenStep");
		EDhApiWorldGenerationStep worldGenStep = EDhApiWorldGenerationStep.fromName(worldGenStepString);
		
		String dataType = (String) objectMap.get("DataType");
		byte binaryDataFormatVersion = (Byte) objectMap.get("BinaryDataFormatVersion");
		
		BaseMetaData baseMetaData = new BaseMetaData(pos, 
				checksum, dataDetailLevel, worldGenStep,
				dataType, binaryDataFormatVersion, dataVersion);
		
		// binary data
		byte[] dataByteArray = (byte[]) objectMap.get("Data");
		
		MetaDataDto metaFile = new MetaDataDto(baseMetaData, dataByteArray);
		return metaFile;
	}
	
	@Override 
	public String createSelectPrimaryKeySql(String primaryKey) { return "SELECT * FROM "+this.getTableName()+" WHERE DhSectionPos = '"+primaryKey+"'"; }
	
	@Override 
	public String createInsertSql(MetaDataDto dto)
	{
		String posString = dto.baseMetaData.pos.serialize();
		String dataHexString = createDataHexString(dto);
		return 
			"INSERT INTO "+this.getTableName() + "\n" +
			"  (DhSectionPos, \n" +
				"Checksum, DataVersion, DataDetailLevel, WorldGenStep, DataType, BinaryDataFormatVersion, \n" +
				"Data) \n" +
			"   VALUES( \n" +
			"    '"+posString+"' \n" +
					
			"   ,"+dto.baseMetaData.checksum + "\n" +
			"   ,"+dto.baseMetaData.dataVersion + "\n" +
			"   ,"+dto.baseMetaData.dataDetailLevel + "\n" +
			"   ,'"+dto.baseMetaData.worldGenStep.name + "' \n" +
			"   ,'"+dto.baseMetaData.dataType + "' \n" +
			"   ,"+dto.baseMetaData.binaryDataFormatVersion + "\n" +
					
			"   ,"+dataHexString + "\n" +
			// created/lastModified are automatically set by Sqlite
			");";
	}
	
	@Override 
	public String createUpdateSql(MetaDataDto dto)
	{
		String posString = dto.baseMetaData.pos.serialize();
		String dataHexString = createDataHexString(dto);
		return
			"UPDATE "+this.getTableName()+" \n" +
			"SET \n" +
					
			"    Checksum = "+dto.baseMetaData.checksum + "\n" +
			"   ,DataVersion = "+dto.baseMetaData.dataVersion + "\n" +
			"   ,DataDetailLevel = "+dto.baseMetaData.dataDetailLevel + "\n" +
			"   ,WorldGenStep = '"+dto.baseMetaData.worldGenStep.name + "' \n" +
			"   ,DataType = '"+dto.baseMetaData.dataType + "' \n" +
			"   ,BinaryDataFormatVersion = "+dto.baseMetaData.binaryDataFormatVersion + "\n" +
					
			"   ,Data = "+dataHexString + "\n" +
					
			"   ,LastModifiedDateTime = CURRENT_TIMESTAMP \n" +
			"WHERE DhSectionPos = '"+posString+"'";
	}
	
	
	/** This creates a string that Sqlite interprets as binary data. */
	private static String createDataHexString(MetaDataDto dto) { return "X'" + StringUtil.byteArrayToHexString(dto.dataArray) + "'"; }
	
}
