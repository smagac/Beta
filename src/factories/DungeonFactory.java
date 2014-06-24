package factories;

import GenericSystems.MovementSystem;
import GenericSystems.RenderSystem;

import com.artemis.World;
import com.artemis.managers.GroupManager;
import com.artemis.managers.TagManager;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectSet;

import core.datatypes.FileType;

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
		TextureRegion tiles = atlas.findRegion("tiles");
		buildTileSet(tiles);
	}
	
	private void buildTileSet(TextureRegion tiles)
	{
		tileset = new TiledMapTileSet();
		//corners
		TextureRegion[][] ti = tiles.split(32, 32);
		
		tileset.putTile(0, new SimpleTile(ti[0][0]));
		tileset.putTile(1, new SimpleTile(ti[0][2]));
		tileset.putTile(2, new SimpleTile(ti[2][0]));
		tileset.putTile(3, new SimpleTile(ti[2][2]));
		
		//sides
		tileset.putTile(4, new SimpleTile(ti[0][1]));
		tileset.putTile(5, new SimpleTile(ti[1][0]));
		tileset.putTile(6, new SimpleTile(ti[1][2]));
		tileset.putTile(7, new SimpleTile(ti[2][1]));
		
		//decor edges
		tileset.putTile(8, new SimpleTile(ti[1][1]));
		tileset.putTile(9, new SimpleTile(ti[3][0]));
		tileset.putTile(10, new SimpleTile(ti[3][1]));
		tileset.putTile(11, new SimpleTile(ti[3][2]));
		
		//floor
		tileset.putTile(12, new SimpleTile(ti[0][3], true));
		//hallway
		tileset.putTile(13, new SimpleTile(ti[1][3], true));
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
			System.err.println("difficulty out of bounds, setting to 3");
			difficulty = 3;
		}
		
		int floors = MathUtils.random(1, difficulty+difficulty) * MathUtils.random(1, difficulty);
		System.out.println("floors : " + floors);
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
			Array<Rectangle> rooms = new Array<Rectangle>();
			
			//generate non-overlapping rooms
			do
			{
				Rectangle r = new Rectangle(MathUtils.random(1, 250),
											MathUtils.random(2, 250),
											MathUtils.random(2, 25),
											MathUtils.random(2, 25));
				boolean place = true;
				Rectangle real = new Rectangle(r.x-1, r.y-2, r.width+1, r.height+3);
				for (int n = 0; n < rooms.size && place; n++)
				{
					Rectangle room = rooms.get(n);
					if (real.overlaps(room))
					{
						place = false;
					}
				}
				
				if (place)
				{
					rooms.add(r);
				}
			} while (rooms.size < roomCount);
			
			//generate doors until all rooms are connected
			ObjectSet<Rectangle> connected = new ObjectSet<Rectangle>();
			Array<Array<Vector2>> paths = new Array<Array<Vector2>>();
			do
			{
				Rectangle roomA = rooms.random();
				Vector2 doorA = pickEdgePoint(roomA);
				
				Rectangle roomB;
				//prevent hallway to same room
				do
				{
					roomB = rooms.random();
				} while (roomB == roomA);
				Vector2 doorB = pickEdgePoint(roomB);
				
				paths.add(connectDoors(doorA, doorB));
				
				connected.add(roomA);
				connected.add(roomB);
				
			} while (connected.size < rooms.size);
			
			//add monsters to rooms
			// monster count is anywhere between 5-20 on easy and 25-100 on hard
			monsterFactory.makeMonsters(world, MathUtils.random(difficulty*5, difficulty*20), rooms, itemFactory);
			
			//generate map from rooms
			TiledMap tm = generateMap(rooms, paths);
			world.getSystem(RenderSystem.class).setMap(tm);
			ms.setMap((TiledMapTileLayer)tm.getLayers().get(0), rooms);
			
			world.initialize();
			dungeon.add(world);
		}
		return dungeon;
	}
	
	/**
	 * Picks a point on the edge of a room in which to place a doorway
	 * @param room - room we're examining the bounds of
	 * @return a door (Vector2)
	 */
	private Vector2 pickEdgePoint(Rectangle room)
	{
		//pick a side
		int side = MathUtils.random(4);
		int x = (int)room.x,
			y = (int)room.y;
		//left
		if (side == 0)
		{
			x = (int)room.x-1;
			y = (int)MathUtils.random(room.y, room.y+room.height);
		}
		if (side == 1)
		{
			x = (int)(room.x+room.width);
			y = (int)MathUtils.random(room.y, room.y+room.height);
		}
		if (side == 2)
		{
			x = (int)MathUtils.random(room.x, room.x+room.width);
			y = (int)room.y-1;
		}
		if (side == 3)
		{
			x = (int)MathUtils.random(room.x, room.x+room.width);
			y = (int)(room.y-room.height);
		}
		
		return new Vector2(x, y);
	}
	
	/**
	 * Generates the tile map
	 */
	private TiledMap generateMap(Array<Rectangle> rooms, Array<Array<Vector2>> paths)
	{
		TiledMap map = new TiledMap();
		
		//calculate bounds;
		int width = 1;
		int height = 1;
		for (Rectangle r : rooms)
		{
			width = Math.max((int)(r.x + r.width + 1), width);
			height = Math.max((int)(r.y + r.height + 2), height);
		}
		
		
		map.getTileSets().addTileSet(tileset);
		
		TiledMapTileLayer layer = new TiledMapTileLayer(width, height, 32, 32);
		
		//draw all the rooms
		for (Rectangle room : rooms)
		{
			drawRoom(room, layer);
		}
		
		//draw all the paths
		for (Array<Vector2> path : paths)
		{
			drawPath(path, layer);
		}
		
		map.getLayers().add(layer);
		
		return map;
	}
	
	/**
	 * Connect 2 door points using A*
	 * @param in
	 * @param out
	 * @param layer
	 */
	private Array<Vector2> connectDoors(Vector2 in, Vector2 out)
	{
		int x = (int)in.x;
		int y = (int)in.y;
		Array<Vector2> path = new Array<Vector2>();
		
		//TODO find path between both doors
		
		return path;
	}
	
	/**
	 * Draws a room into a tile map layer
	 * @param room
	 * @param layer
	 */
	private void drawRoom(Rectangle room, TiledMapTileLayer layer)
	{
		//corners
		{
			//top-left
			Cell c = new Cell();
			c.setTile(tileset.getTile(0));
			layer.setCell((int)room.x-1, (int)(room.y+room.height)+1, c);
			//top-right
			c = new Cell();
			c.setTile(tileset.getTile(1));
			layer.setCell((int)(room.x+room.width)+1, (int)(room.y+room.height)+1, c);
			//bottom-left
			c = new Cell();
			c.setTile(tileset.getTile(2));
			layer.setCell((int)room.x-1, (int)(room.y)-1, c);
			//bottom-right
			c = new Cell();
			c.setTile(tileset.getTile(3));
			layer.setCell((int)(room.x+room.width)+1, (int)(room.y)-1, c);
			//decor bottom-left
			c = new Cell();
			c.setTile(tileset.getTile(9));
			layer.setCell((int)room.x-1, (int)(room.y)-2, c);
			//decor bottom-right
			c = new Cell();
			c.setTile(tileset.getTile(11));
			layer.setCell((int)(room.x+room.width)+1, (int)(room.y)-2, c);
		}
		
		//edges
		//top
		for (int x = (int)room.x, y = (int)(room.y+room.height)+1; x <= (int)(room.x+room.getWidth()); x++)
		{
			Cell c = new Cell();
			c.setTile(tileset.getTile(4));
			layer.setCell(x, y, c);
			//top decor
			c = new Cell();
			c.setTile(tileset.getTile(8));
			layer.setCell(x, y-1, c);
		}
		//bottom
		for (int x = (int)room.x, y = (int)room.y-1; x <= (int)(room.x+room.getWidth()); x++)
		{
			Cell c = new Cell();
			c.setTile(tileset.getTile(7));
			layer.setCell(x, y, c);
			//bottom decor
			c = new Cell();
			c.setTile(tileset.getTile(10));
			layer.setCell(x, y-1, c);
		}
		//left
		for (int y = (int)room.y, x = (int)room.x-1; y <= (int)(room.y+room.getHeight()); y++)
		{
			Cell c = new Cell();
			c.setTile(tileset.getTile(5));
			layer.setCell(x, y, c);
		}
		//right
		for (int y = (int)room.y, x = (int)(room.x+room.width) + 1; y <= (int)(room.y+room.getHeight()); y++)
		{
			Cell c = new Cell();
			c.setTile(tileset.getTile(6));
			layer.setCell(x, y, c);
		}
		//fill floor
		for (int y = (int)room.y, i = 0; i <= room.height; y++, i++)
		{
			for (int x = (int)room.x, n = 0; n <= room.width; x++, n++)
			{
				Cell c = new Cell();
				c.setTile(tileset.getTile(12));
				layer.setCell(x, y, c);
			}
		}
	}
	
	/**
	 * Draws a path onto the layer
	 * @param path
	 * @param layer
	 */
	public void drawPath(Array<Vector2> path, TiledMapTileLayer layer)
	{
		for (Vector2 tile : path)
		{
			int x = (int)tile.x,
				y = (int)tile.y;
			
			Cell c = layer.getCell(x, y);
			//don't draw over floors
			if (c != null && c.getTile().getId() == 12)
			{
				continue;
			}
			
			//catch areas that don't have walls or floor yet
			if (c == null)
				c = new Cell();
			
			c.setTile(tileset.getTile(12));
			layer.setCell((int)tile.x, (int)tile.y, c);
		}
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
		
		SimpleTile(TextureRegion r)
		{
			region = r;
			prop = new MapProperties();
			prop.put("passable", false);
		}
		
		SimpleTile(TextureRegion r, boolean passable)
		{
			this(r);
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
