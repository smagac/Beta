package factories;

import com.artemis.Entity;
import com.artemis.World;
import com.artemis.managers.GroupManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
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
import core.datatypes.FileType;
import core.datatypes.Item;

/**
 * Factory for creating all the monsters in a level
 * @author nhydock
 *
 */
public class MonsterFactory {

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
		
		for (FileType type : FileType.values())
		{
			monsters.put(type, new Array<MonsterTemplate>());
		}
		
		JsonReader json = new JsonReader();
		JsonValue monsterList = json.parse(Gdx.files.classpath("core/data/monsters.json"));
		
		for (JsonValue jv : monsterList)
		{
			MonsterTemplate temp = new MonsterTemplate(jv);
			monsters.get(FileType.getType(temp.location)).add(temp);
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
		final String name;
		final int hp;
		final int str;
		final int def;
		final int mag;
		final int spd;
		final String[] attacks;
		final String[] magic;
		final String location;
		final String type;
		
		MonsterTemplate(final JsonValue src)
		{
			name = src.name;
			hp = src.getInt("hp");
			str = src.getInt("str");
			def = src.getInt("def");
			mag = src.getInt("mag");
			spd = src.getInt("spd");
			attacks = src.get("attacks").asStringArray();
			magic = src.get("magic").asStringArray();
			location = src.getString("where", null);
			type = src.getString("type", "rat");
		}
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
		for (Texture t : this.icons.getTextures())
		{
			t.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
		}
	}
	
	
	/**
	 * Generates all the monsters for a given world
	 * @param world - level representation of entities
	 * @param area - type of file area we need to load monsters for
	 * @param size - the size of the file, indicating how many monsters we need
	 * @param layer - list of rooms to lock each monster into
	 * @return an array of all the monsters that have just been created and added to the world
	 */
	public Array<Entity> makeMonsters(World world, int size, TiledMapTileLayer layer, ItemFactory lootMaker, int floor)
	{
		Array<MonsterTemplate> selection = new Array<MonsterTemplate>();
		Array<Entity> monsters = new Array<Entity>();
		selection.addAll(MonsterFactory.monsters.get(area));
		selection.addAll(MonsterFactory.monsters.get(FileType.Other)); 

		GroupManager gm = world.getManager(GroupManager.class);
		for (int i = 0; i < size; i++)
		{
			MonsterTemplate t = selection.random();
			Entity monster = create(world, t, lootMaker.createItem(), floor);
			monster.addComponent(new Monster());
			
			//add its position into a random room
			int x = 0;
			int y = 0;
			TiledMapTile tile;
			do
			{
				x = MathUtils.random(0, layer.getWidth()-1);
				y = MathUtils.random(0, layer.getHeight()-1);
				tile = layer.getCell(x, y).getTile();
			} while (!tile.getProperties().get("passable", Boolean.class) || tile.getId() == 3 || tile.getId() == 4);
			monster.addComponent(new Position(x, y));
			
			monster.addToWorld();
			
			gm.add(monster, "monsters");
			monsters.add(monster);
		}
		
		return monsters;
	}
	
	/**
	 * Generates a single monster from a template
	 * @param world - entity manager to create an entity from
	 * @param t - template we use to base our entity from
	 * @return an entity
	 */
	private Entity create(World world, MonsterTemplate t, Item item, int floor)
	{
		Entity e = world.createEntity();
		e.addComponent(new Stats(t.hp, 
								MathUtils.random(20), 
								(int)(MathUtils.random(.65f+(floor / 10f), 1.0f+(floor / 10f))*t.str), 
								(int)(MathUtils.random(.4f+(floor / 10f), 1.0f+(floor / 10f))*t.def), 
								t.mag,
								(int)(MathUtils.random(.2f+(floor / 10f), 1.0f+(floor / 10f))*t.spd)
							));
		e.addComponent(new Identifier(t.name, AdjectiveFactory.getAdjective()));
		e.addComponent(new Renderable(icons.findRegion(t.type)));
		
		Combat c = new Combat(t.attacks, t.magic);
		c.setDrop(item);
		
		e.addComponent(c);
		
		return e;
	}
}
