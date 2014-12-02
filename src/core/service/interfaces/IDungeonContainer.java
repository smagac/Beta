package core.service.interfaces;

import com.badlogic.ashley.core.Engine;

import github.nhydock.ssm.Service;
import core.datatypes.dungeon.Dungeon;
import core.datatypes.dungeon.Progress;
import core.datatypes.dungeon.DungeonLoader.DungeonParam;
import core.datatypes.dungeon.FloorLoader.FloorParam;

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
