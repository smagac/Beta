package com.nhydock.storymode.factories;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.nhydock.storymode.datatypes.dungeon.Floor;
import com.nhydock.storymode.datatypes.dungeon.FloorLoader.Parameters;

public class FloorFactory {
    
    /**
     * Prepare a world to be loaded and stepped into
     * 
     * @param ts
     */
    public static ImmutableArray<Entity> populate(Parameters params, int[] progress) {
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
        monsterFactory.makeMonsters(entities, monsters, floor);
        progress[0] = 75;

        // forcibly add some loot monsters
        monsterFactory.makeTreasure(entities, floor);
        progress[0] = 85;
        
        // add doors onto the floor
        monsterFactory.placeDoors(entities, floor);
        progress[0] = 95;
        
        // add keys onto the floor
        monsterFactory.placeKeys(entities, floor);
        progress[0] = 95;
        
        return new ImmutableArray<Entity>(entities);
    }
}
