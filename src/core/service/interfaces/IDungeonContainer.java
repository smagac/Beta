package core.service.interfaces;

import github.nhydock.ssm.Service;

import com.artemis.World;

import core.datatypes.dungeon.Dungeon;
import core.datatypes.dungeon.Floor;

public interface IDungeonContainer extends Service {

    public void setDungeon(Dungeon generatedDungeon);

    void setCurrentFloor(int i, World world);

    public Floor getFloor(int i);

    public int getCurrentFloorNumber();

    public World getCurrentFloor();

    public boolean hasPrevFloor();

    public boolean hasNextFloor();

    public int nextFloor();

    public int prevFloor();
}
