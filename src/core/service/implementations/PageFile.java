package core.service.implementations;

import java.util.Iterator;

import github.nhydock.ssm.Service;
import github.nhydock.ssm.ServiceManager;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectSet;

import core.datatypes.StatModifier;
import core.factories.AdjectiveFactory;
import core.factories.MonsterFactory;
import core.factories.MonsterFactory.MonsterTemplate;
import core.service.interfaces.IGame;

public final class PageFile implements Serializable, Service {
    
    ObjectIntMap<NumberValues> numeric;
    ObjectMap<StringValues, ObjectIntMap<String>> strings;
    ObjectSet<String> discoveredModifiers;
    ObjectIntMap<MonsterTemplate> discoveredMonsters;
    
    
    public PageFile(){
        numeric = new ObjectIntMap<NumberValues>();
        for (NumberValues nv : NumberValues.values()) {
            numeric.put(nv, 0);
        }

        strings = new ObjectMap<StringValues, ObjectIntMap<String>>();
        for (StringValues sv : StringValues.values()) {
            strings.put(sv, new ObjectIntMap<String>());
        }
        
        discoveredMonsters = new ObjectIntMap<MonsterFactory.MonsterTemplate>();
        discoveredModifiers = new ObjectSet<String>();
        
        if (ServiceManager.getService(IGame.class).debug()){
            //debug add modifiers and monsters to test pagefile view
            for (int i = 0; i < 10; i++){
                discover(AdjectiveFactory.getAdjective());
            }
            
            for (int i = 0; i < 5; i++){
                discover(MonsterFactory.getMonster(MonsterFactory.randomSpecies()), MathUtils.random(99));
            }
            
        }
    }
    
    /**
     * Reset all the tracker's values
     */
    public void reset() {
        for (NumberValues nv : NumberValues.values()) {
            numeric.put(nv, 0);
        }

        for (StringValues sv : StringValues.values()) {
            strings.get(sv).clear();
        }
    }

    /**
     * @return calculated score from tracker values
     */
    public int score() {
        // calculate score
        float score = 0;

        // more points awarded per item crafted
        score += numeric.get(NumberValues.Items_Crafted, 0) * 1000;

        // less files needed to explore per item crafted, better your score
        score += ((10 * numeric.get(NumberValues.Items_Crafted, 0)) - numeric.get(NumberValues.Files_Explored, 0)) * 100;

        // get cool points for enemy kdr
        score += (numeric.get(NumberValues.Monsters_Killed, 0) / (numeric.get(NumberValues.Times_Died, 0) + 1)) * 100;

        // less you sleep more points you get
        score += (numeric.get(NumberValues.Times_Slept, 0) + numeric.get(NumberValues.Loot_Sacrificed, 0)) * -50;

        // get points for each piece of loot you find
        score += numeric.get(NumberValues.Loot_Found, 0) * 10;

        return (int) Math.max(0, score);
    }

    /**
     * Calculate a rank for the player based on their tracked stats
     * 
     * @return
     */
    public String rank() {

        String rank = "Straight Shooter";
        if (score() > 50000) {
            rank = "Going for the Gold";
        }
        if (numeric.get(NumberValues.Monsters_Killed, 0) > 500) {
            rank = "Poacher";
        }
        if (numeric.get(NumberValues.Monsters_Killed, 0) > 1000) {
            rank = "Exterminator";
        }
        if (numeric.get(NumberValues.Files_Explored, 0) < 10) {
            rank = "Crawler";
        }
        if (numeric.get(NumberValues.Items_Crafted, 0) > 10) {
            rank = "Apprentice";
        }
        if (numeric.get(NumberValues.Files_Explored, 0) > 10) {
            rank = "Logger";
        }
        if (numeric.get(NumberValues.Deepest_Floor_Traveled, 0) < 20) {
            rank = "Scaredy Cat";
        }
        if (numeric.get(NumberValues.Times_Slept, 0) > 100) {
            rank = "Narclyptic";
        }
        if (numeric.get(NumberValues.Items_Crafted, 0) > 15) {
            rank = "Favourite Slave";
        }
        if (numeric.get(NumberValues.Loot_Sacrificed, 0) > 500) {
            rank = "Garbage Dump";
        }
        if (numeric.get(NumberValues.Files_Explored, 0) > 20) {
            rank = "Living Virus";
        }
        if (numeric.get(NumberValues.Items_Crafted, 0) > 25) {
            rank = "Crafting Addict";
        }
        if (numeric.get(NumberValues.Monsters_Killed, 0) > 10000) {
            rank = "Mass Extintion";
        }
        if (numeric.get(NumberValues.Files_Explored, 0) == 0) {
            rank = "Dungeon Gambler";
        }
        if (numeric.get(NumberValues.Times_Slept, 0) < 10) {
            rank = "Insomniac";
        }
        if (numeric.get(NumberValues.Items_Crafted, 0) > 50) {
            rank = "Dedicated Follower";
        }
        if (numeric.get(NumberValues.Items_Crafted, 0) > 70) {
            rank = "Westboro Craftist";
        }
        if (numeric.get(NumberValues.Times_Died, 0) < 10) {
            rank = "Played it Safe";
        }
        if (numeric.get(NumberValues.Items_Crafted, 0) > 100) {
            rank = "Newly Appointed Crafting God";
        }

        return rank;
    }
    
    public void increment(NumberValues value) {
        numeric.put(value, numeric.get(value, 0) + 1);
    }
    
    public void decrement(NumberValues value) {
        numeric.put(value, Math.max(0, numeric.get(value, 1) - 1));
    }
    
    public void set(NumberValues value, int n) {
        numeric.put(value, n);
    }
    
    public int get(NumberValues value) {
        return numeric.get(value, 0);
    }
    
    public void increment(StringValues value, String str) {
        ObjectIntMap<String> vals = strings.get(value);
        vals.put(str, vals.get(str, 0)+1);
    }
    
    public void decrement(StringValues value, String str) {
        ObjectIntMap<String> vals = strings.get(value);
        vals.put(str, Math.max(0, vals.get(str, 1)-1));
    }
    
    /**
     * Get the formatted numeric string value with tag attached
     * @return
     */
    public String toString(NumberValues value) {
        int count = numeric.get(value, 0);
        if (value.tag != null) {
            return String.format("%d %s", count, value.tag);
        }
        return String.valueOf(count);
    }

    public String max(StringValues value) {
        String m = null;
        int max = Integer.MIN_VALUE;
        ObjectIntMap<String> counters = strings.get(value);
        for (String key : counters.keys()) {
            int k = counters.get(key, 0);
            if (k > max) {
                max = k;
                m = key;
            }
        }
        return m;
    }

    public String min(StringValues value) {
        String m = null;
        int max = Integer.MAX_VALUE;
        ObjectIntMap<String> counters = strings.get(value);
        for (String key : counters.keys()) {
            int k = counters.get(key, 0);
            if (k < max) {
                max = k;
                m = key;
            }
        }
        return m;
    }
    
    /**
     * Values to keep track of over the course of the game
     * 
     * @author nhydock
     *
     */
    public static enum NumberValues {
        Times_Slept,
        Monsters_Killed,
        Items_Crafted,
        Loot_Found,
        Times_Died,
        Loot_Sacrificed,
        Files_Explored,
        Deepest_Floor_Traveled,
        Largest_File("kb");

        private String tag;
        private String name;

        NumberValues() {
            name = String.format("%s", name().replace('_', ' '));
        }

        NumberValues(String tag) {
            this.tag = tag;
        }

        @Override
        public String toString() {
            return name;
        }

    }

    public static enum StringValues {
        Favourite_File_Type;

        private String name;
        
        StringValues(){
            name = String.format("%s", name().replace('_', ' '));
        }
        
        @Override
        public String toString() {
            return name;
        }
        
    }

    @Override
    public void write(Json json) {
        json.writeValue("nv", numeric, ObjectIntMap.class);
        json.writeValue("sv", strings, ObjectMap.class);
        json.writeValue("monsters", discoveredMonsters, ObjectIntMap.class);
        json.writeValue("modifiers", discoveredModifiers, ObjectSet.class);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void read(Json json, JsonValue jsonData) {

        {
            ObjectIntMap val = json.readValue(ObjectIntMap.class, jsonData.get("numeric"));
            if (val != null) {
                numeric = val;
            }
        }
        
        {
            ObjectMap val = json.readValue(ObjectMap.class, jsonData.get("strings"));
            if (val != null) {
                strings = val;
            }
        }
        
        {
            ObjectIntMap val = json.readValue(ObjectIntMap.class, jsonData.get("monsters"));
            if (val != null) {
                discoveredMonsters = val;
            }
        }
        
        {
            ObjectSet val = json.readValue(ObjectSet.class, jsonData.get("modifiers"));
            if (val != null) {
                discoveredModifiers = val;
            }
        }
    }

    @Override
    public void onRegister() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void onUnregister() {
        // TODO Auto-generated method stub
        
    }

    /**
     * Increments an occurence of a tracked number value
     * @param key
     *  Number value to increment
     * @param val
     *  amount to increase it by
     */
    public void increment(NumberValues key, int val) {
        numeric.put(key, numeric.get(key, 0) + val);
    }
    
    /**
     * Decrements an occurence of a tracked number value
     * @param key
     *  Number value to increment
     * @param val
     *  amount to decrease it by
     */
    public void decrement(NumberValues key, int val) {
        numeric.put(key, Math.max(0, numeric.get(key, 0) - val));
    }
    
    /**
     * Increments an occurence of a tracked string value
     * @param key
     *  String category
     * @param val
     *  Value to increment in the category
     * @param val
     *  amount to increase it by
     */
    public void increment(StringValues key, String val, int amount) {
        ObjectIntMap<String> index = strings.get(key);
        index.put(val, index.get(val, 0) + amount);
    }
    
    /**
     * Increments an occurence of a tracked string value
     * @param key
     *  String category
     * @param val
     *  Value to increment in the category
     * @param val
     *  amount to increase it by
     */
    public void decrement(StringValues key, String val, int amount) {
        ObjectIntMap<String> index = strings.get(key);
        index.put(val, Math.max(0, index.get(val, 0) - amount));
    }
    
    /**
     * Add a statmodifier to the records
     * @param modifier
     */
    public void discover(String modifier) {
        discoveredModifiers.add(modifier);
    }
    
    /**
     * Add a monster to the records.  If the monster was already in the records, what is saved is the
     * deepest floor the monster has been encountered on.
     * @param monster
     *  The template of the monster discovered
     * @param floor
     *  The floor the monster was encountered on.
     */
    public void discover(MonsterTemplate monster, int floor) {
        discoveredMonsters.put(monster, Math.max(discoveredMonsters.get(monster, 1), floor));
    }
    
    /**
     * Checks to see if the player has already discovered the specified modifier
     * @param modifier
     * @return
     */
    public boolean hasDiscovered(String modifier) {
        return discoveredModifiers.contains(modifier);
    }

    public Array<String> getDiscoveredModifiers() {
        Array<String> modifiers = new Array<String>();
        Iterator<String> i = discoveredModifiers.iterator();
        while (i.hasNext()) {
            modifiers.add(i.next());
        }
        return modifiers;
    }
    
}
