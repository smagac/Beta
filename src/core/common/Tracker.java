package core.common;

import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

public final class Tracker implements Serializable {

    // not used for anything other than saving json
    protected static Tracker _instance = new Tracker();

    /**
     * Reset all the tracker's values
     */
    public static void reset() {
        for (NumberValues nv : NumberValues.values()) {
            nv.reset();
        }

        for (StringValues sv : StringValues.values()) {
            sv.reset();
        }
    }

    /**
     * @return calculated score from tracker values
     */
    public static int score() {
        // calculate score
        float score = 0;

        // more points awarded per item crafted
        score += NumberValues.Items_Crafted.count * 1000;

        // less files needed to explore per item crafted, better your score
        score += ((10 * NumberValues.Items_Crafted.count) - NumberValues.Files_Explored.count) * 100;

        // get cool points for enemy kdr
        score += (NumberValues.Monsters_Killed.count / (NumberValues.Times_Died.count + 1)) * 100;

        // less you sleep more points you get
        score += (NumberValues.Times_Slept.count + NumberValues.Loot_Sacrificed.count) * -50;

        // get points for each piece of loot you find
        score += NumberValues.Loot_Found.count * 10;

        return (int) Math.max(0, score);
    }

    /**
     * Calculate a rank for the player based on their tracked stats
     * 
     * @return
     */
    public static String rank() {

        String rank = "Straight Shooter";
        if (score() > 50000) {
            rank = "Going for the Gold";
        }
        if (NumberValues.Monsters_Killed.count > 500) {
            rank = "Poacher";
        }
        if (NumberValues.Monsters_Killed.count > 1000) {
            rank = "Exterminator";
        }
        if (NumberValues.Files_Explored.count < 10) {
            rank = "Crawler";
        }
        if (NumberValues.Items_Crafted.count > 10) {
            rank = "Apprentice";
        }
        if (NumberValues.Files_Explored.count > 10) {
            rank = "Logger";
        }
        if (NumberValues.Deepest_Floor_Traveled.count < 20) {
            rank = "Scaredy Cat";
        }
        if (NumberValues.Times_Slept.count > 100) {
            rank = "Narclyptic";
        }
        if (NumberValues.Items_Crafted.count > 15) {
            rank = "Favourite Slave";
        }
        if (NumberValues.Loot_Sacrificed.count > 500) {
            rank = "Garbage Dump";
        }
        if (NumberValues.Files_Explored.count > 20) {
            rank = "Living Virus";
        }
        if (NumberValues.Items_Crafted.count > 25) {
            rank = "Crafting Addict";
        }
        if (NumberValues.Monsters_Killed.count > 10000) {
            rank = "Mass Extintion";
        }
        if (NumberValues.Files_Explored.count == 0) {
            rank = "Dungeon Gambler";
        }
        if (NumberValues.Times_Slept.count < 10) {
            rank = "Insomniac";
        }
        if (NumberValues.Items_Crafted.count > 50) {
            rank = "Dedicated Follower";
        }
        if (NumberValues.Items_Crafted.count > 70) {
            rank = "Westboro Craftist";
        }
        if (NumberValues.Times_Died.count < 10) {
            rank = "Played it Safe";
        }
        if (NumberValues.Items_Crafted.count > 100) {
            rank = "Newly Appointed Crafting God";
        }

        return rank;
    }

    /**
     * Values to keep track of over the course of the game
     * 
     * @author nhydock
     *
     */
    public enum NumberValues {
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
        private int count = 0;

        NumberValues() {
        }

        NumberValues(String tag) {
            this.tag = tag;
        }

        public void increment() {
            count++;
        }

        public void decrement() {
            count--;
        }

        public void set(int val) {
            count = val;
        }

        @Override
        public String toString() {
            return String.format("%s", name().replace('_', ' '));
        }

        public String valString() {
            if (tag != null) {
                return String.format("%d %s", count, tag);
            }
            return "" + count;
        }

        public int value() {
            return count;
        }

        private void reset() {
            count = 0;
        }
    }

    public enum StringValues implements Serializable {
        Favourite_File_Type;

        private final ObjectMap<String, Integer> counters = new ObjectMap<String, Integer>();

        public void increment(String value) {
            counters.put(value, counters.get(value, 0) + 1);
        }

        public void decrement(String value) {
            counters.put(value, counters.get(value, 0) - 1);
        }

        public String max() {
            String m = null;
            int max = Integer.MIN_VALUE;
            for (String key : counters.keys()) {
                int k = counters.get(key);
                if (k > max) {
                    max = k;
                    m = key;
                }
            }
            return m;
        }

        public String min() {
            String m = null;
            int max = Integer.MAX_VALUE;
            for (String key : counters.keys()) {
                int k = counters.get(key);
                if (k < max) {
                    max = k;
                    m = key;
                }
            }
            return m;
        }

        @Override
        public String toString() {
            return String.format("%s", name().replace('_', ' '));
        }

        private void reset() {
            counters.clear();
        }

        @Override
        public void write(Json json) {
            for (String key : counters.keys()) {
                json.writeValue(key, counters.get(key), Integer.class);
            }
        }

        @Override
        public void read(Json json, JsonValue jsonData) {
            counters.clear();
            for (JsonValue val : jsonData) {
                counters.put(val.name, val.asInt());
            }
        }
    }

    @Override
    public void write(Json json) {
        json.writeObjectStart("nv");
        for (NumberValues s : NumberValues.values()) {
            json.writeValue(s.name(), s.count);
        }
        json.writeObjectEnd();
        json.writeObjectStart("sv");
        for (StringValues s : StringValues.values()) {
            json.writeValue(s.name(), s);
        }
        json.writeObjectEnd();
    }

    @Override
    public void read(Json json, JsonValue jsonData) {

        JsonValue nv = jsonData.get("nv");
        for (NumberValues s : NumberValues.values()) {
            s.count = nv.getInt(s.name());
        }

        JsonValue sv = jsonData.get("sv");
        for (StringValues s : StringValues.values()) {
            s.read(json, sv.get(s.name()));
        }
    }
}
