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

import com.seibel.distanthorizons.core.logging.DhLoggerBuilder;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.junit.Assert;

import java.sql.*;
import java.util.*;

/**
 * Handles interfacing with SQL databases.
 * 
 * @param <TDTO> DTO stands for "Data Table Object" 
 */
public abstract class AbstractDhRepo<TDTO extends IBaseDTO>
{
	public static final int TIMEOUT_SECONDS = 30;
	
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	private final Connection connection;
	
	public final String databaseType;
	public final String databaseLocation;
	
	public final Class<? extends TDTO> dtoClass;
	
	
	
	//=============//
	// constructor //
	//=============//
	
	public AbstractDhRepo(String databaseType, String databaseLocation, Class<? extends TDTO> dtoClass) throws SQLException
	{
		this.databaseType = databaseType;
		this.databaseLocation = databaseLocation;
		this.dtoClass = dtoClass;
		
		this.connection = DriverManager.getConnection(this.databaseType+":"+this.databaseLocation);
		
		DatabaseUpdater.runAutoUpdateScripts(this);
	}
	
	
	
	//===============//
	// high level DB //
	//===============//
	
	public TDTO get(TDTO dto) { return this.getByPrimaryKey(dto.getPrimaryKeyString()); }
	public TDTO getByPrimaryKey(String primaryKey)
	{
		Map<String, Object> objectMap = this.queryDictionaryFirst(this.createSelectPrimaryKeySql(primaryKey));
		if (objectMap != null && !objectMap.isEmpty())
		{
			return this.convertDictionaryToDto(objectMap);
		}
		else
		{
			return null;
		}
	}
	
	public void save(TDTO dto)
	{
		if (this.getByPrimaryKey(dto.getPrimaryKeyString()) != null)
		{
			this.update(dto);
		}
		else
		{
			this.insert(dto);
		}
	}
	private void insert(TDTO dto) { this.queryDictionaryFirst(this.createInsertSql(dto)); }
	private void update(TDTO dto) { this.queryDictionaryFirst(this.createUpdateSql(dto)); }
	
	public void delete(TDTO dto) { this.deleteByPrimaryKey(dto.getPrimaryKeyString()); }
	public void deleteByPrimaryKey(String primaryKey) 
	{
		String whereEqualStatement = this.createWherePrimaryKeyStatement(primaryKey);
		this.queryDictionaryFirst("DELETE FROM "+this.getTableName()+" WHERE "+whereEqualStatement); 
	}
	
	public boolean exists(TDTO dto) { return this.existsWithPrimaryKey(dto.getPrimaryKeyString()); }
	public boolean existsWithPrimaryKey(String primaryKey) 
	{
		String whereEqualStatement = this.createWherePrimaryKeyStatement(primaryKey);
		Map<String, Object> result = this.queryDictionaryFirst("SELECT EXISTS(SELECT 1 FROM "+this.getTableName()+" WHERE "+whereEqualStatement+") as 'existingCount';"); 
		return result != null && (int)result.get("existingCount") != 0;
	}
	
	
	//==============//
	// low level DB //
	//==============//
	
	public List<Map<String, Object>> queryDictionary(String sql) { return this.query(sql); }
	@Nullable
	public Map<String, Object> queryDictionaryFirst(String sql) 
	{
		List<Map<String, Object>> objectList = this.query(sql);
		return (objectList != null && !objectList.isEmpty()) ? objectList.get(0) : null;
	}
	
	/** note: this can only handle 1 command at a time */
	private List<Map<String, Object>> query(String sql) throws RuntimeException
	{
		try (Statement statement = this.connection.createStatement())
		{
			statement.setQueryTimeout(TIMEOUT_SECONDS);
			
			// Note: this can only handle 1 command at a time
			boolean resultSetPresent = statement.execute(sql);
			ResultSet resultSet = statement.getResultSet();
			if (resultSetPresent)
			{
				List<Map<String, Object>> resultList = convertResultSetToDictionaryList(resultSet);
				resultSet.close();
				return resultList;
			}
			else
			{
				if (resultSet != null)
				{
					resultSet.close();
				}
				
				return new ArrayList<>();
			}
		}
		catch(SQLException e)
		{
			// SQL exceptions generally only happen when something is wrong with 
			// the database or the query and should cause the system to blow up to notify the developer
			throw new RuntimeException("Unexpected Query error: ["+e.getMessage()+"], for script: ["+sql+"].", e);
		}
	}
	
	public PreparedStatement createPreparedStatement(String sql)
	{
		try
		{
			PreparedStatement statement = this.connection.prepareStatement(sql);
			statement.setQueryTimeout(TIMEOUT_SECONDS);
			return statement;
		}
		catch(SQLException e)
		{
			// SQL exceptions generally only happen when something is wrong with 
			// the database or the query and should cause the system to blow up to notify the developer
			throw new RuntimeException(e);
		}
	}
	
	
	
	public void close()
	{
		try
		{
			if(this.connection != null)
			{
				this.connection.close();
			}
		}
		catch(SQLException e)
		{
			// connection close failed.
			Assert.fail("Unable to close the connection: " + e.getMessage());
		}
	}
	
	
	
	//================//
	// helper methods //
	//================//
	
	/** Example: <code> Id = '0' </code> */
	public String createWherePrimaryKeyStatement(TDTO dto) { return this.createWherePrimaryKeyStatement(dto.getPrimaryKeyString()); }
	/** Example: <code> Id = '0' </code> */
	public String createWherePrimaryKeyStatement(String primaryKeyValue) { return this.getPrimaryKeyName()+" = '"+primaryKeyValue+"'"; }
	
	public static List<Map<String, Object>> convertResultSetToDictionaryList(ResultSet resultSet) throws SQLException
	{
		List<Map<String, Object>> list = new ArrayList<>();
		
		ResultSetMetaData resultMetaData = resultSet.getMetaData();
		int resultColumnCount = resultMetaData.getColumnCount();
		
		while (resultSet.next())
		{
			HashMap<String, Object> object = new HashMap<>();
			for (int columnIndex = 1; columnIndex <= resultColumnCount; columnIndex++) // column indices start at 1
			{
				String columnName = resultMetaData.getColumnName(columnIndex);
				if (columnName == null || columnName.equals(""))
				{
					throw new RuntimeException("SQL result set is missing a column name for column ["+resultMetaData.getTableName(columnIndex)+"."+columnIndex+"].");
				}
				
				Object columnValue = resultSet.getObject(columnIndex);
				
				object.put(columnName, columnValue);
			}
			
			list.add(object);
		}
		
		return list;
	}
	
	
	
	//==================//
	// abstract methods //
	//==================//
	
	public abstract String getTableName();
	public abstract String getPrimaryKeyName();
	
	@Nullable
	public abstract TDTO convertDictionaryToDto(Map<String, Object> objectMap) throws ClassCastException;
	
	public abstract String createSelectPrimaryKeySql(String primaryKey);
	
	public abstract String createInsertSql(TDTO dto);
	public abstract String createUpdateSql(TDTO dto);
	
	public abstract String createDeleteSql(TDTO dto);
	
}
