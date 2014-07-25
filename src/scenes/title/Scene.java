package scenes.title;

import github.nhydock.ssm.SceneManager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;

import core.common.BossListener;

public class Scene extends scenes.Scene<TitleSequence> {
	
	boolean musicStarted;
	int difficulty = 3;
	
	float timer = 0f;
	
	@Override
	public void extend(float delta) {
		ui.draw();
	}

	@Override
	public void show() {
		ui = new TitleSequence(this, manager);
		manager.load("data/audio/title.mp3", Music.class);
	}
	
	protected void startMusic()
	{
		musicStarted = true;
		bgm.play();
	}

	/**
	 * Initialize the ui and load all assets
	 */
	@Override
	protected void init()
	{
		ui.init();
		ui.addListener(new InputListener(){
			@Override
			public boolean keyDown(InputEvent evt, int keycode)
			{
				//skip the intro
				if (keycode == Keys.ENTER || keycode == Keys.SPACE ||
					keycode == Keys.ESCAPE || keycode == Keys.BACKSPACE )
				{
					showDiffSelection();
					return true;
				}
				return false;
			}
		});
		
		//fetch assets
		bgm = manager.get("data/audio/title.mp3", Music.class);
		
		InputMultiplexer input = new InputMultiplexer();
		input.addProcessor(ui);
		input.addProcessor(BossListener.getInstance());
		Gdx.input.setInputProcessor(input);
	}
	
	private void showDiffSelection()
	{
		musicStarted = false;
		bgm.stop();
		SceneManager.switchToScene("newgame");
	}
	
	@Override
	public void hide() {
		dispose();
	}

	@Override
	public void pause() {
		if (musicStarted)
		{
			bgm.pause();
		}
	}

	@Override
	public void resume() {
		if (musicStarted)
		{
			bgm.play();
		}
	}

	@Override
	public void dispose() {
		if (musicStarted)
		{
			bgm.stop();
		}
		super.dispose();
	}

}
