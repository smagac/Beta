package scenes.endgame;

import github.nhydock.ssm.Inject;
import github.nhydock.ssm.SceneManager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;

import core.DataDirs;
import core.service.interfaces.IGame;
import core.service.interfaces.IPlayerContainer;

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
	public void show() {
		ui = new EndUI(this, manager, player);
		
		manager.load(DataDirs.Audio + "story.mp3", Music.class);
		
		manager.load(DataDirs.hit, Sound.class);
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
		
		audio.playBgm(manager.get(DataDirs.Audio + "story.mp3", Music.class));
	}

}
