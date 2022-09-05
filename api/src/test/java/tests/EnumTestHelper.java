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
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * A list of methods related to the Enum unit tests.
 *
 * @author James Seibel
 * @version 2022-9-5
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
		List<Class<?>> classesInPackage = getClassesInPackage(packageFullName, true, "DistantHorizons");
		
		// get the enums from each file
		Assert.assertTrue("No files found in the package [" + packageFullName + "].", classesInPackage.size() != 0);
		for (Class<?> clazz : classesInPackage)
		{
			// ignore internal classes
			if (!clazz.getName().contains("$"))
			{
				// attempt to parse the file's class into an enum
				if (Enum.class.isAssignableFrom(clazz))
				{
					enumList.add((Class<? extends Enum<?>>) clazz);
				}
				else
				{
					System.out.println("The Class [" + clazz + "] isn't an enum.");
				}
			}
		}
		
		return enumList;
	}
	
	/**
	 * Returns every class in the given package. <br><br>
	 * 
	 * Originally from:
	 * https://stackoverflow.com/questions/28678026/how-can-i-get-all-class-files-in-a-specific-package-in-java
	 * 
	 * @param packageName
	 * @param onlySearchJars if true only jar files will be searched, otherwise jars and loose files will be searched
	 * @param expectedJarPathString Only search jars that contain this string
	 */
	public static List<Class<?>> getClassesInPackage(String packageName, boolean onlySearchJars, String expectedJarPathString)
	{
		String path = packageName.replace('.', '/');
		List<Class<?>> classes = new ArrayList<>();
		String[] classPathEntries = System.getProperty("java.class.path").split(
				System.getProperty("path.separator")
		);
		
		String name;
		for (String classpathEntry : classPathEntries)
		{
			if (classpathEntry.endsWith(".jar") && classpathEntry.toLowerCase().contains(expectedJarPathString.toLowerCase()))
			{
				File jar = new File(classpathEntry);
				try
				{
					JarInputStream is = new JarInputStream(new FileInputStream(jar));
					JarEntry entry;
					while ((entry = is.getNextJarEntry()) != null)
					{
						name = entry.getName();
						if (name.endsWith(".class"))
						{
							if (name.contains(path) && name.endsWith(".class"))
							{
								try
								{
									String classPath = name.substring(0, entry.getName().length() - 6);
									classPath = classPath.replaceAll("[\\|/]", ".");
									classes.add(Class.forName(classPath));
								}
								catch (ClassNotFoundException ex)
								{
									// the class wasn't found
									System.err.println("The Class [" + packageName + "." + name + "] failed to load.");
								}
							}
						}
					}
				}
				catch (IOException e) 
				{
					System.err.println("Error reading the jar [" + jar.getPath() + "].");
				}
			}
			else if (!onlySearchJars)
			{
				File base = new File(classpathEntry + File.separatorChar + path);
				File[] files = base.listFiles();
				if (files != null)
				{
					for (File file : files)
					{
						try
						{
							name = file.getName();
							if (name.endsWith(".class"))
							{
								name = name.substring(0, name.length() - 6);
								classes.add(Class.forName(packageName + "." + name));
							}
						}
						catch (ClassNotFoundException ex)
						{
							// the class wasn't found
							System.err.println("The Class [" + packageName + "." + file.getName() + "] failed to load.");
						}
					}
				}
			}
		}
		
		return classes;
	}
	
	
	/**
	 * Returns every loaded package that begins with the given string. <br>
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
