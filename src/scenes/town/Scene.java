package scenes.town;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.assets.AssetManager;

public class Scene extends scenes.Scene<TownUI> {

	boolean loaded;
	
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
		
		InputMultiplexer input = new InputMultiplexer();
		ui.addToInput(input);
		Gdx.input.setInputProcessor(input);
	}

	@Override
	public void hide() {
		// TODO Auto-generated method stub

	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub

	}

	@Override
	public void resume() {
		// TODO Auto-generated method stub

	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

}
