package factories;

import java.io.IOException;

import scenes.dungeon.MovementSystem;
import scenes.dungeon.RenderSystem;

import com.artemis.Entity;
import com.artemis.World;
import com.artemis.managers.GroupManager;
import com.artemis.managers.TagManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
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

import components.Position;
import components.Renderable;
import components.Stats;
import core.datatypes.Dungeon;
import core.datatypes.FileType;
import core.service.IDungeonContainer;

/**
 * Generates tiled maps and populates them for you to explore
 * @author nhydock
 *
 */
public class DungeonFactory {
	private static TiledMapTileSet tileset;
	//directory to save dungeon files into
	//private final String tmpDir;
	
	//acceptable characters for serial id generation
	//private static final String acceptable = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	
	private static TextureAtlas atlas;
	private static TextureRegion character;
	
	public static void prepareFactory(TextureAtlas atlas, TextureRegion player)
	{
		DungeonFactory.atlas = atlas;
		if (tileset == null)
		{
			tileset = new TiledMapTileSet();
		}
		DungeonFactory.character = player;
		buildTileSet();
	}
	
	public static void dispose()
	{
		atlas = null;
		tileset = null;
	}
	
	private static void buildTileSet()
	{
		if (tileset == null)
		{
			tileset = new TiledMapTileSet();
		}
		tileset.putTile(0, new SimpleTile(atlas.findRegion("null"), 0, false));		//empty
		tileset.putTile(1, new SimpleTile(atlas.findRegion("floor"), 1, true));		//room walls
		tileset.putTile(2, new SimpleTile(atlas.findRegion("floor"), 2, true));		//floor
		tileset.putTile(3, new SimpleTile(atlas.findRegion("down"), 3, true)); 		//stairs down
		tileset.putTile(4, new SimpleTile(atlas.findRegion("up"), 4, true)); 		//stairs up
	}
	
	/**
	 * Generate a new dungeon and save all its floors into a temp directory
	 * @param difficulty - defines how large and how many monsters the dungeon will have, but also more loot
	 * @throws IOException 
	 * @return an array of the serial ids of the floors on the file system
	 */
	protected static Array<Dungeon> create(FileType type, int difficulty, DungeonLoader loader)
	{
		//make sure dungeon dir exists
		//Gdx.files.absolute(tmpDir).mkdirs();
		
		//catch crazy out of bounds
		if (difficulty > 5 || difficulty < 1)
		{
			Gdx.app.log("[Invalid param]", "difficulty out of bounds, setting to 3");
			difficulty = 3;
		}
		
		
		//please don't ask about my numbers, they're so randomly picked out from my head
		// I don't even know what the curve looks like on a TI calculator ;_;
		int floors = MathUtils.random(5*difficulty, 10*difficulty+(difficulty-1)*10);
		
		Array<Dungeon> dungeon = new Array<Dungeon>();
		dungeon.ensureCapacity(floors+1);
		dungeon.addAll(new Dungeon[floors+1]);
		final Thread[] makerThreads = new Thread[floors];
		
		Thread makeWatch = new Thread(
			new Runnable(){
				@Override
				public void run(){
					for (Thread t : makerThreads)
					{
						t.start();
					}
					
					boolean alive = true;
					while (alive)
					{
						alive = false;
						for (int i = 0; i < makerThreads.length && !alive; i++)
						{
							Thread maker = makerThreads[i];
							if (maker.isAlive())
							{
								alive = true;
								break;
							}
						}
					}
				}
			}
		);
		
		//to stress test, uncomment next line
		//floors = 90;
		
		for (int floor = 1, width = 50, height = 50; floor <= floors; floor++)
		{
			Runnable run = new FloorMaker(type, difficulty, floor, width, height, loader, dungeon);
			makerThreads[floor-1] = new Thread(run);
			
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
			
			if (floor % 5 == 1)
			{
				width += 5;
				height += 5;
			}
		}
		
		makeWatch.start();
		
		//wait until threads are done
		while (makeWatch.isAlive()) ;
		
		return dungeon;
	}
	
	/**
	 * Prepare a world to be loaded and stepped into
	 * @param ts
	 */
	private static World create(Dungeon dungeon, Stats player, FloorLoader loader)
	{
		ItemFactory itemFactory = new ItemFactory(dungeon.type());
		MonsterFactory monsterFactory = new MonsterFactory(atlas, dungeon.type());
		
		/*
		String suffix = String.format("%02d", floor);
		FileHandle file = Gdx.files.absolute(tmpDir + "/" + serial + "." + suffix);
		Dungeon dungeon = (new Json()).fromJson(Dungeon.class, file);
		*/
		int floor = dungeon.floor();
		World world = new World();
		MovementSystem ms = new MovementSystem(floor);
		
		world.setManager(new TagManager());
		world.setManager(new GroupManager());
		world.setSystem(new RenderSystem(), true);
		world.setSystem(ms, true);
		loader.progress = 5;
		
		TiledMap map = new TiledMap();
		map.getTileSets().addTileSet(tileset);
		TiledMapTileLayer layer = dungeon.paintLayer(tileset, 32, 32);
		map.getLayers().add(layer);
		world.getSystem(RenderSystem.class).setMap(map);
		ms.setMap(layer);
		loader.progress = 40;
		
		//add monsters to rooms
		// monster count is anywhere between 5-20 on easy and 25-100 on hard
		monsterFactory.makeMonsters(world, dungeon.monsterCount(), layer, itemFactory, floor);
		loader.progress = 80;
		
		//forcibly add some loot monsters
		monsterFactory.makeTreasure(world, dungeon.rooms(), layer, itemFactory, floor);
		loader.progress = 90;
		
		world.initialize();
		
		//make player
		Entity e = world.createEntity();
		e.addComponent(new Position(0,0));
		e.addComponent(player);	//shared stats reference
		e.addComponent(new Renderable(character));
		e.addToWorld();
		
		world.getManager(TagManager.class).register("player", e);
		
		world.getSystem(MovementSystem.class).setPlayer();
		loader.progress = 100;
		//put entity at start position on each floor
		
		return world;
	}
	
	/**
	 * Super simple class that just represents a tile with a region.  Nothing special, but
	 * it allows us to generate tile sets in our code
	 * @author nhydock
	 */
	private static class SimpleTile implements TiledMapTile {

		final TextureRegion region;
		final MapProperties prop;
		final int id;
		
		SimpleTile(TextureRegion r, int id)
		{
			region = r;
			prop = new MapProperties();
			prop.put("passable", false);
			this.id = id;
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
		public void setId(int id) { } //do nothing, id is final

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
	
	/**
	 * Simple runnable for making a floor of a dungeon and updating a loader's progress
	 * @author nhydock
	 *
	 */
	private static class FloorMaker implements Runnable {
	
		final FileType type;
		final int difficulty;
		final int floor;
		final int width;
		final int height;
		final DungeonLoader loader;
		final Array<Dungeon> dungeon;
		
		FloorMaker(FileType type, int difficulty, int floor, int width, int height, DungeonLoader loader, Array<Dungeon> dungeon)
		{
			this.type = type;
			this.difficulty = difficulty;
			this.floor = floor;
			this.width = width;
			this.height = height;
			this.loader = loader;
			this.dungeon = dungeon;
		}
		
		@Override
		public void run() {
			Dungeon d = new Dungeon(type, difficulty, floor, width, height);
			dungeon.set(floor-1, d);
			loader.progress += (int)(1/(float)dungeon.size * 100);
		}
	}
	
	/**
	 * Loader for entire dungeons as assets
	 * @author nhydock
	 *
	 */
	@SuppressWarnings("rawtypes")
	public static class DungeonLoader extends AsynchronousAssetLoader<Array, DungeonLoader.DungeonParam> {

		public DungeonLoader(FileHandleResolver resolver) {
			super(resolver);
		}
		
		private Array<Dungeon> generatedDungeon;
		private int progress;
		
		@Override
		public void loadAsync(AssetManager manager, String fileName, FileHandle file, DungeonLoader.DungeonParam param) {
			generatedDungeon = create(param.type, param.difficulty, this);
		}

		@Override
		public Array loadSync(AssetManager manager, String fileName, FileHandle file, DungeonLoader.DungeonParam param) {
			param.dungeonContainer.setDungeon(generatedDungeon);
			
			return generatedDungeon;
		}

		@Override
		public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, DungeonLoader.DungeonParam param) {
			return null;
		}

		public int getProgress()
		{
			return progress;
		}
		
		public static class DungeonParam extends AssetLoaderParameters<Array>
		{
			public IDungeonContainer dungeonContainer;
			public FileType type;
			public int difficulty;
		}
	}
	
	/**
	 * Loader for entire floors as assets.  Makes artemis worlds!
	 * @author nhydock
	 *
	 */
	public static class FloorLoader extends AsynchronousAssetLoader<World, FloorLoader.FloorParam> {

		public FloorLoader(FileHandleResolver resolver) {
			super(resolver);
		}
		
		private World generatedFloor;
		
		private int progress;
		
		@Override
		public void loadAsync(AssetManager manager, String fileName, FileHandle file, FloorLoader.FloorParam param) {
			generatedFloor = create(param.floor, param.player, this);
		}

		@Override
		public World loadSync(AssetManager manager, String fileName, FileHandle file, FloorLoader.FloorParam param) {
			return generatedFloor;
		}

		@SuppressWarnings("rawtypes")
		@Override
		public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, FloorLoader.FloorParam param) {
			return null;
		}

		public int getProgress()
		{
			return progress;
		}
		
		public static class FloorParam extends AssetLoaderParameters<World>
		{
			public int depth;
			public IDungeonContainer dungeonContainer;
			public TextureAtlas atlas;
			
			public Dungeon floor;
			public Stats player;
		}
	}
}
