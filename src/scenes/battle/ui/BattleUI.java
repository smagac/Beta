package scenes.battle.ui;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import core.DataDirs;
import scene2d.ui.extras.FocusGroup;
import scenes.GameUI;

public class BattleUI extends GameUI
{

    Image player;
    Image boss;
    
    CrossMenu mainmenu;
    
	public BattleUI(AssetManager manager)
	{
		super(manager);
		// TODO Auto-generated constructor stub
	}
	
	public void load() {
	    super.load();
	    
	    manager.load(DataDirs.Home + "battle.json", Skin.class);
	}

	@Override
	protected void extend()
	{
	    Skin battleSkin = manager.get(DataDirs.Home + "battle.json", Skin.class);
	    
	    mainmenu = new CrossMenu(battleSkin, this);
	    display.addActor(mainmenu);
	}

	@Override
	protected void triggerAction(int index)
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected FocusGroup focusList()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String[] defineButtons()
	{
		// TODO Auto-generated method stub
		return null;
	}

}
