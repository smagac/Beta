package factories;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;

import components.Position;
import components.Renderable;
import components.Stats;
import core.datatypes.Dungeon;
import core.datatypes.Dungeon.Floor;
import core.datatypes.Dungeon.FloorData;
import core.datatypes.FileType;
import core.service.IDungeonContainer;
import core.util.dungeon.PathMaker;

/**
 * Generates tiled maps and populates them for you to explore
 * @author nhydock
 *
 */
public class DungeonFactory {
	//directory to save dungeon files into
	//private final String tmpDir;
	
	//acceptable characters for serial id generation
	//private static final String acceptable = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	
	public static TiledMapTileSet buildTileSet(TextureAtlas atlas)
	{
		TiledMapTileSet tileset = new TiledMapTileSet();
		
		tileset.putTile(0, new SimpleTile(atlas.findRegion("null"), 0, false));		//empty
		tileset.putTile(1, new SimpleTile(atlas.findRegion("floor"), 1, true));		//room walls
		tileset.putTile(2, new SimpleTile(atlas.findRegion("floor"), 2, true));		//floor
		tileset.putTile(3, new SimpleTile(atlas.findRegion("down"), 3, true)); 		//stairs down
		tileset.putTile(4, new SimpleTile(atlas.findRegion("up"), 4, true)); 		//stairs up
		
		return tileset;
	}
	
	private static String bytesToHex(byte[] b) {
		char hexDigit[] = {'0', '1', '2', '3', '4', '5', '6', '7',
                  '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
		StringBuffer buf = new StringBuffer();
		for (int j=0; j<b.length; j++) {
		  buf.append(hexDigit[(b[j] >> 4) & 0x0f]);
		  buf.append(hexDigit[b[j] & 0x0f]);
		}
		return buf.toString();
	}
	
	/**
	 * Generate a new dungeon and save all its floors into a temp directory
	 * @param difficulty - defines how large and how many monsters the dungeon will have, but also more loot
	 * @param tileset 
	 * @throws IOException 
	 * @return an array of the serial ids of the floors on the file system
	 */
	protected static Dungeon create(String fileName, FileType type, int difficulty, final TiledMapTileSet tileset, DungeonLoader loader)
	{
		//prepare a tile map to hold the floor layers
		final TiledMap map = new TiledMap();
		map.getTileSets().addTileSet(tileset);
		
		//first check if the file has already been registered
		FileHandle hashFile = null;
		if (fileName != null)
		{
			try {
				MessageDigest md = MessageDigest.getInstance("SHA-1");
			
				byte[] hash = md.digest(fileName.getBytes());
				String name = new String(bytesToHex(hash));
				String tmpDir = System.getProperty("java.io.tmpdir");
				hashFile = Gdx.files.absolute(tmpDir+"/storymode/"+name+".tmp");
				//System.err.println(hashFile.path());
				if (!hashFile.exists())
				{
					hashFile.parent().mkdirs();
					hashFile.file().createNewFile();
					hashFile.file().deleteOnExit();
				}
				else
				{
					String outStr = "";
				    try {
				    	FileInputStream in = new FileInputStream(hashFile.file());
				    	GZIPInputStream unzipper = new GZIPInputStream(in);
						InputStreamReader zipRead = new InputStreamReader(unzipper);
						BufferedReader bf = new BufferedReader(zipRead);
						
					    String line;
				        while ((line=bf.readLine())!=null) {
				        	outStr += line;
				        }
				        
				        bf.close();
				        zipRead.close();
				    } catch (IOException e) {
				    	e.printStackTrace();
						System.exit(-1);
					}
					
					Json json = new Json();
					Dungeon d = json.fromJson(Dungeon.class, outStr);
					d.setMap(map);
					d.build(tileset);
					return d;
				}
			} catch (NoSuchAlgorithmException e) {
				e.printStackTrace();
				//shouldn't happen, just java being overly safe
				Gdx.app.exit();
				return null;
			} catch (IOException e) {
				e.printStackTrace();
				//might happen on read/write failure
			}
		}
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
		int depth = MathUtils.random(5*difficulty, 10*difficulty+(difficulty-1)*10);
		//to stress test, uncomment next line
		//int depth = 99;
		final Array<FloorData> floors = new Array<FloorData>();
		floors.addAll(new FloorData[depth]);
		final Thread[] makerThreads = new Thread[Math.min(depth, 4)];
		int[] unavailable = {0};
		for (int i = 0; i < makerThreads.length; i++)
		{
			Runnable run = new FloorMaker(difficulty, unavailable, loader, floors);
			makerThreads[i] = new Thread(run);
		}
		
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
		makeWatch.start();
		
		//wait until threads are done
		while (makeWatch.isAlive()) ;
		
		Dungeon d = new Dungeon(type, difficulty, floors, map);
		
		//ignore random dungeons for caching
		if (fileName != null)
		{
			//only try writing to file if we didn't get an i/o error
			if (hashFile != null && hashFile.exists())
			{
				BufferedWriter writer = null;
				try {
					FileOutputStream out = new FileOutputStream(hashFile.file());
					GZIPOutputStream gzip = new GZIPOutputStream(out);
					writer = new BufferedWriter(new OutputStreamWriter(gzip, "UTF-8"));
					
					Json json = new Json();
					json.toJson(d, Dungeon.class, writer);
				
					out.close();
					
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(-1);
				} finally {
					if (writer != null)
					{
						try { writer.close(); } catch (IOException e) {	e.printStackTrace(); }
					}
				}
			}
		}
		d.build(tileset);
		
		return d;
	}
	
	/**
	 * Prepare a world to be loaded and stepped into
	 * @param ts
	 */
	private static World create(Dungeon dungeon, int depth, Stats player, TextureAtlas atlas, TextureRegion character, FloorLoader loader)
	{
		ItemFactory itemFactory = new ItemFactory(dungeon.type());
		MonsterFactory monsterFactory = new MonsterFactory(atlas, dungeon.type());
		
		/*
		String suffix = String.format("%02d", floor);
		FileHandle file = Gdx.files.absolute(tmpDir + "/" + serial + "." + suffix);
		Dungeon dungeon = (new Json()).fromJson(Dungeon.class, file);
		*/
		Floor floor = dungeon.getFloor(depth);
		int base = Math.min(floor.roomCount, depth*2);
		int a = (int)(base*Math.max(1, dungeon.getDifficulty()*depth/50f));
		int b = (int)(base*Math.max(2, 2*dungeon.getDifficulty()*depth/50f));
		int monsters = MathUtils.random(a, b);
		
		World world = new World();
		MovementSystem ms = new MovementSystem(depth, dungeon);
		RenderSystem rs = new RenderSystem(depth, dungeon);
		world.setManager(new TagManager());
		world.setManager(new GroupManager());
		world.setSystem(rs, true);
		world.setSystem(ms, true);
		loader.progress = 10;
		
		//add monsters to rooms
		// monster count is anywhere between 5-20 on easy and 25-100 on hard
		monsterFactory.makeMonsters(world, monsters, itemFactory, dungeon.getFloor(depth));
		loader.progress = 60;
		
		//forcibly add some loot monsters
		monsterFactory.makeTreasure(world, itemFactory, dungeon.getFloor(depth));
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
	
		final int difficulty;
		private int depth;
		private int width;
		private int height;
		final DungeonLoader loader;
		final Array<FloorData> dungeon;
		volatile int[] unavailable;
		
		private FloorMaker(int difficulty, int[] unavailable, DungeonLoader loader, Array<FloorData> dungeon)
		{
			this.difficulty = difficulty;
			this.loader = loader;
			this.dungeon = dungeon;
			this.unavailable = unavailable;
			this.depth = unavailable[0];
		}
		
		@Override
		public void run() {
			
			while (depth < dungeon.size)
			{
				synchronized (unavailable)
				{
					depth = unavailable[0]++;
					if (depth >= dungeon.size)
					{
						break;
					}	
				}
				width = 50 + (5*(depth/5));
				height = 50 + (5*(depth/5));
				FloorData floor = new FloorData(difficulty, depth+1, width, height);
				
				int roomCount = MathUtils.random(Math.max(5, ((3*depth)/10)+depth), Math.max(5, ((5*depth)/10)+depth));
				PathMaker.run(floor, roomCount);
					
				dungeon.set(depth, floor);
				loader.progress += (int)(1/(float)dungeon.size * 100);
			}
		}
	}
	
	/**
	 * Loader for entire dungeons as assets
	 * @author nhydock
	 *
	 */
	@SuppressWarnings("rawtypes")
	public static class DungeonLoader extends AsynchronousAssetLoader<Dungeon, DungeonLoader.DungeonParam> {

		public DungeonLoader(FileHandleResolver resolver) {
			super(resolver);
		}
		
		private Dungeon generatedDungeon;
		private int progress;
		
		@Override
		public void loadAsync(AssetManager manager, String fileName, FileHandle file, DungeonLoader.DungeonParam param) {
			generatedDungeon = create(param.fileName, param.type, param.difficulty, param.tileset, this);
		}

		@Override
		public Dungeon loadSync(AssetManager manager, String fileName, FileHandle file, DungeonLoader.DungeonParam param) {
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
		
		public static class DungeonParam extends AssetLoaderParameters<Dungeon>
		{
			public IDungeonContainer dungeonContainer;
			public FileType type;
			public String fileName;
			public int difficulty;
			public TiledMapTileSet tileset;
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
			generatedFloor = create(param.dungeon, param.depth, param.player, param.atlas, param.character, this);
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
			public Dungeon dungeon;
			public Stats player;
			public TextureAtlas atlas;
			public TextureRegion character;
		}
	}
}
