package core.components;

import com.artemis.Component;
import com.artemis.ComponentType;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

public class Stats extends Component implements Serializable {
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
	
	public Stats(){}
	
	public Stats(int... values)
	{
		level = 1;
		hp = values[0];
		maxhp = values[0];
		strength = values[1];
		defense = values[2];
		speed = values[3];
		exp = values[4];
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
	
	public int getEvasion()
	{
		return (int)(speed);
	}
	
	public int getVitality()
	{
		return maxhp/2;
	}
	
	public float getExp()
	{
		return exp;
	}
	
	public boolean canLevelUp()
	{
		return exp >= nextExp;
	}
	
	public void levelUp(int[] stats)
	{
		level++;
		strength = stats[0];
		defense = stats[1];
		speed = stats[2];
		maxhp = stats[3]*2;
		exp = 0;
		nextExp = level * 10;
	}

	@Override
	public void write(Json json) {
		json.writeValue("str", strength);
		json.writeValue("def", defense);
		json.writeValue("spd", speed);
		json.writeValue("mhp", maxhp);
		json.writeValue("hp",  hp);
		json.writeValue("exp", exp);
		json.writeValue("lvl", level);
	}

	@Override
	public void read(Json json, JsonValue jsonData) {
		level = jsonData.getInt("lvl");
		nextExp = level * 10;
		strength = jsonData.getInt("str");
		defense = jsonData.getInt("def");
		speed = jsonData.getInt("spd");
		maxhp = jsonData.getInt("mhp");
		hp = jsonData.getInt("hp");
		exp= jsonData.getInt("exp");
	}
}
