package com.nhydock.storymode.factories;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;
import com.nhydock.storymode.DataDirs;
import com.nhydock.storymode.datatypes.Ailment;
import com.nhydock.storymode.datatypes.StatModifier;
import com.nhydock.storymode.datatypes.Ailment.AilmentModifier;

public class AdjectiveFactory {

    private static Array<String> allAdjectives;
    private static ObjectMap<String, String> modifierMap;
    private static Array<String> bossAdjectives;

    private static ObjectMap<String, StatModifier> modifiers;
    private static ObjectMap<String, AilmentModifier> ailments;

    private static boolean loaded;

    /**
     * Load all monster definitions from the monsters.json file
     */
    public static void init() {
        // only allow loading once
        if (loaded)
            return;

        allAdjectives = new Array<String>();
        bossAdjectives = new Array<String>();
        modifierMap = new ObjectMap<String, String>();
        ailments = new ObjectMap<String, AilmentModifier>();
        JsonReader json = new JsonReader();

        // load items
        JsonValue jv = json.parse(Gdx.files.classpath(DataDirs.GameData + "modifiers.json"));
        for (JsonValue modifier : jv) {
            if (modifier.name().equals("boss") || modifier.name().equals("ailments")) {
                continue;
            }
            JsonValue adjectives = modifier.get("adjectives");
            String modName = modifier.name();
            Array<String> adjectiveList = new Array<String>(adjectives.asStringArray());
            for (String adj : adjectiveList) {
                modifierMap.put(adj, modName);
            }
            allAdjectives.addAll(adjectiveList);
        }

        modifiers = new ObjectMap<String, StatModifier>();
        for (JsonValue mod : jv) {
            String modName = mod.name();
            StatModifier modifier = new StatModifier(modName, mod.getFloat("hp", 1.0f), mod.getFloat("str", 1.0f), mod.getFloat(
                    "def", 1.0f), mod.getFloat("spd", 1.0f));

            modifiers.put(modName, modifier);
        }

        JsonValue bossData = jv.get("boss");
        for (String adj : bossData.get("adjectives").asStringArray()) {
            bossAdjectives.add(adj);
            modifierMap.put(adj, "boss");
        }
        
        JsonValue ailmentData = jv.get("ailments");
        for (Ailment ailment : Ailment.ALL) {
            JsonValue adjectives = ailmentData.get(ailment.toString());
            if (adjectives != null) {
                for (JsonValue adj = adjectives.child(); adj.next() != null; adj = adj.next()) {
                    String type = adj.name;
                    float effectiveness = adj.asFloat();
                    
                    if (allAdjectives.contains(type, false)) {
                        allAdjectives.add(type);
                        modifierMap.put(type, "normal");
                    }
                    
                    AilmentModifier mod;
                    if (ailments.containsKey(type)) {
                        mod = ailments.get(type);
                    } else {
                        mod = new AilmentModifier();
                        ailments.put(type, mod);
                    }
                    mod.addAilment(ailment, effectiveness);
                }
            }
        }

        loaded = true;
    }

    /**
     * Get a random adjective to append to a name
     * 
     * @return
     */
    public static String getAdjective() {
        if (!loaded)
            return null;
        return allAdjectives.random();
    }

    /**
     * Get a random adjective specific to bosses
     * 
     * @return
     */
    public static String getBossAdjective() {
        if (!loaded)
            return null;
        return bossAdjectives.random();
    }

    public static String getModifierType(String adjective) {
        return modifierMap.get(adjective);
    }

    public static StatModifier getModifier(String adjective) {
        return modifiers.get(getModifierType(adjective));
    }
    
    public static AilmentModifier getAilment(String adjective) {
        return ailments.get(adjective);
    }
}
