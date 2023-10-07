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

import com.seibel.distanthorizons.core.sql.AbstractDhRepo;

import java.sql.SQLException;
import java.util.Map;

public class TestDataRepo extends AbstractDhRepo<TestDto>
{
	
	public TestDataRepo(String databaseType, String databaseLocation) throws SQLException
	{
		super(databaseType, databaseLocation, TestDto.class);
		
		// note: this should only ever be done with the test repo.
		// All long term tables should be created using a sql Script.
		String createTableSql = 
				"CREATE TABLE IF NOT EXISTS "+this.getTableName()+"(\n" +
				"Id INT NOT NULL PRIMARY KEY\n" +
				"\n" +
				",Value TEXT NULL\n" +
				",LongValue BIGINT NULL\n" +
				",ByteValue TINYINT NULL\n" +
				");";
		this.queryDictionaryFirst(createTableSql);
	}
	
	
	
	@Override
	public String getTableName() { return "Test"; }
	@Override
	public String getPrimaryKeyName() { return "Id"; }
	
	
	@Override 
	public TestDto convertDictionaryToDto(Map<String, Object> objectMap) throws ClassCastException
	{
		int id = (int) objectMap.get("Id");
		String value = (String) objectMap.get("Value");
		long longValue = (Long) objectMap.get("LongValue");
		byte byteValue = (Byte) objectMap.get("ByteValue");
		
		return new TestDto(id, value, longValue, byteValue);
	}
	
	@Override 
	public String createSelectPrimaryKeySql(String primaryKey) { return "SELECT * FROM "+this.getTableName()+" WHERE Id = '"+primaryKey+"'"; }
	
	@Override 
	public String createInsertSql(TestDto dto)
	{
		String id = dto.id+"";
		String value = (dto.value != null) ? dto.value+"" : "NULL";
		
		return 
			"INSERT INTO "+this.getTableName()+" (Id, Value, LongValue, ByteValue) " +
			"VALUES("+id+",'"+value+"',"+dto.longValue+","+dto.byteValue+");";
	}
	
	@Override 
	public String createUpdateSql(TestDto dto)
	{
		String id = dto.id+"";
		String value = (dto.value != null) ? dto.value+"" : "NULL";
		
		return
			"UPDATE "+this.getTableName()+" " +
			"SET " +
			"   Value = '"+value+"' \n" +
			"   ,LongValue = "+dto.longValue + " \n" +
			"   ,ByteValue = "+dto.byteValue + " \n" +
			"WHERE Id = "+id;
	}
	
}
