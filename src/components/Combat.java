package components;

import com.artemis.Component;
import com.badlogic.gdx.math.MathUtils;

import core.datatypes.Item;

public class Combat extends Component {

	String[] attacks;
	String[] magic;
	Item itemDrop;
	
	public Combat(String[] attacks, String[] magic)
	{
		this.attacks = attacks;
		this.magic = magic;
	}
	
	/**
	 * Set the item type this entity will drop when killed
	 * @param item
	 */
	public void setDrop(Item item)
	{
		itemDrop = item;
	}
	
	/**
	 * @return get the loot dropped when the entity is killed
	 */
	public Item getDrop()
	{
		return itemDrop;
	}
	
	/**
	 * Get a random attack name
	 * @return a string
	 */
	public String getAttack()
	{
		return attacks[MathUtils.random(attacks.length)];
	}
	
	/**
	 * Get a random spell name
	 * @return a string
	 */
	public String getSpell()
	{
		return magic[MathUtils.random(magic.length)];
	}
}
