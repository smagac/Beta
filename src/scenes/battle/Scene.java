package scenes.battle;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureWrap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;

import github.nhydock.ssm.Inject;
import core.DataDirs;
import core.service.interfaces.IBattleContainer;
import core.util.dungeon.TsxTileSet;
import scenes.UI;
import scenes.battle.ui.BattleUI;

public class Scene extends scenes.Scene<BattleUI>
{

    @Inject IBattleContainer battle;
    
    private String bgm;
    
    private Texture environment;
    
	@Override
	public void show()
	{
	    ui = new BattleUI(manager, environment);
	    
        bgm = DataDirs.getChildren(Gdx.files.internal(DataDirs.Audio + "combat/")).random();
        manager.load(bgm, Music.class);
	}
	
	/**
	 * Sets the background of the boss fight
	 * @param tileset
	 */
	public void setEnvironment(TiledMapTileSet tileset)
	{
	    Texture tx = new Texture(32, 128, Pixmap.Format.RGBA8888);
	    
	    int[] tileTypes = {TsxTileSet.NULL, 2, TsxTileSet.WALL, 1, TsxTileSet.FLOOR, 1};
	    for (int i = 0, y = 0; i < tileTypes.length; i += 2) {
	        int type = tileTypes[i]; 
	        int n = tileTypes[i+1];
	        
	        TextureRegion tile = tileset.getTile(type).getTextureRegion();
	        Texture tileTx = tile.getTexture();
	        tileTx.getTextureData().prepare();
	        Pixmap pm = tileTx.getTextureData().consumePixmap();
	        
	        for (int k = 0; k < n; k++, y += 32) {
	            tx.draw(pm, 0, y);
	        }
	        //pm.dispose();
	    }
	    
	    environment = tx;
	    environment.setWrap(TextureWrap.Repeat, TextureWrap.ClampToEdge);
	}

	@Override
	protected void init()
	{
	    ui.init();
	    audio.playBgm(manager.get(bgm, Music.class));
	}

	@Override
	protected void extend(float delta)
	{
		ui.draw();
	}

}
