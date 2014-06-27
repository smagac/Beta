package core.common;

import scenes.SceneManager;
import scenes.dungeon.MovementSystem;

import com.artemis.Entity;
import com.artemis.World;
import com.artemis.managers.TagManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

import components.Position;
import components.Renderable;
import components.Stats;
import core.datatypes.FileType;
import core.datatypes.Inventory;
import core.datatypes.Item;
import factories.AdjectiveFactory;
import factories.DungeonFactory;
import factories.MonsterFactory;

public class Storymode extends com.badlogic.gdx.Game {

	private static Storymode instance;
	private Stats player;
	private float time;
	private Inventory inventory;
	private static final String timeFormat = "%03d:%02d:%02d";
	public static final int[] InternalRes = {960, 540};
	
	private boolean resumed;
	
	private boolean fullscreen;
	
	private Array<World> dungeon;
	
	//COOL RENDERING
	private static final Color[] hues = {Color.WHITE, Color.ORANGE, Color.GREEN, Color.CYAN, Color.MAGENTA, Color.MAROON, Color.YELLOW, Color.RED, Color.TEAL, Color.OLIVE};
	private static Color currentHue = hues[0];
	
	private Screen queued;
	
	@Override
	public void create() {
		currentHue = hues[0];
	    
		//setup all factory resources
		Item.init();
		MonsterFactory.init();
		AdjectiveFactory.init();
		
		SceneManager.setGame(this);
		SceneManager.register("town", scenes.town.Scene.class);
		SceneManager.register("dungeon", scenes.dungeon.Scene.class);
		SceneManager.register("title", scenes.title.Scene.class);
		SceneManager.register("newgame", scenes.newgame.Scene.class);
		
		
		SceneManager.switchToScene("title");
		
		
		instance = this;
		
		//test dungeon
		/*
		startGame(3);
		scenes.dungeon.Scene dungeon = (scenes.dungeon.Scene)SceneManager.create("dungeon");
		dungeon.setDungeon(FileType.Other, 5);
		SceneManager.switchToScene(dungeon);
		*/
	}

	public static void startGame(int difficulty) {
		
		//make a player
		instance.player = new Stats(10, 0, MathUtils.random(10), 5, 5, 5, 50);

		//make crafting requirements
		instance.inventory = new Inventory(difficulty);
		
		//reset game clock
		instance.time = 0f;
	}
	
	public String getTimeElapsed()
	{
		return String.format(timeFormat, (int)(time/3600f), (int)(time/60f)%60, (int)(time % 60));
	}
	
	@Override
	public void render()
	{
		//wait until a cycle is over before we acceptably switch screens
		// this way we can call switches from the UI at any point
		if (queued != null)
		{
			super.setScreen(queued);
			queued = null;
		}
		
		//make sure our buffer is always cleared
		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		
		float delta = Gdx.graphics.getDeltaTime();
		//ignore pause time in getting time played
		if (resumed)
		{
			delta = 0;
			resumed = false;
		}
		time += delta;
		
		this.getScreen().render(delta);
		
		//quick reset debug
		if (Gdx.input.isKeyPressed(Keys.F9))
		{
			SceneManager.switchToScene("title");
		}
		
		if ((Gdx.input.isKeyPressed(Keys.ALT_LEFT) || Gdx.input.isKeyPressed(Keys.ALT_RIGHT)) && Gdx.input.isKeyPressed(Keys.ENTER))
		{
			if (fullscreen)
			{
				Gdx.graphics.setDisplayMode(InternalRes[0], InternalRes[1], false);
				fullscreen = false;
			}
			else
			{
				DisplayMode dm = Gdx.graphics.getDesktopDisplayMode();
				Gdx.graphics.setDisplayMode(dm.width, dm.height, true);
				fullscreen = true;
			}	
		}
		
		//control hues
		if ((Gdx.input.isKeyPressed(Keys.NUM_0))) { currentHue = hues[0]; }
		if ((Gdx.input.isKeyPressed(Keys.NUM_1))) { currentHue = hues[1]; }
		if ((Gdx.input.isKeyPressed(Keys.NUM_2))) { currentHue = hues[2]; }
		if ((Gdx.input.isKeyPressed(Keys.NUM_3))) { currentHue = hues[3]; }
		if ((Gdx.input.isKeyPressed(Keys.NUM_4))) { currentHue = hues[4]; }
		if ((Gdx.input.isKeyPressed(Keys.NUM_5))) { currentHue = hues[5]; }
		if ((Gdx.input.isKeyPressed(Keys.NUM_6))) { currentHue = hues[6]; }
		if ((Gdx.input.isKeyPressed(Keys.NUM_7))) { currentHue = hues[7]; }
		if ((Gdx.input.isKeyPressed(Keys.NUM_8))) { currentHue = hues[8]; }
		if ((Gdx.input.isKeyPressed(Keys.NUM_9))) { currentHue = hues[9]; }
		
	}
	
	@Override
	public void setScreen(Screen screen)
	{
		queued = screen;
	}
	
	@Override
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
		Tracker.NumberValues.Times_Slept.increment();
	}

	public Inventory getInventory()
	{
		return inventory;
	}
	
	public Stats getPlayer()
	{
		return player;
	}
	
	public void newDungeon(AssetManager manager, FileType type, int difficulty)
	{
		TextureAtlas atlas = manager.get("data/dungeon.atlas", TextureAtlas.class);
		DungeonFactory factory = new DungeonFactory(atlas, type);
		dungeon = factory.create(difficulty);
		
		//add player into all floors
		for (World floor : dungeon)
		{
			//make player
			Entity e = floor.createEntity();
			e.addComponent(new Position(0,0));
			e.addComponent(player);	//shared stats reference
			e.addComponent(new Renderable(atlas.findRegion("character")));
			e.addToWorld();
			
			floor.getManager(TagManager.class).register("player", e);
			
			//put entity at start position on each floor
			floor.getSystem(MovementSystem.class).moveToStart(e);
		}
	}
	
	/**
	 * @return all the floors of our dungeon
	 */
	public Array<World> getDungeon()
	{
		return dungeon;
	}
	
	public static Color getPalette()
	{
		return currentHue;
	}
}
