package core.datatypes;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.artemis.World;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ObjectMap;

import core.util.dungeon.PathMaker;
import core.util.dungeon.Room;
import factories.ItemFactory;
import factories.MonsterFactory;

public class Dungeon implements Serializable{

	World world;
	
	ObjectMap<Vector2, String> monsters;
	TiledMap map;
	TiledMapTileLayer layer;
	
	int[][] tiles;
	Array<Room> rooms;
	
	int floor;
	int width, height;
	FileType type;
	
	public Dungeon(){}
	
	/**
	 * Make a new dungeon instance using a path maker
	 * @param floor
	 * @param maker
	 */
	public Dungeon(FileType type, int difficulty, int floor, int width, int height, MonsterFactory monsterFactory, ItemFactory itemFactory)
	{
		this.type = type;
		int roomCount = MathUtils.random(Math.max(5, ((3*floor)/10)+floor), Math.max(5, ((5*floor)/10)+floor));
		
		this.width = width;
		this.height = height;
		this.floor = floor;
		map = new TiledMap();
		PathMaker maker = new PathMaker();
		tiles = maker.run(roomCount, width, height);
		rooms = maker.getRooms();
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
				
				TiledMapTile tile;
				if (x < 0 || x >= tiles.length || y < 0 || y >= tiles[0].length)
				{
					tile = tileset.getTile(0);
				}
				else if (tiles[x][y] == PathMaker.NULL)
				{
					tile = tileset.getTile(0);
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
		json.writeValue("type", type.name());
		json.writeValue("floor", floor);
		json.writeValue("width", width);
		json.writeValue("height", height);
		
		//zip and pack the tiles
		String t = ""+tiles[0][0];
		for (int x = 0; x < width; x++)
		{
			for (int y = 0; y < height; y++)
			{
				t += ","+tiles[x][y];
			}
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			GZIPOutputStream gzip = new GZIPOutputStream(out);
			gzip.write(t.getBytes());
			gzip.close();
			json.writeValue("tiles", out.toString("ISO-8859-1"));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
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
		this.type = FileType.valueOf(jsonData.getString("type"));
		this.floor = jsonData.getInt("floor");
		
		int width, height;
		width = jsonData.getInt("width");
		height = jsonData.getInt("height");
		this.width = width;
		this.height = height;
		
		//decrypt tiles
		String tileData = jsonData.getString("tiles");
		String outStr = "";
	    try {
	    	ByteArrayInputStream in = new ByteArrayInputStream(tileData.getBytes("ISO-8859-1"));
			GZIPInputStream unzipper = new GZIPInputStream(in);
			BufferedReader bf = new BufferedReader(new InputStreamReader(unzipper, "ISO-8859-1"));
		    String line;
	        while ((line=bf.readLine())!=null) {
	          outStr += line;
	        }
	    } catch (IOException e) {
	    	e.printStackTrace();
			System.exit(-1);
		}
	    String[] tiles = outStr.split(",");
	    int[][] t = new int[width][height];
        
        //throw tiles back in array
        for (int x = 0, i = 0; x < width; x++)
        {
        	for (int y = 0; y < height; y++, i++)
        	{
        		t[x][y] = Integer.parseInt(tiles[i]);
        	}
        }
        this.tiles = t;
		
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

	public int roomCount() {
		return rooms.size;
	}
	
	public int floor()
	{
		return floor;
	}

	public Array<Room> rooms() {
		return rooms;
	}
}
