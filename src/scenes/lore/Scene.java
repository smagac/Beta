package scenes.lore;

import scene2d.GotoScene;
import scene2d.PlayBGM;
import github.nhydock.ssm.Inject;
import github.nhydock.ssm.SceneManager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;

import core.DataDirs;
import core.service.interfaces.IGame;
import core.service.interfaces.IPlayerContainer;

public class Scene extends scenes.Scene<LoreUI> {

	@Inject public IGame gameService;
	@Inject public IPlayerContainer playerService;
	
	@Override
	public void extend(float delta) {
		ui.draw();
	}
	
	@Override
	public void show() {
		ui = new LoreUI(this, manager);
		
		manager.load(DataDirs.Audio + "lore.mp3", Music.class);
	}
	
	@Override
	protected void init()
	{
		//fetch assets
		ui.init();
		
		input.addProcessor(ui);
		
		Gdx.input.setInputProcessor(input);
		
		audio.playBgm(manager.get(DataDirs.Audio + "lore.mp3", Music.class));
	}
}
