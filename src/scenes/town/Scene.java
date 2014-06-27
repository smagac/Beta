package scenes.town;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;

public class Scene extends scenes.Scene<TownUI> {

	boolean loaded;
	
	Music bgm;
	
	@Override
	public void render(float delta) {
		if (!manager.update()){
			//TODO draw loading screen
			return;
		}
		//load create the ui once the manager is done loading
		if (!loaded)
		{
			ui.init();
			bgm = manager.get("data/audio/town.mp3");
			bgm.play();
			bgm.setLooping(true);
			loaded = true;
		}
		
		ui.update(delta);
		
		ui.draw();
	}

	@Override
	public void resize(int width, int height) {
		ui.resize(width, height);
	}

	@Override
	public void show() {
		manager = new AssetManager();
		
		ui = new TownUI(this, manager);
		manager.load("data/audio/town.mp3", Music.class);
		
		InputMultiplexer input = new InputMultiplexer();
		ui.addToInput(input);
		Gdx.input.setInputProcessor(input);
		
		//new crafts appear when you return to town
		getService().getInventory().refreshCrafts();
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
		bgm.stop();
		bgm.dispose();
		manager.dispose();
		ui.dispose();
	}

}
