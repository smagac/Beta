package scenes.dungeon;

import scenes.UI;
import scenes.dungeon.ui.MenuMessage;
import scenes.dungeon.ui.Transition;
import scenes.dungeon.ui.WanderUI;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.Agent;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;

import core.DLC;
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
import core.factories.DungeonFactory;
import core.service.interfaces.IDungeonContainer;
import core.service.interfaces.IPlayerContainer;
import core.service.interfaces.ISharedResources;
import core.util.dungeon.TsxTileSet;
import github.nhydock.ssm.Inject;
import github.nhydock.ssm.ServiceManager;

public class Scene extends scenes.Scene<UI> implements Agent {

    @Inject
    public IPlayerContainer playerService;
    
    @Inject
    public ISharedResources shared;
    
    @Inject
    public IDungeonContainer dungeonService;

    private boolean descending;
    protected Sound hitSound;

    private DungeonParams params;
    
    private Entity player;
    
    StateMachine<Scene> statemachine;

    WanderUI wanderUI;
    Transition transition;

    private TiledMapTileSet tileset;
    
    public Scene() {
        super();
        
        manager = new AssetManager();

        player = new Entity();
        player.add(new Position(0, 0));
        player.add(new Groups.Player());
        player.add(playerService.getPlayer());
        
        statemachine = new DefaultStateMachine<Scene>(this, GameState.Wander);
        MessageDispatcher.getInstance().addListener(GameState.Messages.FIGHT, this);
        MessageDispatcher.getInstance().addListener(GameState.Messages.KILLED, this);
    }

    public void setDungeon(DungeonParams params, FileHandle file) {
        this.params = params;

        if (params.getType() == FileType.Audio && file != null) {
            if (file.extension().matches("(mp3|ogg|wav)")) {
                audio.setBgm(Gdx.audio.newMusic(file));
            }
        }

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
        rs.setNull(shared.getResource(DataDirs.Home + "null.png", Texture.class));
        rs.setMap(map);
        ms.setScene(this);
        
        input.addProcessor(rs.getStage());
    }

    @Override
    public void show() {
        manager = new AssetManager();
        
        wanderUI = new WanderUI(manager);
        transition = new Transition(manager);
        
        if (!audio.hasBgm()) {
            Array<FileHandle> bgms = new Array<FileHandle>();
            bgms.add(Gdx.files.internal(DataDirs.Audio + "dungeon/001.mp3"));
            bgms.add(Gdx.files.internal(DataDirs.Audio + "dungeon/002.mp3"));
            bgms.add(Gdx.files.internal(DataDirs.Audio + "dungeon/003.mp3"));
            bgms.add(Gdx.files.internal(DataDirs.Audio + "dungeon/004.mp3"));
            for (FileHandle f : DLC.getAll("audio/dungeon", Gdx.files.internal(DataDirs.Audio + "dungeon/"))) {
                if (!f.path().startsWith("data")) {
                    Gdx.app.log("DLC", "found more in " + f.path());
                    bgms.addAll(f.list("mp3"));
                    bgms.addAll(f.list("ogg"));
                }
            }
            audio.setBgm(Gdx.audio.newMusic(bgms.random()));
        }

        audio.playBgm();
        loaded = false;
        
        ui = wanderUI;
    }

    @Override
    public void dispose() {
        dungeonService.clear();
        audio.clearBgm();
        player.removeAll();
        
        MessageDispatcher.getInstance().removeListener(GameState.Messages.FIGHT, this);
        MessageDispatcher.getInstance().removeListener(GameState.Messages.KILLED, this);
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
        
        input.removeProcessor(wanderUI);
        wanderUI.fadeOut(new Runnable(){

            @Override
            public void run() {
                
                FloorParam param = new FloorParam();
                param.depth = depth;
                param.onLoad = new Runnable(){

                    @Override
                    public void run() {
                        input.addProcessor(wanderUI);
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
        MessageDispatcher.getInstance().dispatchMessage(0, null, ui, MenuMessage.Dead);

        // lose all found items
        playerService.getInventory().abandon();

        Tracker.NumberValues.Times_Died.increment();
        audio.playSfx(shared.getResource(DataDirs.dead, Sound.class));
    }

    protected void leave() {
        MessageDispatcher.getInstance().dispatchMessage(0, null, ui, MenuMessage.Exit);

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
        MessageDispatcher.getInstance().dispatchMessage(0, null, ui, MenuMessage.Refresh, dungeonService.getProgress());
    }
    
    @Override
    public void resize(int width, int height) {
        wanderUI.resize(width, height);
    }

    @Override
    protected void init() {
        TextureAtlas atlas = shared.getResource(DataDirs.Home + "dungeon.atlas", TextureAtlas.class);
        tileset = new TsxTileSet(params.getTileset(), shared.getAssetManager());
        Skin skin = shared.getResource(DataDirs.Home + "uiskin.json", Skin.class);
        skin.addRegions(atlas);
        
        wanderUI.init();
        transition.init();
        ui = wanderUI;
        hitSound = shared.getResource(DataDirs.hit, Sound.class);
        // ui.levelUp();

        Renderable r = new Renderable(playerService.getGender());
        r.loadImage(wanderUI.getSkin());
        player.add(r);
        
        DungeonParam param = new DungeonParam();
        param.params = this.params;
        
        if (dungeonService.getDungeon() != null)
        {
            param.generatedDungeon = dungeonService.getDungeon();
            param.onLoad = new Runnable() {

                @Override
                public void run() {
                    prepareMap();
                }
                
            };
        } else {
            param.onLoad = new Runnable() {

                @Override
                public void run() {
                    prepareMap();

                    setFloor(1);
                }
                
            };
        }
        dungeonService.loadDungeon(param);  

        Gdx.input.setInputProcessor(input);
    }

    /**
     * Send a message to the UI that the player has leveled up
     */
    public void levelUp() {
        MessageDispatcher.getInstance().dispatchMessage(0, null, ui, MenuMessage.LevelUp);
    }
    
    /**
     * Sets the active UI of the dungeon scene
     * @param ui
     */
    public void setUI(UI ui) {
        this.ui = ui;
    }
    
    /**
     * Sends a text message to the currently active UI
     * @param msg
     */
    public void log(String msg) {
        wanderUI.setMessage(msg);
    }
    

    @Override
    public void update(float delta) {
        statemachine.update();
    }

    @Override
    public boolean handleMessage(Telegram msg) {
        return statemachine.handleMessage(msg);
    }

}
