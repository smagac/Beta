package core.datatypes.quests;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import core.DataDirs;
import core.factories.AdjectiveFactory;
import core.factories.ItemFactory;
import core.factories.MonsterFactory;

public abstract class Quest implements Telegraph, Serializable {

    /**
     * Response messages for the message dispatcher
     * 
     * @author nhydock
     */
    public static class Actions {
        public static final int Advance = 0x9001;
        public static final int Gather = 0x9002;
        public static final int Hunt = 0x9003;

        // send notifications to generic listeners of quests (AKA UI)
        // allows those systems to display progress information of the quest
        public static final int Notify = 0x9004;
        public static final int Expired = 0x9005;
        public static final int Used = 0x9006;
    }

    // amount of time the quest is available for
    // if a quest is not completed before it expires, no rewards are given
    // if a quest is not accepted before it expires, it is replaced
    protected int expires;

    protected String prompt;
    protected String title;

    public String location;

    public int getExpirationDate() {
        return expires;
    }

    public final void addDay() {
        expires--;
    }

    /**
     * Prints the prompt
     */
    @Override
    public final String toString() {
        return title;
    }

    public final String getPrompt() {
        return prompt;
    }

    public final String getTitle() {
        return title;
    }

    public String getLocation() {
        return location;
    }

    abstract protected String getType();

    /**
     * @return true if requirements for the quest's completion have been met
     */
    abstract public boolean isDone();

    /**
     * @return true if there are no more days left to complete the quest
     */
    public final boolean hasExpired() {
        return expires < 0;
    }

    /**
     * Allow special processes to occur when the quest has been accepted
     * 
     * @return
     */
    abstract public void accept();

    /**
     * @return a string representation of the objective target
     */
    public abstract String getObjective();

    public abstract String getObjectivePrompt();

    public abstract String getObjectiveProgress();

    public abstract float getProgress();

    protected abstract boolean handleQuestNotification(int msg, Object info);

    @Override
    public final boolean handleMessage(Telegram msg) {
        if (handleQuestNotification(msg.message, msg.extraInfo)) {
            MessageDispatcher.getInstance().dispatchMessage(0, this, null, Quest.Actions.Notify, getObjectiveProgress());
            return true;
        }
        return false;
    }

    /**
     * Loader and generator of quest types for a QuestContainer service
     * 
     * @author nhydock
     */
    public static class QuestFactory {
        // keep track of all quest type classes in here so we can randomly
        // generate them with ease
        private static Array<Class<? extends Quest>> questTypes;

        static {
            questTypes = new Array<Class<? extends Quest>>();
            questTypes.add(Hunt.class);
            questTypes.add(Gather.class);
        }

        public Quest createQuest() {
            // load quests on demand
            JsonReader json = new JsonReader();
            JsonValue data = json.parse(Gdx.files.classpath(DataDirs.GameData + "quest.json"));

            Quest quest = null;
            try {
                quest = questTypes.random().newInstance();
                quest.title = randomTitle(data);
                quest.location = randomLocation(data);
                quest.prompt = randomPrompt(data, quest);
                quest.expires = MathUtils.random(1, 8);
            }
            catch (InstantiationException | IllegalAccessException e) {
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
            String title = titles.getString(MathUtils.random(titles.size - 1));

            return title;
        }

        /**
         * Format a prompt with hilarious mumbo jumbo shit
         * 
         * @param p
         * @return
         */
        private static String randomPrompt(JsonValue json, Quest quest) {
            JsonValue prompts = json.get("prompts").get(quest.getType());
            String p = prompts.getString(MathUtils.random(prompts.size - 1));

            String formatted = p;
            while (formatted.contains("~item")) {
                formatted = formatted.replaceFirst("~item", ItemFactory.randomName());
            }

            while (formatted.contains("~enemy")) {
                formatted = formatted.replaceFirst("~enemy", MonsterFactory.randomName());
            }

            while (formatted.contains("~location")) {
                formatted = formatted.replaceFirst("~location", quest.getLocation());
            }

            while (formatted.contains("~adjective")) {
                formatted = formatted.replaceFirst("~adjective", AdjectiveFactory.getAdjective());
            }

            while (formatted.contains("~objective")) {
                formatted = formatted.replaceFirst("~objective", quest.getObjective());
            }

            return formatted;
        }

        /**
         * @return a randomly selected location, already formatted
         */
        private static String randomLocation(JsonValue json) {
            JsonValue locations = json.get("locations");
            String location = locations.getString(MathUtils.random(locations.size - 1));

            String formatted = new String(location);

            while (formatted.contains("%s")) {
                formatted = formatted.replaceFirst("%s", AdjectiveFactory.getAdjective());
            }
            return formatted;
        }
    }
}
