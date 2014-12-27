package core.service.implementations;

import java.util.Scanner;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import core.DataDirs;
import core.service.interfaces.ISharedResources;

public class SharedLoader implements ISharedResources {

    private AssetManager manager;

    @Override
    public void onRegister() {
        manager = new AssetManager();
        
        // add shared resources
        manager.load(DataDirs.Home + "uiskin.json", Skin.class);
        manager.load(DataDirs.Home + "dungeon.atlas", TextureAtlas.class);
        manager.load(DataDirs.Home + "fill.png", Texture.class);
        manager.load(DataDirs.Home + "null.png", Texture.class);
        manager.load(DataDirs.accept, Sound.class);
        manager.load(DataDirs.tick, Sound.class);
        manager.load(DataDirs.hit, Sound.class);
        manager.load(DataDirs.shimmer, Sound.class);
        manager.load(DataDirs.dead, Sound.class);
        
        //load tilesets
        try (Scanner s = new Scanner(Gdx.files.internal(DataDirs.Tilesets + "list.txt").read())) {
            while (s.hasNextLine()) {
                String file = s.nextLine().trim();
                manager.load(DataDirs.Tilesets + file, Texture.class);
            }
        }
        
        // shared resources matter a lot, so make sure they're loaded before
        // doing anything else;
        while (!manager.update());
        
        // skin is used in a lot of places so we add addition regions to it to make things easier
        Skin uiSkin = manager.get(DataDirs.Home + "uiskin.json");
        TextureAtlas dungeonSprites = manager.get(DataDirs.Home + "dungeon.atlas");
        Texture nullSpace = manager.get(DataDirs.Home + "null.png");
        uiSkin.add("null", nullSpace, Texture.class);
        uiSkin.add("fill", new TextureRegion(manager.get(DataDirs.Home + "fill.png", Texture.class)), TextureRegion.class);
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

    @Override
    public AssetManager getAssetManager() {
        return manager;
    }
}