package core.common;

import scenes.SceneManager;
import GenericComponents.Stats;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.MathUtils;

import core.Inventory;
import core.datatypes.Item;
import factories.AdjectiveFactory;
import factories.MonsterFactory;

public class Storymode extends com.badlogic.gdx.Game {

	private Stats player;
	private float time;
	private Inventory inventory;
	private static final String timeFormat = "%03d:%02d:%02d";
	public static final int[] InternalRes = {960, 540};
	
	private boolean resumed;
	
	@Override
	public void create() {
		//setup all factory resources
		Item.init();
		MonsterFactory.init();
		AdjectiveFactory.init();
		
		startGame(3);
		
		SceneManager.setGame(this);
		SceneManager.register("town", scenes.town.Scene.class);
		SceneManager.register("dungeon", scenes.dungeon.Scene.class);
		SceneManager.register("title", scenes.title.Scene.class);
		SceneManager.switchToScene("title");
	}

	public void startGame(int difficulty) {
		//make a player
		player = new Stats(10, 0, MathUtils.random(10), MathUtils.random(10), MathUtils.random(10), MathUtils.random(10));
		player.hp = 5;
		//make crafting requirements
		inventory = new Inventory();
		
		time = 0f;
	}
	
	public String getTimeElapsed()
	{
		return String.format(timeFormat, (int)(time/3600f), (int)(time/60f)%60, (int)(time % 60));
	}
	
	@Override
	public void render()
	{
		//make sure our buffer is always cleared
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		//default render screen
		super.render();
		
		float delta = Gdx.graphics.getDeltaTime();
		//ignore pause time in getting time played
		if (resumed)
		{
			delta = 0;
			resumed = false;
		}
		time += delta;
	}
	
	public void resume()
	{
		super.resume();
		resumed = true;
	}
	
	/**
	 * Fully heal the player
	 */
	public void rest()
	{
		player.hp = player.maxhp;
		Tracker.NumberValues.TimesSlept.increment();
	}

	public Inventory getInventory()
	{
		return inventory;
	}
	
	public Stats getPlayer()
	{
		return player;
	}
}
