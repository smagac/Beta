package core.service.interfaces;

import com.badlogic.gdx.assets.AssetManager;

import github.nhydock.ssm.Service;

public interface ISharedResources extends Service {

    public <T> T getResource(String name, Class<T> cls);
    public AssetManager getAssetManager();
}
