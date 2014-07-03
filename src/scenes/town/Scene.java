package scenes.town;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.audio.Music;

public class Scene extends scenes.Scene<TownUI> {

	Music bgm;

	@Override
	public void resize(int width, int height) {
		((TownUI)ui).resize(width, height);
	}

	@Override
	public void show() {
		ui = new TownUI(this, manager);
		manager.load("data/audio/town.mp3", Music.class);
		
		InputMultiplexer input = new InputMultiplexer();
		input.addProcessor(ui);
		input.addProcessor(getService().getBossInput());
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
		if (bgm != null)
		{
			bgm.stop();
			bgm.dispose();
		}
		manager.dispose();
		ui.dispose();
	}

	@Override
	protected void init() {
		((TownUI)ui).init();
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
