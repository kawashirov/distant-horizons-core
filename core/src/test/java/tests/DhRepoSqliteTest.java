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

package tests;

import com.seibel.distanthorizons.core.sql.DatabaseUpdater;
import org.junit.Assert;
import org.junit.Test;
import testItems.sql.TestDataRepo;
import testItems.sql.TestDto;

import java.io.File;
import java.sql.SQLException;
import java.util.Map;

/**
 * Validates {@link com.seibel.distanthorizons.core.sql.AbstractDhRepo} is set up correctly.
 */
public class DhRepoSqliteTest
{
	public static String DATABASE_TYPE = "jdbc:sqlite";
	
	
	@Test
	public void testFileSqlite()
	{
		String dbFileName = "test_fullData.sqlite";
		
		
		File dbFile = new File(dbFileName);
		if (dbFile.exists())
		{
			Assert.assertTrue("unable to delete old test DB File.", dbFile.delete());
		}
		
		
		TestDataRepo testDataRepo = null;
		try
		{
			testDataRepo = new TestDataRepo(DATABASE_TYPE, dbFileName);
			
			dbFile = new File(dbFileName);
			Assert.assertTrue("dbFile not created", dbFile.exists());
			
			
			
			//==========================//
			// Auto update script tests //
			//==========================//
			
			// check that the schema table is created
			Map<String, Object> autoUpdateTablePresentResult = testDataRepo.queryDictionaryFirst("SELECT name FROM sqlite_master WHERE type='table' AND name='"+DatabaseUpdater.SCHEMA_TABLE_NAME+"';");
			if (autoUpdateTablePresentResult == null || autoUpdateTablePresentResult.get("name") == null)
			{
				Assert.fail("Auto DB update table missing.");
			}
			
			// check that the update scripts aren't run multiple times
			TestDataRepo altDataRepoOne = new TestDataRepo(DATABASE_TYPE, dbFileName);
			TestDataRepo altDataRepoTwo = new TestDataRepo(DATABASE_TYPE, dbFileName);
			
			
			
			//===========//
			// DTO tests //
			//===========//
			
			// insert
			TestDto insertDto = new TestDto(0, "a");
			testDataRepo.save(insertDto);
			
			// get
			TestDto getDto = testDataRepo.getByPrimaryKey("0");
			Assert.assertNotNull("get failed, null returned", getDto);
			Assert.assertEquals("get/insert failed, not equal", insertDto.id, getDto.id);
			Assert.assertEquals("get/insert failed, not equal", insertDto.value, getDto.value);
			
			
			// update
			TestDto updateMetaFile = new TestDto(0, "b");
			testDataRepo.save(updateMetaFile);
			
			// get
			getDto = testDataRepo.getByPrimaryKey("0");
			Assert.assertNotNull("get failed, null returned", getDto);
			Assert.assertEquals("get/insert failed, not equal", updateMetaFile.id, getDto.id);
			Assert.assertEquals("get/insert failed, not equal", updateMetaFile.value, getDto.value);
			
			
			// delete
			testDataRepo.delete(updateMetaFile);
			
			// get
			getDto = testDataRepo.getByPrimaryKey("0");
			Assert.assertNull("delete failed, not null returned", getDto);
			
		}
		catch (SQLException e)
		{
			Assert.fail(e.getMessage());
		}
		finally
		{
			if (testDataRepo != null)
			{
				testDataRepo.close();
			}
		}
	}
	
}
