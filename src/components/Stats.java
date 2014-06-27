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
	public final int mp;
	private int strength;
	private int defense;
	private int magic;
	private float speed;
	public int exp;
	
	public Stats(int... values)
	{
		level = 1;
		hp = values[0];
		maxhp = values[0];
		mp = values[1];
		strength = values[2];
		defense = values[3];
		magic = values[4];
		speed = values[5];
		exp = 0;
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
	
	public int getMagic()
	{
		return magic;
	}
	
	public boolean levelUp()
	{
		if (exp > level * 10)
		{
			level++;
			maxhp = 10+level*5;
			hp = maxhp;
			strength = 5+level;
			defense = 5+level;
			magic++;
			return true;
		}
		return false;
	}
}
