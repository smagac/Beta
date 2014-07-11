package scenes.endgame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

import core.DataDirs;
import core.common.SceneManager;
import core.service.IGame;
import core.service.IPlayerContainer;
import core.service.Inject;

public class Scene extends scenes.Scene<EndUI> {

	@Inject public IGame gameService;
	@Inject public IPlayerContainer player;
	
	@Override
	public void extend(float delta) {
		if (ui.isDone())
		{
			SceneManager.switchToScene("title");
			return;
		}
		
		ui.draw();
	}

	@Override
	public void resize(int width, int height) {
		ui.getViewport().update(width, height);
	}
	
	@Override
	public void show() {
		ui = new EndUI(this, manager, player);
		
		manager.load("data/audio/story.mp3", Music.class);
		
		manager.load(DataDirs.hit, Sound.class);
		manager.load(DataDirs.shimmer, Sound.class);
		manager.load(DataDirs.accept, Sound.class);
	}
	
	protected void init()
	{
		//fetch assets
		ui.init();
		
		input.addProcessor(ui);
		
		Gdx.input.setInputProcessor(input);
		
		bgm = manager.get("data/audio/story.mp3", Music.class);
		bgm.setLooping(true);
		bgm.play();
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
		super.dispose();
	}

}
