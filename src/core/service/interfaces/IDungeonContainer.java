package core.service.interfaces;

import com.badlogic.ashley.core.Engine;

import github.nhydock.ssm.Service;
import core.datatypes.dungeon.Dungeon;
import core.datatypes.dungeon.Floor;

public interface IDungeonContainer extends Service {

    public void setDungeon(Dungeon generatedDungeon);

    void setCurrentFloor(int i);

    public Floor getFloor(int i);

    public int getCurrentFloorNumber();

    public boolean hasPrevFloor();

    public boolean hasNextFloor();

    public int nextFloor();

    public int prevFloor();

    public Engine getCurrentFloor();
}
