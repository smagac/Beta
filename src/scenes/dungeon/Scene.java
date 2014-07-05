package scenes.dungeon;

import com.artemis.World;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ObjectMap;

import core.DataDirs;
import core.common.BossListener;
import core.common.Tracker;
import core.datatypes.FileType;
import core.datatypes.Item;
import core.service.IDungeonContainer;
import core.service.IPlayerContainer;
import core.service.Inject;

public class Scene extends scenes.Scene<WanderUI> {

	private int difficulty;
	private FileType fileType;
	
	ObjectMap<Item, Integer> loot;
	
	private String bgmName;
	private Music bgm;
	
	@Inject public IDungeonContainer dungeonService;
	@Inject public IPlayerContainer playerService;
	
	InputMultiplexer input;
	
	public void setDungeon(FileType type, int difficulty)
	{
		fileType = type;
		this.difficulty = difficulty;
		bgmName = null;
	}
	
	public void setDungeon(FileHandle file, int difficulty)
	{
		fileType = FileType.getType(file.extension());
		if (fileType == FileType.Audio)
		{
			if (file.extension().matches("(mp3|ogg|wav)"))
			{
				bgmName = file.file().getAbsolutePath();
			}
		}
		this.difficulty = difficulty;
	}
	
	@Override
	public void extend(float delta) {
		World floor = dungeonService.getCurrentFloor();
		floor.setDelta(delta);
		floor.process();
		
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
		manager.load(DataDirs.hit, Sound.class);
		manager.load(DataDirs.dead, Sound.class);
		
		ui = new WanderUI(this, manager, playerService, dungeonService);
		
		loot = new ObjectMap<Item, Integer>();
		
		if (bgmName == null)
		{
			bgmName = String.format("data/audio/dungeon_%03d.mp3", MathUtils.random(1,2));
		}
		manager.load(bgmName, Music.class);
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
		bgm.stop();
		bgm.dispose();
		manager.dispose();
		ui.dispose();
	}
	
	public void ascend()
	{
		//remove self from old floor
		World floor = dungeonService.getCurrentFloor();
		MovementSystem ms = floor.getSystem(MovementSystem.class);
		input.removeProcessor(ms);
		
		if (!dungeonService.hasPrevFloor())
		{
			leave();
			return;
		}
		else
		{
			dungeonService.prevFloor();
		}
		floor = dungeonService.getCurrentFloor();
		ms = floor.getSystem(MovementSystem.class);
		floor.getSystem(MovementSystem.class).moveToEnd();
		
		changeFloor();
	}
	
	public void descend()
	{
		//remove self from old floor
		World floor = dungeonService.getCurrentFloor();
		MovementSystem ms = floor.getSystem(MovementSystem.class);
		input.removeProcessor(ms);
		
		if (!dungeonService.hasNextFloor())
		{
			leave();
			return;
		}
		else
		{
			dungeonService.nextFloor();
		}
		
		floor = dungeonService.getCurrentFloor();
		ms = floor.getSystem(MovementSystem.class);
		floor.getSystem(MovementSystem.class).moveToStart();
		
		changeFloor();
	}
	
	private void changeFloor()
	{
		World floor = dungeonService.getCurrentFloor();
		MovementSystem ms = floor.getSystem(MovementSystem.class);
		ui.setFloor(floor);
		ms.setScene(this);
		ms.hit = manager.get(DataDirs.hit, Sound.class);
		
		input.addProcessor(ms);
		
		log("You move onto to floor " + dungeonService.getCurrentFloorNumber()) ;
	}
	
	protected void dead()
	{
		World floor = dungeonService.getCurrentFloor();
		MovementSystem ms = floor.getSystem(MovementSystem.class);
		ms.inputEnabled(false);
		ui.dead();
		
		Tracker.NumberValues.Times_Died.increment();
		manager.get(DataDirs.dead, Sound.class).play();
	}
	
	protected void leave()
	{
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
		input = new InputMultiplexer();
		
		ui.init();
		
		dungeonService.newDungeon(manager, fileType, difficulty);
		changeFloor();
		World floor = dungeonService.getCurrentFloor();
		floor.getSystem(MovementSystem.class).moveToStart();
		
		bgm = manager.get(bgmName, Music.class);
		bgm.setLooping(true);
		bgm.play();
		
		input.addProcessor(ui);
		input.addProcessor(BossListener.getInstance());
		
		Gdx.input.setInputProcessor(input);
	}
}
