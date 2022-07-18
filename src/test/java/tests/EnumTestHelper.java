package tests;/*
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

import org.junit.Assert;

import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;
import java.util.ArrayList;

/**
 * A list of methods related to the Enum unit tests.
 *
 * @author James Seibel
 * @version 6-9-2022
 */
public class EnumTestHelper
{
	
	/**
	 * Returns a list of every Enum in the package with the given full name.
	 *
	 * @param packageFullName includes the package path
	 */
	@SuppressWarnings("unchecked")
	public static ArrayList<Class<? extends Enum<?>>> getAllEnumsFromPackage(String packageFullName)
	{
		ArrayList<Class<? extends Enum<?>>> enumList = new ArrayList<>();
		URL packageRoot = Thread.currentThread().getContextClassLoader().getResource(packageFullName.replace(".", "/"));
		
		// find all .class files in the package folder
		Assert.assertNotNull("No enums found in the package [" + packageFullName + "].", packageRoot); // asserts can be used since this method should only ever be used in the context of unit tests
		File[] files = new File(packageRoot.getFile()).listFiles(new FilenameFilter()
		{
			public boolean accept(File dir, String name)
			{
				return name.endsWith(".class");
			}
		});
		
		// get the enums from each file
		Assert.assertNotNull("No files found in the package [" + packageFullName + "].", files);
		for (File file : files)
		{
			String className = file.getName().replaceAll(".class$", "");
			
			// ignore internal classes
			if (!className.contains("$"))
			{
				String fullClassName = packageFullName + "." + className;
				try
				{
					// attempt to parse the file's class into an enum
					Class<?> enumClass = Class.forName(fullClassName);
					if (Enum.class.isAssignableFrom(enumClass))
					{
						enumList.add((Class<? extends Enum<?>>) enumClass);
					}
					else
					{
						System.out.println("The Class [" + fullClassName + "] isn't an enum.");
					}
				}
				catch (ClassNotFoundException e)
				{
					System.out.println("No enum found with the full name [" + fullClassName + "].");
				}
			}
		}
		
		return enumList;
	}
	
	/**
	 * Returns every loaded package that begins with the given string.
	 *
	 * Note: this will only search packages that have been loaded
	 * at some point during the JVM's lifetime.
	 * To Make sure the package(s) you want to find are loaded you can
	 * initialize an object from that package to load it.
	 */
	public static ArrayList<String> findPackageNamesStartingWith(String packagePrefix)
	{
		ArrayList<String> nestedPackages = new ArrayList<>();
		
		// search all the loaded packages
		Package[] packageArray = Package.getPackages();
		for (Package pack : packageArray)
		{
			String packageName = pack.getName();
			if (packageName.startsWith(packagePrefix))
			{
				nestedPackages.add(packageName);
			}
		}
		
		return nestedPackages;
	}
	
}
