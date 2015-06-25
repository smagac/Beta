package core.factories;

import java.util.Iterator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

import core.DataDirs;
import core.datatypes.Craftable;
import core.datatypes.FileType;
import core.datatypes.Item;
import core.datatypes.quests.Gather;
import core.datatypes.quests.Quest;
import core.service.interfaces.IPlayerContainer;

public final class ItemFactory {
    protected static Array<String> items;
    protected static Array<String> craftables;
    protected static Array<String> loot;
    protected static ObjectMap<FileType, Array<String>> lootLocations;

    private static boolean loaded;

    /**
     * Preload all item data
     */
    public static void init() {
        // only allow loading once
        if (loaded)
            return;

        JsonReader json = new JsonReader();

        // load items
        JsonValue jv = json.parse(Gdx.files.classpath(DataDirs.GameData + "items.json"));

        lootLocations = new ObjectMap<FileType, Array<String>>();
        items = new Array<String>();
        loot = new Array<String>();
        craftables = new Array<String>();

        for (FileType type : FileType.values()) {
            Array<String> tLoot = new Array<String>();
            for (JsonValue data : jv.get(type.toString())) {
                String name = data.asString();
                items.add(name);
                tLoot.add(name);
                loot.add(name);
            }
            lootLocations.put(type, tLoot);
        }
        {
            for (JsonValue data : jv.get("craftable")) {
                String name = data.asString();
                items.add(name);
                craftables.add(name);
            }
        }
        loaded = true;
    }

    public static String randomName() {
        return AdjectiveFactory.getAdjective() + " " + items.random();
    }

    public static String randomNonCraftable() {
        return AdjectiveFactory.getAdjective() + " " + loot.random();
    }

    public static String randomType() {
        return items.random();
    }

    public static String randomNonCraftableType() {
        return loot.random();
    }

    private final Array<String> areaLoot;

    /**
     * Generates an item factory useful for a specific type of dungeon
     * 
     * @param area
     *            - filetype of the dungeon that the item factory should be
     *            associated with
     */
    public ItemFactory(FileType area) {
        // make sure item data is loaded
        areaLoot = new Array<String>();
        areaLoot.addAll(lootLocations.get(area));
        areaLoot.addAll(lootLocations.get(FileType.Other));
    }

    /**
     * Generates a random item
     * @return
     */
    public Item createItem() {
        return new Item(areaLoot.random(), AdjectiveFactory.getAdjective());
    }
    
    /**
     * Create an item from this factory with a priority on being an item that assists
     * in reaching an objective of either a required craft or gather quest.
     * @param playerService
     * @return
     */
    public Item createObjective(IPlayerContainer playerService) {
        Array<String> objectiveTypes = new Array<String>();
        
        for (Craftable c : playerService.getInventory().getRequiredCrafts()){
            objectiveTypes.addAll(c.getRequirementTypes());
        }
        
        for (Quest q : playerService.getQuestTracker().getAcceptedQuests()){
            if (q instanceof Gather) {
                objectiveTypes.add(q.getObjective());
            }
        }
        
        //only create loot that can be made by this factory
        Iterator<String> types = objectiveTypes.iterator();
        while (types.hasNext()) {
            String type = types.next();
            if (!areaLoot.contains(type, false)) {
                types.remove();
            }
        }
        
        if (objectiveTypes.size == 0) {
            return createItem();
        } else {
            return new Item(objectiveTypes.random(), AdjectiveFactory.getAdjective());
        }
    }
}
