package scenes.battle;

import github.nhydock.ssm.ServiceManager;
import scenes.battle.ui.BattleUI;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;

import core.DataDirs;
import core.service.implementations.BossBattle;
import core.service.interfaces.IBattleContainer;

public class Scene extends scenes.Scene<BattleUI>
{
    private String bgm;
    
    public Scene(Entity boss){
        super();
        
        BossBattle battle = new BossBattle();
        battle.setBoss(boss);
        ServiceManager.register(IBattleContainer.class, battle);
    }
    
	@Override
	public void show()
	{
	    ui = new BattleUI(manager);
	    
        bgm = DataDirs.getChildren(Gdx.files.internal(DataDirs.Audio + "combat/")).random();
        manager.load(bgm, Music.class);
        manager.load(DataDirs.Audio + "victory.mp3", Music.class);
        
        input.addProcessor(ui);
    }
	
	@Override
	public void dispose() {
	    super.dispose();
	    ServiceManager.register(IBattleContainer.class, null);
	}
	
	@Override
	protected void init()
	{
	    ui.init();
	    audio.playBgm(manager.get(bgm, Music.class));
	    audio.fadeIn();
	    Gdx.input.setInputProcessor(input);
	}

	@Override
	protected void extend(float delta)
	{
		ui.draw();
	}

}
