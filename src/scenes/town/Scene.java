package scenes.town;

import scenes.town.ui.TownUI;
import github.nhydock.ssm.Inject;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.audio.Music;

import core.common.BossListener;
import core.service.interfaces.IPlayerContainer;
import core.service.interfaces.IQuestContainer;

public class Scene extends scenes.Scene<TownUI> {

	@Inject public IPlayerContainer playerService;
	@Inject public IQuestContainer questService;
	
	@Override
	public void show() {
		ui = new TownUI(manager);
		manager.load("data/audio/town.mp3", Music.class);
		
		InputMultiplexer input = new InputMultiplexer();
		input.addProcessor(ui);
		input.addProcessor(BossListener.getInstance());
		Gdx.input.setInputProcessor(input);
		
		//new crafts appear when you return to town
		playerService.getInventory().refreshCrafts();
		
	}

	@Override
	public void hide() {
		dispose();
	}

	@Override
	public void pause() {
		if (bgm != null)
		{
			bgm.pause();
		}
	}

	@Override
	public void resume() {
		if (bgm != null)
		{
			bgm.play();
		}
	}

	@Override
	public void dispose() {
		if (bgm != null)
		{
			bgm.stop();
		}
		bgm = null;
		super.dispose();
	}

	@Override
	protected void init() {
		ui.init();
		bgm = manager.get("data/audio/town.mp3");
		bgm.play();
		bgm.setLooping(true);
	}

	@Override
	protected void extend(float delta) {
		ui.draw();
	}

}
