package com.nhydock.storymode.datatypes.dungeon;

public class Progress {

    public int monstersKilled;
    public int monstersTotal;

    public int lootFound;
    public int lootTotal;
    public int totalLootFound;

    public int depth;
    public int deepest;
    public int floors;
    public int healed;
    public int keys;

    public boolean hasPrevFloor(int depth){
        return depth - 1 >= 0;
    }
    public boolean hasNextFloor(int depth) {
        return depth + 1 <= floors;
    }

    public int nextFloor() {
        return depth + 1;
    }
    public int prevFloor() {
        return depth - 1;
    }

}