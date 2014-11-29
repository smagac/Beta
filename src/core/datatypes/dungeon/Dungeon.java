package core.datatypes.dungeon;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer.Cell;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

import core.DataDirs;
import core.datatypes.FileType;
import core.util.dungeon.PathMaker;
import core.util.dungeon.Room;

public class Dungeon implements Serializable {
    Array<Floor> floors;
    Array<FloorData> floorData;
    int difficulty;
    FileType type;
    TiledMap map;

    boolean prepared;

    public Dungeon() {
    }

    /**
     * Make a new dungeon instance using a path maker
     * 
     * @param type
     * @param d
     *            - difficulty
     * @param f
     *            - premade floors to register with the dungeon
     * @param map
     */

    public Dungeon(FileType type, int d, Array<FloorData> data, TiledMap map) {
        this.type = type;
        this.floors = new Array<Floor>();
        this.difficulty = d;
        this.map = map;
        this.floorData = data;
    }

    public void setMap(TiledMap map) {
        this.map = map;
    }

    public TiledMap getMap() {
        return this.map;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public FileType type() {
        return type;
    }

    @Override
    public void write(Json json) {
        json.writeValue("type", this.type.name());
        json.writeValue("difficulty", this.difficulty);
        json.writeValue("floors", this.floorData);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void read(Json json, JsonValue jsonData) {

        this.type = FileType.valueOf(jsonData.getString("type"));
        this.difficulty = jsonData.getInt("difficulty");
        this.floorData = json.readValue(Array.class, jsonData.get("floors"));
    }

    public Floor getFloor(int depth) {
        return floors.get(depth - 1);
    }

    public int size() {
        return floors.size;
    }

    public void build(TiledMapTileSet tileset) {
        if (prepared) {
            Gdx.app.error("Dungeon Builder", "Map has already been built, can not build again");
            return;
        }

        // build the tile layers
        floors = new Array<Floor>();
        for (int i = 0; i < floorData.size; i++) {
            FloorData data = floorData.get(i);
            TiledMapTileLayer layer = data.paintLayer(tileset, 32, 32);
            map.getLayers().add(layer);
            floors.add(new Floor(layer, data));
            data.dispose();
        }
        floorData.clear();
        floorData = null;
        prepared = true;
    }
}
