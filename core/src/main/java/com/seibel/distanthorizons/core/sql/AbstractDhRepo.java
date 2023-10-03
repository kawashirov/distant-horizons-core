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
		TDTO dto = null;
		try
		{
			ResultSet resultSet = this.query(this.createSelectPrimaryKeySql(primaryKey));
			dto = this.convertResultSetToDto(resultSet);
			resultSet.close();
		}
		catch (SQLException e)
		{
			System.err.println(e.getMessage());
		}
		
		return dto;
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
	private void insert(TDTO dto) { this.queryNoResult(this.createInsertSql(dto)); }
	private void update(TDTO dto) { this.queryNoResult(this.createUpdateSql(dto)); }
	
	public void delete(TDTO dto) { this.queryNoResult(this.createDeleteSql(dto)); }
	
	
	//==============//
	// low level DB //
	//==============//
	
	public void queryNoResult(String sql) { this.query(sql, false); }
	public ResultSet query(String sql) { return this.query(sql, true); }
	
	/** note: this can only handle 1 command at a time */
	@Nullable
	private ResultSet query(String sql, boolean returnResultSet) throws RuntimeException
	{
		try
		{
			Statement statement = this.connection.createStatement();
			statement.setQueryTimeout(TIMEOUT_SECONDS);
			
			// Note: this can only handle 1 command at a time
			boolean resultSetPresent = statement.execute(sql);
			if (resultSetPresent)
			{
				ResultSet resultSet = statement.getResultSet();
				if (returnResultSet)
				{
					return resultSet;
				}
				else
				{
					resultSet.close();
					return null;
				}
			}
			else
			{
				return null;
			}
		}
		catch(SQLException e)
		{
			// SQL exceptions generally only happen when something is wrong with 
			// the database or the query and should cause the system to blow up to notify the developer
			throw new RuntimeException(e);
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
	
	public String createWherePrimaryKeyStatement(TDTO dto) { return this.createWherePrimaryKeyStatement(dto.getPrimaryKeyString()); }
	public String createWherePrimaryKeyStatement(String primaryKeyValue) { return "WHERE "+this.getPrimaryKeyName()+" = '"+primaryKeyValue+"'"; }
	
	
	
	//==================//
	// abstract methods //
	//==================//
	
	public abstract String getTableName();
	public abstract String getPrimaryKeyName();
	
	@Nullable
	public abstract TDTO convertResultSetToDto(ResultSet resultSet) throws SQLException;
	
	public abstract String createSelectPrimaryKeySql(String primaryKey);
	
	public abstract String createInsertSql(TDTO dto);
	public abstract String createUpdateSql(TDTO dto);
	
	public abstract String createDeleteSql(TDTO dto);
	
}
