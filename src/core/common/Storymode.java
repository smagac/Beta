package core.common;


import scenes.town.TownUI;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import core.DLC;
import core.Palette;
import core.service.interfaces.IColorMode;
import core.service.interfaces.IGame;
import core.service.interfaces.ILoader;
import core.service.interfaces.IPlayerContainer;
import core.service.interfaces.IQuestContainer;
import factories.AllFactories;
import github.nhydock.ssm.SceneManager;
import github.nhydock.ssm.ServiceManager;

public class Storymode extends com.badlogic.gdx.Game implements IColorMode, IGame, ILoader {
	
	public static final int[] InternalRes = {960, 540};
	
	private boolean resumed;

	//COOL RENDERING
	private Palette currentHue = Palette.Original;
	private float contrast = .5f;
	private ShaderProgram hueify;
	
	private Screen queued;
	private boolean invert;
	
	private BossListener boss;
	
	//loading screen data
	private SpriteBatch loadingBatch;
	private BitmapFont loadingFont;
	private String loadingMessage;
	private boolean loading;
	private Viewport standardViewport;
	
	private Texture fill;
	
	private IPlayerContainer playerManager;
	private IQuestContainer questTracker;
	
	protected Storymode(){}
	
	@Override
	public void resize(int width, int height) {
		hueify.begin();
		hueify.setUniformf("u_resolution", width, height);
		hueify.end();
		standardViewport.update(width, height, true);
		super.resize(width, height);
	}
	
	@Override
	public void create() {
		boss = new BossListener(this, this);
		hueify = new ShaderProgram(Gdx.files.classpath("core/util/bg.vertex.glsl"), Gdx.files.classpath("core/util/bg.fragment.glsl"));
		if (!hueify.isCompiled()){
			(new GdxRuntimeException(hueify.getLog())).printStackTrace();
			System.exit(-1);
		}
		
		//setup all factory resources
		DLC.init();
		AllFactories.prepare();
		
		SceneManager.setGame(this);
		
		ServiceManager.register(ILoader.class, this);
		ServiceManager.register(IGame.class, this);
		ServiceManager.register(IColorMode.class, this);
		
		playerManager = new PlayerManager();
		questTracker = new QuestTracker();
		ServiceManager.register(IPlayerContainer.class, playerManager);
		ServiceManager.register(IQuestContainer.class, questTracker);
		
		SceneManager.register("town", scenes.town.Scene.class);
		SceneManager.register("dungeon", scenes.dungeon.Scene.class);
		SceneManager.register("title", scenes.title.Scene.class);
		SceneManager.register("newgame", scenes.newgame.Scene.class);
		SceneManager.register("endgame", scenes.endgame.Scene.class);
		
		
		SceneManager.switchToScene("title");
		
		loadingBatch = new SpriteBatch();
		standardViewport = new ScalingViewport(Scaling.fit, InternalRes[0], InternalRes[1]);
		
		loadingFont = new BitmapFont(Gdx.files.internal("data/loading.fnt"));
		fill = new Texture(Gdx.files.internal("data/fill.png"));
		setLoadingMessage(null);
	}

	@Override
	public void startGame(int difficulty, boolean gender) {
		playerManager.init(difficulty, gender);
		
		questTracker.prepare();
		Tracker.reset();
		TownUI.clearHistory();
	}
	
	/**
	 * Reset back to the title
	 */
	@Override
	public void softReset()
	{
		playerManager = new PlayerManager();
		questTracker = new QuestTracker();
		ServiceManager.register(IPlayerContainer.class, playerManager);
		ServiceManager.register(IQuestContainer.class, questTracker);
		
		SceneManager.switchToScene("title");
	}
	
	/**
	 * Skip all the title sequence and story and just jump into a normal difficulty game
	 */
	@Override
	public void fastStart()
	{
		playerManager = new PlayerManager();
		questTracker = new QuestTracker();
		ServiceManager.register(IPlayerContainer.class, playerManager);
		ServiceManager.register(IQuestContainer.class, questTracker);
		
		startGame(3, true);
		SceneManager.switchToScene("town");
	}
	
	@Override
	public void render()
	{
		//wait until a cycle is over before we acceptably switch screens
		// this way we can call switches from the UI at any point
		if (queued != null)
		{
			Screen old = super.getScreen();
			super.setScreen(queued);
			queued = null;
			if (old != null)
			{
				ServiceManager.unhook(old);
				System.gc();
			}
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
		hueify.setUniformi("vignette", p.vignette?1:0);
		hueify.end();
		
		
		float delta = Gdx.graphics.getDeltaTime();
		//ignore pause time in getting time played
		if (resumed)
		{
			delta = 0;
			resumed = false;
		}
		
		playerManager.updateTime(delta);
		this.getScreen().render(delta);
		
		if (loading)
		{
			Gdx.gl.glClearColor(clear.r, clear.g, clear.b, 1f);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			
			//draw load screen
			loadingBatch.setShader(hueify);
			loadingBatch.setProjectionMatrix(standardViewport.getCamera().combined);
			loadingBatch.begin();
			loadingBatch.setColor(clear);
			loadingBatch.draw(fill, 0, 0, InternalRes[0], InternalRes[1]);
			loadingBatch.setColor(Color.WHITE);
			loadingFont.draw(loadingBatch, loadingMessage, InternalRes[0]/2-loadingFont.getBounds(loadingMessage).width/2, 35f);
			loadingBatch.end();
		}
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
	
	@Override
	public Palette getPalette()
	{
		return currentHue;
	}
	
	@Override
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
	
	@Override
	public void invert() {
		invert = !invert;
	}
	
	@Override
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

	@Override
	public void setLoading(boolean loading) {
		this.loading = loading;
	}

	@Override
	public boolean isLoading() {
		return loading;
	}
	
	@Override
	public void setLoadingMessage(String message)
	{
		if (message == null || message.trim().length() == 0)
			message = "Loading...";
		this.loadingMessage = message;
	}

	@Override
	public void endGame() {
		if (playerManager.isPrepared())
		{
			SceneManager.switchToScene("endgame");
			System.out.println("cheater");
		}
	}

}
