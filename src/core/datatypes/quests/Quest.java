package core.datatypes.quests;

import factories.ItemFactory;
import factories.MonsterFactory;

public abstract class Quest {

	protected int expires;
	
	public int getExpirationDate()
	{
		return expires;
	}
	
	public void addDay()
	{
		expires--;
	}
	
	abstract public String getPrompt();
	
	/**
	 * Format a prompt with hilarious mumbo jumbo shit
	 * @param p
	 * @return
	 */
	protected String formatPrompt(String p)
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
		
		return formatted;
	}
}
