package factories;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

public class AdjectiveFactory {

	private static Array<String> allAdjectives;
	private static ObjectMap<String, String> modifierMap;
	
	private static boolean loaded;
	
	/**
	 * Load all monster definitions from the monsters.json file
	 */
	public static void init()
	{
		//only allow loading once
		if (loaded)
			return;
		
		allAdjectives = new Array<String>();
		modifierMap = new ObjectMap<String, String>();
		JsonReader json = new JsonReader();
		
		//load items
		JsonValue jv = json.parse(Gdx.files.classpath("data/modifiers.json"));
		for (JsonValue modifier : jv)
		{
			JsonValue adjectives = modifier.get("adjectives");
			String modName = modifier.name();
			Array<String> adjectiveList = new Array<String>(adjectives.asStringArray());
			for (String adj : adjectiveList)
			{
				modifierMap.put(adj, modName);
			}
			allAdjectives.addAll(adjectiveList);
		}
		loaded = true;
	}
	
	/**
	 * Get a random adjective to append to a name
	 * @return
	 */
	public static String getAdjective()
	{
		if (!loaded)
			return null;
		return allAdjectives.random();
	}

	public static String getModifierType(String adjective) {
		return modifierMap.get(adjective);
	}
}
