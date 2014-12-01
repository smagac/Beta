package core.factories;

import scenes.dungeon.MovementSystem;
import scenes.dungeon.RenderSystem;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.assets.AssetDescriptor;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.AsynchronousAssetLoader;
import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

import core.datatypes.dungeon.Dungeon;
import core.datatypes.dungeon.Floor;
import core.factories.FloorFactory.FloorLoader.FloorParam;
import core.service.interfaces.IDungeonContainer;

public class FloorFactory {
    /**
     * Loader for entire floors as assets. Makes artemis worlds!
     * 
     * @author nhydock
     *
     */
    public static class FloorLoader extends AsynchronousAssetLoader<ImmutableArray, FloorLoader.FloorParam> {

        public FloorLoader(FileHandleResolver resolver) {
            super(resolver);
        }

        private int progress;
        private ImmutableArray data;

        @Override
        public void loadAsync(AssetManager manager, String fileName, FileHandle file, FloorLoader.FloorParam param) {
            data = populate(param, this);
        }

        @Override
        public ImmutableArray loadSync(AssetManager manager, String fileName, FileHandle file, FloorLoader.FloorParam param) {
            return data;
        }

        @SuppressWarnings("rawtypes")
        @Override
        public Array<AssetDescriptor> getDependencies(String fileName, FileHandle file, FloorLoader.FloorParam param) {
            return null;
        }

        public int getProgress() {
            return progress;
        }

        public static class FloorParam extends AssetLoaderParameters<ImmutableArray> {
            public int depth;
            public IDungeonContainer dungeonContainer;
            public Dungeon dungeon;
            public TextureAtlas atlas;
        }
    }
    
    private static FloorLoader loader = new FloorLoader(new InternalFileHandleResolver());
    public static void prepareManager(AssetManager manager) {
        manager.setLoader(ImmutableArray.class, loader);
    }
    
    
    /**
     * Prepare a world to be loaded and stepped into
     * 
     * @param ts
     */
    private static ImmutableArray<Entity> populate(FloorParam params, FloorLoader loader) {
        ItemFactory itemFactory = new ItemFactory(params.dungeon.type());
        MonsterFactory monsterFactory = new MonsterFactory(params.atlas, params.dungeon.type());

        Floor floor = params.dungeon.getFloor(params.depth);
        int base = Math.min(floor.roomCount, params.depth * 2);
        int monsters = floor.monsters;
        if (monsters == -1) {
            int a = (int) (base * Math.max(1, params.dungeon.getDifficulty() * params.depth / 50f));
            int b = (int) (base * Math.max(2, 2 * params.dungeon.getDifficulty() * params.depth / 50f));
            monsters = MathUtils.random(a, b);
            floor.monsters = monsters;
        }
        
        loader.progress = 10;
        
        Array<Entity> entities = new Array<Entity>();
        // add monsters to rooms
        // monster count is anywhere between 5-20 on easy and 25-100 on hard
        monsterFactory.makeMonsters(entities, monsters, itemFactory, floor);
        loader.progress = 75;

        // forcibly add some loot monsters
        monsterFactory.makeTreasure(entities, itemFactory, floor);

        loader.progress = 100;
        
        return new ImmutableArray<Entity>(entities);
    }
}
