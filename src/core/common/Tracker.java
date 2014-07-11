package core.common;

import com.badlogic.gdx.utils.ObjectMap;

public final class Tracker {

	public static int score()
	{
		//calculate score
		int score = 0;
		
		return score;
	}
	
	/**
	 * Calculate a rank for the player based on their tracked stats
	 * @return
	 */
	public static String rank() {
		
		String rank = "Straight Shooter";
		if (score() > 50000)
		{
			rank = "Going for the Gold";
		}
		if (NumberValues.Monsters_Killed.count > 500)
		{
			rank = "Poacher";
		}
		if (NumberValues.Monsters_Killed.count > 1000)
		{
			rank = "Exterminator";
		}
		if (NumberValues.Files_Explored.count < 10)
		{
			rank = "Crawler";
		}
		if (NumberValues.Items_Crafted.count > 10)
		{
			rank = "Apprentice";
		}
		if (NumberValues.Files_Explored.count > 10)
		{
			rank = "Logger";
		}
		if (NumberValues.Times_Slept.count > 100)
		{
			rank = "Narclyptic";
		}
		if (NumberValues.Items_Crafted.count > 15)
		{
			rank = "Favourite Slave";
		}
		if (NumberValues.Loot_Sacrificed.count > 500)
		{
			rank = "Garbage Dump";
		}
		if (NumberValues.Files_Explored.count > 20)
		{
			rank = "Living Virus";
		}
		if (NumberValues.Items_Crafted.count > 25)
		{
			rank = "Crafting Addict";
		}
		if (NumberValues.Monsters_Killed.count > 10000)
		{
			rank = "Mass Extintion";
		}
		if (NumberValues.Files_Explored.count == 0)
		{
			rank = "Dungeon Gambler";
		}
		if (NumberValues.Times_Slept.count < 10)
		{
			rank = "Insomniac";
		}
		if (NumberValues.Items_Crafted.count > 50)
		{
			rank = "Dedicated Follower";
		}
		if (NumberValues.Items_Crafted.count > 70)
		{
			rank = "Westboro Craftist";
		}
		if (NumberValues.Times_Died.count < 10)
		{
			rank = "Played it Safe";
		}
		if (NumberValues.Items_Crafted.count > 100)
		{
			rank = "Newly Appointed Crafting God";
		}
		
		return rank;
	}
	
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
		
		@Override
		public String toString()
		{
			return String.format("%s", name().replace('_', ' '));
		}

		public int value() {
			return count;
		}
	}
	
	public enum StringValues {
		Favourite_File_Type;
		
		private final ObjectMap<String, Integer> counters = new ObjectMap<String, Integer>();
		
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
		
		@Override
		public String toString()
		{
			return String.format("%s", name().replace('_', ' '));
		}
	}
}
