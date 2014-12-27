package scenes.battle;

import github.nhydock.ssm.Inject;
import core.service.interfaces.IBattleContainer;
import scenes.battle.ui.BattleUI;

public class Scene extends scenes.Scene<BattleUI>
{

    @Inject IBattleContainer battle;
    
    
	@Override
	public void show()
	{
	    
	}

	@Override
	protected void init()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void extend(float delta)
	{
		// TODO Auto-generated method stub
		
	}

}
