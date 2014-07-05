package factories;

import java.io.IOException;

import scenes.dungeon.MovementSystem;
import scenes.dungeon.RenderSystem;

import com.artemis.Entity;
import com.artemis.World;
import com.artemis.managers.GroupManager;
import com.artemis.managers.TagManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;

import components.Position;
import components.Renderable;
import components.Stats;
import core.datatypes.Dungeon;
import core.datatypes.FileType;

/**
 * Generates tiled maps and populates them for you to explore
 * @author nhydock
 *
 */
public class DungeonFactory {

	//directory to save dungeon files into
	private final String tmpDir;
	
	//acceptable characters for serial id generation
	private static final String acceptable = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	
	
	private ItemFactory itemFactory;
	private MonsterFactory monsterFactory;
	private TiledMapTileSet tileset;
	private TextureAtlas atlas;
	
	int difficulty;
	
	public DungeonFactory(TextureAtlas atlas)
	{
		this.atlas = atlas;
		TextureRegion tiles = atlas.findRegion("tiles2");
		buildTileSet(tiles);
		
		tmpDir = Gdx.files.getExternalStoragePath()+"storymode/tmp";// System.getProperty("java.io.tmpdir");
		System.out.println(tmpDir);
	}
	
	private void buildTileSet(TextureRegion tiles)
	{
		tileset = new TiledMapTileSet();
		//corners
		TextureRegion[][] ti = tiles.split(32, 32);
		
		tileset.putTile(0, new SimpleTile(ti[0][1], 0, false));		//empty
		tileset.putTile(1, new SimpleTile(ti[0][2], 1, true));		//room walls
		tileset.putTile(2, new SimpleTile(ti[0][2], 2, true));		//floor
		tileset.putTile(3, new SimpleTile(ti[1][0], 3, true)); 		//stairs down
		tileset.putTile(4, new SimpleTile(ti[1][1], 4, true)); 		//stairs up
	}
	
	/**
	 * Generate a new dungeon and save all its floors into a temp directory
	 * @param difficulty - defines how large and how many monsters the dungeon will have, but also more loot
	 * @throws IOException 
	 * @return an array of the serial ids of the floors on the file system
	 */
	public Array<Dungeon> create(FileType type, int difficulty) throws IOException
	{
		this.difficulty = difficulty;
		itemFactory = new ItemFactory(type);
		monsterFactory = new MonsterFactory(atlas, type);
		
		//make sure dungeon dir exists
		Gdx.files.absolute(tmpDir).mkdirs();
		
		//catch crazy out of bounds
		if (difficulty > 5 || difficulty < 1)
		{
			Gdx.app.log("[Invalid param]", "difficulty out of bounds, setting to 3");
			difficulty = 3;
		}
		
		Array<Dungeon> dungeon = new Array<Dungeon>();
		
		//please don't ask about my numbers, they're so randomly picked out from my head
		// I don't even know what the curve looks like on a TI calculator ;_;
		int floors = MathUtils.random(5*difficulty, 10*difficulty+(difficulty-1)*10);
		//stress
		floors = 90;
		for (int floor = 1, width = 50, height = 50; floor <= floors; floor++, width += 5, height += 5)
		{
			Dungeon d = new Dungeon(type, difficulty, floor, width, height, monsterFactory, itemFactory);
			
			/* Use for dumping
			//generate a serialized tmp file
			String serial;
			String suffix = String.format("%02d", floor);
			do
			{
				serial = "";
				for (int i = 0; i < 12; i++)
				{
					serial += acceptable.charAt(MathUtils.random(acceptable.length()-1));
				}
			}
			while (Gdx.files.absolute(tmpDir + "/" + serial + "." + suffix).exists());
			FileHandle tmp = Gdx.files.absolute(tmpDir + "/" + serial + "." + suffix);
			tmp.file().createNewFile();
			
			Json serializer = new Json();
			serializer.prettyPrint(d);
			serializer.toJson(d, Dungeon.class, tmp);
			*/
			dungeon.add(d);
		}
		return dungeon;
	}
	
	/**
	 * Prepare a world to be loaded and stepped into
	 * @param ts
	 */
	public World create(Dungeon dungeon, Stats player)
	{
		/*
		String suffix = String.format("%02d", floor);
		FileHandle file = Gdx.files.absolute(tmpDir + "/" + serial + "." + suffix);
		Dungeon dungeon = (new Json()).fromJson(Dungeon.class, file);
		*/
		int floor = dungeon.floor();
		World world = new World();
		
		world.setManager(new TagManager());
		world.setManager(new GroupManager());
		
		world.setSystem(new RenderSystem(), true);
		MovementSystem ms = new MovementSystem(floor);
		world.setSystem(ms, true);
	
		TiledMap map = new TiledMap();
		TiledMapTileLayer layer = dungeon.paintLayer(tileset, 32, 32);
		map.getLayers().add(layer);
		
		world.getSystem(RenderSystem.class).setMap(map);
		ms.setMap(layer);
		
		//add monsters to rooms
		// monster count is anywhere between 5-20 on easy and 25-100 on hard
		monsterFactory.makeMonsters(world, MathUtils.random(dungeon.roomCount(), dungeon.roomCount()+floor*(floor*difficulty)), layer, itemFactory, floor);
		
		//forcibly add some loot monsters
		monsterFactory.makeTreasure(world, dungeon.rooms(), layer, itemFactory, floor);
		
		world.initialize();
		
		//make player
		Entity e = world.createEntity();
		e.addComponent(new Position(0,0));
		e.addComponent(player);	//shared stats reference
		e.addComponent(new Renderable(atlas.findRegion("character")));
		e.addToWorld();
		
		world.getManager(TagManager.class).register("player", e);
		
		world.getSystem(MovementSystem.class).setPlayer();
		
		//put entity at start position on each floor
		
		return world;
	}
	
	public void dispose()
	{
		//Gdx.files.absolute(tmpDir).deleteDirectory();
	}
	
	/**
	 * Super simple class that just represents a tile with a region.  Nothing special, but
	 * it allows us to generate tile sets in our code
	 * @author nhydock
	 */
	private class SimpleTile implements TiledMapTile {

		TextureRegion region;
		MapProperties prop;
		int id;
		
		SimpleTile(TextureRegion r, int id)
		{
			region = r;
			prop = new MapProperties();
			prop.put("passable", false);
			setId(id);
		}
		
		SimpleTile(TextureRegion r, int id, boolean passable)
		{
			this(r, id);
			prop.put("passable", passable);
		}
		
		@Override
		public int getId() { return id; }

		@Override
		public void setId(int id) {	this.id = id; }

		@Override
		public BlendMode getBlendMode() {
			return BlendMode.NONE;
		}

		@Override
		public void setBlendMode(BlendMode blendMode) {	}

		@Override
		public TextureRegion getTextureRegion() {
			return region;
		}

		@Override
		public float getOffsetX() {	return 0; }

		@Override
		public void setOffsetX(float offsetX) {	}

		@Override
		public float getOffsetY() {	return 0; }

		@Override
		public void setOffsetY(float offsetY) {	}

		@Override
		public MapProperties getProperties() {
			return prop;
		}
		
	}
}
