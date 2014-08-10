package core.datatypes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

public class Item implements Comparable<Item>, Serializable
{
	public static Array<String> items;
	public static Array<String> loot;
	public static Array<String> craftables;
	public static ObjectMap<FileType, Array<String>> lootLocations;
	
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
		JsonValue jv = json.parse(Gdx.files.classpath("core/data/items.json"));
		
		lootLocations = new ObjectMap<FileType, Array<String>>();
		items = new Array<String>();
		loot = new Array<String>();
		craftables = new Array<String>();
		
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
				craftables.add(name);
			}
		}
		loaded = true;
	}
	
	String adj;
	String name;
	
	/**
	 * Used for JsonSerializable
	 */
	public Item(){}
	
	public Item(String name, String adj)
	{
		this.adj = adj;
		this.name = name;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
	    int result = 1;
	    result = prime * result + adj.hashCode();
	    result = prime * result + name.hashCode();
	    
	    return result;
	}
	
	public String fullname()
	{
		return String.format("%s %s", adj, name);
	}
	
	public String type()
	{
		return adj;
	}
	
	@Override
	public String toString()
	{
		return fullname();
	}

	@Override
	public int compareTo(Item o) {
		//only compare names against craftables
		if (o instanceof Craftable)
		{
			Craftable c = (Craftable)o;
			return name.compareTo(c.name);
		}
		return fullname().compareTo(o.fullname());
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o == null) {
			return false;
		}
		if (o == this) {
			return true;
		}
		if (o.hashCode() == this.hashCode()) {
			return true;
		}
		if (o instanceof String)
		{
			String s = (String)o;
			return name.equals(s); 
		}
		if (o instanceof Craftable)
		{
			Craftable c = (Craftable)o;
			return name.equals(c.name);
		}
		if (o instanceof Item)
		{
			Item i = (Item)o;
			return fullname().equals(i.fullname());
		}
		return false;
	}

	@Override
	public void write(Json json) {
		json.writeValue("name", name);
		json.writeValue("adj", name);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		name = jsonData.getString("name");
		adj = jsonData.getString("adj");
	}
}