package core.datatypes.quests;

import com.badlogic.gdx.ai.Agent;

import factories.ItemFactory;
import factories.MonsterFactory;

public abstract class Quest implements Agent {

	/**
	 * Response messages for the message dispatcher
	 * @author nhydock
	 */
	public static class Actions
	{
		public static final int Advance = 1;
		public static final int Gather = 2;
		public static final int Hunt = 3;
	}
	
	//amount of time the quest is available for
	// if a quest is not completed before it expires, no rewards are given
	// if a quest is not accepted before it expires, it is replaced
	protected int expires;
	
	protected String prompt;
	
	public int getExpirationDate()
	{
		return expires;
	}
	
	public final void addDay()
	{
		expires--;
	}
	
	public final String getPrompt()
	{
		return prompt;
	}
	
	/**
	 * Format a prompt with hilarious mumbo jumbo shit
	 * @param p
	 * @return
	 */
	public final void formatPrompt(String p)
	{
		String formatted = p;
		while (formatted.contains("{item}"))
		{
			formatted = formatted.replaceFirst("{item}", ItemFactory.randomName());
		}
		
		while (formatted.contains("{enemy}"))
		{
			formatted = formatted.replaceFirst("{enemy}", MonsterFactory.randomName());
		}
		
		prompt = formatted;
	}
	
	/**
	 * Inherited from Gdx AI, ignore
	 */
	@Override
	public final void update(float delta) { }
	
	/**
	 * @return true if requirements for the quest's completion have been met
	 */
	abstract public boolean isDone(); 
	
	/**
	 * @return true if there are no more days left to complete the quest
	 */
	public final boolean hasExpired()
	{
		return expires < 0;
	}
	
	/**
	 * Allow special processes to occur when the quest has been accepted
	 * @return
	 */
	abstract public void accept();
}
