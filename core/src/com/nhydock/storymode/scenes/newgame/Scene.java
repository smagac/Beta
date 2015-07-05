package com.nhydock.storymode.scenes.newgame;

import github.nhydock.ssm.Inject;
import github.nhydock.ssm.SceneManager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.nhydock.storymode.DataDirs;
import com.nhydock.storymode.Storymode.StorymodePreferences;
import com.nhydock.storymode.service.interfaces.IGame;

public class Scene extends com.nhydock.storymode.scenes.Scene<NewUI> {

    @Inject
    public IGame gameService;

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
        ui = new NewUI(this, manager);

        manager.load(DataDirs.Audio + "story.mp3", Music.class);
    }

    @Override
    protected void init() {
        // fetch assets
        ui.init();

        input.addProcessor(ui);

        Gdx.input.setInputProcessor(input);
    }

    protected void prepareStory() {
        StorymodePreferences preferences = new StorymodePreferences();
        preferences.difficulty = ui.getDifficulty();
        preferences.gender = ui.getGender();
        preferences.hardcore = ui.isHardcore();
        
        gameService.startGame(preferences);
        
        audio.playBgm(manager.get(DataDirs.Audio + "story.mp3", Music.class));
    }

}
