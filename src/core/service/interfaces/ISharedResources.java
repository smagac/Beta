package core.service.interfaces;

import github.nhydock.ssm.Service;

import com.badlogic.gdx.assets.AssetManager;

public interface ISharedResources extends Service {

    public <T> T getResource(String name, Class<T> cls);
    public AssetManager getAssetManager();
}
