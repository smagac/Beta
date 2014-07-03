package scenes;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;

import core.service.IColorMode;
import core.service.Inject;

/**
 * Generic scene class with injectable service support and provided assumed ui
 * @author nhydock
 *
 * @param <View>
 */
public abstract class Scene<View extends UI> implements Screen {

	/**
	 * Provided asset manager for asynchronous loading
	 */
	protected final AssetManager manager;
	
	@Inject public IColorMode color;
	
	protected View ui;
	
	private boolean loaded;
	
	public Scene()
	{
		manager = new AssetManager();
	}
	
	@Override
	public void resize(int width, int height)
	{
		ui.resize(width, height);
	}
	
	@Override
	public final void render(float delta)
	{
		//don't do anything while trying to load
		if (!manager.update()){
			return;
		}
		
		if (!loaded)
		{
			init();
			loaded = true;
		}
		
		ui.getBatch().setShader(color.getShader());
		ui.act(delta);
		
		extend(delta);
	}
	
	/**
	 * Post-load initialization of features
	 */
	protected abstract void init();
	
	/**
	 * Extended rendering process invoked after loading
	 * @param delta
	 */
	protected abstract void extend(float delta);
}
