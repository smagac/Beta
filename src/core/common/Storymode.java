package core.common;


import scenes.dungeon.MovementSystem;

import com.artemis.Entity;
import com.artemis.World;
import com.artemis.managers.TagManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;

import components.Position;
import components.Renderable;
import components.Stats;
import core.Palette;
import core.datatypes.FileType;
import core.datatypes.Inventory;
import core.datatypes.Item;
import core.service.IColorMode;
import core.service.IDungeonContainer;
import core.service.IGame;
import core.service.IPlayerContainer;
import factories.AdjectiveFactory;
import factories.DungeonFactory;
import factories.MonsterFactory;

public class Storymode extends com.badlogic.gdx.Game implements IColorMode, IGame, IPlayerContainer, IDungeonContainer {

	private Stats player;
	private float time;
	private Inventory inventory;
	private static final String timeFormat = "%03d:%02d:%02d";
	public static final int[] InternalRes = {960, 540};
	
	private boolean resumed;
	
	private boolean fullscreen;
	
	private Array<World> dungeon;
	
	//COOL RENDERING
	private Palette currentHue = Palette.Original;
	private float contrast = .5f;
	private ShaderProgram hueify;
	
	private Screen queued;
	private boolean invert;
	
	private BossListener boss;
	
	protected Storymode(){}
	
	@Override
	public void create() {
		boss = new BossListener(this, this);
		hueify = new ShaderProgram(Gdx.files.classpath("core/util/bg.vertex.glsl"), Gdx.files.classpath("core/util/bg.fragment.glsl"));
		if (!hueify.isCompiled()){
			(new GdxRuntimeException(hueify.getLog())).printStackTrace();
			System.exit(-1);
		}
		
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
		
		//test dungeon
		/*
		startGame(3);
		scenes.dungeon.Scene dungeon = (scenes.dungeon.Scene)SceneManager.create("dungeon");
		dungeon.setDungeon(FileType.Other, 5);
		SceneManager.switchToScene(dungeon);
		*/
	}

	public void startGame(int difficulty) {
		
		//make a player
		player = new Stats(10, 0, MathUtils.random(10), 5, 5, 5, 50);

		//make crafting requirements
		inventory = new Inventory(difficulty);
		
		//reset game clock
		time = 0f;
	}
	
	/**
	 * Reset back to the title
	 */
	public void softReset()
	{
		player = null;
		inventory = null;
		time = 0f;
		
		SceneManager.switchToScene("title");
	}
	
	/**
	 * Skip all the title sequence and story and just jump into a normal difficulty game
	 */
	public void fastStart()
	{
		startGame(3);
		SceneManager.switchToScene("town");
	}
	
	public void toggleFullscreen()
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
	
		Palette p = getPalette();
		Color clear = new Color(isInverted()?p.high:p.low);
		float contrast = getContrast();
		
		//Copy AMD formula on wikipedia for smooth step in GLSL so our background can be the same as the shader
	    // Scale, bias and saturate x to 0..1 range
	    contrast = MathUtils.clamp((contrast - .5f)/(1f - .5f), 0.0f, 1.0f); 
	    // Evaluate polynomial
	    contrast = contrast*contrast*(3 - 2*contrast);
		
		clear.lerp(isInverted()?p.low:p.high, contrast);
		
		//make sure our buffer is always cleared
		Gdx.gl.glClearColor(clear.r, clear.g, clear.b, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		

		//bind the attribute
		hueify.begin();
		hueify.setUniformf("contrast", getContrast());
		if (isInverted())
		{
			
			hueify.setUniformf("low", p.high);
			hueify.setUniformf("high", p.low);
		}
		else
		{
			hueify.setUniformf("low", p.low);
			hueify.setUniformf("high",p.high);
		}
		hueify.end();
		
		
		float delta = Gdx.graphics.getDeltaTime();
		//ignore pause time in getting time played
		if (resumed)
		{
			delta = 0;
			resumed = false;
		}
		time += delta;
		
		this.getScreen().render(delta);
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
	
	public Palette getPalette()
	{
		return currentHue;
	}
	
	public void setPalette(Palette p)
	{
		if (p.equals(currentHue))
		{
			invert();
		}
		else
		{
			currentHue = p;
			invert = false;
		}
	}

	@Override
	public float getContrast() {
		return (invert)?1f-contrast:contrast;
	}

	@Override
	public float brighten() {
		return contrast = Math.min(.9f, contrast + .1f);
	}

	@Override
	public float darken() {
		return contrast = Math.max(.1f, contrast - .1f);
	}
	
	public void invert() {
		invert = !invert;
	}
	
	public boolean isInverted() {
		return invert;
	}

	@Override
	public ShaderProgram getShader() {
		return hueify;
	}
	
	public BossListener getBossInput()
	{
		return boss;
	}
}
