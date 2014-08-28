package core.service.interfaces;

import com.badlogic.gdx.ai.Agent;
import com.badlogic.gdx.utils.Array;

import core.datatypes.quests.Quest;
import github.nhydock.ssm.Service;

public interface IQuestContainer extends Service, Agent {

	public Array<Quest> getQuests();
	
	public Array<Quest> getAcceptedQuests();

	public static class Actions
	{
		public static final int Accept = 1;
		public static final int Gather = 2;
		public static final int Hunt = 3;
		public static final int Advance = -1;
	}
	
}
