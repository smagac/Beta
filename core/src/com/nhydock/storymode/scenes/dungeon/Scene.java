package com.nhydock.storymode.scenes.dungeon;

import github.nhydock.ssm.Inject;
import github.nhydock.ssm.ServiceManager;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.nhydock.gdx.scenes.scene2d.runnables.GotoScene;
import com.nhydock.storymode.DataDirs;
import com.nhydock.storymode.components.Equipment;
import com.nhydock.storymode.datatypes.FileType;
import com.nhydock.storymode.datatypes.dungeon.Dungeon;
import com.nhydock.storymode.datatypes.dungeon.DungeonLoader;
import com.nhydock.storymode.datatypes.dungeon.Floor;
import com.nhydock.storymode.datatypes.dungeon.FloorLoader;
import com.nhydock.storymode.datatypes.dungeon.Progress;
import com.nhydock.storymode.factories.DungeonFactory;
import com.nhydock.storymode.scenes.Messages;
import com.nhydock.storymode.scenes.UI;
import com.nhydock.storymode.scenes.dungeon.ui.Transition;
import com.nhydock.storymode.scenes.dungeon.ui.WanderUI;
import com.nhydock.storymode.service.implementations.DungeonManager;
import com.nhydock.storymode.service.implementations.PageFile;
import com.nhydock.storymode.service.implementations.PageFile.NumberValues;
import com.nhydock.storymode.service.implementations.PageFile.StringValues;
import com.nhydock.storymode.service.interfaces.IDungeonContainer;
import com.nhydock.storymode.service.interfaces.IPlayerContainer;
import com.nhydock.storymode.service.interfaces.ISharedResources;
import com.nhydock.storymode.util.dungeon.TsxTileSet;

public class Scene extends com.nhydock.storymode.scenes.Scene<UI> implements Telegraph {

    @Inject
    public IPlayerContainer playerService;
    
    @Inject
    public ISharedResources shared;
    
    public IDungeonContainer dungeonService;
    
    @Inject
    public PageFile tracker;

    private boolean descending;

    private Dungeon.Parameters params;
    
    private Entity player;

    WanderUI wanderUI;
    Transition transition;

    TiledMapTileSet tileset;
    
    public Scene() {
        super();
        
        manager = new AssetManager();

        player = playerService.getPlayer();
    }

    public void setDungeon(Dungeon.Parameters params, FileHandle file) {
        this.params = params;

        tracker.increment(NumberValues.Files_Explored);
        tracker.increment(StringValues.Favourite_File_Type, params.getType().name());

        if (file != null) {
            tracker.set(NumberValues.Largest_File, (int) Math.max(tracker.get(NumberValues.Largest_File), file.length() / 1000f));
        }
    }

    @Override
    public void extend(float delta) {
        if (!dungeonService.isLoading())
        {
            dungeonService.getEngine().update(delta);
        }
        
        ui.draw();
    }

    private void prepareMap() {
        TiledMap map = dungeonService.getDungeon().build(tileset);
        
        RenderSystem rs = dungeonService.getEngine().getSystem(RenderSystem.class);

        rs.setView(wanderUI, shared.getResource(DataDirs.Home + "uiskin.json", Skin.class));
        rs.setMap(map);
        
        input.addProcessor(wanderUI);
        input.addProcessor(rs.getStage());
        
        dungeonService.getProgress().deepest = dungeonService.getDungeon().getDeepestTraversal();
    }

    @Override
    public void show() {
        manager = new AssetManager();
        
        dungeonService = ServiceManager.getService(IDungeonContainer.class);
        if (dungeonService == null) {
            dungeonService = new DungeonManager();
            ServiceManager.register(IDungeonContainer.class, dungeonService);
            
            //make sure equipment is reset whenever you enter a dungeon
            Equipment.Map.get(player).reset();
        }
        
        wanderUI = new WanderUI(this, manager);
        transition = new Transition(this, manager);

        loaded = false;
        
        MessageManager.getInstance().addListener(this, Messages.Dungeon.FIGHT);
        MessageManager.getInstance().addListener(this, Messages.Dungeon.KILLED);        
        MessageManager.getInstance().addListener(this, Messages.Dungeon.Ascend);
        MessageManager.getInstance().addListener(this, Messages.Dungeon.Descend);
        MessageManager.getInstance().addListener(this, Messages.Dungeon.Dead);
        MessageManager.getInstance().addListener(this, Messages.Dungeon.Leave);
        MessageManager.getInstance().addListener(this, Messages.Dungeon.Exit);
        MessageManager.getInstance().addListener(this, Messages.Dungeon.Warp);
        
        ui = wanderUI;
        //input.addProcessor(wanderUI);
    }

    @Override
    public void dispose() {
        audio.clearBgm();
        
        MessageManager.getInstance().removeListener(this, Messages.Dungeon.FIGHT);
        MessageManager.getInstance().removeListener(this, Messages.Dungeon.KILLED);
        MessageManager.getInstance().removeListener(this, Messages.Dungeon.Ascend);
        MessageManager.getInstance().removeListener(this, Messages.Dungeon.Descend);
        MessageManager.getInstance().removeListener(this, Messages.Dungeon.Dead);
        MessageManager.getInstance().removeListener(this, Messages.Dungeon.Leave);
        MessageManager.getInstance().removeListener(this, Messages.Dungeon.Exit);
        MessageManager.getInstance().removeListener(this, Messages.Dungeon.Warp);
        
        wanderUI.dispose();
        transition.dispose();
        ui = null;
        super.dispose();
    }
    
    public void setFloor(final int depth) {
        final Progress p = dungeonService.getProgress();
        final Dungeon d = dungeonService.getDungeon();
        final Engine e = dungeonService.getEngine();
        
        if (!p.hasPrevFloor(depth)){
            MessageManager.getInstance().dispatchMessage(null, Messages.Dungeon.Exit);
            return;
        } else if (!p.hasNextFloor(depth)) {
            if (MathUtils.randomBoolean(depth / 30f)) {
                MessageManager.getInstance().dispatchMessage(null, Messages.Dungeon.Proceed);
            } else {
                MessageManager.getInstance().dispatchMessage(null, Messages.Dungeon.Exit);    
            }
            return;
        }
        
        if (depth > 1 && depth != p.depth && p.depth > 0) {
            // prevent more monsters from respawning after clearing a floor so
            // then you can't just keep grinding on lower levels in a single dungeon run
            Floor floor = dungeonService.getDungeon().getFloor(p.depth);
            floor.monsters = dungeonService.getProgress().monstersTotal - dungeonService.getProgress().monstersKilled;
            floor.loot = floor.loot - dungeonService.getProgress().lootFound;
        }
        descending = (depth > p.depth);
        if (depth > p.deepest){
            p.deepest = depth;
            params.change();
        }
        
        wanderUI.fadeOut(new Runnable(){

            @Override
            public void run() {
                
                FloorLoader.Parameters param = new FloorLoader.Parameters();
                param.depth = depth;
                param.onLoad = new Runnable(){

                    @Override
                    public void run() {
                        wanderUI.addEventMessage("You move onto floor " + depth + " of " + d.size());
                        
                        MessageManager.getInstance().dispatchMessage(null, Messages.Dungeon.Refresh, dungeonService.getProgress());
                        
                        e.getSystem(MovementSystem.class).setMap(d.getFloor(depth));
                        e.getSystem(RenderSystem.class).setFloor(depth);
                        
                        MovementSystem ms = e.getSystem(MovementSystem.class);
                        if (descending) {
                            ms.moveToStart(player);
                        }
                        else {
                            ms.moveToEnd(player);
                        }
                        e.addEntity(player);
                        
                        wanderUI.fadeIn();
                    }
                    
                };

                dungeonService.loadFloor(param);
            }
        });

    }
    
    protected void dead() {
        // lose all found items
        playerService.getInventory().abandon();

        tracker.increment(NumberValues.Times_Died);
        audio.playSfx(DataDirs.Sounds.dead);
    }

    protected void leave() {
        // merge loot into inventory
        playerService.getInventory().merge();
    }
    
    @Override
    public void resize(int width, int height) {
        wanderUI.resize(width, height);
    }
    
    /**
     * Sets the audio for the dungeon once everything's been loaded
     */
    private void setAudio() {

        Dungeon d = dungeonService.getDungeon();
        if (d.getType() == FileType.Audio && d.getFilename() != null) {
            FileHandle src = d.getSrc();
            if (src.extension().matches("(mp3|ogg|wav)")) {
                try {
                    Music music = Gdx.audio.newMusic(src);
                    audio.setBgm(music);
                } 
                //in case the music can't actually be opened
                catch (GdxRuntimeException e) {
                    Gdx.app.log("Music", e.getMessage());
                    Array<String> files = DataDirs.getChildren(Gdx.files.internal(DataDirs.Audio + "dungeon/"));
                    String bgm = files.random();
                    audio.setBgm(Gdx.audio.newMusic(Gdx.files.internal(bgm)));
                }
            }
        } else { 
            Array<String> files = DataDirs.getChildren(Gdx.files.internal(DataDirs.Audio + "dungeon/"));
            String bgm = files.random();
            audio.setBgm(Gdx.audio.newMusic(Gdx.files.internal(bgm)));
        }
        audio.fadeIn();
        audio.playBgm();
    }

    @Override
    protected void init() {
        ui = wanderUI;
        
        if (dungeonService.getDungeon() != null){
            params = dungeonService.getParams();
            tileset = dungeonService.getDungeon().getTileset();
            
            RenderSystem rs = dungeonService.getEngine().getSystem(RenderSystem.class);
            input.addProcessor(rs.getStage());
            input.addProcessor(wanderUI);
            
            wanderUI.init();
            MessageManager.getInstance().dispatchMessage(null, Messages.Dungeon.Refresh, dungeonService.getProgress());
            
            setAudio();
            
            setFloor(dungeonService.getProgress().depth);
        }
        else {
            tileset = new TsxTileSet(Gdx.files.internal(DataDirs.Tilesets + params.getTileset() + ".tsx"), shared.getAssetManager());

            DungeonLoader.Parameters param = new DungeonLoader.Parameters();
            param.params = this.params;
            
            param.onLoad = new Runnable() {

                @Override
                public void run() {
                    prepareMap();

                    wanderUI.init();
                    
                    setAudio();
                }
                
            };
            dungeonService.loadDungeon(param);  
        }
        
        Gdx.input.setInputProcessor(input);
    }

    @Override
    public boolean handleMessage(final Telegram msg) {
        if (msg.message == Messages.Dungeon.FIGHT) {
            final Entity boss = (Entity)msg.extraInfo;
            final com.nhydock.storymode.scenes.battle.Scene scene = new com.nhydock.storymode.scenes.battle.Scene(boss);
            
            ui.draw();
            transition.init();
            transition.playAnimation(new GotoScene(scene));
            ui = transition;
            
            return true;
        }
        if (msg.message == Messages.Dungeon.Dead && msg.extraInfo == playerService.getPlayer()) {
            dead();
            dungeonService.getDungeon().setDeepestTraversal(dungeonService.getProgress().deepest);
            DungeonFactory.writeCacheFile(params, dungeonService.getDungeon());
            return true;
        }
        if (msg.message == Messages.Dungeon.Exit) {
            leave();
            dungeonService.getDungeon().setDeepestTraversal(dungeonService.getProgress().deepest);
            DungeonFactory.writeCacheFile(params, dungeonService.getDungeon());
            return true;
        }
        if (msg.message == Messages.Dungeon.Proceed) {
            input.disable();
            leave();
            wanderUI.addAction(
                Actions.sequence(
                    Actions.run(new GotoScene("lore"))
                )
            );
            return true;
        }
        if (msg.message == Messages.Dungeon.Descend) {
            setFloor(dungeonService.getProgress().nextFloor());
            return true;
        }
        if (msg.message == Messages.Dungeon.Ascend){
            setFloor(dungeonService.getProgress().prevFloor());
            return true;
        }
        if (msg.message == Messages.Dungeon.Warp){
            setFloor((int)msg.extraInfo);
            return true;
        }
        return false;
    }

}
