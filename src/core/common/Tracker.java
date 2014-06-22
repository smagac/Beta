package core.common;

import com.badlogic.gdx.utils.ObjectMap;

import core.datatypes.FileType;

public class Tracker {

	/**
	 * Values to keep track of over the course of the game
	 * @author nhydock
	 *
	 */
	public enum NumberValues {
		TimesSlept,
		MonstersKilled,
		ItemsCrafted,
		LootFound,
		TimesDied,
		LootSacrificed,
		FilesExplored;
		
		private int count = 0;
		
		public void increment()
		{
			count++;
		}
		
		public void decrement()
		{
			count--;
		}
	}
	
	public enum StringValues {
		FavouriteFileType;
		
		private ObjectMap<String, Integer> counters;
		
		public void increment(String value)
		{
			counters.put(value, counters.get(value, 0) + 1);
		}
		
		public void decrement(String value)
		{
			counters.put(value, counters.get(value, 0) - 1);
		}
		
		public String max()
		{
			String m = null;
			int max = Integer.MIN_VALUE;
			for (String key : counters.keys())
			{
				int k = counters.get(key);
				if (k > max)
				{
					max = k;
					m = key;
				}
			}
			return m;
		}
		
		public String min()
		{
			String m = null;
			int max = Integer.MAX_VALUE;
			for (String key : counters.keys())
			{
				int k = counters.get(key);
				if (k < max)
				{
					max = k;
					m = key;
				}
			}
			return m;
		}
	}
	
	private static ObjectMap<FileType, Integer> typeTracker;
	
	public static void increment(FileType v)
	{
		typeTracker.put(v, typeTracker.get(v, 0) + 1);
	}
	
	public static void decrement(FileType v)
	{
		typeTracker.put(v, typeTracker.get(v, 0) - 1);
	}
}
