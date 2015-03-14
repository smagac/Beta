package core.service.implementations;

import github.nhydock.ssm.Inject;
import github.nhydock.ssm.ServiceManager;
import scenes.dungeon.MovementSystem;
import scenes.dungeon.RenderSystem;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.EntityListener;
import com.badlogic.ashley.core.EntitySystem;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.assets.AssetLoaderParameters;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;

import core.components.Groups.Monster;
import core.components.Identifier;
import core.datatypes.dungeon.Dungeon;
import core.datatypes.dungeon.DungeonLoader;
import core.datatypes.dungeon.DungeonLoader.DungeonParam;
import core.datatypes.dungeon.FloorLoader;
import core.datatypes.dungeon.FloorLoader.FloorParam;
import core.datatypes.dungeon.Progress;
import core.service.interfaces.IDungeonContainer;
import core.service.interfaces.ILoader;

public class DungeonManager implements IDungeonContainer {

    @Inject public ILoader loader;
    
    Dungeon dungeon;
    Engine engine;

    Progress progress;

    Pool<Entity> entityPool = Pools.get(Entity.class, 200);
    AssetManager dungeonLoader;
    
    boolean dungeonLoaded;
    DungeonLoader dl;
    FloorLoader fl;
    
    public DungeonManager() {
        dl = new DungeonLoader(new InternalFileHandleResolver());
        fl = new FloorLoader(new InternalFileHandleResolver());
    }
    
    @Override
    public void setDungeon(Dungeon generatedDungeon) {
        engine.removeAllEntities();
        dungeon = generatedDungeon;
    }

    @Override
    public Engine getEngine() {
        return engine;
    }

    @Override
    public void loadDungeon(final DungeonParam params) {
        params.loadedCallback = new AssetLoaderParameters.LoadedCallback() {

            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            public void finishedLoading(AssetManager assetManager, String fileName, Class type) {
                dungeon = (Dungeon) dungeonLoader.get(fileName, type);
                progress = new Progress();
                progress.floors = dungeon.size();
                
                if (params.onLoad != null) {
                    params.onLoad.run();
                }

                dungeonLoader.unload(fileName);
                loader.setLoading(false);
            }
            
        };
        dungeonLoader.load("dungeon", Dungeon.class, params);
        
        dungeon = null;
    }
    
    @Override
    public void loadFloor(final FloorParam params) {
        params.floor = dungeon.getFloor(params.depth);
        params.type = dungeon.getType();
        params.difficulty = dungeon.getDifficulty();
        dungeonLoader.load("floor", ImmutableArray.class, params);
        
        params.loadedCallback = new AssetLoaderParameters.LoadedCallback() {

            @SuppressWarnings("rawtypes")
            @Override
            public void finishedLoading(AssetManager assetManager, String fileName, Class type) {
                progress.depth = params.depth;
                progress.monstersKilled = 0;
                progress.monstersTotal = 0;
                progress.lootTotal = 0;
                progress.lootFound = 0;
                
                // setup progress
                engine.removeAllEntities();
                
                @SuppressWarnings("unchecked")
                ImmutableArray<Entity> monsters = (ImmutableArray<Entity>) assetManager.get(fileName, type);
                for (Entity entity : monsters) {
                    Identifier id = entity.getComponent(Identifier.class);
                    if (id.getType().endsWith(Monster.Loot)) {
                        progress.lootTotal++;
                    }
                    else if 
                        (!id.getType().endsWith(Monster.Key) &&
                         !id.getType().endsWith(Monster.Door)){
                        progress.monstersTotal++;
                    }
                    engine.addEntity(entity);
                }
                
                if (params.onLoad != null) {
                    params.onLoad.run();
                }

                dungeonLoader.unload(fileName);
            }
        };
    }

    @Override
    public boolean isLoading() {
        if (!dungeonLoader.update()) {
            if (!dungeonLoader.isLoaded("dungeon") && dungeon == null) {
                loader.setLoading(true);
                loader.setLoadingMessage(String.format("Creating Dungeon...%d%%", (int)dl.getProgress()));
            }
            return true;
        } else {
            loader.setLoading(false);
            return false;
        }
    }

    @Override
    public Dungeon getDungeon() {
        return dungeon;
    }

    public void unload() {
        for (EntitySystem system : engine.getSystems()) { 
            engine.removeSystem(system);
            if (system instanceof EntityListener) {
                engine.removeEntityListener((EntityListener)system);
            }
        }
    }

    @Override
    public Progress getProgress() {
        return progress;
    }

    @Override
    public void onRegister() {
        dungeonLoader = new AssetManager();
        
        dungeonLoader = new AssetManager();
        dungeonLoader.setLoader(Dungeon.class, dl);
        dungeonLoader.setLoader(ImmutableArray.class, fl);
        
        engine = new Engine();
        progress = new Progress();

        MovementSystem ms = new MovementSystem();
        RenderSystem rs = new RenderSystem();

        ServiceManager.inject(ms);
        ServiceManager.inject(rs);        

        ms.setProcessing(false);
        rs.setProcessing(false);
        
        engine.addSystem(ms);
        engine.addSystem(rs);
        engine.addEntityListener(ms);
        engine.addEntityListener(rs);
    }

    @Override
    public void onUnregister() {
        dungeonLoader.dispose();
        
        engine.removeAllEntities();
        
        for (EntitySystem system : engine.getSystems()) { 
            engine.removeSystem(system);
            if (system instanceof EntityListener) {
                engine.removeEntityListener((EntityListener)system);
            }
        }
        
        dungeon = null;
        progress = null;
        engine = null;
    }

}
