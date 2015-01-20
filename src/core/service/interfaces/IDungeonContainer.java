package core.service.interfaces;

import github.nhydock.ssm.Service;

import com.badlogic.ashley.core.Engine;

import core.datatypes.dungeon.Dungeon;
import core.datatypes.dungeon.DungeonLoader.DungeonParam;
import core.datatypes.dungeon.FloorLoader.FloorParam;
import core.datatypes.dungeon.Progress;

public interface IDungeonContainer extends Service {

    public void loadDungeon(DungeonParam params);
    public void loadFloor(FloorParam params);
    public boolean isLoading();
    
    public void setDungeon(Dungeon generatedDungeon);

    public Engine getEngine();
    public Dungeon getDungeon();
    public Progress getProgress();
    
    public void clear();
}
