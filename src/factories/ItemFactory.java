package factories;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

import core.datatypes.FileType;
import core.datatypes.Item;

public final class ItemFactory {
	protected static Array<String> items;
	protected static Array<String> loot;
	protected static ObjectMap<FileType, Array<String>> lootLocations;
	
	private static boolean loaded;
	
	/**
	 * Preload all item data
	 */
	public static void init()
	{
		//only allow loading once
		if (loaded)
			return;
		
		JsonReader json = new JsonReader();
		
		//load items
		JsonValue jv = json.parse(Gdx.files.classpath("data/items.json"));
		
		lootLocations = new ObjectMap<FileType, Array<String>>();
		items = new Array<String>();
		loot = new Array<String>();
		
		for (FileType type : FileType.values())
		{
			Array<String> tLoot = new Array<String>();
			for (JsonValue data : jv.get(type.toString()))
			{
				String name = data.asString();
				items.add(name);
				tLoot.add(name);
				loot.add(name);
			}
			lootLocations.put(type, tLoot);
		}
		{
			for (JsonValue data : jv.get("craftable"))
			{
				String name = data.asString();
				items.add(name);
			}
		}
		loaded = true;
	}
	
	public static String randomName()
	{
		return AdjectiveFactory.getAdjective() + " " + items.random();
	}
	
	public static String randomType()
	{
		return items.random();
	}
	
	private final Array<String> areaLoot;
	
	/**
	 * Generates an item factory useful for a specific type of dungeon
	 * @param area - filetype of the dungeon that the item factory should be associated with
	 */
	public ItemFactory(FileType area)
	{
		//make sure item data is loaded
		areaLoot = new Array<String>();
		areaLoot.addAll(lootLocations.get(area));
		areaLoot.addAll(lootLocations.get(FileType.Other));
	}
	
	public Item createItem()
	{
		return new Item(areaLoot.random(), AdjectiveFactory.getAdjective());
	}
}
