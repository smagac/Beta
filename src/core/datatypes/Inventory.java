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
	ObjectMap<Item, Integer> tmp;
	ObjectMap<Item, Integer> all;
	
	private int progress = 0;
	
	public Inventory(int difficulty)
	{
		cf = new CraftableFactory();
		
		required = new Array<Craftable>();
		do
		{
			Craftable c = cf.createRandomCraftable();
			if (!required.contains(c, false))
			{
				required.add(c);
			}
		}
		while (required.size < difficulty*2);
		
		refreshCrafts();
		
		loot = new ObjectMap<Item, Integer>();
		tmp = new ObjectMap<Item, Integer>();
		all = new ObjectMap<Item, Integer>();
		
//		//debug add loot to test crafting
//		for (int i = 0; i < 30; i++)
//		{
//			loot.put(new Item(Item.items.random(), AdjectiveFactory.getAdjective()), MathUtils.random(1, 20));
//		}
//		
//		//debug add loot to be able to craft at least one item
//		Craftable c = required.random();
//		for (String s : c.getRequirements().keys())
//		{
//			loot.put(new Item(s, AdjectiveFactory.getAdjective()), c.getRequirements().get(s) + MathUtils.random(1, 5));
//		}
	}
	
	/**
	 * Refreshes the list of today's craftable items with a new list
	 */
	public void refreshCrafts() {
		todaysCrafts = new Array<Craftable>();
		todaysCrafts.clear();
		do
		{
			Craftable c = cf.createRandomCraftable();
			if (!todaysCrafts.contains(c, false))
			{
				todaysCrafts.add(c);
			}
		}
		while (todaysCrafts.size < 5);
	}
	
	public Array<Craftable> getTodaysCrafts() {
		return todaysCrafts;
	}
	
	public Array<Craftable> getRequiredCrafts() {
		for (Craftable c : required)
		{
			c.canMake = canMake(c);
		}
		return required;
	}

	public int getProgress() {
		return progress;
	}
	
	public float getProgressPercentage() {
		return (float)progress/(float)required.size;
	}
	
	public boolean canMake(Craftable c)
	{
		ObjectMap<String, Integer> requirements = c.getRequirements();
		boolean make = true;
		
		for (String required : requirements.keys())
		{
			int need = requirements.get(required);
			int have = 0;
			
			for (Item lootName : loot.keys())
			{
				if (lootName.equals(required))
				{
					Integer amount = loot.get(lootName);
					
					have = have + amount;
			
					if (have >= need)
					{
						break;
					}
				}
			}
		
			if (have < need)
			{
				make = false;
				break;
			}
		}
		return make;
	}
	
	/**
	 * Make a craftable out of the loot you have
	 * @param c
	 */
	public boolean makeItem(Craftable c)
	{
		ObjectMap<String, Integer> requirements = c.getRequirements();
		
		if (!c.canMake)
		{
			return false;
		}
		
		for (String required : requirements.keys())
		{
			int need = requirements.get(required);
			int have = 0;
			
			for (Item i : loot.keys())
			{
				if (i.equals(required))
				{
					Integer amount = loot.get(i);
					
					have = have + amount;
					
					if (have > need)
					{
						amount = have-need;
					}
					loot.put(i,  loot.get(i) - amount);
					
					if (loot.get(i) <= 0)
					{
						loot.remove(i);
					}
					
					if (have >= need)
					{
						break;
					}
				}
			}
		}
		
		//add the item to your loot
		Item crafted = new Item(c.name, c.adj);
		loot.put(crafted, loot.get(crafted, 0)+1);
		
		Tracker.NumberValues.Items_Crafted.increment();
			
		//count progress after making
		progress = 0;
		for (Craftable r : required)
		{
			Item i = null;
			
			Keys<Item> keys = loot.keys();
			for (; keys.hasNext && i == null;)
			{
				Item i2 = keys.next();
				if (i2.fullname().equals(r.fullname())) {
					i = i2;
				}
			}
			
			if (i != null && loot.get(i, 0) > 0)
			{
				progress++;
			}
		}
		return true;
	}

	public ObjectMap<Item, Integer> getLoot() {
		return all;
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
			if (all.get(item, 0) < i)
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
				int total = sacrifices.get(item);
				int tmpSub = Math.min(total, tmp.get(item, 0));
				int lootSub = Math.min(total-tmpSub, loot.get(item, 0));
				
				if (loot.containsKey(item))	
				{
					int count = loot.get(item, 0) - lootSub;
					if (count == 0) { loot.remove(item); }
					else { loot.put(item, count); }
				}
				if (tmp.containsKey(item))
				{
					int count = tmp.get(item, 0) - tmpSub;
					if (count == 0){ tmp.remove(item); }
					else { tmp.put(item, count);	}
				}
				if (all.containsKey(item)){ 
					int count = all.get(item, 0) - (tmpSub + lootSub);
					if (count == 0){ all.remove(item); }
					else { all.put(item, count); }
				}
			}
		}
		
		return canSacrifice;
	}
	
	public void pickup(Item i)
	{
		tmp.put(i, tmp.get(i, 0)+1);
		all.put(i, all.get(i, 0)+1);
	}
	
	public void merge()
	{
		tmp.clear();
		loot.clear();
		loot.putAll(all);
	}
	
	public void abandon()
	{
		tmp.clear();
		all.clear();
		all.putAll(loot);
	}
}
