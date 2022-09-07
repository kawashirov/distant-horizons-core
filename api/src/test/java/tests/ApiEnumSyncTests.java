/*
 *    This file is part of the Distant Horizons mod (formerly the LOD Mod),
 *    licensed under the GNU LGPL v3 License.
 *
 *    Copyright (C) 2020-2022  James Seibel
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

import com.seibel.lod.api.enums.DhApiEnumAssembly;
import com.seibel.lod.core.enums.rendering.EFogDrawMode;
import com.seibel.lod.core.enums.CoreEnumAssembly;
import com.seibel.lod.core.enums.config.EVerticalQuality;
import com.seibel.lod.core.util.EnumUtil;
import org.junit.Test;
import org.junit.Assert;

import java.util.ArrayList;

/**
 * These tests were primary created to confirm that the
 * API enums are properly synced with their Core variants.
 *
 * @author James Seibel
 * @version 2022-6-9
 */
public class ApiEnumSyncTests
{
	
	/** Make sure each DhApi enum has the same values as its corresponding core enum. */
//	@Test
	public void ConfirmEnumsAreSynced()
	{
		//=================//
		// test validation //
		//=================//
		
		// this should always succeed (comparing an enum to itself)
		AssertEnumsValuesAreEqual(EnumUtil.compareEnumClassesByValues(EVerticalQuality.class, EVerticalQuality.class), true);
		// this should always fail (two completely different enums)
		AssertEnumsValuesAreEqual(EnumUtil.compareEnumClassesByValues(EVerticalQuality.class, EFogDrawMode.class), false);
		
		
		
		
		//================//
		// Api enum Setup //
		//================//
		
		// make sure the enum packages are loaded
		new DhApiEnumAssembly();
		new CoreEnumAssembly();
		
		// get the list of API enums
		ArrayList<Class<? extends Enum<?>>> apiEnumClassList = new ArrayList<>();
		ArrayList<String> apiConfigEnumPackageNames = EnumTestHelper.findPackageNamesStartingWith(DhApiEnumAssembly.class.getPackage().getName());
		for (String apiEnumPackageName : apiConfigEnumPackageNames)
		{
			apiEnumClassList.addAll(EnumTestHelper.getAllEnumsFromPackage(apiEnumPackageName));
		}
		
		
		// get the list of core enums
		ArrayList<Class<? extends Enum<?>>> coreEnumClassList = new ArrayList<>();
		ArrayList<String> coreEnumPackageNames = EnumTestHelper.findPackageNamesStartingWith(CoreEnumAssembly.class.getPackage().getName());
		for (String coreEnumPackageName : coreEnumPackageNames)
		{
			coreEnumClassList.addAll(EnumTestHelper.getAllEnumsFromPackage(coreEnumPackageName));
		}
		
		
		//======================//
		// Api enum comparisons //
		//======================//
		
		// compare each API enum to its corresponding Core enum
		for (Class<? extends Enum<?>> apiEnumClass : apiEnumClassList)
		{
			String coreEnumName = CoreEnumAssembly.ENUM_PREFIX + apiEnumClass.getSimpleName().substring(DhApiEnumAssembly.API_ENUM_PREFIX.length());
			boolean coreEnumFound = false;
			
			// find the core enum to compare against
			for (Class<? extends Enum<?>> coreEnumClass : coreEnumClassList)
			{
				if (coreEnumClass.getSimpleName().equals(coreEnumName))
				{
					AssertEnumsValuesAreEqual(EnumUtil.compareEnumClassesByValues(coreEnumClass, apiEnumClass), true);
					coreEnumFound = true;
					break;
				}
			}
			
			if (!coreEnumFound)
			{
				Assert.fail("API enum [" + coreEnumName + "] not found in Core.");
			}
		}
		
		
	}
	
	
	
	/** Helper method to make enum comparisons a little cleaner */
	private void AssertEnumsValuesAreEqual(EnumUtil.EnumComparisonResult comparisonResult, boolean assertEqual)
	{
		if (assertEqual)
		{
			Assert.assertTrue(comparisonResult.failMessage, comparisonResult.success);
		}
		else
		{
			Assert.assertFalse(comparisonResult.failMessage, comparisonResult.success);
		}
	}
	
}
