package core.service.implementations;

import github.nhydock.ssm.Service;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectMap;

public final class ScoreTracker implements Serializable, Service {
    
    ObjectIntMap<NumberValues> numeric;
    ObjectMap<StringValues, ObjectIntMap<String>> strings;
    
    public ScoreTracker(){
        numeric = new ObjectIntMap<NumberValues>();
        for (NumberValues nv : NumberValues.values()) {
            numeric.put(nv, 0);
        }

        strings = new ObjectMap<StringValues, ObjectIntMap<String>>();
        for (StringValues sv : StringValues.values()) {
            strings.put(sv, new ObjectIntMap<String>());
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
        json.writeObjectStart("nv");
        for (NumberValues s : NumberValues.values()) {
            json.writeValue(s.name(), numeric.get(s, 0));
        }
        json.writeObjectEnd();
        json.writeObjectStart("sv");
        for (StringValues s : StringValues.values()) {
            ObjectIntMap<String> counters = strings.get(s);
            json.writeObjectStart(s.name());
            for (String key : counters.keys()) {
                json.writeValue(key, counters.get(key, 0), Integer.class);
            }
            json.writeObjectEnd();
        }
        json.writeObjectEnd();
    }

    @Override
    public void read(Json json, JsonValue jsonData) {

        JsonValue nv = jsonData.get("nv");
        for (NumberValues s : NumberValues.values()) {
            numeric.put(s, nv.getInt(s.name(), 0));
        }

        JsonValue sv = jsonData.get("sv");
        for (StringValues s : StringValues.values()) {
            ObjectIntMap<String> counters = strings.get(s);
            for (JsonValue val : sv.get(s.name())) {
                counters.put(val.name, val.asInt());
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

    public void increment(NumberValues key, int val) {
        numeric.put(key, numeric.get(key, 0) + val);
    }
    
    public void decrement(NumberValues key, int val) {
        numeric.put(key, Math.max(0, numeric.get(key, 0) - val));
    }
    
    public void increment(StringValues key, String val, int amount) {
        ObjectIntMap<String> index = strings.get(key);
        index.put(val, index.get(val, 0) + amount);
    }
    
    public void decrement(StringValues key, String val, int amount) {
        ObjectIntMap<String> index = strings.get(key);
        index.put(val, Math.max(0, index.get(val, 0) - amount));
    }
    
}
