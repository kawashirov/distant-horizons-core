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

package com.seibel.distanthorizons.core.jar;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.io.ParsingMode;
import com.electronwill.nightconfig.json.JsonFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Get info on the git for the mod <br>
 * Warning: Gets generated on runtime
 *
 * @author coolGi
 */
public final class ModGitInfo
{
	private static final Logger LOGGER = LogManager.getLogger();
	private static final String FILE_NAME = "build_info.json";
	
	static
	{
		String gitMainBranch = "UNKNOWN";
		String gitMainCommit = "UNKNOWN";
		String gitCoreCommit = "UNKNOWN";
		
		try
		{
			// Warning: Atm, this file is in the common subproject as the processResources task in gradle doesn't work for core
			String jsonString = JarUtils.convertInputStreamToString(JarUtils.accessFile(FILE_NAME));
			
			Config jsonObject = Config.inMemory();
			JsonFormat.minimalInstance().createParser().parse(jsonString, jsonObject, ParsingMode.REPLACE);
			
			gitCoreCommit = jsonObject.get("git_main_branch");
			gitMainCommit = jsonObject.get("git_main_commit");
			gitMainBranch = jsonObject.get("git_core_commit");
		}
		catch (Exception | Error e)
		{
			LOGGER.warn("Unable to get the Git information from " + FILE_NAME);
		}
		
		Git_Core_Commit = gitMainBranch;
		Git_Main_Commit = gitMainCommit;
		Git_Main_Branch = gitCoreCommit;
	}
	
	public static final String Git_Main_Branch;
	public static final String Git_Main_Commit;
	public static final String Git_Core_Commit;
	
}
