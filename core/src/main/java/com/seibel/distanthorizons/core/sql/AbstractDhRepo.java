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

import org.jetbrains.annotations.Nullable;
import org.junit.Assert;

import java.io.*;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * 
 * @param <TDTO> DTO stands for "Data Table Object" 
 */
public abstract class AbstractDhRepo<TDTO extends IBaseDTO>
{
	public static final int TIMEOUT_SECONDS = 30;
	
	private final Connection connection;
	
	public final String databaseType;
	public final String databaseLocation;
	
	public final Class<? extends TDTO> dtoClass;
	
	
	
	// constructor //
	
	public AbstractDhRepo(String databaseType, String databaseLocation, Class<? extends TDTO> dtoClass) throws SQLException
	{
		this.databaseType = databaseType;
		this.databaseLocation = databaseLocation;
		this.dtoClass = dtoClass;
		
		this.connection = DriverManager.getConnection(this.databaseType+":"+this.databaseLocation);
		
		this.runFirstTimeSetup();
	}
	private void runFirstTimeSetup()
	{
		// get all sql scripts
		ArrayList<String> sqlScriptNames = new ArrayList<>(); 
		try
		{
			Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources("sqlScripts"); // package/name/with/slashes/instead/dots
			while (resources.hasMoreElements())
			{
				URL url = resources.nextElement();
				
				String fileName = new Scanner((InputStream) url.getContent()).useDelimiter("\\A").next();
				fileName = fileName.trim();
				
				if (fileName.endsWith(".sql"))
				{
					sqlScriptNames.add(fileName);
				}
			}
		}
		catch (IOException e)
		{
			
		}
		
		// attempt to run them
		for (String scriptName : sqlScriptNames)
		{
			InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("sqlScripts/"+scriptName);
			if (inputStream != null)
			{
				String script = new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
				System.out.println("Running script: "+scriptName);
				
				this.queryNoResult(script);
			}
		}
	}
	
	
	
	// high level DB //
	
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
	
	
	
	// low level DB //
	
	public void queryNoResult(String sql) { this.query(sql, false); }
	public ResultSet query(String sql) { return this.query(sql, true); }
	
	@Nullable
	private ResultSet query(String sql, boolean returnResultSet)
	{
		try
		{
			Statement statement = this.connection .createStatement();
			statement.setQueryTimeout(TIMEOUT_SECONDS);
			
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
			// if the error message is "out of memory",
			// it probably means no database file is found
			Assert.fail("Unexpected error for query ["+sql+"]: " + e.getMessage());
		}
		
		return null;
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
	
	
	
	// abstract //
	
	public abstract String getTableName();
	
	@Nullable
	public abstract TDTO convertResultSetToDto(ResultSet resultSet) throws SQLException;
	
	public abstract String createSelectPrimaryKeySql(String primaryKey);
	
	public abstract String createInsertSql(TDTO dto);
	public abstract String createUpdateSql(TDTO dto);
	
	public abstract String createDeleteSql(TDTO dto);
	
}
