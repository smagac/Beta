package GenericComponents;

import com.artemis.Component;
import com.badlogic.gdx.math.MathUtils;

public class Combat extends Component {

	String[] attacks;
	String[] magic;
	
	public Combat(String[] attacks, String[] magic)
	{
		this.attacks = attacks;
		this.magic = magic;
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
