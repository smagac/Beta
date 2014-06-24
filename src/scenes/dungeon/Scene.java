package scenes.dungeon;

import GenericSystems.MovementSystem;
import GenericSystems.RenderSystem;

import com.artemis.World;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import core.datatypes.FileType;
import core.datatypes.Item;

public class Scene extends scenes.Scene<WanderUI> {

	private boolean loaded;
	private int difficulty;
	private FileType fileType;
	private int currentFloor;
	
	ObjectMap<Item, Integer> loot;
	
	public void setDungeon(FileType type, int difficulty)
	{
		fileType = type;
		this.difficulty = difficulty;
	}
	
	@Override
	public void render(float delta) {
		if (!manager.update()){
			//TODO draw loading screen
			return;
		}
		//load create the ui once the manager is done loading
		if (!loaded)
		{
			ui.init();
			getService().newDungeon(manager, fileType, difficulty);
			changeFloor(0);
			loaded = true;
		}
		
		World floor = getService().getDungeon().get(currentFloor);
		floor.setDelta(delta);
		floor.process();
		
		ui.update(delta);
		ui.draw();
		
		floor.getSystem(RenderSystem.class).process();
	}

	@Override
	public void resize(int width, int height) {
		ui.resize(width, height);
	}

	@Override
	public void show() {
		manager = new AssetManager();
		
		manager.load("data/dungeon.atlas", TextureAtlas.class);
		ui = new WanderUI(this, manager);
		
		loot = new ObjectMap<Item, Integer>();
	}

	@Override
	public void hide() {
		// TODO Auto-generated method stub

	}

	@Override
	public void pause() { }

	@Override
	public void resume() { }

	@Override
	public void dispose() {	}
	
	public void changeFloor(int i)
	{
		currentFloor = i;
		World floor = getService().getDungeon().get(currentFloor);
		ui.setFloor(floor);
		Gdx.input.setInputProcessor(floor.getSystem(MovementSystem.class));
	}
	
	public void dead()
	{
		ui.dead();
		
		//remove input from stage
		InputMultiplexer input = new InputMultiplexer();
		ui.addToInput(input);
		Gdx.input.setInputProcessor(input);
	}
	
	public void leave()
	{
		ui.leave();
		
		//TODO merge loot into inventory
		
		//remove input from stage
		InputMultiplexer input = new InputMultiplexer();
		ui.addToInput(input);
		Gdx.input.setInputProcessor(input);
	}
	
	/**
	 * Add an item after a monster has dropped it
	 * @param item
	 */
	public void getItem(Item item)
	{
		loot.put(item, loot.get(item, 0) + 1);
		ui.setMessage("Obtained " + item.fullname());
	}
}
