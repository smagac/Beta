package scenes.newgame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

import core.DataDirs;
import core.common.SceneManager;
import core.service.IGame;
import core.service.IPlayerContainer;
import core.service.Inject;

public class Scene extends scenes.Scene<NewUI> {

	@Inject public IGame gameService;
	@Inject public IPlayerContainer playerService;
	
	@Override
	public void extend(float delta) {
		if (ui.isDone())
		{
			
			SceneManager.switchToScene("town");
			return;
		}
		
		ui.draw();
		//Table.drawDebug(ui);
	}

	@Override
	public void resize(int width, int height) {
		ui.getViewport().update(width, height);
	}
	
	@Override
	public void show() {
		ui = new NewUI(this, manager, playerService);
		
		manager.load("data/audio/story.mp3", Music.class);
		
		manager.load(DataDirs.tick, Sound.class);
		manager.load(DataDirs.shimmer, Sound.class);
		manager.load(DataDirs.accept, Sound.class);
	}
	
	@Override
	protected void init()
	{
		//fetch assets
		ui.init();
		
		input.addProcessor(ui);
		
		Gdx.input.setInputProcessor(input);
	}
	
	protected void prepareStory()
	{
		gameService.startGame(ui.getDifficulty(), ui.getGender());
		bgm = manager.get("data/audio/story.mp3", Music.class);
		bgm.setLooping(true);
		bgm.play();
		
		ui.prepareStory();
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
