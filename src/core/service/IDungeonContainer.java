package core.service;

import com.artemis.World;
import com.badlogic.gdx.utils.Array;

import core.datatypes.Dungeon;
import core.datatypes.Dungeon.Floor;

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
