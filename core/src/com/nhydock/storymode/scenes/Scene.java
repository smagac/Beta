package com.nhydock.storymode.scenes;

import github.nhydock.ssm.Inject;
import github.nhydock.ssm.ServiceManager;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.assets.AssetManager;
import com.nhydock.storymode.service.implementations.InputHandler;
import com.nhydock.storymode.service.interfaces.IAudioManager;
import com.nhydock.storymode.service.interfaces.IColorMode;
import com.nhydock.storymode.service.interfaces.ILoader;

/**
 * Generic scene class with injectable service support and provided assumed ui
 * 
 * @author nhydock
 *
 * @param <View>
 */
public abstract class Scene<View extends UI> implements Screen {

    /**
     * Provided asset manager for asynchronous loading
     */
    protected AssetManager manager;

    @Inject
    public IColorMode color;
    @Inject
    public ILoader loader;
    @Inject
    public IAudioManager audio;

    protected View ui;

    protected boolean loaded;
    
    @Inject
    public InputHandler input;

    public Scene() {
        manager = new AssetManager();
        ServiceManager.register(InputHandler.class, new InputHandler());

        ServiceManager.inject(this);
    }

    @Override
    public void resize(int width, int height) {
        ui.resize(width, height);
    }

    @Override
    public final void render(float delta) {
        // don't do anything while trying to load
        if (!manager.update()) {
            loader.setLoading(true);
            loader.setLoadingMessage(null);
            return;
        }

        if (!loaded) {
            init();
            loader.setLoading(false);
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
     * 
     * @param delta
     */
    protected abstract void extend(float delta);

    @Override
    public void dispose() {
        if (ui != null) {
            ui.dispose();
        }
        audio.clearBgm();
        input.clear();
        manager.dispose();
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
        dispose();
    }
    
    public final InputHandler getInput() {
        return input;
    }
}
