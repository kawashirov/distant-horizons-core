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

package testItems.sql;

import com.seibel.distanthorizons.api.enums.worldGeneration.EDhApiWorldGenerationStep;
import com.seibel.distanthorizons.core.file.fullDatafile.FullDataMetaFile;
import com.seibel.distanthorizons.core.file.metaData.BaseMetaData;
import com.seibel.distanthorizons.core.pos.DhSectionPos;
import com.seibel.distanthorizons.core.sql.AbstractDhRepo;

import java.sql.ResultSet;
import java.sql.SQLException;

public class TestDataRepo extends AbstractDhRepo<TestDto>
{
	
	public TestDataRepo(String databaseType, String databaseLocation) throws SQLException
	{
		super(databaseType, databaseLocation, TestDto.class);
		
		// note: this should only ever be done with the test repo.
		// All long term tables should be created using a sql Script.
		String createTableSql = 
				"CREATE TABLE "+this.getTableName()+"(\n" +
				"Id INT NOT NULL PRIMARY KEY\n" +
				"\n" +
				",Value TEXT NULL\n" +
				");";
		this.queryNoResult(createTableSql);
	}
	
	
	
	@Override
	public String getTableName() { return "Test"; }
	@Override
	public String getPrimaryKeyName() { return "Id"; }
	
	
	@Override 
	public TestDto convertResultSetToDto(ResultSet resultSet) throws SQLException
	{
		if (!resultSet.next())
		{
			return null;
		}
		
		
		String idString = resultSet.getString("Id");
		int id = Integer.parseInt(idString);
		
		String value = resultSet.getString("Value");
		
		return new TestDto(id, value);
	}
	
	@Override 
	public String createSelectPrimaryKeySql(String primaryKey) { return "SELECT * FROM "+this.getTableName()+" WHERE Id = '"+primaryKey+"'"; }
	
	@Override 
	public String createInsertSql(TestDto dto)
	{
		String id = dto.id+"";
		String value = (dto.value != null) ? dto.value+"" : "NULL";
		
		return 
			"INSERT INTO "+this.getTableName()+" (Id, Value) " +
			"VALUES("+id+",'"+value+"');";
	}
	
	@Override 
	public String createUpdateSql(TestDto dto)
	{
		String id = dto.id+"";
		String value = (dto.value != null) ? dto.value+"" : "NULL";
		
		return
			"UPDATE "+this.getTableName()+" " +
			"SET Value = '"+value+"' " +
			"WHERE Id = "+id;
	}
	
	@Override 
	public String createDeleteSql(TestDto dto)
	{
		return "DELETE FROM "+this.getTableName()+" WHERE Id = '"+dto.id+"'";
	}
	
}
