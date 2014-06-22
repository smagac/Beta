package factories;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

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
		Array<String> parts = new Array<String>();
		
		final int ingredientCount = MathUtils.random(1, 5);
		for (int i = 0; i < ingredientCount; i++)
		{
			parts.add(Item.items.random());
		}
		return new Craftable(name, adj);
	}
}
