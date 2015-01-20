package core.datatypes.dungeon;

import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;

import core.datatypes.FileType;
import core.factories.FloorFactory;

/**
 * Loader for entire floors as assets. Makes artemis worlds!
 * 
 * @author nhydock
 *
 */
@SuppressWarnings("rawtypes")
public class FloorLoader extends AsynchronousAssetLoader<ImmutableArray, FloorLoader.FloorParam> {

    public FloorLoader(FileHandleResolver resolver) {
        super(resolver);
    }

    private int[] progress = {0};
    private ImmutableArray<Entity> data;

    @Override
    public void loadAsync(AssetManager manager, String fileName, FileHandle file, FloorLoader.FloorParam param) {
        data = FloorFactory.populate(param, progress);
    }

    @Override
    public ImmutableArray<?> loadSync(AssetManager manager, String fileName, FileHandle file, FloorLoader.FloorParam param) {
        return data;
    }

    @Override
    public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, FloorLoader.FloorParam param) {
        return null;
    }

    public static class FloorParam extends AssetLoaderParameters<ImmutableArray> {
        public int depth;
        public Floor floor;
        public Runnable onLoad;
        public FileType type;
        public int difficulty;
    }
}