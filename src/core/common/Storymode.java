package core.common;

import scenes.town.ui.TownUI;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;

import core.DLC;
import core.factories.AllFactories;
import core.service.implementations.AudioManager;
import core.service.implementations.ColorManager;
import core.service.implementations.DungeonManager;
import core.service.implementations.LoadScreen;
import core.service.implementations.PlayerManager;
import core.service.implementations.SharedLoader;
import core.service.interfaces.IAudioManager;
import core.service.interfaces.IColorMode;
import core.service.interfaces.IDungeonContainer;
import core.service.interfaces.IGame;
import core.service.interfaces.ILoader;
import core.service.interfaces.IPlayerContainer;
import core.service.interfaces.ISharedResources;
import github.nhydock.ssm.SceneManager;
import github.nhydock.ssm.ServiceManager;

public class Storymode extends com.badlogic.gdx.Game implements IGame {

    public static final int[] InternalRes = { 960, 540 };

    private boolean resumed;

    private Screen queued;

    private BossListener boss;

    private IPlayerContainer playerManager;
    private IDungeonContainer dungeonManager;
    private AudioManager audioManager;
    private LoadScreen loadScreen;
    private ColorManager colorMode;

    protected Storymode() {}

    @Override
    public void resize(int width, int height) {
        colorMode.resize(width, height);
        super.resize(width, height);
    }

    @Override
    public void create() {
        // setup all factory resources
        DLC.init();
        AllFactories.prepare();

        SceneManager.setGame(this);

        ServiceManager.register(IGame.class, this);
        ServiceManager.register(IColorMode.class, new ColorManager());
        ServiceManager.register(ISharedResources.class, new SharedLoader());
        
        audioManager = new AudioManager();
        ServiceManager.register(IAudioManager.class, audioManager);
        playerManager = new PlayerManager();
        ServiceManager.register(IPlayerContainer.class, playerManager);
        dungeonManager = new DungeonManager();
        ServiceManager.register(IDungeonContainer.class, dungeonManager);
        loadScreen = new LoadScreen();
        ServiceManager.register(ILoader.class, loadScreen);
        
        SceneManager.register("town", scenes.town.Scene.class);
        SceneManager.register("dungeon", scenes.dungeon.Scene.class);
        SceneManager.register("title", scenes.title.Scene.class);
        SceneManager.register("newgame", scenes.newgame.Scene.class);
        SceneManager.register("endgame", scenes.endgame.Scene.class);

        SceneManager.switchToScene("title");

        loadScreen.setLoadingMessage(null);
        boss = new BossListener(colorMode, this);
    }

    @Override
    public void startGame(int difficulty, boolean gender) {
        playerManager.init(difficulty, gender);
        dungeonManager.clear();
        Tracker.reset();
        TownUI.clearHistory();
    }

    /**
     * Reset back to the title
     */
    @Override
    public void softReset() {
        playerManager = new PlayerManager();
        ServiceManager.register(IPlayerContainer.class, playerManager);

        SceneManager.switchToScene("title");
    }

    /**
     * Skip all the title sequence and story and just jump into a normal
     * difficulty game
     */
    @Override
    public void fastStart() {
        playerManager = new PlayerManager();
        ServiceManager.register(IPlayerContainer.class, playerManager);

        startGame(3, true);
        SceneManager.switchToScene("town");
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
            loadScreen.draw();
        }
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

    public void setBgmVol(float vol) {
        audioManager.setBgmVol(vol);
    }

    public void setSfxVol(float vol) {
        audioManager.setSfxVol(vol);
    }

}
