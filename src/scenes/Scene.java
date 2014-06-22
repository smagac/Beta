package scenes;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;

import core.common.Storymode;

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
	protected AssetManager manager;
	
	private Storymode service;
	protected View ui;
	
	protected Storymode getService()
	{
		return service;
	}
	
	protected void setService(Storymode service)
	{
		this.service = service;
	}
}
