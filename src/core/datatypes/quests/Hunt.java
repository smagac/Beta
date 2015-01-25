package core.datatypes.quests;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;

import core.factories.MonsterFactory;

/**
 * Basic kill x of monster y quest. Generic hunts only require killing of a
 * specific species, regardless of adjective.
 * 
 * @author nhydock
 */
class Hunt extends Quest {

    private String monster;
    private int need;
    private int hunted;

    private String prompt;
    
    /**
     * Creates a new randomly generated hunt quest.
     */
    public Hunt() {
        this.need = MathUtils.random(3, 15);
        
        do {
            monster = MonsterFactory.randomSpecies();
        } 
        while (monster.equals("treasure chest"));
        
        prompt = String.format("Hunt %d %s", need, monster);
    }

    @Override
    public boolean handleQuestNotification(int msg, Object info) {
        /**
         * Increment hunted count of the name of the monster slain is the kind
         * we are looking for
         */
        if (msg == Actions.Hunt) {
            String name = (String) info;
            if (name.contains(monster)) {
                hunted++;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isDone() {
        return hunted >= need;
    }

    @Override
    public void accept() {
        // do nothing special
    }

    @Override
    protected String getType() {
        return "hunt";
    }

    @Override
    public String getObjective() {
        return monster;
    }

    @Override
    public String getObjectivePrompt() {
        return prompt;
    }

    @Override
    public String getObjectiveProgress() {
        return String.format("Hunted %d/%d %s", hunted, need, monster);
    }

    @Override
    public float getProgress() {
        return hunted / (float) need;
    }

    @Override
    public void write(Json json) {
        json.writeValue("prompt", prompt);
        json.writeValue("need", need);
        json.writeValue("have", hunted);
        json.writeValue("objective", monster);
        json.writeValue("title", title);
        json.writeValue("expires", expires);
        json.writeValue("loc", location);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        prompt = jsonData.getString("prompt");
        need = jsonData.getInt("need");
        hunted = jsonData.getInt("have");
        monster = jsonData.getString("objective");
        title = jsonData.getString("title");
        expires = jsonData.getInt("expires");
        location = jsonData.getString("loc");
    }
}
