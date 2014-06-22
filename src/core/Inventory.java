package core;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import core.datatypes.Craftable;
import core.datatypes.Item;
import factories.CraftableFactory;

public class Inventory {

	Array<Craftable> required;
	ObjectMap<Item, Integer> loot;
	
	public Inventory()
	{
		CraftableFactory cf = new CraftableFactory();
		
		required = new Array<Craftable>();
		for (int i = 0; i < 5; i++)
		{
			required.add(cf.createRandomCraftable());
		}
	}
	
	public Array<Craftable> getRequiredCrafts() {
		return required;
	}
}
