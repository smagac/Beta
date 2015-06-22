package core.service.implementations;

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
        manager.load(DataDirs.Home + "battle.atlas", TextureAtlas.class);
        manager.load(DataDirs.Home + "town.atlas", TextureAtlas.class);
        manager.load(DataDirs.Home + "fill.png", Texture.class);
        manager.load(DataDirs.Home + "wfill.png", Texture.class);
        manager.load(DataDirs.Home + "particle.png", Texture.class);
        manager.load(DataDirs.Home + "dargon.png", Texture.class);
        manager.load(DataDirs.Home + "cursor.png", Texture.class);
        
        //load all small sound fx
        for (String path : DataDirs.Sounds.allSounds) {
            manager.load(path, Sound.class);
        }
        
        //load tilesets
        for (String file : DataDirs.getChildren(Gdx.files.internal(DataDirs.Tilesets))) {
            manager.load(file, Texture.class);
        }
        
        // shared resources matter a lot, so make sure they're loaded before
        // doing anything else;
        while (!manager.update());
        
        // skin is used in a lot of places so we add addition regions to it to make things easier
        Skin uiSkin = manager.get(DataDirs.Home + "uiskin.json");
        Texture fillTex = manager.get(DataDirs.Home + "fill.png", Texture.class);
        uiSkin.add("fill", new TextureRegion(fillTex), TextureRegion.class);
        Texture wfillTex = manager.get(DataDirs.Home + "wfill.png", Texture.class);
        uiSkin.add("wfill", new TextureRegion(wfillTex), TextureRegion.class);
        Texture particleTex = manager.get(DataDirs.Home + "particle.png", Texture.class);
        uiSkin.add("particle", new TextureRegion(particleTex), TextureRegion.class);
        Texture dargon = manager.get(DataDirs.Home + "dargon.png");
        uiSkin.add("dargon", dargon, Texture.class);
        Texture cursor = manager.get(DataDirs.Home + "cursor.png");
        uiSkin.add("cursor", cursor, Texture.class);
        
        TextureAtlas dungeonSprites = manager.get(DataDirs.Home + "dungeon.atlas");
        uiSkin.addRegions(dungeonSprites);
        TextureAtlas battleSprites = manager.get(DataDirs.Home + "battle.atlas");
        uiSkin.addRegions(battleSprites);
        TextureAtlas townSprites = manager.get(DataDirs.Home + "town.atlas");
        uiSkin.addRegions(townSprites);
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
