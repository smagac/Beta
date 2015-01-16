package scenes.battle;

import com.badlogic.ashley.core.Entity;
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
import github.nhydock.ssm.ServiceManager;
import core.DataDirs;
import core.service.implementations.BossBattle;
import core.service.interfaces.IBattleContainer;
import core.util.dungeon.TsxTileSet;
import scenes.UI;
import scenes.battle.ui.BattleUI;

public class Scene extends scenes.Scene<BattleUI>
{

    private String bgm;
    
    private TiledMapTileSet environment;
    
    public Scene(Entity boss){
        super();
        
        BossBattle battle = new BossBattle();
        battle.setBoss(boss);
        ServiceManager.register(IBattleContainer.class, battle);
    }
    
	@Override
	public void show()
	{
	    ui = new BattleUI(manager, environment);
	    
        bgm = DataDirs.getChildren(Gdx.files.internal(DataDirs.Audio + "combat/")).random();
        manager.load(bgm, Music.class);
        manager.load(DataDirs.Audio + "victory.mp3", Music.class);
        
        input.addProcessor(ui);
        Gdx.input.setInputProcessor(input);
	}
	
	@Override
	public void dispose() {
	    super.dispose();
	    ServiceManager.register(IBattleContainer.class, null);
	}
	
	/**
	 * Sets the background of the boss fight
	 * @param tileset
	 */
	public void setEnvironment(TiledMapTileSet tileset)
	{
	    environment = tileset;
	}

	@Override
	protected void init()
	{
	    ui.init();
	    audio.playBgm(manager.get(bgm, Music.class));
	    audio.fadeIn();
	}

	@Override
	protected void extend(float delta)
	{
		ui.draw();
	}

}
