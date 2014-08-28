package core.service.interfaces;

import com.badlogic.gdx.ai.Agent;
import com.badlogic.gdx.utils.Array;

import core.datatypes.quests.Quest;
import github.nhydock.ssm.Service;

public interface IQuestContainer extends Service, Agent {
	public Array<Quest> getQuests();
	public Array<Quest> getAcceptedQuests();
	public void accept(Quest q);
	public boolean complete(Quest q);
}
