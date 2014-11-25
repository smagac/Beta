package core.datatypes.dungeon;

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

public class RandomFloorData implements Serializable, FloorData {
	int monsters;
	int[][] tiles;
	Array<Room> rooms;

	int floor;
	int width, height;
	int size;

	public RandomFloorData() {
	}

	/**
	 * Make a new dungeon instance using a path maker
	 * 
	 * @param floor
	 * @param maker
	 */
	public RandomFloorData(int difficulty, int floor, int width, int height) {

		this.width = width;
		this.height = height;
		this.floor = floor;
		this.size = width * height;

		tiles = new int[width][height];
		rooms = new Array<Room>();
	}

	/**
	 * Convert a generated map into a tiledmap layer using a tileset
	 * 
	 * @param tileset
	 * @param tW
	 *            - the pixel width of a single tile
	 * @param tH
	 *            - pixel height of a tile
	 * @return a newly made TiledMapTileLayer
	 */
	@Override
	public TiledMapTileLayer paintLayer(TiledMapTileSet tileset, int tW, int tH) {
		// add padding so it doesn't look like the rooms flood into nothingness
		TiledMapTileLayer layer = new TiledMapTileLayer(tiles.length + 2,
				tiles[0].length + 2, tW, tH);

		for (int x = -1, sX = 0; sX <= layer.getWidth(); x++, sX++) {
			for (int y = -1, sY = 0; sY <= layer.getHeight(); y++, sY++) {
				Cell cell = new Cell();

				TiledMapTile tile = null;
				if (x < 0 || x >= tiles.length || y < 0 || y >= tiles[0].length) {
					boolean set = false;
					for (int i = Math.max(0, x - 1); i <= Math.min(x + 1,
							tiles.length - 1) && !set; i++) {
						for (int j = Math.max(0, y - 1); j <= Math.min(y + 1,
								tiles[0].length - 1) && !set; j++) {
							if (tiles[i][j] != PathMaker.NULL) {
								tile = tileset.getTile(0);
								set = true;
							}
						}
					}
				} else if (tiles[x][y] == PathMaker.NULL) {
					boolean set = false;
					for (int i = Math.max(0, x - 1); i <= Math.min(x + 1,
							tiles.length - 1) && !set; i++) {
						for (int j = Math.max(0, y - 1); j <= Math.min(y + 1,
								tiles[0].length - 1) && !set; j++) {
							if (tiles[i][j] != PathMaker.NULL) {
								tile = tileset.getTile(0);
								set = true;
							}
						}
					}
				} else if (tiles[x][y] == PathMaker.WALL) {
					tile = tileset.getTile(1);
				} else if (tiles[x][y] == PathMaker.UP) {
					tile = tileset.getTile(4);
				} else if (tiles[x][y] == PathMaker.DOWN) {
					tile = tileset.getTile(3);
				} else {
					tile = tileset.getTile(2);
				}
				cell.setTile(tile);
				layer.setCell(x + 1, y + 1, cell);
			}
		}

		return layer;
	}

	@Override
	public void write(Json json) {

		// numerical properties
		json.writeValue("floor", floor);
		json.writeValue("width", width);
		json.writeValue("height", height);
		json.writeValue("monsters", monsters);

		json.writeValue("tiles", tiles);

		// write rooms
		json.writeArrayStart("rooms");
		for (Room r : rooms) {
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

		// load rooms
		Array<Room> rooms = new Array<Room>();
		JsonValue roomsData = jsonData.get("rooms");
		for (JsonValue room : roomsData) {
			int x = room.getInt("x");
			int y = room.getInt("y");
			int w = room.getInt("w");
			int h = room.getInt("h");
			rooms.add(new Room(x, y, w, h));
		}
		this.rooms = rooms;
	}

	@Override
	public void dispose() {
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