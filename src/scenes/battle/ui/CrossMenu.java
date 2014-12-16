package scenes.battle.ui;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

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
	ButtonGroup menu;
	Button attack;
	Button item;
	Button defend;
	
	/**
	 * Generate the cross menu
	 * @param skin
	 */
	public CrossMenu(Skin skin, final BattleUI ui) {
		super();
		
		attack = new ImageButton(skin, "attack");
		item = new ImageButton(skin, "item");
		defend = new ImageButton(skin, "defend");
		
		menu = new ButtonGroup(attack, item, defend);
		
		addActor(attack);
		addActor(item);
		addActor(defend);
		
		attack.addListener(new InputListener() {
		   @Override
		   public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
		       if (menu.getChecked() == attack) {
		           MessageDispatcher.getInstance().dispatchMessage(0f, null, ui, BattleMessages.ATTACK);
		           return true;
		       } else {
		           attack.setChecked(true);
		           return true;
		       }
		   }
		});
		
		item.addListener(new InputListener() {
           @Override
           public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
               if (menu.getChecked() == item) {
                   MessageDispatcher.getInstance().dispatchMessage(0f, null, ui, BattleMessages.ITEM);
                   return true;
               } else {
                   item.setChecked(true);
                   return true;
               }
           }
        });
		
		defend.addListener(new InputListener() {
           @Override
           public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
               if (menu.getChecked() == defend) {
                   MessageDispatcher.getInstance().dispatchMessage(0f, null, ui, BattleMessages.DEFEND);
                   return true;
               } else {
                   defend.setChecked(true);
                   return true;
               }
           }
        });
		
		hide();
		act(1f);
	}
	
	/**
	 * Opens the menu and enables input
	 */
	public void show(){
	    attack.addAction(Actions.moveTo(getCenterX() - 100f, getCenterY() + 50f, .5f));
        item.addAction(Actions.moveTo(getCenterX(), getCenterY() - 50f, .5f));
        defend.addAction(Actions.moveTo(getCenterX() + 100f, getCenterY() + 50f, .5f));
        
        addAction(Actions.sequence(Actions.alpha(0f, .2f)));
        
        setTouchable(Touchable.childrenOnly);
	}
	
	/**
	 * Closes the menu and prevents further input
	 */
	public void hide(){
	    attack.addAction(Actions.moveTo(getCenterX(), getCenterY(), .5f));
	    item.addAction(Actions.moveTo(getCenterX(), getCenterY(), .5f));
	    defend.addAction(Actions.moveTo(getCenterX(), getCenterY(), .5f));
	    
	    addAction(Actions.sequence(Actions.delay(.3f), Actions.alpha(0f, .2f)));
	    
	    setTouchable(Touchable.disabled);
	}
}
