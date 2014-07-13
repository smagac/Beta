package scenes.dungeon;

import com.artemis.World;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.AbsoluteFileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import core.DataDirs;
import core.common.Tracker;
import core.datatypes.Dungeon;
import core.datatypes.FileType;
import core.datatypes.Item;
import core.service.IDungeonContainer;
import core.service.IPlayerContainer;
import core.service.Inject;
import factories.DungeonFactory;
import factories.DungeonFactory.DungeonLoader;
import factories.DungeonFactory.DungeonLoader.DungeonParam;
import factories.DungeonFactory.FloorLoader;
import factories.DungeonFactory.FloorLoader.FloorParam;


public class Scene extends scenes.Scene<WanderUI> implements IDungeonContainer {

	private int difficulty;
	private FileType fileType;
	
	ObjectMap<Item, Integer> loot;
	
	@Inject public IPlayerContainer playerService;
	
	AssetManager dungeonManager;
	DungeonLoader dungeonLoader;
	boolean dungeonLoaded;
	boolean floorLoaded;
	private boolean descending;
	protected Sound hitSound;
	private FloorLoader floorLoader;
	
	//factory data
	private Array<Dungeon> dungeon;
	private int currentFloorNumber;
	private World currentFloor;
	
	public Scene()
	{
		super();
		dungeonManager = new AssetManager(new AbsoluteFileHandleResolver());
		dungeonLoader = new DungeonLoader(new InternalFileHandleResolver());
		floorLoader = new FloorLoader(new InternalFileHandleResolver());
		dungeonManager.setLoader(Array.class, dungeonLoader);
		dungeonManager.setLoader(World.class, floorLoader);
	}
	
	public void setDungeon(FileType type, int difficulty)
	{
		fileType = type;
		this.difficulty = difficulty;
	}
	
	public void setDungeon(FileHandle file, int difficulty)
	{
		fileType = FileType.getType(file.extension());
		if (fileType == FileType.Audio)
		{
			if (file.extension().matches("(mp3|ogg|wav)"))
			{
				bgm = Gdx.audio.newMusic(file);
			}
		}
		this.difficulty = difficulty;
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
			World floor = getCurrentFloor();
			floor.setDelta(delta);
			floor.process();	
		}
		
		ui.act(delta);
		ui.draw();
	}
	
	@Override
	public void resize(int width, int height) {
		ui.resize(width, height);
	}

	@Override
	public void show() {
		manager.load("data/dungeon.atlas", TextureAtlas.class);
		manager.load("data/null.png", Texture.class);
		manager.load(DataDirs.hit, Sound.class);
		manager.load(DataDirs.dead, Sound.class);
		
		ui = new WanderUI(manager, playerService, this);
		
		loot = new ObjectMap<Item, Integer>();
		
		if (bgm == null)
		{
			bgm = Gdx.audio.newMusic(Gdx.files.internal(String.format("data/audio/dungeon_%03d.mp3", MathUtils.random(1,2))));
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
		DungeonFactory.dispose();
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
			input.removeProcessor(ui.wanderControls);
			
			FloorParam param = new FloorParam();
			param.atlas = manager.get("data/dungeon.atlas", TextureAtlas.class);
			param.dungeonContainer = this;
			param.depth = prevFloor();
			param.floor = getFloor(param.depth);
			param.player = playerService.getPlayer();
			
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
			input.removeProcessor(ui.wanderControls);
			
			FloorParam param = new FloorParam();
			param.atlas = manager.get("data/dungeon.atlas", TextureAtlas.class);
			param.dungeonContainer = this;
			param.depth = nextFloor();
			param.floor = getFloor(param.depth);
			param.player = playerService.getPlayer();
			
			dungeonManager.load("floor", World.class, param);
			descending = true;
			floorLoaded = false;
		}
	}
	
	private void changeFloor()
	{
		int depth = getCurrentFloorNumber();
		
		World floor = dungeonManager.get("floor", World.class);
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
		
		log("You move onto floor " + depth + " of " + dungeon.size) ;

		input.addProcessor(ui);
		input.addProcessor(ui.wanderControls);
		setCurrentFloor(depth, floor);
		
		dungeonManager.unload("floor");
		resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}
	
	protected void dead()
	{
		World floor = getCurrentFloor();
		MovementSystem ms = floor.getSystem(MovementSystem.class);
		ms.inputEnabled(false);
		ui.dead();
		
		Tracker.NumberValues.Times_Died.increment();
		manager.get(DataDirs.dead, Sound.class).play();
	}
	
	protected void leave()
	{
		World floor = getCurrentFloor();
		MovementSystem ms = floor.getSystem(MovementSystem.class);
		ms.inputEnabled(false);
		ui.leave();
		
		//merge loot into inventory
		playerService.getInventory().merge(this.loot);
	}
	
	/**
	 * Add an item after a monster has dropped it
	 * @param item
	 */
	protected void getItem(Item item)
	{
		loot.put(item, loot.get(item, 0) + 1);
		ui.setMessage("Obtained " + item.fullname());
	}
	
	protected void log(String message)
	{
		ui.setMessage(message);
	}

	@Override
	protected void init() {
		ui.init();
		
		TextureAtlas atlas = manager.get("data/dungeon.atlas", TextureAtlas.class);
		DungeonFactory.prepareFactory(atlas, atlas.findRegion(playerService.getGender()));
		
		DungeonParam param =  new DungeonParam();
		param.difficulty = difficulty;
		param.dungeonContainer = this;
		param.type = fileType;
		
		hitSound = manager.get(DataDirs.hit, Sound.class);
		dungeonManager.load("dungeon", Array.class, param);
		
		
		Gdx.input.setInputProcessor(input);
	}
	
	@SuppressWarnings("unchecked")
	protected void initPostDungeon()
	{
		setDungeon((Array<Dungeon>)dungeonManager.get("dungeon", Array.class));
		descend();
	}

	@Override
	public void setDungeon(Array<Dungeon> floors)
	{
		this.dungeon = floors;
		//currentFloorNumber = floors.size-2;
		currentFloorNumber = 0;
		currentFloor = null;
	}

	@Override
	public Dungeon getFloor(int i) {
		return dungeon.get(i);
	}
	
	/**
	 * Allow setting/overriding the current world
	 * @param i
	 * @param world
	 */
	@Override
	public void setCurrentFloor(int i, World world)
	{
		if (currentFloor != null)
		{
			input.removeProcessor(currentFloor.getSystem(RenderSystem.class).getStage());
			currentFloor.getSystem(RenderSystem.class).dispose();
			currentFloor.getSystem(MovementSystem.class).setScene(null);
			currentFloor.getSystem(RenderSystem.class).setNull(null);
			currentFloor.process();
		}
		currentFloor = world;
		//make sure enemy list is populated at least once
		currentFloor.getSystem(MovementSystem.class).begin();
		currentFloor.getSystem(RenderSystem.class).setView(ui, ui.getSkin());
		currentFloor.getSystem(RenderSystem.class).setNull(manager.get("data/null.png", Texture.class));
		currentFloor.getSystem(RenderSystem.class).getStage().getBatch().setShader(color.getShader());;
		
		input.addProcessor(currentFloor.getSystem(RenderSystem.class).getStage());
		currentFloorNumber = i;
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
		return nextFloor() < dungeon.size;
	}

	@Override
	public int getCurrentFloorNumber() {
		return currentFloorNumber;
	}
}
