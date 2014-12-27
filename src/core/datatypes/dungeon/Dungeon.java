package core.datatypes.dungeon;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

import core.datatypes.FileType;

public class Dungeon implements Serializable {
    Array<FloorData> floorData;
    Array<Floor> floors = new Array<Floor>();
    IntMap<BossFloor> bossFloors = new IntMap<BossFloor>();
    int difficulty;
    FileType type;
    int depth;
    
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
     */

    public Dungeon(FileType type, int d, Array<FloorData> data) {
        this.type = type;
        this.floors = new Array<Floor>();
        this.difficulty = d;
        this.floorData = data;
        this.depth = data.size;
        genBossFloors();
    }
    
    /**
     * pick out which floors are floors where a boss appears
     */
    private void genBossFloors() {
        this.bossFloors.put(0, new BossFloor(difficulty, 0));
        for (int i = 0, set = 1; i < 1 + (size() / 10); i++, set += 10) {
            int floor = MathUtils.random(1, 10) + set;
            this.bossFloors.put(floor, new BossFloor(difficulty, floor));
        }
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
        this.depth = floorData.size;
        this.genBossFloors();
    }

    public Floor getFloor(int depth) {
        return floors.get(depth - 1);
    }

    public int size() {
        return depth;
    }

    public TiledMap build(TiledMapTileSet tileset) {
        TiledMap map = new TiledMap();
        map.getTileSets().addTileSet(tileset);

        // build the tile layers
        floors = new Array<Floor>();
        for (int i = 0; i < floorData.size; i++) {
            FloorData data;
            if (bossFloors.containsKey(i)) {
                data = bossFloors.get(i);
            } else {
                data = floorData.get(i);
            }
            TiledMapTileLayer layer = data.paintLayer(tileset, 32, 32);
            map.getLayers().add(layer);
            floors.add(new Floor(layer, data));
            data.dispose();
        }
        floorData.clear();
        floorData = null;
        
        return map;
    }
}