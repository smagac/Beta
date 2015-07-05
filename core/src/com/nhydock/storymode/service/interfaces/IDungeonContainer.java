package com.nhydock.storymode.service.interfaces;

import github.nhydock.ssm.Service;

import com.badlogic.ashley.core.Engine;
import com.nhydock.storymode.datatypes.dungeon.Dungeon;
import com.nhydock.storymode.datatypes.dungeon.DungeonLoader;
import com.nhydock.storymode.datatypes.dungeon.FloorLoader;
import com.nhydock.storymode.datatypes.dungeon.Progress;

public interface IDungeonContainer extends Service {

    public void loadDungeon(DungeonLoader.Parameters params);
    public void loadFloor(FloorLoader.Parameters params);
    public boolean isLoading();
    
    public void setDungeon(Dungeon generatedDungeon);

    public Engine getEngine();
    public Dungeon getDungeon();
    public Progress getProgress();
    public Dungeon.Parameters getParams();
}
