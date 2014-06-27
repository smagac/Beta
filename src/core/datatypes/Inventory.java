package core.datatypes;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Keys;

import core.common.Tracker;
import factories.AdjectiveFactory;
import factories.CraftableFactory;

public class Inventory {

	CraftableFactory cf;
	Array<Craftable> required;
	Array<Craftable> todaysCrafts;
	ObjectMap<Item, Integer> loot;
	private int progress = 0;
	
	public Inventory(int difficulty)
	{
		cf = new CraftableFactory();
		
		required = new Array<Craftable>();
		for (int i = 0; i < difficulty; i++)
		{
			required.add(cf.createRandomCraftable());
		}
		
		refreshCrafts();
		
		loot = new ObjectMap<Item, Integer>();
		
		//debug add loot to test crafting
		for (int i = 0; i < 30; i++)
		{
			loot.put(new Item(Item.items.random(), AdjectiveFactory.getAdjective(), ""), MathUtils.random(1, 20));
		}
	}
	
	/**
	 * Refreshes the list of today's craftable items with a new list
	 */
	public void refreshCrafts() {
		todaysCrafts = new Array<Craftable>();
		for (int i = 0; i < 5; i++)
		{
			todaysCrafts.add(cf.createRandomCraftable());
		}
	}
	
	public Array<Craftable> getTodaysCrafts() {
		return todaysCrafts;
	}
	
	public Array<Craftable> getRequiredCrafts() {
		return required;
	}

	public int getProgress() {
		return progress;
	}
	
	/**
	 * Make a craftable out of the loot you have
	 * @param c
	 */
	public boolean makeItem(Craftable c)
	{
		ObjectMap<String, Integer> requirements = c.getRequirements();
		Keys<String> keys = requirements.keys();
		ObjectMap<Item, Integer> take = new ObjectMap<Item, Integer>();
		boolean make = true;
		Keys<Item> lootKeys = loot.keys();
		
		while (make && keys.hasNext)
		{
			String item = keys.next();
			Item i = null;
			lootKeys.reset();
			for (; lootKeys.hasNext && i == null;)
			{
				Item i2 = lootKeys.next();
				if (i2.equals(item)) {
					i = i2;
				}
				
			}
			if (i != null)
			{
				int count = loot.get(i, 0);
				int want = requirements.get(item);
				if (count < want)
				{
					make = false;
				}
				else
				{
					take.put(i, want);
				}
			} else
			{
				make = false;
			}
		}
		
		if (make)
		{
			for (Item i : take.keys())
			{
				int amount = take.get(i);
				loot.put(i, loot.get(i) - amount);
				if (loot.get(i) <= 0)
				{
					loot.remove(i);
				}
				
				//add the item to your loot
				loot.put(c, 1);
			}
			Tracker.NumberValues.Items_Crafted.increment();
			
			//count progress after making
			progress = 0;
			for (Craftable r : required)
			{
				Item i = null;
				lootKeys.reset();
				for (; lootKeys.hasNext && i == null;)
				{
					Item i2 = lootKeys.next();
					if (i2.equals(r)) {
						i = i2;
					}
				}
				if (i != null && loot.get(i, 0) > 0)
				{
					progress++;
				}
			}
		}
		return make;
	}

	public ObjectMap<Item, Integer> getLoot() {
		return loot;
	}

	/**
	 * Removes a list of items from your loot
	 * @param sacrifices
	 */
	public boolean sacrifice(ObjectMap<Item, Integer> sacrifices, int required) {
		int pieces = 0;
		boolean canSacrifice = true;
		for (Item item : sacrifices.keys())
		{
			Integer i = sacrifices.get(item);
			if (loot.get(item, 0) < i)
			{
				canSacrifice = false;
				break;
			}
			else
			{
				pieces += i;
			}
		}
		
		if (pieces < required)
		{
			canSacrifice = false;
		}
		
		if (canSacrifice)
		{
			for (Item item : sacrifices.keys())
			{
				Integer i = sacrifices.get(item);
				Integer k = loot.get(item) - i;
				if (k == 0)
				{
					loot.remove(item);
				}
				else
				{
					loot.put(item, k);
				}
			}
		}
		
		return canSacrifice;
	}
	
	public void merge(ObjectMap<Item, Integer> more)
	{
		for (Item item : more.keys())
		{
			loot.put(item, loot.get(item, 0) + more.get(item));
		}
	}
}
