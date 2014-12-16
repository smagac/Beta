package core.datatypes.dungeon;

import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
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

    final boolean[][] collision;
    final float[][] shadow;
    private int[] start;
    private int[] end;
    
    public Floor(TiledMapTileLayer layer, FloorData data) {
        this.layer = layer;
        roomCount = data.getRoomCount();
        depth = data.getDepth();
        rooms = data.getRooms();
        monsters = -1;
        loot = -1;
        isBossFloor = (data instanceof BossFloor);
        
        collision = new boolean[layer.getWidth()][layer.getHeight()];
        shadow = new float[layer.getWidth()][layer.getHeight()];
        
        for (int x = 0; x < collision.length; x++) {
            for (int y = 0; y < collision[0].length; y++) {
                Cell c = layer.getCell(x, y);
                if (c == null || c.getTile() == null) {
                    collision[x][y] = false;
                    shadow[x][y] = 0.0f;
                }
                else {
                    TiledMapTile t = c.getTile();
                    collision[x][y] = t.getProperties().get("passable", Boolean.class);
                    shadow[x][y] = collision[x][y]?0.0f:1.0f;
                    // set as start or end if they're step tiles
                    if (t.getId() == 4) {
                        start = new int[] { x, y };
                    }
                    else if (t.getId() == 3) {
                        end = new int[] { x, y };
                    }
                }
            }
        }    
    }


    public boolean[][] getBooleanMap() {
        return collision;
    }
    
    public float[][] getShadowMap() {
        return shadow;
    }
    
    public int[] getStartPosition() {
        return start;
    }
    
    public int[] getEndPosition() {
        return end;
    }
}