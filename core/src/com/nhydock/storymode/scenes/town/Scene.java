package com.nhydock.storymode.scenes.town;

import github.nhydock.ssm.Inject;
import github.nhydock.ssm.ServiceManager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.nhydock.storymode.DataDirs;
import com.nhydock.storymode.scenes.town.ui.TownUI;
import com.nhydock.storymode.service.interfaces.IDungeonContainer;
import com.nhydock.storymode.service.interfaces.IPlayerContainer;

public class Scene extends com.nhydock.storymode.scenes.Scene<TownUI> {

    @Inject
    public IPlayerContainer playerService;

    private String bgm;
    
    @Override
    public void show() {
        ServiceManager.register(IDungeonContainer.class, null);
    
        playerService.getAilments().reset();
        
        ui = new TownUI(this, manager);
        
        bgm = DataDirs.getChildren(Gdx.files.internal(DataDirs.Audio + "town/")).random();
        manager.load(bgm, Music.class);

        input.addProcessor(ui);
        Gdx.input.setInputProcessor(input);
    }

    @Override
    protected void init() {
        ui.init();
        audio.playBgm(manager.get(bgm, Music.class));
    }

    @Override
    protected void extend(float delta) {
        ui.draw();
    }

}
