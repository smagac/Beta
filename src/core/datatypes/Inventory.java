package core.datatypes;

import github.nhydock.ssm.ServiceManager;
import scenes.Messages;
import scenes.Messages.Player.ItemMsg;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectIntMap.Keys;

import core.datatypes.quests.Quest;
import core.datatypes.quests.info.GatherInfo;
import core.factories.AdjectiveFactory;
import core.factories.CraftableFactory;
import core.factories.ItemFactory;
import core.service.implementations.ScoreTracker;
import core.service.implementations.ScoreTracker.NumberValues;
import core.service.interfaces.IGame;

public class Inventory implements Serializable {
    Array<Craftable> required;
    Array<Craftable> todaysCrafts;
    ObjectIntMap<Item> loot;
    ObjectIntMap<Item> tmp;
    ObjectIntMap<Item> all;

    private int progress = 0;

    public Inventory() {
        required = new Array<Craftable>();
        todaysCrafts = new Array<Craftable>();
        loot = new ObjectIntMap<Item>();
        tmp = new ObjectIntMap<Item>();
        all = new ObjectIntMap<Item>();
    }

    public Inventory(int difficulty) {
        this();
        do {
            Craftable c = CraftableFactory.createRandomCraftable();
            if (!required.contains(c, false)) {
                required.add(c);
            }
        }
        while (required.size < difficulty * 2);

        refreshCrafts();

        if (ServiceManager.getService(IGame.class).debug()){
             //debug add loot to test crafting
             for (int i = 0; i < 100; i++)
             {
                 Item item = new Item(ItemFactory.randomType(), AdjectiveFactory.getAdjective());
                 all.put(item, all.get(item, 0) + MathUtils.random(1, 20));
             }
            
             //debug add loot to be able to craft at least one item
             Craftable c = required.random();
             for (String s : c.getRequirements().keys())
             {
                 all.put(new Item(s, AdjectiveFactory.getAdjective()),
                 c.getRequirements().get(s, 1) + MathUtils.random(1, 5));
             }
             loot.putAll(all);
        }
    }

    /**
     * Refreshes the list of today's craftable items with a new list
     */
    public void refreshCrafts() {
        todaysCrafts = new Array<Craftable>();
        todaysCrafts.clear();
        do {
            Craftable c = CraftableFactory.createRandomCraftable();
            if (!todaysCrafts.contains(c, false)) {
                todaysCrafts.add(c);
            }
        }
        while (todaysCrafts.size < 5);
    }

    /**
     * Updates the list of Crafts to be aware of if they can be made right now or not
     */
    public void refreshRequirements() {
        for (Craftable c : todaysCrafts) {
            c.canMake = canMake(c);
        }
        
        for (Craftable c : required) {
            c.canMake = canMake(c);
        }
    }

    public Array<Craftable> getTodaysCrafts() {
        return todaysCrafts;
    }

    public Array<Craftable> getRequiredCrafts() {
        return required;
    }

    public int getProgress() {
        return progress;
    }

    public float getProgressPercentage() {
        return (float) progress / (float) required.size;
    }

    public boolean canMake(Craftable c) {
        ObjectIntMap<String> requirements = c.getRequirements();
        boolean make = true;

        for (String required : requirements.keys()) {
            int need = requirements.get(required, 1);
            int have = 0;

            for (Item lootName : all.keys()) {
                if (lootName.equals(required)) {
                    Integer amount = all.get(lootName, 0);

                    have = have + amount;

                    if (have >= need) {
                        break;
                    }
                }
            }

            if (have < need) {
                make = false;
                break;
            }
        }
        return make;
    }

    /**
     * Make a craftable out of the loot you have
     * 
     * @param c
     */
    public boolean makeItem(Craftable c) {
        ObjectIntMap<String> requirements = c.getRequirements();

        if (!c.canMake) {
            //Gdx.app.log("Craft", "craftable not marked as having enough resources");
            return false;
        }

        for (String required : requirements.keys()) {
            int need = requirements.get(required, 1);
            int have = 0;

            for (Item i : all.keys()) {
                if (i.equals(required)) {
                    Integer amount = all.get(i, 0);

                    have = have + amount;

                    amount = 0;
                    if (have > need) {
                        amount = have - need;
                    }

                    if (amount == 0) {
                        //Gdx.app.log("Crafting", "removing " + i);
                        all.remove(i, 0);
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Player.RemoveItem, i);
                    }
                    else {
                        all.put(i, amount);
                        ItemMsg im = new ItemMsg();
                        im.item = i;
                        im.amount = amount;
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Player.UpdateItem, im);
                    }

                    if (have >= need) {
                        break;
                    }
                }
            }
        }

        // add the item to your loot
        Item crafted = new Item(c.name, c.adj);
        pickup(crafted);
        merge();
        
        ServiceManager.getService(ScoreTracker.class).increment(NumberValues.Items_Crafted);

        // count progress after making
        calcProgress();
        loot.clear();
        loot.putAll(all);
        return true;
    }

    private void calcProgress() {
        int p = progress;
        progress = 0;
        for (Craftable r : required) {
            Item i = null;

            Keys<Item> keys = all.keys();
            for (; keys.hasNext && i == null;) {
                Item i2 = keys.next();
                if (i2.fullname().equals(r.fullname())) {
                    i = i2;
                }
            }

            if (i != null && all.get(i, 0) > 0) {
                progress++;
            }
        }
        if (p != progress) {
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Player.Progress);
        }
    }

    public ObjectIntMap<Item> getLoot() {
        return all;
    }

    /**
     * Removes a list of items from your loot
     * 
     * @param sacrifices
     */
    public boolean sacrifice(ObjectIntMap<Item> sacrifices, int required) {
        int pieces = 0;
        boolean canSacrifice = true;
        for (Item item : sacrifices.keys()) {
            Integer i = sacrifices.get(item, 1);
            if (all.get(item, 0) < i) {
                canSacrifice = false;
                break;
            }
            else {
                pieces += i;
            }
        }

        if (pieces < required) {
            canSacrifice = false;
        }

        if (canSacrifice) {
            for (Item item : sacrifices.keys()) {
                int total = sacrifices.get(item, 1);
                int tmpCount = tmp.get(item, 0);
                int lootCount = loot.get(item, 0); 
                int tmpSub = Math.min(total, tmpCount);
                int lootSub = Math.min(total - tmpSub, lootCount);
                int allCount = (tmpCount + lootCount) - (tmpSub + lootSub);
                
                if (lootSub > 0) {
                    int count = lootCount - lootSub;
                    if (count == 0) {
                        loot.remove(item, 0);
                    }
                    else {
                        loot.put(item, count);
                    }
                }
                if (tmpSub > 0) {
                    int count = tmpCount - tmpSub;
                    if (count == 0) {
                        tmp.remove(item, 0);
                    }
                    else {
                        tmp.put(item, count);
                    }
                }
                if (allCount == 0) {
                    all.remove(item, 0);
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Player.RemoveItem, item);
                }
                else {
                    all.put(item, allCount);
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Player.UpdateItem, item);
                }
            }
            
            for (Item i : sacrifices.keys()) {
                ServiceManager.getService(ScoreTracker.class).increment(NumberValues.Loot_Sacrificed, sacrifices.get(i, 0));
            }
            
        }
        
        return canSacrifice;
    }

    /**
     * Adds a single item into the user's inventory
     * 
     * @param item
     */
    public void pickup(Item item) {
        pickup(item, 1);
    }

    /**
     * Adds a single item into the user's inventory
     * 
     * @param i
     */
    public void pickup(Item item, int i) {
        int amount = tmp.get(item, 0);
        int amount2 = all.get(item, 0);
        tmp.put(item, amount + i);
        all.put(item, amount2 + i);
        
        MessageDispatcher.getInstance().dispatchMessage(0, null, null, Quest.Actions.Gather, new GatherInfo(item.fullname(), amount2));
        
        ItemMsg im = new ItemMsg();
        im.item = item;
        im.amount = amount2 + i;
        
        if (amount == 0 && amount2 == 0) {
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Player.NewItem, im);
        } else {
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Player.UpdateItem, im);
        }
    }

    /**
     * Combines temporary loot with player's actual loot USED WHEN PLAYER LEAVES
     * THE DUNGEON W/O DYING
     */
    public void merge() {
        tmp.clear();
        loot.clear();
        loot.putAll(all);
    }

    /**
     * Abandons all picked up items that haven't been used yet USED WHEN PLAYER
     * DIES IN A DUNGEON
     */
    public void abandon() {
        tmp.clear();
        all.clear();
        all.putAll(loot);
    }

    /**
     * Get's a generic count of all items with the same base name as the
     * specified item
     * 
     * @param i
     */
    public int genericCount(String i) {
        int sum = 0;
        for (Item item : all.keys()) {
            if (item.equals(i)) {
                int c = all.get(item, 0);
                sum += c;
            }
        }
        return sum;
    }

    @Override
    public void write(Json json) {
        json.writeArrayStart("loot");
        for (Item key : all.keys()) {
            json.writeObjectStart();
            json.writeValue("name", key.name);
            json.writeValue("adj", key.adj);
            json.writeValue("count", all.get(key, 0));
            json.writeObjectEnd();
        }
        json.writeObjectEnd();
        json.writeValue("craft", required, Array.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void read(Json json, JsonValue jsonData) {

        JsonValue loot = jsonData.get("loot");
        all.clear();
        for (JsonValue item : loot) {
            all.put(new Item(item.getString("name"), item.getString("adj")), item.getInt("count"));
        }
        this.loot.putAll(all);
        required = (Array<Craftable>) json.readValue(Array.class, jsonData.get("craft"));
        refreshCrafts();
        calcProgress();
    }
}
