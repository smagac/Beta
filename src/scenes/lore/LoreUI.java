package scenes.lore;

import github.nhydock.ssm.Inject;
import scenes.UI;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;

import core.DataDirs;
import core.service.interfaces.IPlayerContainer;

public class LoreUI extends UI {
	
	Scene parent;
	@Inject public IPlayerContainer player;
	
	
	//here's the table that holds all of the lore
	// we're going to pan it up accross the screen
	Table lore;
	Rectangle crop;
	Rectangle view;
	private StateMachine<LoreUI> sm;
	
	
	public LoreUI(Scene scene, AssetManager manager) {
		super(manager);
		parent = scene;
		
		sm = new DefaultStateMachine<LoreUI>(this);
		crop = new Rectangle();
		crop.x = 80;
		crop.y = 80;
		crop.width = getWidth() - 80;
		crop.height = getHeight() - 80;
		
		view = new Rectangle();
	}
	
	@Override
	protected void load() {
		manager.load(DataDirs.Home + "lore.json", Skin.class);	
	}

	@Override
	public void init() {
		skin = manager.get(DataDirs.Home + "lore.json", Skin.class);
		
		final LoreUI ui = this;
		
		lore = new Table();
		lore.setVisible(false);
        
	}

	@Override
	public void update(float delta) { }

	@Override
	public void draw() {
	    super.draw();
	    
	    super.calculateScissors(crop, view);
	    ScissorStack.pushScissors(view);
	    lore.draw(getBatch(), 1.0f);
	    ScissorStack.popScissors();
	}
	
	@Override
	public boolean handleMessage(Telegram msg) {
		return sm.handleMessage(msg);
	}

    public boolean isDone() {
        // TODO Auto-generated method stub
        return false;
    }
}
