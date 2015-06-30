package scenes.dungeon;

import github.nhydock.ssm.Inject;
import github.nhydock.ssm.ServiceManager;
import scene2d.runnables.GotoScene;
import scenes.Messages;
import scenes.UI;
import scenes.dungeon.ui.Transition;
import scenes.dungeon.ui.WanderUI;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
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

import core.DataDirs;
import core.components.Equipment;
import core.datatypes.FileType;
import core.datatypes.dungeon.Dungeon;
import core.datatypes.dungeon.DungeonLoader.Parameters;
import core.datatypes.dungeon.Floor;
import core.datatypes.dungeon.FloorLoader.FloorParam;
import core.datatypes.dungeon.Progress;
import core.factories.DungeonFactory;
import core.service.implementations.DungeonManager;
import core.service.implementations.PageFile;
import core.service.implementations.PageFile.NumberValues;
import core.service.implementations.PageFile.StringValues;
import core.service.interfaces.IDungeonContainer;
import core.service.interfaces.IPlayerContainer;
import core.service.interfaces.ISharedResources;
import core.util.dungeon.TsxTileSet;

public class Scene extends scenes.Scene<UI> implements Telegraph {

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
        
        MessageDispatcher.getInstance().addListener(this, Messages.Dungeon.FIGHT);
        MessageDispatcher.getInstance().addListener(this, Messages.Dungeon.KILLED);        
        MessageDispatcher.getInstance().addListener(this, Messages.Dungeon.Ascend);
        MessageDispatcher.getInstance().addListener(this, Messages.Dungeon.Descend);
        MessageDispatcher.getInstance().addListener(this, Messages.Dungeon.Dead);
        MessageDispatcher.getInstance().addListener(this, Messages.Dungeon.Leave);
        MessageDispatcher.getInstance().addListener(this, Messages.Dungeon.Exit);
        MessageDispatcher.getInstance().addListener(this, Messages.Dungeon.Warp);
        
        ui = wanderUI;
        //input.addProcessor(wanderUI);
    }

    @Override
    public void dispose() {
        audio.clearBgm();
        
        MessageDispatcher.getInstance().removeListener(this, Messages.Dungeon.FIGHT);
        MessageDispatcher.getInstance().removeListener(this, Messages.Dungeon.KILLED);
        MessageDispatcher.getInstance().removeListener(this, Messages.Dungeon.Ascend);
        MessageDispatcher.getInstance().removeListener(this, Messages.Dungeon.Descend);
        MessageDispatcher.getInstance().removeListener(this, Messages.Dungeon.Dead);
        MessageDispatcher.getInstance().removeListener(this, Messages.Dungeon.Leave);
        MessageDispatcher.getInstance().removeListener(this, Messages.Dungeon.Exit);
        MessageDispatcher.getInstance().removeListener(this, Messages.Dungeon.Warp);
        
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
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Exit);
            return;
        } else if (!p.hasNextFloor(depth)) {
            if (MathUtils.randomBoolean(depth / 30f)) {
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Proceed);
            } else {
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Exit);    
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
                
                FloorParam param = new FloorParam();
                param.depth = depth;
                param.onLoad = new Runnable(){

                    @Override
                    public void run() {
                        wanderUI.addEventMessage("You move onto floor " + depth + " of " + d.size());
                        
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Refresh, dungeonService.getProgress());
                        
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
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Refresh, dungeonService.getProgress());
            
            setAudio();
            
            setFloor(dungeonService.getProgress().depth);
        }
        else {
            tileset = new TsxTileSet(Gdx.files.internal(DataDirs.Tilesets + params.getTileset() + ".tsx"), shared.getAssetManager());

            Parameters param = new Parameters();
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
            final scenes.battle.Scene scene = new scenes.battle.Scene(boss);
            
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
