package com.nhydock.storymode.datatypes.dungeon;

import github.nhydock.ssm.ServiceManager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.nhydock.storymode.DataDirs;
import com.nhydock.storymode.service.interfaces.IPlayerContainer;
import com.nhydock.storymode.util.dungeon.Room;
import com.nhydock.storymode.util.dungeon.TsxTileSet;

public class BossFloor implements FloorData {

    JsonValue map;
    int depth;
    int[][] tiles;
    Array<Room> rooms;
    int[] start, end;
    boolean[][] collision;

    public BossFloor(int difficulty, int depth) {
        super();
        this.depth = depth;

        JsonReader reader = new JsonReader();
        JsonValue map = reader.parse(Gdx.files.classpath(DataDirs.GameData + "boss.json"));

        // read the shit from the json in the data folder
        JsonValue data = map.get("layers").get(0);
        int width = data.getInt("width");
        int height = data.getInt("height");
        tiles = new int[width][height];
        int[] t = data.get("data").asIntArray();
        collision = new boolean[width][height];
        for (int i = 0, x = 0, y = height-1; i < t.length; y--) {
            for (x = 0; x < width; x++, i++) {
                tiles[x][y] = t[i];
                collision[x][y] = true;
                if (t[i] == 3) {
                    end = new int[]{x, y};
                    collision[x][y] = false;
                }
                else if (t[i] == 4) {
                    start = new int[]{x, y};
                    collision[x][y] = false;
                }
                else if (t[i] == 1) {
                    collision[x][y] = false;
                }
            }
        }
        
        rooms = new Array<Room>();
        rooms.add(new Room(width/2, height/2, 1, 1));
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
        return rooms;
    }

    /**
     * Maps json file to our programmed tileset
     */
    @Override
    public TiledMapTileLayer paintLayer(TiledMapTileSet tileset, int width, int height) {
        TiledMapTileLayer layer = new TiledMapTileLayer(tiles[0].length, tiles.length, width, height);
        IPlayerContainer player = ServiceManager.getService(IPlayerContainer.class);
        
        for (int col = 0; col < tiles.length; col++) {
            for (int row = 0; row < tiles[0].length; row++) {
                Cell cell = new Cell();
                int tile = tiles[col][row];
                //wall
                if (tile == 2) {
                    cell.setTile(tileset.getTile(TsxTileSet.WALL));
                }
                //floor
                else if (tile == 1) {
                    cell.setTile(tileset.getTile(TsxTileSet.FLOOR));
                }
                //down stairs
                else if (tile == 3) {
                    cell.setTile(tileset.getTile(TsxTileSet.DOWN));
                }
                //up stairs
                else if (tile == 4 && !player.isHardcore()) {
                    cell.setTile(tileset.getTile(TsxTileSet.UP));
                }
                //null
                else {
                    cell.setTile(tileset.getTile(TsxTileSet.NULL));
                }

                layer.setCell(col, row, cell);
            }
        }
        return layer;
    }

    @Override
    public int[][] getTiles() {
        return tiles;
    }

    @Override
    public int getMonsters() {
        //there are no monsters aside from the boss
        return 0;
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
        return tiles.length;
    }

    @Override
    public int getHeight() {
        return tiles[0].length;
    }

    @Override
    public long getSeed() {
        return 0;
    }
}