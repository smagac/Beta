package scenes.battle.ui;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Action;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

/**
 * Little menu used in the battle scene for showing the
 * commands that one may enter.
 * </p>
 * Nothing more than an interactive group designed for one
 * specific UI purpose.
 * @author nhydock
 *
 */
public class CrossMenu extends Group
{
    BattleUI ui;
    
    Image bg;
	Image attack;
	Image item;
	Image defend;
	
	private enum Button {
	    Attack, Item, Defend;
	}
	
	Button checked;
	
	private class ButtonInput extends InputListener {
	    Actor button;
	    int msg;
	    Button index;
	    
	    public ButtonInput(Actor focus, int msg, Button index) {
	        this.button = focus;
	        this.msg = msg;
	        this.index = index;
	    }
	    
	    @Override
        public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
            if (checked == this.index) {
                MessageDispatcher.getInstance().dispatchMessage(0f, null, ui, msg);
                return true;
            }
            return false;
        }
        
        @Override
        public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            setChecked(index.ordinal());
        }
	}
	
	/**
	 * Generate the cross menu
	 * @param skin
	 */
	public CrossMenu(Skin skin, final BattleUI ui) {
		super();
		
		this.ui = ui;
		bg = new Image(skin, "fill");
		bg.setWidth(ui.getDisplayWidth());
		bg.setOrigin(Align.center);
		bg.setHeight(48f);
		
		attack = new Image(skin, "attack");
		item = new Image(skin, "item");
		defend = new Image(skin, "defend");
		
		attack.setSize(64f, 64f);
		item.setSize(64f, 64f);
		defend.setSize(64f, 64f);
		attack.setOrigin(Align.center);
		item.setOrigin(Align.center);
		defend.setOrigin(Align.center);
		
		addActor(attack);
		addActor(item);
		addActor(defend);
		
		attack.addListener(new ButtonInput(attack, BattleMessages.ATTACK, Button.Attack));
		item.addListener(new ButtonInput(item, BattleMessages.ITEM, Button.Item));
		defend.addListener(new ButtonInput(defend, BattleMessages.DEFEND, Button.Defend));
		
		setSize(ui.getDisplayWidth(), ui.getDisplayHeight());
		addAction(Actions.alpha(0f));
		act(1f);
		
		addListener(new InputListener(){
		   
		    @Override
		    public boolean keyDown(InputEvent evt, int key) {
		        if (key == Keys.LEFT || key == Keys.A) {
		            setChecked(checked.ordinal()-1);
		            return true;
		        }
		        if (key == Keys.RIGHT || key == Keys.D) {
                    setChecked(checked.ordinal()+1);
                    return true;
                }
		        return false;
		    }
		});
	}
	
    private static Action grow() {
        return Actions.scaleTo(1.1f, 1.1f, .2f, Interpolation.circleOut);
    }
    
    private static Action shrink() {
        return Actions.scaleTo(.8f, .8f, .2f, Interpolation.circleOut);
    }
	
	private void setChecked(int index) {
	    if (index < 0) {
	        index = Button.values().length-1;
	    } else if (index >= Button.values().length) {
	        index = 0;
	    }
	    
	    Button val = Button.values()[index];

	    switch(val) {
	        case Attack:
	            attack.addAction(grow());
	            defend.addAction(shrink());
	            item.addAction(shrink());
	            break;
	        case Defend:
	            attack.addAction(shrink());
                defend.addAction(grow());
                item.addAction(shrink());
                break;
	        case Item:
	            attack.addAction(shrink());
                defend.addAction(shrink());
                item.addAction(grow());
                break;
	    }
	    
	    checked = val;
	}
	
	/**
	 * Opens the menu and enables input
	 */
	public void show(){
	    attack.addAction(Actions.moveToAligned(-(attack.getWidth() + 16f), 0, Align.center, .3f));
        item.addAction(Actions.moveToAligned(0, 0, Align.center, .3f));
        defend.addAction(Actions.moveToAligned(defend.getWidth() + 16f, 0, Align.center, .3f));
        
        addAction(Actions.sequence(Actions.alpha(1f, .2f)));
        
        setTouchable(Touchable.enabled);
        
        setChecked(1);
	}
	
	/**
	 * Closes the menu and prevents further input
	 */
	public void hide(){
	    attack.addAction(Actions.moveToAligned(0, 0, Align.center, .3f));
	    item.addAction(Actions.moveToAligned(0, 0, Align.center, .3f));
	    defend.addAction(Actions.moveToAligned(0, 0, Align.center, .3f));
	    
	    addAction(Actions.sequence(Actions.delay(.3f), Actions.alpha(0f, .2f)));
	    
	    setTouchable(Touchable.disabled);
	}
}
