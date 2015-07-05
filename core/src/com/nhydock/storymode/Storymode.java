package com.nhydock.storymode;

import github.nhydock.ssm.SceneManager;
import github.nhydock.ssm.ServiceManager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.math.MathUtils;
import com.nhydock.storymode.factories.AllFactories;
import com.nhydock.storymode.scenes.town.ui.FileBrowser;
import com.nhydock.storymode.service.implementations.AudioManager;
import com.nhydock.storymode.service.implementations.ColorManager;
import com.nhydock.storymode.service.implementations.LoadScreen;
import com.nhydock.storymode.service.implementations.PageFile;
import com.nhydock.storymode.service.implementations.PlayerManager;
import com.nhydock.storymode.service.implementations.SharedLoader;
import com.nhydock.storymode.service.interfaces.IAudioManager;
import com.nhydock.storymode.service.interfaces.IColorMode;
import com.nhydock.storymode.service.interfaces.IGame;
import com.nhydock.storymode.service.interfaces.ILoader;
import com.nhydock.storymode.service.interfaces.IPlayerContainer;
import com.nhydock.storymode.service.interfaces.ISharedResources;

public class Storymode extends com.badlogic.gdx.Game implements IGame {

    public static class StorymodePreferences implements GamePreferences {
        public boolean gender;
        public boolean hardcore;
        public int difficulty;
    }
    
    boolean debug = false;
    
    public static final int[] InternalRes = { 960, 540 };
    protected static float[] InternalVolume = {1.0f, 1.0f};
    
    private boolean resumed;
    
    private Screen queued;

    private BossListener boss;

    private IPlayerContainer playerManager;
    private AudioManager audioManager;
    private LoadScreen loadScreen;
    private ColorManager colorMode;
    private PageFile tracker;
    
    protected Storymode() {}
    
    private boolean started;

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        colorMode.resize(width, height);
    }

    @Override
    public void create() {
        // setup all factory resources
        DLC.init();
        AllFactories.prepare();

        SceneManager.setGame(this);

        ServiceManager.register(IGame.class, this);
        ServiceManager.register(ISharedResources.class, new SharedLoader());
        
        colorMode = new ColorManager();
        ServiceManager.register(IColorMode.class, colorMode);
        loadScreen = new LoadScreen();
        ServiceManager.register(ILoader.class, loadScreen);
        tracker = new PageFile();
        ServiceManager.register(PageFile.class, tracker);
        
        audioManager = new AudioManager();
        audioManager.setBgmVol(InternalVolume[0]);
        audioManager.setSfxVol(InternalVolume[1]);
        ServiceManager.register(IAudioManager.class, audioManager);
        playerManager = new PlayerManager();
        ServiceManager.register(IPlayerContainer.class, playerManager);
        
        boss = new BossListener(colorMode, this);
        
        SceneManager.register("town", com.nhydock.storymode.scenes.town.Scene.class);
        SceneManager.register("dungeon", com.nhydock.storymode.scenes.dungeon.Scene.class);
        SceneManager.register("title", com.nhydock.storymode.scenes.title.Scene.class);
        SceneManager.register("newgame", com.nhydock.storymode.scenes.newgame.Scene.class);
        SceneManager.register("endgame", com.nhydock.storymode.scenes.endgame.Scene.class);
        SceneManager.register("lore", com.nhydock.storymode.scenes.lore.Scene.class);

        SceneManager.switchToScene("title");

        loadScreen.setLoadingMessage(null);
    }

    @Override
    public void startGame(GamePreferences preferences) {
        StorymodePreferences p = (StorymodePreferences)preferences;
        playerManager.init(p.difficulty, p.gender, p.hardcore);
        tracker.reset();
        FileBrowser.clearHistory();
        started = true;
    }

    /**
     * Reset back to the title
     */
    @Override
    public void softReset() {
        playerManager = new PlayerManager();
        ServiceManager.register(IPlayerContainer.class, playerManager);

        SceneManager.switchToScene("title");
        started = false;
    }

    /**
     * Skip all the title sequence and story and just jump into a normal
     * difficulty game
     */
    @Override
    public void fastStart() {
        playerManager = new PlayerManager();
        ServiceManager.register(IPlayerContainer.class, playerManager);

        StorymodePreferences preferences = new StorymodePreferences();
        preferences.gender = MathUtils.randomBoolean();
        preferences.difficulty = 3;
        preferences.hardcore = false;
        
        startGame(preferences);
        SceneManager.switchToScene("town");
        started = true;
    }

    @Override
    public void render() {
        // wait until a cycle is over before we acceptably switch screens
        // this way we can call switches from the UI at any point
        if (queued != null) {
            Screen old = super.getScreen();
            ServiceManager.inject(queued);
            super.setScreen(queued);
            queued = null;
            if (old != null) {
                ServiceManager.unhook(old);
                System.gc();
            }
        }
        
        colorMode.update();
        
        Color clear = colorMode.getClear();
        // make sure our buffer is always cleared
        Gdx.gl.glClearColor(clear.r, clear.g, clear.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        
        float delta = Gdx.graphics.getDeltaTime();
        // ignore pause time in getting time played
        if (resumed) {
            delta = 0;
            resumed = false;
        }

        playerManager.updateTime(delta);
        audioManager.update(delta);
        this.getScreen().render(delta);
        if (loadScreen.isLoading()) {
            loadScreen.draw(delta);
        }
        
        BossListener.getInstance().run();
    }

    @Override
    public void setScreen(Screen screen) {
        queued = screen;
    }

    @Override
    public void resume() {
        super.resume();
        resumed = true;
        audioManager.playBgm();
    }
    
    @Override
    public void pause() {
        super.pause();
        audioManager.pauseBgm();
    }
    
    public BossListener getBossInput() {
        return boss;
    }

    @Override
    public void endGame() {
        if (playerManager.isPrepared()) {
            SceneManager.switchToScene("endgame");
            Gdx.app.log("Game", "cheater");
        }
    }

    @Override
    public void onRegister() {
        // Do nothing
    }

    @Override
    public void onUnregister() {
        // Do nothing
    }

    @Override
    public boolean hasStarted() {
        return started;
    }

    @Override
    public boolean debug() {
        return debug;
    }

}
