package scenes.town;

import github.nhydock.ssm.Inject;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.audio.Music;

import core.common.BossListener;
import core.service.IPlayerContainer;

public class Scene extends scenes.Scene<TownUI> {

	@Inject public IPlayerContainer playerService;
	
	@Override
	public void resize(int width, int height) {
		ui.resize(width, height);
	}

	@Override
	public void show() {
		ui = new TownUI(manager, playerService);
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
		ui.act(delta);
		
		ui.draw();
	}

}
