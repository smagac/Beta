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
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import core.DataDirs;
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
			roomCount = data.getRoomCount();
			depth = data.getDepth();
			rooms = data.getRooms();
			monsters = -1;
			loot = -1;
		}
	}
	
	public static interface FloorData {
		public int getRoomCount();
		public int getDepth();
		public Array<Room> getRooms();
		public int[][] getTiles();
		TiledMapTileLayer paintLayer(TiledMapTileSet tileset, int width, int height);
		void dispose();
	}
	
	public static class RandomFloorData implements Serializable, FloorData {
		int monsters;
		int[][] tiles;
		Array<Room> rooms;
		
		int floor;
		int width, height;
		int size;
		
		public RandomFloorData(){}
		
		/**
		 * Make a new dungeon instance using a path maker
		 * @param floor
		 * @param maker
		 */
		public RandomFloorData(int difficulty, int floor, int width, int height)
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
		@Override
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
	
		@Override
		public void dispose()
		{
			rooms = null;
			tiles = null;
		}

		@Override
		public int getRoomCount() {
			return rooms.size;
		}

		@Override
		public int getDepth() {
			return floor;
		}

		@Override
		public Array<Room> getRooms() {
			return rooms;
		}

		@Override
		public int[][] getTiles() {
			return tiles;
		}
	}
	
	public static class BossFloor implements FloorData {

		JsonValue map;
		int depth;
		int[][] tiles;
		
		public BossFloor(int difficulty, int depth) {
			super();
			this.depth = depth;
			
			JsonReader reader = new JsonReader();
			JsonValue map = reader.parse(Gdx.files.classpath(DataDirs.GameData + "boss.json"));
			
			//read the shit from the json in the data folder
			JsonValue data = map.get("layers").get(0);
			int width = data.getInt("width");
			int height = data.getInt("height");
			tiles = new int[height][width];
			int[] t = data.get("data").asIntArray();
			for (int i = 0, x = 0, y = 0; i < t.length; y++)
			{
				for (x = 0; x < width; x++, i++)
				{
					tiles[y][x] = t[i];
				}
			}
		}

		@Override
		public int getRoomCount() {
			return 1;
		}

		@Override
		public int getDepth() {
			return depth;
		}

		/**
		 * We don't need room calculation for the dungeon
		 */
		@Override
		public Array<Room> getRooms() {
			return null;
		}

		/**
		 * Maps json file to our programmed tileset
		 */
		@Override
		public TiledMapTileLayer paintLayer(TiledMapTileSet tileset, int width,
				int height) {
			TiledMapTileLayer layer = new TiledMapTileLayer(tiles[0].length, tiles.length, width, height);
			
			for (int col = 0; col < tiles.length; col++)
			{
				for (int row = 0; row < tiles[0].length; row++)
				{
					Cell cell = new Cell();
					int tile = tiles[col][row];
					if (tile == 0)
					{
						cell.setTile(tileset.getTile(0));
					}
					else if (tile == 1)
					{
						cell.setTile(tileset.getTile(2));
					}
					else if (tile == 3)
					{
						cell.setTile(tileset.getTile(3));
					}
					else if (tile == 4)
					{
						cell.setTile(tileset.getTile(4));
					}
					else
					{
						cell.setTile(tileset.getTile(1));
					}
					
					layer.setCell(col, row, cell);
				}
			}
			return layer;
		}

		@Override
		public void dispose() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public int[][] getTiles() {
			return tiles;
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
