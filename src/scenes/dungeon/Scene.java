package scenes.dungeon;

import com.artemis.World;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.ObjectMap;

import core.DataDirs;
import core.common.BossListener;
import core.common.Tracker;
import core.datatypes.FileType;
import core.datatypes.Item;

public class Scene extends scenes.Scene<WanderUI> {

	private int difficulty;
	private FileType fileType;
	private int currentFloor;
	
	ObjectMap<Item, Integer> loot;
	
	private String bgmName;
	private Music bgm;
	
	private InputMultiplexer input;
	
	public void setDungeon(FileType type, int difficulty)
	{
		fileType = type;
		this.difficulty = difficulty;
	}
	
	@Override
	public void extend(float delta) {
		World floor = getService().getDungeon().get(currentFloor);
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
		
		ui = new WanderUI(this, manager);
		
		loot = new ObjectMap<Item, Integer>();
		
		bgmName = String.format("data/audio/dungeon_%03d.mp3", MathUtils.random(1,2));
		manager.load(bgmName, Music.class);
		
		input = new InputMultiplexer();
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
		if (i < 0 || i >= getService().getDungeon().size)
		{
			leave();
			return;
		}
		
		//remove self from old floor
		World floor = getService().getDungeon().get(currentFloor);
		MovementSystem ms = floor.getSystem(MovementSystem.class);
		//ms.setScene(null);
		
		currentFloor = i;
		floor = getService().getDungeon().get(currentFloor);
		ui.setFloor(floor);
		ms = floor.getSystem(MovementSystem.class);
		ms.setScene(this);
		ms.hit = manager.get(DataDirs.hit, Sound.class);
		
		input.clear();
		input.addProcessor(ms);
		input.addProcessor(ui);
		input.addProcessor(new BossListener(getService()));
		
		Gdx.input.setInputProcessor(input);
	}
	
	protected void dead()
	{
		ui.dead();
		Tracker.NumberValues.Times_Died.increment();
		manager.get(DataDirs.dead, Sound.class).play();
		//remove input from stage
		input.clear();
		input.addProcessor(ui);
		input.addProcessor(new BossListener(getService()));
		
		Gdx.input.setInputProcessor(input);
	}
	
	protected void leave()
	{
		ui.leave();
		
		//merge loot into inventory
		getService().getInventory().merge(this.loot);
		
		//remove input from stage
		input.clear();
		input.addProcessor(ui);
		input.addProcessor(getService().getBossInput());
		Gdx.input.setInputProcessor(input);
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
		getService().newDungeon(manager, fileType, difficulty);
		changeFloor(0);
		bgm = manager.get(bgmName, Music.class);
		bgm.setLooping(true);
		bgm.play();
	}
}
