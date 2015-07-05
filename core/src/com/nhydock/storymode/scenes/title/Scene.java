package com.nhydock.storymode.scenes.title;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.nhydock.storymode.DataDirs;

public class Scene extends com.nhydock.storymode.scenes.Scene<TitleSequence> {

    int difficulty = 3;

    float timer = 0f;

    @Override
    public void extend(float delta) {
        ui.draw();
    }

    @Override
    public void show() {
        ui = new TitleSequence(this, manager);
        manager.load(DataDirs.Audio + "title.mp3", Music.class);
    }

    /**
     * Initialize the ui and load all assets
     */
    @Override
    protected void init() {
        ui.init();

        input.addProcessor(ui);
        Gdx.input.setInputProcessor(input);
    }
}
