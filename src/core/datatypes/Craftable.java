package core.datatypes;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ObjectMap;

public class Craftable extends Item
{
	/**
	 * String - item type
	 * Integer - quantity required
	 */
	private ObjectMap<String, Integer> requirements;
	private boolean made = false;
	
	public Craftable(String name, String adj, String... parts) {
		super(name, adj, null);
		
		requirements = new ObjectMap<String, Integer>();

		for (int i = 0; i < parts.length; i++)
		{
			String part = parts[i];
			int count = MathUtils.random(1, 10);
			requirements.put(part, count);
		}
	}
	
	public ObjectMap<String, Integer> getRequirements()
	{
		return requirements;
	}
	
	public String toString()
	{
		return String.format("[%s] %s %s", (made)?"x":" ", adj, name);
	}
}