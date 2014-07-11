package components;

import com.artemis.Component;
import com.artemis.ComponentType;

public class Stats extends Component {
	/**
	 * Fast referencing type for this component
	 */
	public static final ComponentType CType = ComponentType.getTypeFor(Stats.class);	
	
	private int level;
	public int hp;
	public int maxhp;
	private int strength;
	private int defense;
	private float speed;
	public int exp;
	public int nextExp;
	public boolean hidden;
	
	public Stats(int... values)
	{
		level = 1;
		hp = values[0];
		maxhp = values[0];
		strength = values[1];
		defense = values[2];
		speed = values[3];
		exp = values[4];
		exp = 0;
		nextExp = 10;
	}
	
	public int getLevel()
	{
		return level;
	}
	
	public int getStrength()
	{
		return strength;
	}
	
	public int getDefense()
	{
		return defense;
	}
	
	public float getSpeed()
	{
		return speed;
	}
	
	public float getExp()
	{
		return exp;
	}
	
	public boolean levelUp()
	{
		if (exp >= nextExp)
		{
			level++;
			maxhp += 5;
			hp = maxhp;
			strength++;
			defense++;
			nextExp += 10;
			exp = 0;
			return true;
		}
		return false;
	}
}
