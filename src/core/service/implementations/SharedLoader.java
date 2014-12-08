package core.service.implementations;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import core.DataDirs;
import core.service.interfaces.ISharedResources;

public class SharedLoader implements ISharedResources {

    AssetManager manager;

    @Override
    public void onRegister() {
        manager = new AssetManager();
        
        // add shared resources
        manager.load(DataDirs.Home + "uiskin.json", Skin.class);
        manager.load(DataDirs.Home + "dungeon.atlas", TextureAtlas.class);
        manager.load(DataDirs.Home + "loading.fnt", BitmapFont.class);
        manager.load(DataDirs.Home + "fill.png", Texture.class);
        manager.load(DataDirs.accept, Sound.class);
        manager.load(DataDirs.tick, Sound.class);
        manager.load(DataDirs.hit, Sound.class);
        manager.load(DataDirs.shimmer, Sound.class);
        
        // shared resources matter a lot, so make sure they're loaded before
        // doing anything else;
        while (!manager.update());
        
        // skin is used in a lot of places so we add addition regions to it to make things easier
        Skin uiSkin = manager.get(DataDirs.Home + "uiskin.json");
        TextureAtlas dungeonSprites = manager.get(DataDirs.Home + "dungeon.atlas");
        uiSkin.add("fill", new TextureRegion(manager.get(DataDirs.Home + "fill.png", Texture.class)), TextureRegion.class);
        uiSkin.add("loading", manager.get(DataDirs.Home + "loading.fnt", BitmapFont.class), BitmapFont.class);
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
