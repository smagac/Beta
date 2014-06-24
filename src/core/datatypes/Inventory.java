package core.datatypes;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Keys;

import core.common.Tracker;
import factories.AdjectiveFactory;
import factories.CraftableFactory;
import factories.ItemFactory;

public class Inventory {

	Array<Craftable> required;
	ObjectMap<Item, Integer> loot;
	private int progress = 0;
	
	public Inventory(int difficulty)
	{
		CraftableFactory cf = new CraftableFactory();
		
		required = new Array<Craftable>();
		for (int i = 0; i < difficulty; i++)
		{
			required.add(cf.createRandomCraftable());
		}
		
		loot = new ObjectMap<Item, Integer>();
		
		//debug add loot to test crafting
		Craftable c = required.random();
		ObjectMap<String, Integer> ik = c.getRequirements();
		for (String s : ik.keys())
		{
			loot.put(new Item(s, AdjectiveFactory.getAdjective(), ""), ik.get(s) + MathUtils.random(1, 20));
		}
		
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
				Tracker.NumberValues.Items_Crafted.increment();
			}
			
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
}
