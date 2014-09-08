package core.datatypes.quests;

import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.math.MathUtils;

import factories.MonsterFactory;

/**
 * Basic kill x of monster y quest.  Generic hunts only require
 * killing of a specific species, regardless of adjective.
 * 
 * @author nhydock
 */
public class Hunt extends Quest {
	
	private String monster;
	private int need;
	private int hunted;
	
	/**
	 * Creates a new randomly generated hunt quest.
	 */
	public Hunt()
	{
		this.need = MathUtils.random(3, 15);
		this.monster = MonsterFactory.randomName();
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		/**
		 * Increment hunted count of the name of the monster slain
		 * is the kind we are looking for
		 */
		if (msg.message == Actions.Hunt)
		{
			String name = (String)msg.extraInfo;
			if (name.equals(monster))
			{
				hunted++;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isDone() {
		return hunted >= need;
	}

	@Override
	public void accept() {
		//do nothing special
	}

	@Override
	protected String getType() {
		return "hunt";
	}

	@Override
	public String getObjective() {
		return monster;
	}

	@Override
	public String getObjectivePrompt() {
		return String.format("Hunt %s: %d", monster, need);
	}
}
