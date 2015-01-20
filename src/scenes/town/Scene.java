package scenes.town;

import scenes.town.ui.TownUI;
import github.nhydock.ssm.Inject;
import github.nhydock.ssm.ServiceManager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.audio.Music;

import core.DataDirs;
import core.common.BossListener;
import core.service.interfaces.IDungeonContainer;
import core.service.interfaces.IPlayerContainer;

public class Scene extends scenes.Scene<TownUI> {

    @Inject
    public IPlayerContainer playerService;

    private String bgm;
    
    @Override
    public void show() {
        ServiceManager.getService(IDungeonContainer.class).clear();
        scenes.dungeon.Scene.fight = false;
    
        ui = new TownUI(manager);
        
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
