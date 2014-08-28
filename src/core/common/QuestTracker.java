package core.common;

import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.utils.Array;

import core.datatypes.quests.Quest;
import core.datatypes.quests.Quest.Actions;
import core.service.interfaces.IQuestContainer;

public class QuestTracker implements IQuestContainer {

	Array<Quest> quests;
	Array<Quest> activeQuests;
	
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
		for (Quest q : quests)
		{
			q.addDay();
		}
	}
	
	/**
	 * Accepts a quest
	 * @param q
	 */
	@Override
	public void accept(Quest q)
	{
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
