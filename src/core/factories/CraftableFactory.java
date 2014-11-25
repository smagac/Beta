package core.factories;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import core.DataDirs;
import core.datatypes.Craftable;

public class CraftableFactory {
    public static Array<String> craftables;
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

        craftables = new Array<String>();

        for (JsonValue data : jv.get("craftable")) {
            String name = data.asString();
            craftables.add(name);
        }
        loaded = true;
    }

    public CraftableFactory() {
    }

    public Craftable createRandomCraftable() {
        String name = craftables.random();
        String adj = AdjectiveFactory.getAdjective();
        String[] parts = new String[MathUtils.random(1, 5)];

        for (int i = 0; i < parts.length; i++) {
            parts[i] = ItemFactory.items.random();
        }
        return new Craftable(name, adj, parts);
    }
}
