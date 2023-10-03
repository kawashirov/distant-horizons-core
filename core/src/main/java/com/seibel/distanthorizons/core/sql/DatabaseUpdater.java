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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;

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
			// shouldn't normally happen, but just in case
			throw new RuntimeException(e);
		}
		
		
		// create the base update table if necessary
		Map<String, Object> schemaTableExistsResult = repo.queryDictionaryFirst("SELECT COUNT(name) as 'tableCount' FROM sqlite_master WHERE type='table' AND name='"+SCHEMA_TABLE_NAME+"';");
		if (schemaTableExistsResult == null || (int) schemaTableExistsResult.get("tableCount") == 0)
		{
			// Note: if this table ever needs to be modified, that should be done via an auto update script to prevent issues with updating old databases
			String createBaseSchemaTable =
					"CREATE TABLE "+SCHEMA_TABLE_NAME+"( \n" +
					"    FileName TEXT NOT NULL PRIMARY KEY \n" +
					"   ,AppliedDateTime DATETIME NOT NULL default CURRENT_TIMESTAMP --in UTC 0 timezone \n" +
					");";
			repo.queryDictionaryFirst(createBaseSchemaTable);
		}
		
		
		// attempt to run any un-run update scripts
		for (ResourceUtil.ResourceFile resource : sqlScripts)
		{
			Map<String, Object> scriptAlreadyRunResult = repo.queryDictionaryFirst("SELECT EXISTS(SELECT 1 FROM "+SCHEMA_TABLE_NAME+" WHERE FileName='"+resource.name+"') as 'existingCount';");
			if (scriptAlreadyRunResult != null && (int) scriptAlreadyRunResult.get("existingCount") == 0)
			{
				LOGGER.info("Running SQL update script: ["+resource.name+"], for repo: ["+repo.databaseLocation+"]");
				try
				{
					repo.queryDictionaryFirst(resource.content);	
				}
				catch (RuntimeException e)
				{
					// updating needs to stop to prevent any further data corruption
					LOGGER.error("Unexpected error running database update script ["+resource.name+"] on database ["+repo.databaseLocation+"], stopping database update, data saving may fail. \n" +
							"Error: ["+e.getMessage()+"]. \n" +
							"Sql Script:["+resource.content+"]", e);
					break;
				}
				
				
				// record the successfully run script
				repo.queryDictionaryFirst("INSERT INTO "+SCHEMA_TABLE_NAME+" (FileName) VALUES('"+resource.name+"');");
			}
		}
	}
	
}
