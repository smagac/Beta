package scenes.dungeon.ui;

import github.nhydock.ssm.ServiceManager;

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
import core.components.Stats;
import core.components.Stats.Stat;
import core.datatypes.Item;
import core.datatypes.npc.Trainer;
import core.service.interfaces.IPlayerContainer;
import scene2d.InputDisabler;
import scene2d.ui.extras.Card;
import scenes.Messages;
import scenes.SacrificeSubmenu;

public class TrainMenu {

    private static final String HEALFMT = "";
    
    private Group menu;
    
    private Card card;
    
    private SacrificeSubmenu submenu;
    
    private Trainer trainer;
    
    public TrainMenu(Skin skin, SacrificeSubmenu sacrificeSubmenu){
        menu = new Group();
        menu.setWidth(720);
        menu.setHeight(400);
        
        //cards for displaying the description
        {
            card = new Card(skin, "");
            card.setIcon("villager");    
            card.setDescription("Hey kid, I see you're lookin' to get buff.\n \nWell you've come to the right trainer!\n \nGimme enough items and I'll see what we can make of that scrawny bod of yours");
            card.setTitle("TRAINING");
            card.setTitlePosition(Align.top);
            card.setPosition(0, getHeight()/2f, Align.left);
            card.setName("card");
            menu.addActor(card);
        }

        submenu = sacrificeSubmenu;
        
        //start view as invisible
        menu.setColor(1f, 1f, 1f, 0f);
        menu.setOrigin(Align.center);
        menu.setTouchable(Touchable.disabled);
    }
    
    public void setTrainer(Trainer trainer){
        if (trainer != null){
            IPlayerContainer playerService = ServiceManager.getService(IPlayerContainer.class);
            Stats stats = Stats.Map.get(playerService.getPlayer());
            submenu.setPrompt("For " + trainer.calcPoints(stats) + " items this man will train your " + trainer.getTrainingType().name() 
                              +".\n \nHe really likes " + trainer.getBonus());
        }
        this.trainer = trainer;
    }
    
    public Trainer getTrainer(){
        return trainer;
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
                            Actions.moveToAligned(200, getHeight()/2f, Align.left),
                            Actions.alpha(0f),
                            Actions.visible(false)
                    ),
                    submenu.getGroup()
                ),
                submenu.show(),
                Actions.parallel(
                        Actions.alpha(1f, .15f),
                        Actions.scaleTo(1f, 1f, .25f, Interpolation.circleOut)
                ),
                Actions.run(InputDisabler.instance)
            )
        );
        menu.setTouchable(Touchable.childrenOnly);
        submenu.getGroup().setTouchable(Touchable.childrenOnly);
    }
    
    private void clearActions(){
        menu.clearActions();
    }
    
    /**
     * Hide all elements
     */
    public void hide() {
        clearActions();
        trainer = null;
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
