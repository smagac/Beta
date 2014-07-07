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
		nextExp = level*10;
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
		if (exp >= level * 10)
		{
			level++;
			maxhp = 10+level*5;
			hp = maxhp;
			strength = 5+level;
			defense = 5+level;
			nextExp = level*10;
			exp = 0;
			return true;
		}
		return false;
	}
}
