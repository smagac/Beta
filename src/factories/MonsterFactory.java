package factories;

import com.artemis.Entity;
import com.artemis.World;
import com.artemis.managers.GroupManager;
import com.artemis.utils.ImmutableBag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

import components.Combat;
import components.Identifier;
import components.Monster;
import components.Position;
import components.Renderable;
import components.Stats;
import core.DLC;
import core.datatypes.Dungeon.Floor;
import core.datatypes.FileType;
import core.datatypes.Item;
import core.util.dungeon.Room;

/**
 * Factory for creating all the monsters in a level
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
	public static void init()
	{
		//only allow loading once
		if (loaded)
			return;
		
		monsters = new ObjectMap<FileType, Array<MonsterTemplate>>();
		allMonsters = new ObjectMap<String, MonsterTemplate>();
		for (FileType type : FileType.values())
		{
			monsters.put(type, new Array<MonsterTemplate>());
		}
		
		JsonReader json = new JsonReader();
		JsonValue monsterList;
		
		Array<FileHandle> dlcMonsters = DLC.getAll("data/monsters.json", Gdx.files.classpath("core/data/monsters.json"));
		for (FileHandle dlc : dlcMonsters)
		{
			monsterList = json.parse(dlc);
			
			for (JsonValue jv : monsterList)
			{
				MonsterTemplate temp = new MonsterTemplate(jv);
				monsters.get(FileType.getType(temp.location)).add(temp);
				allMonsters.put(temp.name, temp);
			}
		}
		
		loaded = true;
	}
	
	/**
	 * Json loaded definition of a monster
	 * @author nhydock
	 *
	 */
	private static class MonsterTemplate
	{
		//base name of the monster
		final String name;
		
		//stats
		private final int hp,  maxhp;
		private final int str, maxstr;
		private final int def, maxdef;
		private final int exp, maxexp;
		private final int spd, maxspd;
		
		//movement rates
		final float norm;
		final float agro;
		
		//passive enemies only attack/pursue as soon as they've been attacked
		// normally enemies will become agro as soon as you enter their visibility range of 3 tiles
		// as soon as a passive enemy is attacked, they no longer return to passive and act like normal enemies
		final boolean passive;
		
		//special death message for killing the enemy
		final String die;
		
		//defines the kind of files you can find them in
		final String location;
		
		//sprite type to use
		final String type;
		
		//certain monsters are able to hide their name and/or stat bubble
		final boolean boss;     //hp turns into ???/???, this is useful for rare/boss enemies
		final boolean hideName;	//hides the entire bubble if no name is available
		
		MonsterTemplate(final JsonValue src)
		{
			name = src.name;
			hp = src.getInt("hp", 1); maxhp = src.getInt("maxhp", hp);
			str = src.getInt("str", 1); maxstr = src.getInt("maxstr", str);
			def = src.getInt("def", 1); maxdef = src.getInt("maxdef", def);
			exp = src.getInt("exp", 1); maxexp = src.getInt("maxexp", exp);
			spd = src.getInt("spd", 1); maxspd = src.getInt("maxspd", spd);
			norm = src.getFloat("norm", .4f);
			agro = src.getFloat("agro", .75f);
			die = src.getString("die", "You have slain %s");
			passive = src.getBoolean("passive", false);
			location = src.getString("where", null);
			type = src.getString("type", "rat");
			boss = src.getBoolean("boss", false);
			hideName = src.getBoolean("hideName", false);
		}
		
		public int getHp(float floor) { return (int)MathUtils.lerp(hp, maxhp, floor/100f); }
		public int getStr(float floor) { return (int)MathUtils.lerp(str, maxstr, floor/100f); }
		public int getDef(float floor) { return (int)MathUtils.lerp(def, maxdef, floor/100f); }
		public int getSpd(float floor) { return (int)MathUtils.lerp(spd, maxspd, floor/100f); }
		public int getExp(float floor) { return (int)MathUtils.lerp(exp, maxexp, floor/100f); }
		
	}
	
	private final TextureAtlas icons;
	private final FileType area;
	
	/**
	 * 
	 * @param icons - file containing all of the image representations of the monsters
	 * @param type - type of factory we should create
	 */
	public MonsterFactory(TextureAtlas icons, FileType type)
	{
		this.icons = icons;
		this.area = type;
	}
	
	
	/**
	 * Generates all the monsters for a given world
	 * @param world - level representation of entities
	 * @param area - type of file area we need to load monsters for
	 * @param size - the size of the file, indicating how many monsters we need
	 * @param map - list of rooms to lock each monster into
	 * @return an array of all the monsters that have just been created and added to the world
	 */
	public void makeMonsters(World world, int size, ItemFactory lootMaker, Floor floor)
	{
		Array<MonsterTemplate> selection = new Array<MonsterTemplate>();
		selection.addAll(MonsterFactory.monsters.get(area));
		selection.addAll(MonsterFactory.monsters.get(FileType.Other)); 

		GroupManager gm = world.getManager(GroupManager.class);
		for (int i = 0; i < size; i++)
		{
			MonsterTemplate t;
			do
			{
				t = selection.random();
			}
			//don't allow mimics as normal enemies
			while (t.name.equals("mimic") || t.name.equals("treasure chest"));
			
			Entity monster = create(world, t, lootMaker.createItem(), floor.depth);
			monster.addComponent(new Monster());
			
			//add its position into a random room
			int x = 0;
			int y = 0;
			TiledMapTile tile;
			do
			{
				Room r = floor.rooms.random();
				x = MathUtils.random(r.innerLeft(), r.innerRight());
				y = MathUtils.random(r.innerBottom(), r.innerTop());
				tile = floor.layer.getCell(x, y).getTile();
			} while (!tile.getProperties().get("passable", Boolean.class) || tile.getId() == 3 || tile.getId() == 4);
			monster.addComponent(new Position(x, y));
			
			monster.addToWorld();
			
			gm.add(monster, "monsters");
		}
	}
	
	/**
	 * Generates a single monster from a template
	 * @param world - entity manager to create an entity from
	 * @param t - template we use to base our entity from
	 * @return an entity
	 */
	private Entity create(World world, MonsterTemplate t, Item item, int depth)
	{
		Entity e = world.createEntity();
		Stats s = new Stats(
				t.getHp(depth),
				t.getStr(depth),
				t.getDef(depth),
				t.getSpd(depth),
				t.getExp(depth));
		s.hidden = t.boss;
		e.addComponent(s);
		e.addComponent(new Identifier(t.name, AdjectiveFactory.getAdjective(), t.hideName));
		e.addComponent(new Renderable(icons.findRegion(t.type)));
		
		Combat c = new Combat(t.norm, t.agro, t.passive, item, t.die);
		
		e.addComponent(c);
		
		return e;
	}
	
	private float calculateDensity(World world, Room r)
	{
		
		//calculate population density
		int mCount = 0;
		ImmutableBag<Entity> m = world.getManager(GroupManager.class).getEntities("monsters");
		for (int i = 0; i < m.size(); i++)
		{
			Position position = m.get(i).getComponent(Position.class);
			if (r.contains(position.getX(), position.getY()))
			{
				mCount++;
			}
		}
		float density = Math.min(.75f, (mCount * 9) / (r.getWidth() * r.getHeight()));
	
		return density;
	}

	/**
	 * Populates empty parts of the dungeon with random treasure chests
	 * @param world
	 * @param rooms
	 * @param map
	 * @param lootMaker
	 * @param depth
	 */
	public void makeTreasure(World world, ItemFactory lootMaker, Floor floor) {
		GroupManager gm = world.getManager(GroupManager.class);
		MonsterTemplate treasure = allMonsters.get("treasure chest");
		MonsterTemplate mimic = allMonsters.get("mimic");
		
		int limit = floor.loot;
		int made = 0;
		
		for (Room r : floor.rooms)
		{
			if (limit == 0)
				break;
			
			float density = calculateDensity(world, r);
			
			//randomly place a treasure chest
			// higher chance of there being a chest if the room is empty
			if (MathUtils.randomBoolean(Math.max(0, .75f-MathUtils.random(.5f*density, 1.5f*density))))
			{
				//low chance of chest actually being a mimic
				Entity monster;
				if (MathUtils.randomBoolean(.02f + (floor.depth/300f)))
				{
					monster = create(world, mimic, lootMaker.createItem(), floor.depth);
				}
				else
				{
					monster = create(world, treasure, lootMaker.createItem(), floor.depth);	
				}
				monster.addComponent(new Monster());
				int x = 0;
				int y = 0;
				TiledMapTile tile;
				do
				{
					x = MathUtils.random(r.innerLeft(), r.innerRight());
					y = MathUtils.random(r.innerBottom(), r.innerTop());
					tile = floor.layer.getCell(x, y).getTile();
				} while (!tile.getProperties().get("passable", Boolean.class) || tile.getId() == 3 || tile.getId() == 4);
				monster.addComponent(new Position(x, y));
					
				monster.addToWorld();
				
				gm.add(monster, "monsters");
				limit--;
				made++;
			}
		}
		if (limit < 0)
		{
			floor.loot = made;
		}
	}
}
