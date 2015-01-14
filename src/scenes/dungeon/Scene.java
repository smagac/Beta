package scenes.dungeon;

import scene2d.InputDisabler;
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
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

import core.DataDirs;
import core.common.Tracker;
import core.components.Groups;
import core.components.Position;
import core.components.Renderable;
import core.datatypes.dungeon.Dungeon;
import core.datatypes.dungeon.DungeonLoader.DungeonParam;
import core.datatypes.dungeon.Floor;
import core.datatypes.dungeon.DungeonParams;
import core.datatypes.dungeon.FloorLoader.FloorParam;
import core.datatypes.dungeon.Progress;
import core.datatypes.quests.Quest;
import core.datatypes.FileType;
import core.datatypes.Item;
import core.service.interfaces.IBattleContainer;
import core.service.interfaces.IDungeonContainer;
import core.service.interfaces.IPlayerContainer;
import core.service.interfaces.ISharedResources;
import core.util.dungeon.TsxTileSet;
import github.nhydock.ssm.Inject;
import github.nhydock.ssm.SceneManager;
import github.nhydock.ssm.ServiceManager;

public class Scene extends scenes.Scene<UI> implements Telegraph {

    @Inject
    public IPlayerContainer playerService;
    
    @Inject
    public ISharedResources shared;
    
    @Inject
    public IDungeonContainer dungeonService;
    
    @Inject
    public IBattleContainer battleService;

    private boolean descending;

    private DungeonParams params;
    
    private Entity player;

    WanderUI wanderUI;
    Transition transition;

    TiledMapTileSet tileset;
    
    /**
     * Identify that we're entering a boss battle, do not dispose of the dungeon
     */
    private static boolean fight;
    
    public Scene() {
        super();
        
        manager = new AssetManager();

        player = playerService.getPlayer();
        player.add(new Position(0, 0));
        
        MessageDispatcher.getInstance().addListener(this, Messages.Dungeon.FIGHT);
        MessageDispatcher.getInstance().addListener(this, Messages.Dungeon.KILLED);
    }

    public void setDungeon(DungeonParams params, FileHandle file) {
        this.params = params;

        Tracker.NumberValues.Files_Explored.increment();
        Tracker.StringValues.Favourite_File_Type.increment(params.getType().name());

        if (file != null) {
            Tracker.NumberValues.Largest_File.set((int) Math.max(Tracker.NumberValues.Largest_File.value(),
                    file.length() / 1000f));
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
        Engine floorEngine = dungeonService.getEngine();
        
        MovementSystem ms = new MovementSystem();
        RenderSystem rs = new RenderSystem();

        ServiceManager.inject(ms);
        ServiceManager.inject(rs);
        
        floorEngine.addSystem(ms);
        floorEngine.addSystem(rs);
        floorEngine.addEntityListener(ms);
        floorEngine.addEntityListener(rs);
        ms.setProcessing(false);
        rs.setProcessing(false);
        
        rs.setView(wanderUI, shared.getResource(DataDirs.Home + "uiskin.json", Skin.class));
        rs.setMap(map);
        ms.setScene(this);
        
        input.addProcessor(rs.getStage());
    }

    @Override
    public void show() {
        manager = new AssetManager();
        
        wanderUI = new WanderUI(manager);
        transition = new Transition(manager);

        loaded = false;
        
        ui = wanderUI;
        input.addProcessor(wanderUI);
    }

    @Override
    public void dispose() {
        if (!fight) {
            dungeonService.clear();
        }
        audio.clearBgm();
        
        MessageDispatcher.getInstance().removeListener(this, Messages.Dungeon.FIGHT);
        MessageDispatcher.getInstance().removeListener(this, Messages.Dungeon.KILLED);
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
            leave();
            return;
        } else if (!p.hasNextFloor(depth)) {
            leave();
            return;
        }
        
        if (depth > 1 && depth != p.depth) {
            // prevent more monsters from respawning after clearing a floor so
            // then you can't just keep grinding on lower levels in a single dungeon run
            Floor floor = dungeonService.getDungeon().getFloor(p.depth);
            floor.monsters = dungeonService.getProgress().monstersTotal - dungeonService.getProgress().monstersKilled;
            floor.loot = floor.loot - dungeonService.getProgress().lootFound;
        }
        descending = (depth > p.depth);
        
        InputDisabler.swap();
        wanderUI.fadeOut(new Runnable(){

            @Override
            public void run() {
                
                FloorParam param = new FloorParam();
                param.depth = depth;
                param.onLoad = new Runnable(){

                    @Override
                    public void run() {
                        InputDisabler.swap();
                        wanderUI.setMessage("You move onto floor " + depth + " of " + d.size());
                        
                        refresh();
                    
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
        MessageDispatcher.getInstance().dispatchMessage(0, null, ui, Messages.Dungeon.Dead);

        // lose all found items
        playerService.getInventory().abandon();

        Tracker.NumberValues.Times_Died.increment();
        audio.playSfx(DataDirs.Sounds.dead);
    }

    protected void leave() {
        MessageDispatcher.getInstance().dispatchMessage(0, null, ui, Messages.Dungeon.Exit);

        // merge loot into inventory
        playerService.getInventory().merge();
    }

    /**
     * Add an item after a monster has dropped it
     * 
     * @param item
     */
    protected void getItem(Item item) {
        playerService.getInventory().pickup(item);
        MessageDispatcher.getInstance().dispatchMessage(0, null, playerService.getQuestTracker(), Quest.Actions.Gather,
                item.type());
        wanderUI.setMessage("Obtained " + item.fullname());
    }
    
    /**
     * Refreshes the HUD at the top of the screen to display the proper current
     * progress of the dungeon
     */
    protected void refresh() {
        MessageDispatcher.getInstance().dispatchMessage(0, null, ui, Messages.Dungeon.Refresh, dungeonService.getProgress());
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
        wanderUI.init();
        ui = wanderUI;
        
        if (fight){
            fight = false;

            tileset = dungeonService.getDungeon().getTileset();
            
            RenderSystem rs = dungeonService.getEngine().getSystem(RenderSystem.class);
            input.addProcessor(rs.getStage());
            
            refresh();
            setAudio();
        }
        else {
            tileset = new TsxTileSet(Gdx.files.internal(DataDirs.Tilesets + params.getTileset() + ".tsx"), shared.getAssetManager());

            DungeonParam param = new DungeonParam();
            param.params = this.params;
            
            param.onLoad = new Runnable() {

                @Override
                public void run() {
                    prepareMap();

                    setFloor(1);
                    
                    setAudio();
                }
                
            };
            dungeonService.loadDungeon(param);  
        }
        
        Gdx.input.setInputProcessor(input);
        
    }

    /**
     * Send a message to the UI that the player has leveled up
     */
    public void levelUp() {
        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.LevelUp);
    }
    
    /**
     * Sends a text message to the currently active UI
     * @param msg
     */
    public void log(String msg) {
        wanderUI.setMessage(msg);
    }

    @Override
    public boolean handleMessage(Telegram msg) {
        if (msg.message == Messages.Dungeon.FIGHT) {
            fight = true;
            battleService.setBoss((Entity)msg.extraInfo);
            battleService.setPlayer(player);
            transition.init();
            transition.playAnimation(new Runnable(){

                @Override
                public void run() {
                    scenes.battle.Scene scene = (scenes.battle.Scene)SceneManager.switchToScene("battle");
                    scene.setEnvironment(tileset);
                }

            });
            ui = transition;
            Gdx.input.setInputProcessor(null);
            return true;
        }
        return false;
    }

}
