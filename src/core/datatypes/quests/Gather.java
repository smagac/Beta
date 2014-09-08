package core.datatypes.quests;

import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.math.MathUtils;

import factories.ItemFactory;

/**
 * Basic gather x of item y quest.  Just like Hunts, generic gather
 * quests do not require fetching specific items with an assigned adjective.
 * Only need to grab enough of a item, much like crafting.
 * 
 * @author nhydock
 */
public class Gather extends Quest {
	
	private String item;
	private int need;
	private int gathered;
	
	/**
	 * Creates a new randomly generated item
	 */
	public Gather()
	{
		this.need = MathUtils.random(3, 10);
		this.item = ItemFactory.randomType();
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		/**
		 * Increment hunted count of the name of the item gathered
		 * is the kind we are looking for
		 */
		if (msg.message == Actions.Gather)
		{
			String name = (String)msg.extraInfo;
			if (name.equals(item))
			{
				gathered++;
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isDone() {
		return gathered >= need;
	}

	@Override
	public void accept() {
		//do nothing special
	}

	@Override
	protected String getType() {
		return "gather";
	}

	@Override
	public String getObjective() {
		return item;
	}

	@Override
	public String getObjectivePrompt() {
		return String.format("Find %s: %d", item, need);
	}
}
