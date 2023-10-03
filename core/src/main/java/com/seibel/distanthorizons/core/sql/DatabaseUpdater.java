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
import com.seibel.distanthorizons.core.util.ResourceUtil;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class DatabaseUpdater
{
	private static final Logger LOGGER = DhLoggerBuilder.getLogger();
	
	public static final String SCHEMA_TABLE_NAME = "Schema";
	
	
	
	/** Handles both initial setup and  */
	public static <TDTO extends IBaseDTO> void runAutoUpdateScripts(AbstractDhRepo<TDTO> repo) throws SQLException
	{
		// get the resource scripts
		ArrayList<ResourceUtil.ResourceFile> sqlScripts;
		try
		{
			sqlScripts = ResourceUtil.getFilesInFolder("sqlScripts", ".sql");
		}
		catch (URISyntaxException | IOException e)
		{
			// shouldn't normally happen, but just incase
			throw new RuntimeException(e);
		}
		
		
		// create the base update table if necessary
		ResultSet schemaTableExistsResult = repo.query("SELECT COUNT(name) FROM sqlite_master WHERE type='table' AND name='Schema';");
		if (schemaTableExistsResult.next())
		{
			boolean schemaTableMissing = schemaTableExistsResult.getInt(1) == 0;
			if (schemaTableMissing)
			{
				// Note: if this table ever needs to be modified, that should be done via an auto update script to prevent issues with updating old databases
				String createBaseSchemaTable =
						"CREATE TABLE "+SCHEMA_TABLE_NAME+"( \n" +
						"    FileName TEXT NOT NULL PRIMARY KEY \n" +
						"   ,AppliedDateTime DATETIME NOT NULL default CURRENT_TIMESTAMP --in UTC 0 timezone \n" +
						");";
				repo.queryNoResult(createBaseSchemaTable);
			}
		}
		
		
		// attempt to run any un-run update scripts
		for (ResourceUtil.ResourceFile file : sqlScripts)
		{
			ResultSet scriptAlreadyRunResult = repo.query("SELECT EXISTS(SELECT 1 FROM Schema WHERE FileName='"+file.name+"');");
			if (!scriptAlreadyRunResult.next() || !scriptAlreadyRunResult.getBoolean(1))
			{
				LOGGER.info("Running SQL update script: ["+file.name+"], for repo: ["+repo.databaseLocation+"]");
				repo.queryNoResult(file.content);
				
				// record the successfully run script
				repo.queryNoResult("INSERT INTO Schema (FileName) VALUES('"+file.name+"');");
			}
		}
	}
	
}
