package factories;

import scenes.dungeon.MovementSystem;
import scenes.dungeon.RenderSystem;

import com.artemis.World;
import com.artemis.managers.GroupManager;
import com.artemis.managers.TagManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

import core.datatypes.FileType;
import dungeon.PathMaker;

/**
 * Generates tiled maps and populates them for you to explore
 * @author nhydock
 *
 */
public class DungeonFactory {

	private ItemFactory itemFactory;
	private MonsterFactory monsterFactory;
	private TiledMapTileSet tileset;
	
	public DungeonFactory(TextureAtlas atlas, FileType type)
	{
		itemFactory = new ItemFactory(type);
		monsterFactory = new MonsterFactory(atlas, type);
		TextureRegion tiles = atlas.findRegion("tiles2");
		buildTileSet(tiles);
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
	 * Generate a new dungeon
	 * @param difficulty - defines how large and how many monsters the dungeon will have, but also more loot
	 */
	public Array<World> create(int difficulty)
	{
		//catch crazy out of bounds
		if (difficulty > 5 || difficulty < 1)
		{
			Gdx.app.log("[Invalid param]", "difficulty out of bounds, setting to 3");
			difficulty = 3;
		}
		
		int floors = MathUtils.random(1, difficulty+difficulty) * MathUtils.random(1, difficulty);
		Array<World> dungeon = new Array<World>();
		for (int i = 0; i < floors; i++)
		{
			World world = new World();
			
			world.setManager(new TagManager());
			world.setManager(new GroupManager());
			
			world.setSystem(new RenderSystem(), true);
			MovementSystem ms = new MovementSystem(i);
			world.setSystem(ms, true);
			
			//make rooms and doors
			int roomCount = MathUtils.random(1+(difficulty*2), 3+(difficulty*4));

			TiledMap tm = new TiledMap();
			TiledMapTileLayer layer;

			PathMaker maker = new PathMaker(50, 50);
			maker.run(roomCount);
			layer = maker.paintLayer(tileset, 32, 32);
			
			tm.getLayers().add(layer);
			
			//add monsters to rooms
			// monster count is anywhere between 5-20 on easy and 25-100 on hard
			monsterFactory.makeMonsters(world, MathUtils.random(difficulty*2, difficulty*4), layer, itemFactory);
			
			
			
			//generate map from rooms
			world.getSystem(RenderSystem.class).setMap(tm);
			ms.setMap(layer);
			
			world.initialize();
			dungeon.add(world);
		}
		return dungeon;
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
