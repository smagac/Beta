package scenes.newgame;

import scenes.SceneManager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

import core.DataDirs;
import core.common.Storymode;

public class Scene extends scenes.Scene<NewUI> {

	private Music bgm;
	
	@Override
	public void extend(float delta) {
		if (ui.isDone())
		{
			Storymode.startGame(ui.getDifficulty());
			SceneManager.switchToScene("town");
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
		ui = new NewUI(this, manager);
		
		manager.load("data/audio/story.mp3", Music.class);
		
		manager.load(DataDirs.tick, Sound.class);
		manager.load(DataDirs.shimmer, Sound.class);
		manager.load(DataDirs.accept, Sound.class);
	}
	
	protected void init()
	{
		//fetch assets
		ui.init();
		
		InputMultiplexer input = new InputMultiplexer();
		input.addProcessor(ui);
		input.addProcessor(getService().getBossInput());
		Gdx.input.setInputProcessor(input);
	}
	
	protected void prepareStory()
	{
		bgm = manager.get("data/audio/story.mp3", Music.class);
		bgm.setLooping(true);
		bgm.play();
		
		ui.prepareStory();
		
		ui.clear();
	}
	
	@Override
	public void hide() {
		if (bgm != null)
		{
			bgm.stop();
		}
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
		manager.dispose();
		ui.dispose();
	}

}
