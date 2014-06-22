package factories;

import GenericComponents.Combat;
import GenericComponents.Identifier;
import GenericComponents.Renderable;
import GenericComponents.Stats;

import com.artemis.Entity;
import com.artemis.World;
import com.artemis.managers.GroupManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

import core.datatypes.FileType;

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
		JsonValue monsterList = json.parse(Gdx.files.classpath("factories/data/monsters.json"));
		
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
			type = src.getString("type", "beast");
		}
	}
	
	private final TextureAtlas icons;
	private final FileType area;
	
	/**
	 * 
	 * @param icons - file containing all of the image representations of the monsters
	 * @param area - type of factory we should create
	 */
	public MonsterFactory(TextureAtlas icons, String area)
	{
		this.icons = icons;
		this.area = FileType.valueOf(area);
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
	 */
	public void makeMonsters(World world, int size)
	{
		Array<MonsterTemplate> selection = new Array<MonsterTemplate>();
		selection.addAll(monsters.get(area));
		selection.addAll(monsters.get(FileType.Other)); 

		GroupManager gm = world.getManager(GroupManager.class);
		for (int i = 0; i < size; i++)
		{
			MonsterTemplate t = selection.get(MathUtils.random(selection.size));
			Entity e = create(world, t);
			gm.add(e, Group);
			world.addEntity(e);
		}
	}
	
	/**
	 * Generates a single monster from a template
	 * @param world - entity manager to create an entity from
	 * @param t - template we use to base our entity from
	 * @return an entity
	 */
	private Entity create(World world, MonsterTemplate t)
	{
		Entity e = world.createEntity();
		e.addComponent(new Stats(t.hp, MathUtils.random(20), t.str, t.def, t.mag, t.spd), Stats.CType);
		e.addComponent(new Combat(t.attacks, t.magic));
		e.addComponent(new Identifier(t.name, AdjectiveFactory.getAdjective()));
		e.addComponent(new Renderable(icons.findRegion(t.type)));
		
		return e;
	}
}
