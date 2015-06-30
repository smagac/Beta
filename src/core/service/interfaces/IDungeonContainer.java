package core.service.interfaces;

import github.nhydock.ssm.Service;

import com.badlogic.ashley.core.Engine;

import core.datatypes.dungeon.Dungeon;
import core.datatypes.dungeon.DungeonLoader;
import core.datatypes.dungeon.FloorLoader;
import core.datatypes.dungeon.Progress;

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
