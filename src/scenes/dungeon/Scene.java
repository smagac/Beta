package scenes.dungeon;

import com.artemis.Entity;
import com.artemis.World;
import com.artemis.managers.GroupManager;
import com.artemis.utils.ImmutableBag;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.AbsoluteFileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.utils.Array;

import core.DLC;
import core.DataDirs;
import core.common.Tracker;
import core.components.Identifier;
import core.components.Monster;
import core.datatypes.Dungeon;
import core.datatypes.Dungeon.Floor;
import core.datatypes.quests.Quest;
import core.datatypes.FileType;
import core.datatypes.Item;
import core.service.interfaces.IDungeonContainer;
import core.service.interfaces.IPlayerContainer;
import core.service.interfaces.IQuestContainer;
import factories.DungeonFactory;
import factories.DungeonFactory.DungeonLoader;
import factories.DungeonFactory.DungeonLoader.DungeonParam;
import factories.DungeonFactory.FloorLoader;
import factories.DungeonFactory.FloorLoader.FloorParam;
import github.nhydock.ssm.Inject;
import github.nhydock.ssm.ServiceManager;

public class Scene extends scenes.Scene<WanderUI> implements IDungeonContainer {

	private int difficulty;
	private FileType fileType;
	
	@Inject public IPlayerContainer playerService;
	@Inject public IQuestContainer questService;
	
	AssetManager dungeonManager;
	DungeonLoader dungeonLoader;
	boolean dungeonLoaded;
	boolean floorLoaded;
	private boolean descending;
	protected Sound hitSound;
	private FloorLoader floorLoader;
	
	//factory data
	private Dungeon dungeon;
	private int currentFloorNumber;
	private World currentFloor;
	private String fileName;
	
	protected Progress progress;
	
	public Scene()
	{
		super();
		dungeonManager = new AssetManager(new AbsoluteFileHandleResolver());
		dungeonLoader = new DungeonLoader(new InternalFileHandleResolver());
		floorLoader = new FloorLoader(new InternalFileHandleResolver());
		dungeonManager.setLoader(Dungeon.class, dungeonLoader);
		dungeonManager.setLoader(World.class, floorLoader);
		progress = new Progress();
		
		ServiceManager.register(IDungeonContainer.class, this);
	}
	
	public void setDungeon(FileType type, int difficulty)
	{
		fileType = type;
		this.difficulty = difficulty;
		this.fileName = null;
	}
	
	public void setDungeon(FileHandle file, int difficulty)
	{
		this.fileName = file.path();
		fileType = FileType.getType(file.extension());
		if (fileType == FileType.Audio)
		{
			if (file.extension().matches("(mp3|ogg|wav)"))
			{
				bgm = Gdx.audio.newMusic(file);
			}
		}
		this.difficulty = difficulty;
		
		Tracker.NumberValues.Files_Explored.increment();
		Tracker.StringValues.Favourite_File_Type.increment(fileType.name());
		Tracker.NumberValues.Largest_File.set((int)Math.max(Tracker.NumberValues.Largest_File.value(), file.length() / 1000f));
	}
	
	@Override
	public void extend(float delta) {
		if (!dungeonManager.update())
		{
			if (!dungeonLoaded)
			{
				loader.setLoading(true);
				loader.setLoadingMessage(String.format("Creating Dungeon...%d%%", dungeonLoader.getProgress()));
				return;
			}
			else if (!floorLoaded)
			{
				loader.setLoadingMessage("Populating Floor");
			}
		}
		else if (!dungeonLoaded)
		{
			loader.setLoading(false);
			initPostDungeon();
			dungeonLoaded = true;
			return;
		}
		else if (!floorLoaded)
		{
			changeFloor();
			floorLoaded = true;
		}
		else
		{
			if (getCurrentFloor() != null)
			{
				World floor = getCurrentFloor();
				floor.setDelta(delta);
				floor.process();
			}
		}
		
		ui.draw();
	}
	
	@Override
	public void show() {
		ui = new WanderUI(manager);
		
		if (bgm == null)
		{
			Array<FileHandle> bgms = new Array<FileHandle>();
			bgms.add(Gdx.files.internal("data/audio/dungeon/001.mp3"));
			bgms.add(Gdx.files.internal("data/audio/dungeon/002.mp3"));
			for (FileHandle f : DLC.getAll("audio/dungeon", Gdx.files.internal("data/audio/dungeon/")))
			{
				if (!f.path().startsWith("data"))
				{
					Gdx.app.log("DLC", "found more in " + f.path());
					bgms.addAll(f.list("mp3"));
					bgms.addAll(f.list("ogg"));
				}
			}
			bgm = Gdx.audio.newMusic(bgms.random());
		}
		
		bgm.setLooping(true);
		bgm.play();
		
	}

	@Override
	public void hide() {
		dispose();
	}

	@Override
	public void pause() { 
		if (bgm != null)
		{
			bgm.pause();
		}
	}

	@Override
	public void resume() { 
		if (bgm != null)
		{
			bgm.play();
		}
	}

	@Override
	public void dispose() {
		if (bgm != null)
		{
			bgm.stop();	
		}
		dungeonManager.dispose();
		ServiceManager.register(IDungeonContainer.class, null);
		super.dispose();
	}
	
	public void ascend()
	{
		if (!hasPrevFloor())
		{
			leave();
			return;
		}
		else
		{
			input.removeProcessor(ui);
			
			FloorParam param = new FloorParam();
			param.atlas = manager.get("data/dungeon.atlas", TextureAtlas.class);
			param.dungeonContainer = this;
			param.depth = prevFloor();
			param.dungeon = dungeon;
			param.player = playerService.getPlayer();
			param.character = param.atlas.findRegion(playerService.getGender());
			
			dungeonManager.load("floor", World.class, param);
			descending = false;
			floorLoaded = false;
		}
	}
	
	public void descend()
	{
		if (!hasNextFloor())
		{
			leave();
			return;
		}
		else
		{
			input.removeProcessor(ui);
			
			FloorParam param = new FloorParam();
			param.atlas = manager.get("data/dungeon.atlas", TextureAtlas.class);
			param.dungeonContainer = this;
			param.depth = nextFloor();
			param.dungeon = dungeon;
			param.player = playerService.getPlayer();
			param.character = param.atlas.findRegion(playerService.getGender());
			
			dungeonManager.load("floor", World.class, param);
			descending = true;
			floorLoaded = false;
		}
	}
	
	private void changeFloor()
	{
		int depth = getCurrentFloorNumber();
		
		final World floor = dungeonManager.get("floor", World.class);
		MovementSystem ms = floor.getSystem(MovementSystem.class);
		
		if (descending)
		{
			ms.moveToStart();
			depth++;
		}
		else
		{
			ms.moveToEnd();
			depth--;
		}
		ms.setScene(this);
		
		log("You move onto floor " + depth + " of " + dungeon.size()) ;

		final int d = depth;
		
		ui.fade(new Runnable(){

			@Override
			public void run() {
				input.addProcessor(ui);
				setCurrentFloor(d, floor);
				dungeonManager.unload("floor");
				resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			}
		});
	}
	
	protected void dead()
	{
		MessageDispatcher.getInstance().dispatchMessage(0, null, ui, WanderUI.MenuMessage.Dead);
		
		//lose all found items
		playerService.getInventory().abandon();
		
		Tracker.NumberValues.Times_Died.increment();
		manager.get(DataDirs.dead, Sound.class).play();
	}
	
	protected void leave()
	{
		MessageDispatcher.getInstance().dispatchMessage(0, null, ui, WanderUI.MenuMessage.Exit);
		
		//merge loot into inventory
		playerService.getInventory().merge();
	}
	
	/**
	 * Add an item after a monster has dropped it
	 * @param item
	 */
	protected void getItem(Item item)
	{
		playerService.getInventory().pickup(item);
		MessageDispatcher.getInstance().dispatchMessage(0, null, questService, Quest.Actions.Gather, item.type());
		ui.setMessage("Obtained " + item.fullname());
	}
	
	/**
	 * Adds a new message to the combat log in the bottom right corner of the screen
	 * @param message
	 */
	protected void log(String message)
	{
		ui.setMessage(message);
	}
	
	/**
	 * Refreshes the HUD at the top of the screen to display the proper current progress of the dungeon
	 */
	protected void refresh()
	{
		ui.refresh(progress);
	}

	@Override
	protected void init() {
		ui.init();
		//ui.levelUp();
		
		TextureAtlas atlas = manager.get("data/dungeon.atlas", TextureAtlas.class);
		TiledMapTileSet ts = DungeonFactory.buildTileSet(atlas);
		DungeonParam param =  new DungeonParam();
		param.tileset = ts;
		param.fileName = fileName;
		param.difficulty = difficulty;
		param.dungeonContainer = this;
		param.type = fileType;
		
		hitSound = manager.get(DataDirs.hit, Sound.class);
		dungeonManager.load("dungeon", Dungeon.class, param);

		Gdx.input.setInputProcessor(input);
	}
	
	protected void initPostDungeon()
	{
		setDungeon(dungeonManager.get("dungeon", Dungeon.class));
		descend();
	}

	@Override
	public void setDungeon(Dungeon floors)
	{
		this.dungeon = floors;
		//currentFloorNumber = floors.size()-1;
		currentFloorNumber = 0;
		currentFloor = null;
		progress.floors = dungeon.size();
	}

	@Override
	public Floor getFloor(int i) {
		return dungeon.getFloor(i);
	}
	
	/**
	 * Allow setting/overriding the current world
	 * @param depth
	 * @param world
	 */
	@Override
	public void setCurrentFloor(int depth, World world)
	{
		if (currentFloor != null)
		{
			input.removeProcessor(currentFloor.getSystem(RenderSystem.class).getStage());
			
			MovementSystem ms = currentFloor.getSystem(MovementSystem.class);
			//prevent more monsters from respawning after clearing a floor so then you can't
			// just keep grinding on lower levels in a single dungeon run
			if (ms.monsters != null)
			{
				Floor floor = dungeon.getFloor(currentFloorNumber);
				floor.monsters = progress.monstersTotal - progress.monstersKilled;
				floor.loot = floor.loot - progress.lootFound;
			}
			currentFloor.getSystem(RenderSystem.class).dispose();
			ms.dispose();
			
			for (int i = 0; i < currentFloor.getSystems().size(); i++)
			{
				ServiceManager.unhook(currentFloor.getSystems().get(i));
			}
		}
		currentFloor = world;
		for (int i = 0; i < currentFloor.getSystems().size(); i++)
		{
			ServiceManager.inject(currentFloor.getSystems().get(i));
		}
	
		//setup progress
		{
			progress.depth = depth;
			progress.monstersKilled = 0;
			progress.monstersTotal = 0;
			progress.lootTotal = 0;
			progress.lootFound = 0;
			
			ImmutableBag<Entity> monsters = currentFloor.getManager(GroupManager.class).getEntities("monsters");
			for (int i = 0; i < monsters.size(); i++)
			{
				Identifier id = monsters.get(i).getComponent(Identifier.class);
				if (id.toString().endsWith(Monster.Loot))
				{
					progress.lootTotal++;
				}
				else
				{
					progress.monstersTotal++;
				}
			}
			
			refresh();
		}
		
		//make sure enemy list is populated at least once
		currentFloor.getSystem(MovementSystem.class).begin();
		
		//ensure the render system is properly tied into the rendering of everything else
		currentFloor.getSystem(RenderSystem.class).setView(ui, ui.getSkin());
		currentFloor.getSystem(RenderSystem.class).setNull(manager.get("data/null.png", Texture.class));
		
		//hide processing but set up everything
		currentFloor.getSystem(RenderSystem.class).process(true);
		
		input.addProcessor(currentFloor.getSystem(RenderSystem.class).getStage());
		currentFloorNumber = depth;
	}

	@Override
	public World getCurrentFloor() {
		return currentFloor;
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
		return nextFloor() < dungeon.size();
	}

	@Override
	public int getCurrentFloorNumber() {
		return currentFloorNumber;
	}

	public void levelUp() {
		MessageDispatcher.getInstance().dispatchMessage(0, null, ui, WanderUI.MenuMessage.LevelUp);
	}
}
