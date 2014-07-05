package core.service;

import com.artemis.World;
import com.badlogic.gdx.assets.AssetManager;

import core.datatypes.FileType;

public interface IDungeonContainer extends Service {

	public void newDungeon(AssetManager manager, FileType type, int difficulty);
	public World loadFloor(int i);
	public int getCurrentFloorNumber();
	public World getCurrentFloor();
	public void nextFloor();
	public void prevFloor();
	public boolean hasPrevFloor();
	public boolean hasNextFloor();
}
