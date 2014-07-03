package core.service;

import com.artemis.World;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.utils.Array;

import core.datatypes.FileType;

public interface IDungeonContainer extends Service {

	public void newDungeon(AssetManager manager, FileType type, int difficulty);
	public Array<World> getDungeon();
}
