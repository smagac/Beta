package core.datatypes.dungeon;

import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

import core.util.dungeon.PathMaker;
import core.util.dungeon.Room;

public class Floor {
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
    private Array<int[]> hallways;
    
    public Floor(TiledMapTileLayer layer, FloorData data) {
        roomCount = data.getRoomCount();
        depth = data.getDepth();
        rooms = data.getRooms();
        monsters = -1;
        loot = -1;
        isBossFloor = (data instanceof BossFloor);
        
        collision = data.getCollision();
        shadow = new float[data.getWidth()][layer.getHeight()];
        for (int i = 0; i < layer.getWidth(); i++)
        {
            for (int n = 0; n < layer.getHeight(); n++)
            {
                TiledMapTile tile = layer.getCell(i, n).getTile();
                float density = tile.getProperties().get("density", 0f, Float.class);
                shadow[i][n] = density;
            }
        }
        
        hallways = new Array<int[]>();
        for (int i = 0; i < data.getWidth(); i++)
        {
            for (int n = 0; n < data.getHeight(); n++) 
            {
                if (data.getTiles()[i][n] == PathMaker.HALL) {
                    hallways.add(new int[]{i, n});
                }
            }
        }
        
        start = data.getStart();
        end = data.getEnd();
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
    
    /**
     * @return an available tile on this floor
     */
    public int[] getOpenTile()
    {
        Room r = rooms.random();
        int x, y;
        do {
            x = MathUtils.random(r.innerLeft(), r.innerRight());
            y = MathUtils.random(r.innerBottom(), r.innerTop());
        } while ((x == start[0] && y == start[1]) || (x == end[0] && y == end[1]));
        
        return new int[]{x, y};
    }
    
    public int[] getHallwayTile()
    {
        return hallways.random();
    }
}