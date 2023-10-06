package com.seibel.distanthorizons.coreapi.util.jar;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

/**
 * Deletes the first file in the arguments when a lock is lifted from it (for Windows) <br>
 * DON'T MOVE: If this class is moved, then the updater will no longer work on Windows
 * 
 * @author coolgi 
 */
public class DeleteOnUnlock
{
	/**
	 * @param args Takes whatever the first argument is, treats it as a file, and deletes it once a process lock is lifted from it
	 */
	public static void main(String[] args)
	{
		File file = new File(args[0]);
		try
		{
			for (int i = 0; i < 600; i++) // As it rests for around 0.1 second each loop, this should last a minute
			{
				if (file.canWrite())
				{
					Files.delete(file.toPath());
					break;
				}
				TimeUnit.MILLISECONDS.sleep(100);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new RuntimeException("Deletion failed");
		}


		// If it isn't deleted by the end, crash
		if (Files.exists(file.toPath()))
			throw new RuntimeException("File was not able to be deleted");
	}
}
