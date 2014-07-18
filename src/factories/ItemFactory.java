package factories;

import com.badlogic.gdx.utils.Array;

import core.datatypes.FileType;
import core.datatypes.Item;

public class ItemFactory {
	
	private final Array<String> areaLoot;
	
	public ItemFactory(FileType area)
	{
		//make sure item data is loaded
		Item.init();
		
		areaLoot = new Array<String>();
		areaLoot.addAll(Item.lootLocations.get(area));
		areaLoot.addAll(Item.lootLocations.get(FileType.Other));
	}
	
	public Item createItem()
	{
		return new Item(areaLoot.random(), AdjectiveFactory.getAdjective());
	}
}
