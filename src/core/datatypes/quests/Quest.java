package core.datatypes.quests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.Agent;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import factories.AdjectiveFactory;
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
	protected String title;

	public String location;
	
	public int getExpirationDate()
	{
		return expires;
	}
	
	public final void addDay()
	{
		expires--;
	}
	
	/**
	 * Prints the prompt
	 */
	@Override
	public final String toString()
	{
		return title;
	}
	
	public final String getPrompt()
	{
		return prompt;
	}
	
	public final String getTitle()
	{
		return title;
	}

	public String getLocation() {
		return location;
	}
	
	abstract protected String getType();
	
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
	
	/**
	 * @return a string representation of the objective target
	 */
	public abstract String getObjective();
	
	public abstract String getObjectivePrompt();
	
	/**
	 * Loader and generator of quest types for a QuestContainer service
	 * 
	 * @author nhydock
	 */
	public static class QuestFactory
	{
		//keep track of all quest type classes in here so we can randomly
		// generate them with ease
		private static Array<Class<? extends Quest>> questTypes;
		
		static
		{
			questTypes = new Array<Class<? extends Quest>>();
			questTypes.add(Hunt.class);
			questTypes.add(Gather.class);
		}
		
		public Quest createQuest()
		{
			//load quests on demand
			JsonReader json = new JsonReader();
			JsonValue data = json.parse(Gdx.files.classpath("data/quest.json"));
			
			Quest quest = null;
			try {
				quest = questTypes.random().newInstance();
				
				String type = quest.getType();
				quest.title = randomTitle(data);
				quest.location = randomLocation(data);
				quest.prompt = randomPrompt(data, type);
				quest.expires = MathUtils.random(1, 8);
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				System.exit(-1);
			}
			
			return quest;
		}
		/**
		 * @return a randomly selected quest title
		 */
		private String randomTitle(JsonValue jsonValue) {
			JsonValue titles = jsonValue.get("titles");
			String title = titles.getString(MathUtils.random(titles.size-1));
			
			return title;
		}

		/**
		 * Format a prompt with hilarious mumbo jumbo shit
		 * @param p
		 * @return
		 */
		private static String randomPrompt(JsonValue json, String type)
		{
			JsonValue prompts = json.get("prompts").get(type);
			String p = prompts.getString(MathUtils.random(prompts.size-1));
			
			String formatted = p;
			while (formatted.contains("~item"))
			{
				formatted = formatted.replaceFirst("~item", ItemFactory.randomName());
			}
			
			while (formatted.contains("~enemy"))
			{
				formatted = formatted.replaceFirst("~enemy", MonsterFactory.randomName());
			}
			
			while (formatted.contains("~location"))
			{
				formatted = formatted.replaceFirst("~location", randomLocation(json));
			}
			
			return formatted;
		}
		
		/**
		 * @return a randomly selected location, already formatted
		 */
		private static String randomLocation(JsonValue json)
		{
			JsonValue locations = json.get("locations");
			String location = locations.getString(MathUtils.random(locations.size-1));
			
			String formatted = ""+location;
			
			while (formatted.contains("%s"))
			{
				formatted = formatted.replaceFirst("%s", AdjectiveFactory.getAdjective());
			}
			return formatted;
		}
	}
}
