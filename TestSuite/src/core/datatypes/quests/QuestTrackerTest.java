package core.datatypes.quests;
import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.backends.headless.HeadlessApplication;

import core.datatypes.Craftable;
import core.datatypes.QuestTracker;
import core.datatypes.QuestTracker.Reward;
import core.datatypes.quests.Hunt;
import core.datatypes.quests.Quest;
import core.datatypes.quests.Quest.QuestFactory;
import core.factories.*;

public class QuestTrackerTest {

	private class TestApp extends Game
	{

		@Override
		public void create() {
			
		}
		
	}

	HeadlessApplication app;
	
	@Before
	public void setup()
	{
		app = new HeadlessApplication(new TestApp());
		ItemFactory.init();
		CraftableFactory.init();
		MonsterFactory.init();
		AdjectiveFactory.init();
	}
	
	@After
	public void tearDown()
	{
		app.exit();	
	}
	
	@Test
	public void testAcceptingQuest() {
		QuestTracker qt = new QuestTracker();
		
		QuestFactory qf = new QuestFactory();
		Quest sample = qf.createQuest();
		
		assertEquals(0, qt.getAcceptedQuests().size);
		
		qt.accept(sample);
		assertEquals(1, qt.getAcceptedQuests().size);
		
		//don't allow adding the same quest multiple times
		qt.accept(sample);
		assertEquals(1, qt.getAcceptedQuests().size);
	}

	
	@Test
	public void testCompletingQuest() {
		QuestTracker qt = new QuestTracker();
		
		Hunt hunt = new Hunt();
		
		qt.accept(hunt);
		
		assertTrue(qt.getAcceptedQuests().contains(hunt, true));
		assertEquals(0.0f, hunt.getProgress(), .01f);
		
		//attempt completing a quest when not done
		assertFalse(qt.complete(hunt));
		
		String monster = hunt.getObjective();
		while (hunt.getProgress() < 1.0f)
		{
			MessageDispatcher.getInstance().dispatchMessage(0, null, qt, Quest.Actions.Hunt, monster);
		}
		
		assertEquals(1.0f, hunt.getProgress(), 1.0f);
		
		assertTrue(qt.complete(hunt));
		assertFalse(qt.getAcceptedQuests().contains(hunt, true));
		
		
	}
	
	@Test
	public void testGettingReward() {
		QuestTracker qt = new QuestTracker();
		
		Craftable c = CraftableFactory.createRandomCraftable();
		Reward reward = qt.getReward(c);
		
		assertTrue(c.getRequirementTypes().contains(reward.item.type(), false));
		assertTrue(reward.count <= c.getRequirements().get(reward.item.type(), 0));
		assertTrue(reward.count >= 1);
		
	}
}
