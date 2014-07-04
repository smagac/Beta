package scenes.dungeon;

import com.artemis.World;
import com.badlogic.gdx.Audio;
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
	private int currentFloor;
	
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
		World floor = dungeonService.getDungeon().get(currentFloor);
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
	
	public void changeFloor(int i)
	{
		if (i < 0 || i >= dungeonService.getDungeon().size)
		{
			leave();
			return;
		}
		
		//remove self from old floor
		World floor = dungeonService.getDungeon().get(currentFloor);
		MovementSystem ms = floor.getSystem(MovementSystem.class);
		//ms.setScene(null);
		
		input.removeProcessor(ms);
		
		currentFloor = i;
		floor = dungeonService.getDungeon().get(currentFloor);
		ui.setFloor(floor);
		ms = floor.getSystem(MovementSystem.class);
		ms.setScene(this);
		ms.hit = manager.get(DataDirs.hit, Sound.class);
		
		input.addProcessor(ms);
	}
	
	protected void dead()
	{
		ui.dead();
		Tracker.NumberValues.Times_Died.increment();
		manager.get(DataDirs.dead, Sound.class).play();
		((InputMultiplexer)Gdx.input.getInputProcessor()).addProcessor(ui);
	}
	
	protected void leave()
	{
		ui.leave();
		
		//merge loot into inventory
		playerService.getInventory().merge(this.loot);
		
		//remove input from stage
		((InputMultiplexer)Gdx.input.getInputProcessor()).addProcessor(ui);
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
		changeFloor(0);
		bgm = manager.get(bgmName, Music.class);
		bgm.setLooping(true);
		bgm.play();
		
		input.addProcessor(ui);
		input.addProcessor(BossListener.getInstance());
		
		Gdx.input.setInputProcessor(input);
	}
}
