package scenes.town;

import scenes.town.ui.TownUI;
import github.nhydock.ssm.Inject;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.audio.Music;

import core.DataDirs;
import core.common.BossListener;
import core.service.interfaces.IPlayerContainer;
import core.service.interfaces.IQuestContainer;

public class Scene extends scenes.Scene<TownUI> {

	@Inject public IPlayerContainer playerService;
	@Inject public IQuestContainer questService;
	
	@Override
	public void show() {
		ui = new TownUI(manager);
		manager.load(DataDirs.Audio + "town.mp3", Music.class);
		
		InputMultiplexer input = new InputMultiplexer();
		input.addProcessor(ui);
		input.addProcessor(BossListener.getInstance());
		Gdx.input.setInputProcessor(input);
	}

	@Override
	protected void init() {
		ui.init();
		audio.playBgm(manager.get(DataDirs.Audio + "town.mp3", Music.class));
	}

	@Override
	protected void extend(float delta) {
		ui.draw();
	}

}
