package core.datatypes;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

import core.datatypes.quests.Quest;
import core.datatypes.quests.Quest.Actions;
import core.datatypes.quests.Quest.QuestFactory;
import core.factories.AdjectiveFactory;

public class QuestTracker implements Telegraph, Serializable {

    private static final int MAX_QUESTS = 10;

    Array<Quest> quests;
    Array<Quest> activeQuests;
    QuestFactory factory;

    public QuestTracker() {
        factory = new QuestFactory();
        quests = new Array<Quest>();
        activeQuests = new Array<Quest>();
        refresh();
        
        MessageDispatcher.getInstance().addListener(this, Actions.Gather);
        MessageDispatcher.getInstance().addListener(this, Actions.Hunt);
        MessageDispatcher.getInstance().addListener(this, Actions.Used);
    }

    @Override
    public boolean handleMessage(Telegram msg) {
        if (msg.message == Actions.Advance) {
            refresh();
            return true;
        }
        if (msg.message == Actions.Gather || msg.message == Actions.Hunt || msg.message == Actions.Used) {
            for (Quest q : activeQuests) {
                q.handleMessage(msg);
            }
            return true;
        }
        return false;
    }

    /**
     * @return list of all currently available quests
     */
    public Array<Quest> getQuests() {
        return quests;
    }

    /**
     * @return list of all quests that the player has accepted
     */
    public Array<Quest> getAcceptedQuests() {
        return activeQuests;
    }

    /**
     * Updates the current quest listing and all accepted quests by advancing
     * the time by one day.
     */
    private void refresh() {
        for (int i = 0; i < quests.size;) {
            Quest q = quests.get(i);
            q.addDay();

            if (q.hasExpired()) {
                quests.removeIndex(i);
                // inform any quest listeners that a quest has expired
                // only care about notifying of quests that have been accepted
                // by the player
                if (activeQuests.contains(q, true)) {
                    activeQuests.removeValue(q, true);
                    MessageDispatcher.getInstance().dispatchMessage(0, this, null, Actions.Expired, q);
                }
            }
            else {
                i++;
            }
        }

        for (int i = MAX_QUESTS - quests.size; i > 0; i--) {
            boolean makeQuest = MathUtils.randomBoolean(.3f);
            if (makeQuest) {
                quests.add(factory.createQuest());
            }
        }
    }

    /**
     * Accepts a quest
     * 
     * @param q
     */
    public void accept(Quest q) {
        if (q == null) {
            throw (new NullPointerException("Can not insert null quests into the tracker"));
        }
        if (!activeQuests.contains(q, true)) {
            activeQuests.add(q);
        }
    }

    /**
     * @param craft
     *            - craft to pick random requirement from
     * @return a random amount of an item that is required for crafting
     */
    public Reward getReward(Craftable craft) {
        String name = craft.getRequirementTypes().random();
        int rand = MathUtils.random(1, craft.getRequirements().get(name));

        Item item = new Item(name, AdjectiveFactory.getAdjective());

        Reward reward = new Reward(item, rand);

        return reward;
    }

    /**
     * Completes a quest if it is done and hasn't expired and rewards the user
     * 
     * @param q
     * @return
     */
    public boolean complete(Quest q) {
        if (!q.hasExpired() && q.isDone()) {
            activeQuests.removeValue(q, true);
            return true;
        }
        return false;
    }

    /**
     * Reward object container gifted by quests upon completion
     * 
     * @author nhydock
     */
    public static class Reward {
        public final Item item;
        public final int count;

        public Reward(Item item, int amount) {
            this.item = item;
            this.count = amount;
        }
    }

    @Override
    public void write(Json json) {
        json.writeValue("active", activeQuests, Array.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void read(Json json, JsonValue jsonData) {
        activeQuests = (Array<Quest>) json.readValue(Array.class, jsonData.get("active"));
        quests.addAll(activeQuests);
    }
}
