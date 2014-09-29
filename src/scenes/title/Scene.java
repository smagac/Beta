package scenes.title;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.audio.Music;

import core.DataDirs;
import core.common.BossListener;

public class Scene extends scenes.Scene<TitleSequence> {
	
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
	
	public void startMusic()
	{
		if (!audio.isPlayingBgm())
		{
			audio.playBgm();
		}
	}
	
	/**
	 * Initialize the ui and load all assets
	 */
	@Override
	protected void init()
	{
		ui.init();
		
		
		//fetch assets
		audio.setBgm(manager.get(DataDirs.Audio + "title.mp3", Music.class), false);
		
		InputMultiplexer input = new InputMultiplexer();
		input.addProcessor(ui);
		input.addProcessor(BossListener.getInstance());
		Gdx.input.setInputProcessor(input);
	}
}
