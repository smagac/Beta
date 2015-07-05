package com.nhydock.storymode.datatypes.quests;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue;
import com.nhydock.storymode.datatypes.quests.info.GatherInfo;
import com.nhydock.storymode.factories.ItemFactory;

/**
 * Basic gather x of item y quest. Just like Hunts, generic gather quests do not
 * require fetching specific items with an assigned adjective. Only need to grab
 * enough of a item, much like crafting.
 * 
 * @author nhydock
 */
public class Gather extends Quest {

    private String item;
    private int need;
    private int gathered;
    
    private String objectivePrompt;

    /**
     * Creates a new randomly generated item
     */
    public Gather() {
        this.need = MathUtils.random(3, 10);
        this.item = ItemFactory.randomNonCraftableType();
        
        objectivePrompt = String.format("Find %d %s", need, item);
    }

    @Override
    public boolean handleQuestNotification(int msg, Object info) {
        /**
         * Increment hunted count of the name of the item gathered is the kind
         * we are looking for
         */
        if (msg == Actions.Gather) {
            GatherInfo data = (GatherInfo) info;
            String name = data.itemName;
            if (name.equals(item)) {
                gathered = data.itemCount;
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isDone() {
        return gathered >= need;
    }

    @Override
    public void accept() {
        // do nothing special
    }

    @Override
    protected String getType() {
        return "gather";
    }

    @Override
    public String getObjective() {
        return item;
    }

    @Override
    public String getObjectivePrompt() {
        return objectivePrompt;
    }

    @Override
    public String getObjectiveProgress() {
        return String.format("Found %d/%d %s", gathered, need, item);
    }

    @Override
    public float getProgress() {
        return gathered / (float) need;
    }

    @Override
    public void write(Json json) {
        json.writeValue("objectPrompt", objectivePrompt);
        json.writeValue("prompt", objectivePrompt);
        json.writeValue("need", need);
        json.writeValue("have", gathered);
        json.writeValue("objective", item);
        json.writeValue("title", title);
        json.writeValue("expires", expires);
        json.writeValue("loc", location);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        objectivePrompt = jsonData.getString("objectivePrompt", "");
        prompt = jsonData.getString("prompt");
        need = jsonData.getInt("need");
        gathered = jsonData.getInt("have");
        item = jsonData.getString("objective");
        title = jsonData.getString("title");
        expires = jsonData.getInt("expires");
        location = jsonData.getString("loc");
    }
    

}
