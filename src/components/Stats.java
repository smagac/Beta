package components;

import com.artemis.Component;
import com.artemis.ComponentType;

public class Stats extends Component {
	/**
	 * Fast referencing type for this component
	 */
	public static final ComponentType CType = ComponentType.getTypeFor(Stats.class);	
	
	public int hp;
	public final int maxhp;
	public final int mp;
	public final int strength;
	public final int defense;
	public final int magic;
	public final int speed;
	
	public Stats(int... values)
	{
		hp = values[0];
		maxhp = values[0];
		mp = values[1];
		strength = values[2];
		defense = values[3];
		magic = values[4];
		speed = values[5];
	}
}
