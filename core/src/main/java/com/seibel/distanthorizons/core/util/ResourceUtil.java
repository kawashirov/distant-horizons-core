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

package com.seibel.distanthorizons.core.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ResourceUtil
{
	/**
	 * Returns all files in the "resources" folders. <br.
	 * Source: https://stackoverflow.com/a/48190582
	 */
	public static ArrayList<ResourceFile> getFilesInFolder(String directoryName, String fileExtension) throws URISyntaxException, IOException
	{
		ArrayList<ResourceFile> resourceFiles = new ArrayList<>();
		
		URL url = Thread.currentThread().getContextClassLoader().getResource(directoryName);
		if (url != null)
		{
			if (url.getProtocol().equals("file"))
			{
				File rootFile = Paths.get(url.toURI()).toFile();
				File[] files = rootFile.listFiles();
				if (files != null)
				{
					for (File file : files)
					{
						if (file.getName().endsWith(fileExtension))
						{
							String content = FileUtil.readFile(file, Charset.defaultCharset());
							ResourceFile resourceFile = new ResourceFile(file.getName(), content);
							resourceFiles.add(resourceFile);
						}
					}
				}
			}
			else if (url.getProtocol().equals("jar"))
			{
				String dirname = directoryName + "/";
				String path = url.getPath();
				String jarPath = path.substring(5, path.indexOf("!"));
				try (JarFile jar = new JarFile(URLDecoder.decode(jarPath, StandardCharsets.UTF_8.name())))
				{
					Enumeration<JarEntry> entries = jar.entries();
					while (entries.hasMoreElements())
					{
						JarEntry entry = entries.nextElement();
						String fileName = entry.getName();
						if (fileName.startsWith(dirname) && !dirname.equals(fileName))
						{
							if (fileName.endsWith(fileExtension))
							{
								URL resource = Thread.currentThread().getContextClassLoader().getResource(fileName);
								File file = new File(resource.getFile());
								String content = FileUtil.readFile(file, Charset.defaultCharset());
								
								ResourceFile resourceFile = new ResourceFile(file.getName(), content);
								resourceFiles.add(resourceFile);
							}
						}
					}
				}
			}
		}
		
		return resourceFiles;
	}
	
	
	
	//==============//
	// helper class //
	//==============//
	
	public static class ResourceFile
	{
		public final String name;
		public final String content;
		
		public ResourceFile(String name, String content)
		{
			this.name = name;
			this.content = content;
		}
	}
	
}
