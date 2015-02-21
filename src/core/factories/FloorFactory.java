package core.factories;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

import core.datatypes.dungeon.Floor;
import core.datatypes.dungeon.FloorLoader.FloorParam;

public class FloorFactory {
    
    /**
     * Prepare a world to be loaded and stepped into
     * 
     * @param ts
     */
    public static ImmutableArray<Entity> populate(FloorParam params, int[] progress) {
        ItemFactory itemFactory = new ItemFactory(params.type);
        MonsterFactory monsterFactory = new MonsterFactory(params.type);

        Floor floor = params.floor;
        int base = Math.min(floor.roomCount, params.depth * 2);
        int monsters = floor.monsters;
        if (monsters == -1) {
            int a = (int) (base * Math.max(1, params.difficulty * params.depth / 50f));
            int b = (int) (base * Math.max(2, 2 * params.difficulty * params.depth / 50f));
            monsters = MathUtils.random(a, b);
            floor.monsters = monsters;
        }
        
        progress[0] = 10;
        
        Array<Entity> entities = new Array<Entity>();
        // add monsters to rooms
        // monster count is anywhere between 5-20 on easy and 25-100 on hard
        monsterFactory.makeMonsters(entities, monsters, itemFactory, floor);
        progress[0] = 75;

        // forcibly add some loot monsters
        monsterFactory.makeTreasure(entities, itemFactory, floor);
        progress[0] = 100;
        
        // add doors onto the floor
        monsterFactory.placeDoors(entities, itemFactory, floor);
        progress[0] = 100;
        
        return new ImmutableArray<Entity>(entities);
    }
}
