package core.common;

import com.badlogic.gdx.utils.ObjectMap;

public class Tracker {

	/**
	 * Values to keep track of over the course of the game
	 * @author nhydock
	 *
	 */
	public enum NumberValues {
		Times_Slept,
		Monsters_Killed,
		Items_Crafted,
		Loot_Found,
		Times_Died,
		Loot_Sacrificed,
		Files_Explored;
		
		private int count = 0;
		
		public void increment()
		{
			count++;
		}
		
		public void decrement()
		{
			count--;
		}
		
		public String toString()
		{
			return String.format("%s: %d", name().replace('_', ' '), count);
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
}
