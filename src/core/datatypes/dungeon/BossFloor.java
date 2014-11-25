package core.datatypes.dungeon;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import core.DataDirs;
import core.util.dungeon.Room;

public class BossFloor implements FloorData {

    JsonValue map;
    int depth;
    int[][] tiles;

    public BossFloor(int difficulty, int depth) {
        super();
        this.depth = depth;

        JsonReader reader = new JsonReader();
        JsonValue map = reader.parse(Gdx.files.classpath(DataDirs.GameData + "boss.json"));

        // read the shit from the json in the data folder
        JsonValue data = map.get("layers").get(0);
        int width = data.getInt("width");
        int height = data.getInt("height");
        tiles = new int[height][width];
        int[] t = data.get("data").asIntArray();
        for (int i = 0, x = 0, y = 0; i < t.length; y++) {
            for (x = 0; x < width; x++, i++) {
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
    public TiledMapTileLayer paintLayer(TiledMapTileSet tileset, int width, int height) {
        TiledMapTileLayer layer = new TiledMapTileLayer(tiles[0].length, tiles.length, width, height);

        for (int col = 0; col < tiles.length; col++) {
            for (int row = 0; row < tiles[0].length; row++) {
                Cell cell = new Cell();
                int tile = tiles[col][row];
                if (tile == 0) {
                    cell.setTile(tileset.getTile(0));
                }
                else if (tile == 1) {
                    cell.setTile(tileset.getTile(2));
                }
                else if (tile == 3) {
                    cell.setTile(tileset.getTile(3));
                }
                else if (tile == 4) {
                    cell.setTile(tileset.getTile(4));
                }
                else {
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