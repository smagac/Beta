package core.datatypes.dungeon;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.utils.Array;

import core.util.dungeon.Room;

public interface FloorData {
    public long getSeed();
    
    public int getRoomCount();

    public int getDepth();

    public Array<Room> getRooms();

    public int getMonsters();
    
    public int[][] getTiles();

    TiledMapTileLayer paintLayer(TiledMapTileSet tileset, int width, int height);
    
    public int[] getStart();

    public int[] getEnd();
    
    public boolean[][] getCollision();

    public int getWidth();
    public int getHeight();
}