package core.factories;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

import core.DLC;
import core.DataDirs;
import core.components.Combat;
import core.components.Identifier;
import core.components.Groups.*;
import core.components.Position;
import core.components.Renderable;
import core.components.Stats;
import core.datatypes.dungeon.Floor;
import core.datatypes.FileType;
import core.datatypes.Item;
import core.datatypes.StatModifier;
import core.util.dungeon.Room;

/**
 * Factory for creating all the monsters in a level
 * 
 * @author nhydock
 *
 */
public class MonsterFactory {

    private static ObjectMap<String, MonsterTemplate> allMonsters;
    private static ObjectMap<FileType, Array<MonsterTemplate>> monsters;

    public static final String Group = "Monster";

    private static boolean loaded;

    /**
     * Load all monster definitions from the monsters.json file
     */
    public static void init() {
        // only allow loading once
        if (loaded)
            return;

        monsters = new ObjectMap<FileType, Array<MonsterTemplate>>();
        allMonsters = new ObjectMap<String, MonsterTemplate>();
        for (FileType type : FileType.values()) {
            monsters.put(type, new Array<MonsterTemplate>());
        }

        JsonReader json = new JsonReader();
        JsonValue monsterList;

        Array<FileHandle> dlcMonsters = DLC.getAll(DataDirs.GameData + "monsters.json",
                Gdx.files.classpath(DataDirs.GameData + "monsters.json"));
        for (FileHandle dlc : dlcMonsters) {
            monsterList = json.parse(dlc);

            for (JsonValue jv : monsterList) {
                MonsterTemplate temp = new MonsterTemplate(jv);
                monsters.get(FileType.getType(temp.location)).add(temp);
                allMonsters.put(temp.name, temp);
            }
        }

        loaded = true;
    }

    /**
     * Json loaded definition of a monster
     * 
     * @author nhydock
     *
     */
    private static class MonsterTemplate {
        // base name of the monster
        final String name;

        // stats
        private final int hp, maxhp;
        private final int str, maxstr;
        private final int def, maxdef;
        private final int exp, maxexp;
        private final int spd, maxspd;

        // movement rates
        final float norm;
        final float agro;

        // passive enemies only attack/pursue as soon as they've been attacked
        // normally enemies will become agro as soon as you enter their
        // visibility range of 3 tiles
        // as soon as a passive enemy is attacked, they no longer return to
        // passive and act like normal enemies
        final boolean passive;

        // special death message for killing the enemy
        final String die;

        // defines the kind of files you can find them in
        final String location;

        // sprite type to use
        final String type;

        final boolean hideName; // hides the entire bubble if no name is
                                // available

        MonsterTemplate(final JsonValue src) {
            name = src.name;
            hp = src.getInt("hp", 1);
            maxhp = src.getInt("maxhp", hp);
            str = src.getInt("str", 1);
            maxstr = src.getInt("maxstr", str);
            def = src.getInt("def", 1);
            maxdef = src.getInt("maxdef", def);
            exp = src.getInt("exp", 1);
            maxexp = src.getInt("maxexp", exp);
            spd = src.getInt("spd", 1);
            maxspd = src.getInt("maxspd", spd);
            norm = src.getFloat("norm", .4f);
            agro = src.getFloat("agro", .75f);
            die = src.getString("die", "You have slain %s");
            passive = src.getBoolean("passive", false);
            location = src.getString("where", null);
            type = src.getString("type", "rat");
            hideName = src.getBoolean("hideName", false);
        }

        public int getHp(float floor) {
            return (int) MathUtils.lerp(hp, maxhp, floor / 100f);
        }

        public int getStr(float floor) {
            return (int) MathUtils.lerp(str, maxstr, floor / 100f);
        }

        public int getDef(float floor) {
            return (int) MathUtils.lerp(def, maxdef, floor / 100f);
        }

        public int getSpd(float floor) {
            return (int) MathUtils.lerp(spd, maxspd, floor / 100f);
        }

        public int getExp(float floor) {
            return (int) MathUtils.lerp(exp, maxexp, floor / 100f);
        }

    }

    /**
     * @return a randomly generated enemy name and nothing more
     */
    public static String randomName() {
        return AdjectiveFactory.getAdjective() + " " + allMonsters.keys().toArray().random();
    }

    /**
     * @return a random enemy species
     */
    public static String randomSpecies() {
        return allMonsters.keys().toArray().random();
    }

    private final FileType area;

    /**
     * 
     * @param icons
     *            - file containing all of the image representations of the
     *            monsters
     * @param type
     *            - type of factory we should create
     */
    public MonsterFactory(FileType type) {
        this.area = type;
    }

    /**
     * Generates all the monsters for a given world
     * 
     * @param world
     *            - level representation of entities
     * @param area
     *            - type of file area we need to load monsters for
     * @param size
     *            - the size of the file, indicating how many monsters we need
     * @param map
     *            - list of rooms to lock each monster into
     * @return an array of all the monsters that have just been created and
     *         added to the world
     */
    public void makeMonsters(Array<Entity> entities, int size, ItemFactory lootMaker, core.datatypes.dungeon.Floor floor) {

        Array<MonsterTemplate> selection = new Array<MonsterTemplate>();
        selection.addAll(MonsterFactory.monsters.get(area));
        selection.addAll(MonsterFactory.monsters.get(FileType.Other));

        if (floor.isBossFloor)
        {
            //Make boss to fight
            MonsterTemplate t;
            do {
                t = selection.random();
            }
            // don't allow loot chests to be bosses
            while (t.name.equals("treasure chest"));
            
            Entity monster = create(t, lootMaker.createItem(), floor.depth, true);
            monster.add(new Monster());
            monster.add(new Boss());
            monster.add(new Position((int)floor.rooms.get(0).x, (int)floor.rooms.get(0).y));

            entities.add(monster);
            return;
        }

        for (int i = 0; i < size; i++) {
            MonsterTemplate t;
            do {
                t = selection.random();
            }
            // don't allow mimics as normal enemies
            while (t.name.equals("mimic") || t.name.equals("treasure chest"));

            Entity monster = create(t, lootMaker.createItem(), floor.depth, false);
            monster.add(new Monster());

            // add its position into a random room
            monster.add(new Position(floor.getOpenTile()));

            entities.add(monster);
        }
    }

    /**
     * Generates a single monster from a template
     * 
     * @param world
     *            - entity manager to create an entity from
     * @param t
     *            - template we use to base our entity from
     * @return an entity
     */
    private Entity create(MonsterTemplate t, Item item, int depth, boolean boss) {
        Entity e = new Entity();
        String suffix = "";
        
        int modify = MathUtils.random(1, Math.max(1, depth/15));
        
        StatModifier[] mods;
        if (boss) {
            String adj = AdjectiveFactory.getBossAdjective();
            
            modify -= 1;
            suffix = " " + adj;
            mods = new StatModifier[modify + 1];
            mods[0] = AdjectiveFactory.getModifier(adj);
        } else {
            mods = new StatModifier[modify];
        }
        String[] adjs = new String[modify];
        
        for (int i = 0, n = (boss)?1:0; i < modify; i++, n++) {
            String adj = AdjectiveFactory.getAdjective();
            adjs[i] = adj;
            mods[n] = AdjectiveFactory.getModifier(adj);
        }
        
        Stats s = new Stats(new int[]{
                        t.getHp(depth), 
                        t.getStr(depth),
                        t.getDef(depth), 
                        t.getSpd(depth), 
                        t.getExp(depth)
                    },
                    mods
                  );
        s.hidden = boss;
        e.add(s);
        Identifier id = new Identifier(t.name, suffix, adjs);
        if (t.hideName || s.hidden) {
            id.hide();
        }
        e.add(id);
        e.add(new Renderable(t.type));

        Combat c = new Combat(t.norm, t.agro, t.passive, item, t.die);
        e.add(c);

        return e;
    }

    private float calculateDensity(Array<Entity> m, Room r) {

        // calculate population density
        int mCount = 0;
        for (int i = 0; i < m.size; i++) {
            Position position = m.get(i).getComponent(Position.class);
            if (r.contains(position.getX(), position.getY())) {
                mCount++;
            }
        }
        float density = Math.min(.75f, (mCount * 9) / (r.getWidth() * r.getHeight()));

        return density;
    }

    /**
     * Populates empty parts of the dungeon with random treasure chests
     * 
     * @param world
     * @param rooms
     * @param map
     * @param lootMaker
     * @param depth
     */
    public void makeTreasure(Array<Entity> entities, ItemFactory lootMaker, Floor floor) {
        if (floor.isBossFloor)
            return;
        
        MonsterTemplate treasure = allMonsters.get("treasure chest");
        MonsterTemplate mimic = allMonsters.get("mimic");

        int limit = floor.loot;
        int made = 0;

        for (Room r : floor.rooms) {
            if (limit == 0)
                break;

            float density = calculateDensity(entities, r);

            // randomly place a treasure chest
            // higher chance of there being a chest if the room is empty
            if (MathUtils.randomBoolean(Math.max(0, .75f - MathUtils.random(.5f * density, 1.5f * density)))) {
                // low chance of chest actually being a mimic
                Entity monster;
                if (MathUtils.randomBoolean(.02f + (floor.depth / 300f))) {
                    monster = create(mimic, lootMaker.createItem(), floor.depth, false);
                }
                else {
                    monster = create(treasure, lootMaker.createItem(), floor.depth, false);
                }
                monster.add(new Monster());
                monster.add(new Position(floor.getOpenTile()));
                entities.add(monster);
                
                limit--;
                made++;
            }
        }
        if (limit < 0) {
            floor.loot = made;
        }
    }
}