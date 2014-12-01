package scenes.dungeon;

import scenes.UI;
import scenes.dungeon.ui.BattleUI;
import scenes.dungeon.ui.MenuMessage;
import scenes.dungeon.ui.Transition;
import scenes.dungeon.ui.WanderUI;

import com.badlogic.ashley.core.Engine;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.Agent;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.AbsoluteFileHandleResolver;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.utils.Array;

import core.DLC;
import core.DataDirs;
import core.common.Tracker;
import core.components.Groups;
import core.components.Identifier;
import core.components.Position;
import core.components.Renderable;
import core.components.Groups.*;
import core.datatypes.dungeon.Dungeon;
import core.datatypes.dungeon.Floor;
import core.datatypes.dungeon.DungeonParams;
import core.datatypes.quests.Quest;
import core.datatypes.FileType;
import core.datatypes.Item;
import core.factories.DungeonFactory;
import core.factories.FloorFactory;
import core.factories.DungeonFactory.DungeonLoader.DungeonParam;
import core.factories.FloorFactory.FloorLoader.FloorParam;
import core.service.interfaces.IDungeonContainer;
import core.service.interfaces.IPlayerContainer;
import github.nhydock.ssm.Inject;
import github.nhydock.ssm.ServiceManager;

public class Scene extends scenes.Scene<UI> implements IDungeonContainer, Agent {

    @Inject
    public IPlayerContainer playerService;

    AssetManager dungeonManager;
    boolean dungeonLoaded;
    boolean floorLoaded;
    private boolean descending;
    protected Sound hitSound;

    // factory data
    private Dungeon dungeon;
    private int currentFloorNumber;
    private Engine floorEngine;

    protected Progress progress;

    private DungeonParams params;
    
    private Entity player;
    
    StateMachine<Scene> statemachine;

    WanderUI wanderUI;
    BattleUI battleUI;
    Transition transition;
    
    public Scene() {
        super();
        progress = new Progress();
        
        manager = new AssetManager();
        floorEngine = new Engine();

        MovementSystem ms = new MovementSystem();
        RenderSystem rs = new RenderSystem();
        floorEngine.addSystem(ms);
        floorEngine.addSystem(rs);
        floorEngine.addEntityListener(ms);
        floorEngine.addEntityListener(rs);
        ms.setProcessing(false);
        rs.setProcessing(false);
        
        ServiceManager.inject(ms);
        ServiceManager.inject(rs);
        
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
        if (!dungeonManager.update()) {
            if (!dungeonLoaded) {
                loader.setLoading(true);
                loader.setLoadingMessage(String.format("Creating Dungeon...%d%%", DungeonFactory.dungeonLoader.getProgress()));
                return;
            }
            else if (!floorLoaded) {
                loader.setLoadingMessage("Populating Floor");
            }
        }
        else if (!dungeonLoaded) {
            loader.setLoading(false);
            initPostDungeon();
            dungeonLoaded = true;
            return;
        }
        else if (!floorLoaded) {
            changeFloor();
            floorLoaded = true;
        }
        else {
            if (getCurrentFloorNumber() > 0) {
                floorEngine.update(delta);
            }
        }

        ui.draw();
    }

    @Override
    public void show() {
        ServiceManager.register(IDungeonContainer.class, this);
        ServiceManager.inject(this);
        
        manager = new AssetManager();
        dungeonManager = new AssetManager(new AbsoluteFileHandleResolver());
        DungeonFactory.prepareManager(dungeonManager);
        FloorFactory.prepareManager(dungeonManager);
        
        wanderUI = new WanderUI(manager);
        battleUI = new BattleUI(manager);
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
    }

    @Override
    public void dispose() {
        floorEngine.removeAllEntities();
        player.removeAll();
        dungeonManager.dispose();
        ServiceManager.register(IDungeonContainer.class, null);
        MessageDispatcher.getInstance().removeListener(GameState.Messages.FIGHT, this);
        MessageDispatcher.getInstance().removeListener(GameState.Messages.KILLED, this);
        
        floorEngine.getSystem(RenderSystem.class).dispose();
        super.dispose();
    }
    
    public void setFloor(int i) {
        if (i == 0) {
            leave();
            return;
        } else if (i == dungeon.getDepth()) {
            leave();
            return;
        }
        
        if (i > 1) {
            // prevent more monsters from respawning after clearing a floor so
            // then you can't just keep grinding on lower levels in a single dungeon run
            Floor floor = dungeon.getFloor(i-1);
            floor.monsters = progress.monstersTotal - progress.monstersKilled;
            floor.loot = floor.loot - progress.lootFound;
        }
        descending = (i > currentFloorNumber);
        currentFloorNumber = i;
        
        FloorParam param = new FloorParam();
        param.atlas = manager.get("data/dungeon.atlas", TextureAtlas.class);
        param.dungeonContainer = this;
        param.depth = i;
        param.dungeon = dungeon;

        dungeonManager.load("floor", ImmutableArray.class, param);
        floorLoaded = false;
    }

    private void changeFloor() {
        int depth = getCurrentFloorNumber();

        wanderUI.setMessage("You move onto floor " + depth + " of " + dungeon.size());

        final int d = depth;

        wanderUI.fade(new Runnable() {

            @Override
            public void run() {
                input.addProcessor(ui);
                
                setCurrentFloor(d);
                
                dungeonManager.unload("floor");
                resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            }
        });
    }

    protected void dead() {
        MessageDispatcher.getInstance().dispatchMessage(0, null, ui, MenuMessage.Dead);

        // lose all found items
        playerService.getInventory().abandon();

        Tracker.NumberValues.Times_Died.increment();
        manager.get(DataDirs.dead, Sound.class).play();
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
        MessageDispatcher.getInstance().dispatchMessage(0, null, ui, MenuMessage.Refresh, progress);
    }
    
    @Override
    public void resize(int width, int height) {
        wanderUI.resize(width, height);
        battleUI.resize(width, height);
    }

    @Override
    protected void init() {
        wanderUI.init();
        transition.init();
        ui = wanderUI;
        hitSound = manager.get(DataDirs.hit, Sound.class);
        // ui.levelUp();

        player.add(new Renderable(playerService.getGender(), wanderUI.getSkin().getRegion(playerService.getGender())));
        floorEngine.getSystem(RenderSystem.class).setView(wanderUI, wanderUI.getSkin());
        floorEngine.getSystem(RenderSystem.class).setNull(manager.get(DataDirs.Home + "null.png", Texture.class));
        floorEngine.getSystem(MovementSystem.class).setScene(this);
        
        TextureAtlas atlas = manager.get(DataDirs.Home + "dungeon.atlas", TextureAtlas.class);
        TiledMapTileSet ts = DungeonFactory.buildTileSet(atlas);
        DungeonParam param = new DungeonParam();
        param.tileset = ts;
        param.params = this.params;
        param.dungeonContainer = this;
        if (dungeon != null)
        {
            param.generatedDungeon = dungeon;
        }

        dungeonManager.load("dungeon", Dungeon.class, param);  

        Gdx.input.setInputProcessor(input);
    }

    protected void initPostDungeon() {
        setDungeon(dungeonManager.get("dungeon", Dungeon.class));
        setFloor(1);
    }

    @Override
    public void setDungeon(Dungeon floors) {
        this.dungeon = floors;
        // currentFloorNumber = floors.size()-1;
        currentFloorNumber = 0;
        progress.floors = dungeon.size();
        
        this.floorEngine.getSystem(RenderSystem.class).setMap(floors);
    }

    @Override
    public Floor getFloor(int i) {
        return dungeon.getFloor(i);
    }

    /**
     * Allow setting/overriding the current world
     * 
     * @param depth
     * @param world
     */
    @Override
    public void setCurrentFloor(int depth) {

        // setup progress
        {
            progress.depth = depth;
            progress.monstersKilled = 0;
            progress.monstersTotal = 0;
            progress.lootTotal = 0;
            progress.lootFound = 0;

            ImmutableArray<Entity> monsters = floorEngine.getEntitiesFor(Groups.monsterType);
            for (Entity e : monsters) {
                Identifier id = e.getComponent(Identifier.class);
                if (id.getType().endsWith(Monster.Loot)) {
                    progress.lootTotal++;
                }
                else {
                    progress.monstersTotal++;
                }
            }

            refresh();
        }

        floorEngine.removeAllEntities();
        floorEngine.getSystem(MovementSystem.class).setMap(getFloor(currentFloorNumber));
        floorEngine.getSystem(RenderSystem.class).setFloor(currentFloorNumber);
        for (Entity e : (ImmutableArray<Entity>)dungeonManager.get("floor", ImmutableArray.class)) {
            floorEngine.addEntity(e);   
        }
        floorEngine.addEntity(player);
        
        MovementSystem ms = floorEngine.getSystem(MovementSystem.class);
        if (descending) {
            ms.moveToStart();
        }
        else {
            ms.moveToEnd();
        }
    }

    @Override
    public int nextFloor() {
        return currentFloorNumber + 1;
    }

    @Override
    public int prevFloor() {
        return currentFloorNumber - 1;
    }

    @Override
    public boolean hasPrevFloor() {
        return prevFloor() > 0;
    }

    @Override
    public boolean hasNextFloor() {
        return nextFloor() <= dungeon.size();
    }

    @Override
    public int getCurrentFloorNumber() {
        return currentFloorNumber;
    }

    public void levelUp() {
        MessageDispatcher.getInstance().dispatchMessage(0, null, ui, MenuMessage.LevelUp);
    }
    
    public void setUI(UI ui) {
        this.ui = ui;
    }

    @Override
    public Engine getCurrentFloor() {
        return floorEngine;
    }

    @Override
    public void update(float delta) {
        statemachine.update();
    }

    @Override
    public boolean handleMessage(Telegram msg) {
        return statemachine.handleMessage(msg);
    }

    public void log(String msg) {
        wanderUI.setMessage(msg);
    }
}
