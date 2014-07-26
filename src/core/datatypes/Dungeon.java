package core.datatypes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

import core.util.dungeon.PathMaker;
import core.util.dungeon.Room;

public class Dungeon implements Serializable{
	Array<Floor> floors;
	Array<FloorData> floorData;
	int difficulty;
	FileType type;
	TiledMap map;
	
	boolean prepared;
	
	public Dungeon(){}
	
	/**
	 * Make a new dungeon instance using a path maker
	 * @param type
	 * @param d - difficulty
	 * @param f - premade floors to register with the dungeon
	 * @param map 
	 */
	
	public Dungeon(FileType type, int d, Array<FloorData> data, TiledMap map) {
		this.type = type;
		this.floors = new Array<Floor>();
		this.difficulty = d;
		this.map = map;
		this.floorData = data;
	}
	
	public void setMap(TiledMap map)
	{
		this.map = map;
	}
	
	public TiledMap getMap()
	{
		return this.map;
	}
	
	public int getDifficulty()
	{
		return difficulty;
	}

	public FileType type() {
		return type;
	}
	
	public static class Floor {
		final public TiledMapTileLayer layer;
		final public int roomCount;
		final public int depth;
		final public Array<Room> rooms;
		public int monsters;
		public int loot;
		
		public Floor(TiledMapTileLayer layer, FloorData data){
			this.layer = layer;
			roomCount = data.rooms.size;
			depth = data.floor;
			rooms = data.rooms;
			monsters = -1;
			loot = -1;
		}
	}
	
	public static class FloorData implements Serializable {
		int monsters;
		public int[][] tiles;
		public Array<Room> rooms;
		
		int floor;
		int width, height;
		int size;
		
		public FloorData(){}
		
		/**
		 * Make a new dungeon instance using a path maker
		 * @param floor
		 * @param maker
		 */
		public FloorData(int difficulty, int floor, int width, int height)
		{
			
			this.width = width;
			this.height = height;
			this.floor = floor;
			this.size = width * height;
			
			tiles = new int[width][height];
			rooms = new Array<Room>();
		}

		/**
		 * Convert a generated map into a tiledmap layer using a tileset
		 * @param tileset
		 * @param tW - the pixel width of a single tile
		 * @param tH - pixel height of a tile
		 * @return a newly made TiledMapTileLayer
		 */
		public TiledMapTileLayer paintLayer(TiledMapTileSet tileset, int tW, int tH)
		{
			//add padding so it doesn't look like the rooms flood into nothingness
			TiledMapTileLayer layer = new TiledMapTileLayer(tiles.length+2, tiles[0].length+2, tW, tH);
			
			for (int x = -1, sX = 0; sX <= layer.getWidth(); x++, sX++)
			{
				for (int y = -1, sY = 0; sY <= layer.getHeight(); y++, sY++)
				{
					Cell cell = new Cell();
					
					TiledMapTile tile = null;
					if (x < 0 || x >= tiles.length || y < 0 || y >= tiles[0].length)
					{
						boolean set = false;
						for (int i = Math.max(0, x-1); i <= Math.min(x+1, tiles.length-1) && !set; i++)
						{
							for (int j = Math.max(0, y-1); j <= Math.min(y+1, tiles[0].length-1) && !set; j++)
							{
								if (tiles[i][j] != PathMaker.NULL)
								{
									tile = tileset.getTile(0);
									set = true;
								}
							}
						}
					}
					else if (tiles[x][y] == PathMaker.NULL)
					{
						boolean set = false;
						for (int i = Math.max(0, x-1); i <= Math.min(x+1, tiles.length-1) && !set; i++)
						{
							for (int j = Math.max(0, y-1); j <= Math.min(y+1, tiles[0].length-1) && !set; j++)
							{
								if (tiles[i][j] != PathMaker.NULL)
								{
									tile = tileset.getTile(0);
									set = true;
								}
							}
						}
					}
					else if (tiles[x][y] == PathMaker.WALL)
					{
						tile = tileset.getTile(1);
					}	
					else if (tiles[x][y] == PathMaker.UP)
					{
						tile = tileset.getTile(4);
					}
					else if (tiles[x][y] == PathMaker.DOWN)
					{
						tile = tileset.getTile(3);
					}
					else
					{
						tile = tileset.getTile(2);
					}
					cell.setTile(tile);
					layer.setCell(x+1, y+1, cell);
				}
			}
			
			return layer;
		}

		@Override
		public void write(Json json) {

			//numerical properties
			json.writeValue("floor", floor);
			json.writeValue("width", width);
			json.writeValue("height", height);
			json.writeValue("monsters", monsters);
			
			json.writeValue("tiles", tiles);
			
			//write rooms
			json.writeArrayStart("rooms");
			for (Room r : rooms)
			{
				json.writeObjectStart();
				json.writeValue("x", r.x);
				json.writeValue("y", r.y);
				json.writeValue("w", r.width);
				json.writeValue("h", r.height);
				json.writeObjectEnd();
			}
			json.writeArrayEnd();
		}

		@Override
		public void read(Json json, JsonValue jsonData) {

	        this.tiles = json.readValue(int[][].class, jsonData.get("tiles"));
			

		    this.floor = jsonData.getInt("floor");
			this.width = jsonData.getInt("width");
			this.height = jsonData.getInt("height");
			this.monsters = jsonData.getInt("monsters");
			
		    
		    //load rooms
			Array<Room> rooms = new Array<Room>();
			JsonValue roomsData = jsonData.get("rooms");
			for (JsonValue room : roomsData)
			{
				int x = room.getInt("x");
				int y = room.getInt("y");
				int w = room.getInt("w");
				int h = room.getInt("h");
				rooms.add(new Room(x, y, w, h));
			}
			this.rooms = rooms;
		}
	
		private void dispose()
		{
			rooms = null;
			tiles = null;
		}
	}

	@Override
	public void write(Json json) {
		json.writeValue("type", this.type.name());
		json.writeValue("difficulty", this.difficulty);
		json.writeValue("floors", this.floorData);	
	}

	@SuppressWarnings("unchecked")
	@Override
	public void read(Json json, JsonValue jsonData) {
	    
		this.type = FileType.valueOf(jsonData.getString("type"));
		this.difficulty = jsonData.getInt("difficulty");
		this.floorData = json.readValue(Array.class, jsonData.get("floors"));
	}

	public Floor getFloor(int depth) {
		return floors.get(depth-1);
	}

	public int size() {
		return floors.size;
	}

	public void build(TiledMapTileSet tileset) {
		if (prepared) {
			Gdx.app.error("Dungeon Builder", "Map has already been built, can not build again");
			return;
		}
		
		//build the tile layers
		floors = new Array<Floor>();
		for (int i = 0; i < floorData.size; i++)
		{
			FloorData data = floorData.get(i);
			TiledMapTileLayer layer = data.paintLayer(tileset, 32, 32);
			map.getLayers().add(layer);
			floors.add(new Floor(layer, data));
			data.dispose();
		}
		floorData.clear();
		floorData = null;
		prepared = true;
	}
}
