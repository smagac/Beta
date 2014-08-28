package core.datatypes.quests;

import com.badlogic.gdx.ai.msg.Telegram;

/**
 * Basic kill x of monster y quest
 * 
 * @author nhydock
 */
public class Hunt extends Quest {
	
	private String monster;
	private int need;
	private int hunted;
	
	/**
	 * Creates a new randomly generated hunt
	 */
	public Hunt(String monster, int count)
	{
		this.need = count;
		this.monster = monster;
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
}
