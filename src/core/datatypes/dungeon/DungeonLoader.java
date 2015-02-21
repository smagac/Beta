package core.datatypes.dungeon;

import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;

import core.factories.DungeonFactory;

/**
 * Loader for entire dungeons as assets
 * 
 * @author nhydock
 *
 */
@SuppressWarnings("rawtypes")
public class DungeonLoader extends AsynchronousAssetLoader<Dungeon, DungeonLoader.DungeonParam> {

    public DungeonLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    private Dungeon generatedDungeon;
    protected float[] progress = {0};

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, DungeonLoader.DungeonParam param) {
        progress[0] = 0;
        // check if we just need to rebuild a dungeon
        if (param.generatedDungeon == null)
        {
            // first check if the file has already been registered
            generatedDungeon = DungeonFactory.loadFromCache(param.params);

            // if no dungeon could be loaded from cache, createa new one
            if (generatedDungeon == null) {
                generatedDungeon = DungeonFactory.create(param.params, progress);

                // try saving the dungeon to cache
                DungeonFactory.writeCacheFile(param.params, generatedDungeon);
            }
        } else {
            generatedDungeon = param.generatedDungeon;
        }
    }

    @Override
    public Dungeon loadSync(AssetManager manager, String fileName, FileHandle file, DungeonLoader.DungeonParam param) {
        Dungeon val = generatedDungeon;
        generatedDungeon = null;
        return val;
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, DungeonLoader.DungeonParam param) {
        return null;
    }
    
    public static class DungeonParam extends AssetLoaderParameters<Dungeon> {
        public DungeonParams params;
        public Runnable onLoad;
        public Dungeon generatedDungeon;
    }

    public float getProgress() {
        return progress[0];
    }
}