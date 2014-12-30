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
    int[] start, end;
    private boolean[][] collision;

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
        start = new int[2];
        end = new int[2];
        collision = new boolean[width][height];
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
        TiledMapTileLayer layer = new TiledMapTileLayer(tiles.length, tiles[0].length, tW, tH);

        for (int x = 0; x < tiles.length; x++) {
            for (int y = 0; y < tiles[0].length; y++) {
                Cell cell = new Cell();
                TiledMapTile tile = null;
                if (tiles[x][y] == PathMaker.NULL)
                {
                    tile = tileset.getTile(2);
                }
                else if (tiles[x][y] == PathMaker.ROOM || tiles[x][y] == PathMaker.HALL)
                {
                    tile = tileset.getTile(1);
                }
                else if (tiles[x][y] == PathMaker.WALL) {
                    tile = tileset.getTile(0);
                }
                else if (tiles[x][y] == PathMaker.UP) {
                    tile = tileset.getTile(3);
                }
                else if (tiles[x][y] == PathMaker.DOWN) {
                    tile = tileset.getTile(5);
                }
                cell.setTile(tile);
                layer.setCell(x, y, cell);
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
        json.writeValue("start", start);
        json.writeValue("end", end);

        json.writeValue("tiles", tiles);
        json.writeValue("collision", collision);
        
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
        this.start = jsonData.get("start").asIntArray();
        this.end = jsonData.get("end").asIntArray();
        this.collision = new boolean[width][];
        
        for (int i = 0; i < width; i++)
        {
            this.collision[i] = jsonData.get("collision").get(i).asBooleanArray();
        }
        
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
    
    @Override
    public int getMonsters() {
        return monsters;
    }

    @Override
    public int[] getStart() {
        return start;
    }

    @Override
    public int[] getEnd() {
        return end;
    }

    @Override
    public boolean[][] getCollision() {
        return collision;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }
}