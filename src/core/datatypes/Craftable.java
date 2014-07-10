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
	
	public Craftable(String name, String adj, String... parts) {
		super(name, adj);
		
		requirements = new ObjectMap<String, Integer>();

		for (int i = 0; i < parts.length; i++)
		{
			String part = parts[i];
			int count = MathUtils.random(1, 5);
			requirements.put(part, count);
		}
	}
	
	public ObjectMap<String, Integer> getRequirements()
	{
		return requirements;
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
			return fullname().equals(c.fullname());
		}
		if (o instanceof Item)
		{
			Item i = (Item)o;
			return name.equals(i.name);
		}
		return false;
	}
}