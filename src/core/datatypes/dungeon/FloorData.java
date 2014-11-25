package core.datatypes.dungeon;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.utils.Array;

import core.util.dungeon.Room;

public interface FloorData {
	public int getRoomCount();

	public int getDepth();

	public Array<Room> getRooms();

	public int[][] getTiles();

	TiledMapTileLayer paintLayer(TiledMapTileSet tileset, int width, int height);

	void dispose();
}