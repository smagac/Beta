package factories;

import com.badlogic.gdx.math.MathUtils;

import core.datatypes.Craftable;
import core.datatypes.Item;

public class CraftableFactory {
	
	public CraftableFactory()
	{
		//make sure item data is loaded
		Item.init();
	}
	
	public Craftable createRandomCraftable()
	{
		String name = Item.craftables.random();
		String adj = AdjectiveFactory.getAdjective();
		String[] parts = new String[MathUtils.random(1, 5)];
		
		for (int i = 0; i < parts.length; i++)
		{
			parts[i] = Item.items.random();
		}
		return new Craftable(name, adj, parts);
	}
}
