package scenes.dungeon.ui;

import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Pool;
import com.badlogic.gdx.utils.Pools;

import core.common.Input;
import core.datatypes.Item;
import core.service.interfaces.IPlayerContainer;
import scene2d.InputDisabler;
import scene2d.ui.extras.Card;
import scenes.Messages;

public class AssistMenu {

    private static final String HEALFMT = "So you'd like me to heal you?\n \n%d loot recovers hp\n%d loot cures ailments.";
    private static final String LEAVEFMT = "Each floor deep you are costs another piece of loot.\n \nYou're currently %d floors deep.";
    
    private Group menu;
    
    private Card healCard;
    private Card leaveCard;
    
    private SacrificeSubmenu submenu;
    
    public AssistMenu(Skin skin, SacrificeSubmenu sacrificeSubmenu){
        menu = new Group();
        menu.setWidth(720);
        menu.setHeight(400);
        
        //cards for choosing menu
        {
            healCard = new Card(skin, "Heal", "Sacrifice items to recover all of your hp and/or status ailments", "god");
            healCard.setPosition(getWidth()/2f - 20, getHeight()/2f, Align.right);
            healCard.setName("healName");
            healCard.addListener(new InputListener(){
                @Override
                public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Heal);
                    return true;
                }
            });
            menu.addActor(healCard);
            
            leaveCard = new Card(skin, "Escape", "Sacrifice items to instantly escape from this dungeon with all of your loot", "up");
            leaveCard.setPosition(getWidth()/2f + 20, getHeight()/2f, Align.left);
            leaveCard.setName("leaveName");
            leaveCard.addListener(new InputListener(){
                @Override
                public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Leave);
                    return true;
                }
            });
            menu.addActor(leaveCard);
        }

        submenu = sacrificeSubmenu;
        
        //start view as invisible
        menu.setColor(1f, 1f, 1f, 0f);
        menu.setOrigin(Align.center);
        menu.setTouchable(Touchable.disabled);
    }
    
    private float getWidth() {
        return menu.getWidth();
    }
    
    private float getHeight() {
        return menu.getHeight();
    }
    
    /**
     * Show initial state with cards
     */
    public void show() {
        clearActions();
        menu.addActor(submenu.getGroup());
        menu.addAction(
            Actions.sequence(
                Actions.scaleTo(.7f, .7f),
                Actions.alpha(0f),
                Actions.run(InputDisabler.instance),
                Actions.addAction(
                    Actions.parallel(
                        Actions.alpha(1f),
                        Actions.moveToAligned(getWidth()/2f - 20, getHeight()/2f, Align.right)
                    ),
                    healCard
                ),
                Actions.addAction(
                    Actions.parallel(
                        Actions.alpha(1f),
                        Actions.moveToAligned(getWidth()/2f + 20, getHeight()/2f, Align.left)
                    ), 
                    leaveCard
                ),
                Actions.addAction(
                    Actions.parallel(
                            Actions.moveToAligned(200, getHeight()/2f, Align.left),
                            Actions.alpha(0f),
                            Actions.visible(false)
                    ),
                    submenu.getGroup()
                ),
                Actions.parallel(
                        Actions.alpha(1f, .15f),
                        Actions.scaleTo(1f, 1f, .25f, Interpolation.circleOut)
                ),
                Actions.run(InputDisabler.instance)
            )
        );
        menu.setTouchable(Touchable.childrenOnly);
        submenu.getGroup().setTouchable(Touchable.childrenOnly);
        healCard.setTouchable(Touchable.enabled);
        leaveCard.setTouchable(Touchable.enabled);
    }
    
    /**
     * Show all elements and prompt associated with healing
     */
    public void showHeal(int cost) {
        clearActions();
        
        menu.addAction(
            Actions.sequence(
                Actions.run(InputDisabler.instance),
                Actions.addAction(
                    Actions.moveToAligned(0, getHeight()/2f, Align.left, .3f, Interpolation.circleOut),
                    healCard
                ),
                Actions.addAction(
                    Actions.alpha(0f, .15f),
                    leaveCard
                ),
                Actions.delay(.25f),
                submenu.show(),
                Actions.run(InputDisabler.instance)
            )
        );

        submenu.setPrompt(String.format(HEALFMT, cost, cost * 2));
    }
    
    /**
     * Show all elements and prompt associated with escaping
     */
    public void showEscape(int cost) {
        clearActions();
        
        menu.addAction(
            Actions.sequence(
                Actions.run(InputDisabler.instance),
                Actions.addAction(
                    Actions.moveToAligned(0, getHeight()/2f, Align.left, .3f, Interpolation.circleOut),
                    leaveCard
                ),
                Actions.addAction(
                    Actions.alpha(0f, .15f),
                    healCard
                ),
                Actions.delay(.25f),
                submenu.show(),
                Actions.run(InputDisabler.instance)
            )
        );
        
        submenu.setPrompt(String.format(LEAVEFMT, cost));
    }
    
    private void clearActions(){
        healCard.clearActions();
        leaveCard.clearActions();
        menu.clearActions();
        
        healCard.setTouchable(Touchable.disabled);
        leaveCard.setTouchable(Touchable.disabled);
    }
    
    /**
     * Hide all elements
     */
    public void hide() {
        clearActions();
        submenu.reset();
        menu.addAction(
            Actions.sequence(
                Actions.run(InputDisabler.instance),
                Actions.parallel(
                        Actions.alpha(0f, .2f),
                        Actions.scaleTo(2f, 2f, .3f, Interpolation.circleOut)
                ),
                Actions.addAction(Actions.visible(false), submenu.getGroup()),
                Actions.removeActor(submenu.getGroup()),
                Actions.run(InputDisabler.instance)
            )
        );
        menu.setTouchable(Touchable.disabled);
    }

    public Group getGroup() {
        return menu;
    }
}
