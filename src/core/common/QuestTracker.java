package core.common;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

import core.datatypes.quests.Quest;
import core.datatypes.quests.Quest.Actions;
import core.datatypes.quests.Quest.QuestFactory;
import core.service.interfaces.IQuestContainer;

public class QuestTracker implements IQuestContainer {

	private static final int MAX_QUESTS = 10;
	
	Array<Quest> quests;
	Array<Quest> activeQuests;
	QuestFactory factory;
	
	public QuestTracker()
	{
		factory = new QuestFactory();
	}
	
	/**
	 * Prepares a new set of quests.  Do this when starting a new game
	 */
	@Override
	public void prepare()
	{
		quests = new Array<Quest>();
		activeQuests = new Array<Quest>();
		
		refresh();
	}
	
	/**
	 * Our tracker is entirely turn based, so we don't need to use this
	 */
	@Override
	public final void update(float delta) {	/* do nothing */ }

	@Override
	public boolean handleMessage(Telegram msg) {
		if (msg.message == Actions.Advance)
		{
			refresh();
			return true;
		}
		if (msg.message == Actions.Gather || msg.message == Actions.Hunt)
		{
			for (Quest q : activeQuests)
			{
				q.handleMessage(msg);
			}
			return true;
		}
		return false;
	}

	/**
	 * @return list of all currently available quests
	 */
	@Override
	public Array<Quest> getQuests() {
		return quests;
	}

	/**
	 * @return list of all quests that the player has accepted
	 */
	@Override
	public Array<Quest> getAcceptedQuests() {
		return activeQuests;
	}

	
	/**
	 * Updates the current quest listing and all accepted quests by advancing the time
	 * by one day.
	 */
	private void refresh()
	{
		for (int i = 0; i < quests.size;)
		{
			Quest q = quests.get(i);
			q.addDay();
			
			if (q.hasExpired())
			{
				quests.removeIndex(i);
				//inform any quest listeners that a quest has expired
				//only care about notifying of quests that have been accepted by the player
				if (activeQuests.contains(q, true))
				{
					activeQuests.removeValue(q, true);
					MessageDispatcher.getInstance().dispatchMessage(0, this, null, Actions.Expired, q);
				}
			}
			else
			{
				i++;
			}
		}
		
		for (int i = MAX_QUESTS - quests.size; i > 0; i--)
		{
			boolean makeQuest = MathUtils.randomBoolean(.3f);
			if (makeQuest)
			{
				quests.add(factory.createQuest());
			}
		}
	}
	
	/**
	 * Accepts a quest
	 * @param q
	 */
	@Override
	public void accept(Quest q)
	{
		if (q == null)
		{
			throw (new NullPointerException("Can not insert null quests into the tracker"));
		}
		activeQuests.add(q);
	}

	/**
	 * Completes a quest if it is done and hasn't expired and rewards the user
	 * @param q
	 * @return
	 */
	@Override
	public boolean complete(Quest q)
	{
		if (!q.hasExpired() && q.isDone())
		{
			activeQuests.removeValue(q, true);
			return true;
		}
		return false;
	}
}
