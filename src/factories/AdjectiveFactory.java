package factories;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

public class AdjectiveFactory {

	private static Array<String> adjectives;
	private static boolean loaded;
	/**
	 * Load all monster definitions from the monsters.json file
	 */
	public static void init()
	{
		//only allow loading once
		if (loaded)
			return;
		
		adjectives = new Array<String>();
		JsonReader json = new JsonReader();
		
		//load items
		JsonValue jv = json.parse(Gdx.files.classpath("core/data/words.json"));
		adjectives.addAll(jv.get(0).asStringArray());
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
		return adjectives.random();
	}
}
