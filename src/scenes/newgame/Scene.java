package scenes.newgame;

import github.nhydock.ssm.Inject;
import github.nhydock.ssm.SceneManager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

import core.DataDirs;
import core.service.interfaces.IGame;
import core.service.interfaces.IPlayerContainer;

public class Scene extends scenes.Scene<NewUI> {

    @Inject
    public IGame gameService;
    @Inject
    public IPlayerContainer playerService;

    @Override
    public void extend(float delta) {
        if (ui.isDone()) {
            SceneManager.switchToScene("town");
            return;
        }

        ui.draw();
    }

    @Override
    public void show() {
        ui = new NewUI(this, manager, playerService);

        manager.load(DataDirs.Audio + "story.mp3", Music.class);

        manager.load(DataDirs.tick, Sound.class);
        manager.load(DataDirs.shimmer, Sound.class);
        manager.load(DataDirs.accept, Sound.class);
    }

    @Override
    protected void init() {
        // fetch assets
        ui.init();

        input.addProcessor(ui);

        Gdx.input.setInputProcessor(input);
    }

    protected void prepareStory() {
        gameService.startGame(ui.getDifficulty(), ui.getGender());
        audio.playBgm(manager.get(DataDirs.Audio + "story.mp3", Music.class));
    }

}
