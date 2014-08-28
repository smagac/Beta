package core.common;

import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.utils.Array;

import core.datatypes.quests.Quest;
import core.service.interfaces.IQuestContainer;

public class QuestTracker implements IQuestContainer {

	Array<Quest> quests;
	Array<Quest> activeQuests;
	
	/**
	 * Our tracker is entirely turn based, so we don't need to use this
	 */
	@Override
	public void update(float delta) {	/* do nothing */ }

	@Override
	public boolean handleMessage(Telegram msg) {
		if (msg.message == Actions.Accept)
		{
			accept((Quest)msg.extraInfo);
		}
		if (msg.message == Actions.Advance)
		{
			refresh();
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
	private void accept(Quest q)
	{
		
	}

}
