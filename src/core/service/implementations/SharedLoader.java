package core.service.implementations;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import core.DataDirs;
import core.service.interfaces.ISharedResources;

public class SharedLoader implements ISharedResources {

    AssetManager manager;

    @Override
    public void onRegister() {
        manager = new AssetManager();
        
        // add shared resources
        
        manager.load(DataDirs.GameData + "uiskin.json", Skin.class);
        manager.load(DataDirs.GameData + "dungeon.atlas", TextureAtlas.class);
        
        // shared resources matter a lot, so make sure they're loaded before
        // doing anything else;
        while (manager.update());
        
        Skin uiSkin = manager.get(DataDirs.GameData + "uiskin.json");
        TextureAtlas dungeonSprites = manager.get(DataDirs.GameData + "dungeon.atlas");
        uiSkin.addRegions(dungeonSprites);
    }
    
    @Override
    public <T> T getResource(String name, Class<T> cls) {
        return manager.get(name, cls);
    }

    @Override
    public void onUnregister() {
        manager.dispose();
        manager = null;
    }
}
