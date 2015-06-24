package scenes.endgame;

import github.nhydock.ssm.ServiceManager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

import core.DataDirs;
import core.service.interfaces.IGame;

public class Scene extends scenes.Scene<EndUI> {

    @Override
    public void extend(float delta) {
        if (ui.isDone()) {
            ServiceManager.getService(IGame.class).softReset();
            return;
        }

        ui.draw();
    }

    @Override
    public void show() {
        ui = new EndUI(this, manager);

        manager.load(DataDirs.Audio + "story.mp3", Music.class);
    }

    @Override
    protected void init() {
        // fetch assets
        ui.init();

        input.addProcessor(ui);

        Gdx.input.setInputProcessor(input);

        audio.playBgm(manager.get(DataDirs.Audio + "story.mp3", Music.class));
    }

}
