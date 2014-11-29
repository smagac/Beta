package core.datatypes.dungeon;

import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.utils.Array;

import core.util.dungeon.Room;

public class Floor {
    final public TiledMapTileLayer layer;
    final public int roomCount;
    final public int depth;
    final public Array<Room> rooms;
    final public boolean isBossFloor;
    public int monsters;
    public int loot;

    
    public Floor(TiledMapTileLayer layer, FloorData data) {
        this.layer = layer;
        roomCount = data.getRoomCount();
        depth = data.getDepth();
        rooms = data.getRooms();
        monsters = -1;
        loot = -1;
        isBossFloor = (data instanceof BossFloor);
    }
}