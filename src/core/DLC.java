package core;

import java.io.InputStream;
import java.util.Scanner;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

/**
 * Simple loading mechanism that allows for file override and extension
 * by prioritizing user defined files over the files packaged with the game
 * by default.
 * 
 * @author nhydock
 */
public class DLC {

	private static Array<FileHandle> packs = new Array<FileHandle>();
	
	/**
	 * Loads the dlc dirs from the definitions
	 */
	public static void init()
	{
		if (Gdx.files.internal("dlc").exists())
		{
			packs.clear();
			//force load order
			InputStream io = Gdx.files.internal("dlc/load.dlc").read();
			Scanner s = new Scanner(io);
			while (s.hasNextLine())
			{
				FileHandle path = Gdx.files.internal("dlc/"+s.nextLine());
				if (path.exists())
				{
					packs.add(path);
				}
			}
			packs.reverse();
			s.close();
		}
	}
	
	public static Array<FileHandle> getAll(String base, FileHandle fallback)
	{
		return getAll(base, -1, fallback);
	}
	
	/**
	 * Checks if a file exists in the dlc folder
	 * @param base
	 */
	public static Array<FileHandle> getAll(String base, int limit, FileHandle fallback)
	{
		Array<FileHandle> match = new Array<FileHandle>();
		match.add(fallback);
		if (limit == -1)
			limit = Integer.MAX_VALUE;
		if (limit < 0)
			return match;
		
		for (FileHandle dir : DLC.packs)
		{
			try
			{
				FileHandle path = dir.child(base);
				if (path.exists())
				{
					match.add(path);
					if (match.size >= limit)
					{
						break;
					}
				}
			}
			catch (GdxRuntimeException e)
			{
				continue;
			}
		}
		return match;
	}
	
	/**
	 * Gets the path to a dlc file, fallback to data folder if it doesn't exist.
	 * Only works with internal based files
	 * @param base
	 * @param type
	 * @return
	 */
	public static String get(String base, String fallback)
	{
		return get(base, Gdx.files.internal(fallback));
	}
	
	/**
	 * Gets the path to a dlc file, fallback to data folder if it doesn't exist.
	 * Only works with internal based files
	 * @param base
	 * @param type
	 * @return
	 */
	public static String get(String base, FileHandle fallback)
	{
		return getAll(base, 1, fallback).peek().path();
	}
	
}
