package scenes.town.ui;

import github.nhydock.ssm.ServiceManager;
import scene2d.ui.extras.Card;
import scene2d.ui.extras.FocusGroup;
import scenes.Messages;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import core.common.Input;
import core.components.Stats;
import core.components.Stats.Stat;
import core.datatypes.npc.Trainer;
import core.service.implementations.InputHandler;
import core.service.interfaces.IPlayerContainer;

public class TrainMenu {

    private Group menu;
    
    private Array<Card> cards;
    private Card selectedCard;
    private SacrificeSubmenu submenu;
    
    private FocusGroup cardGroup;
    
    private Trainer trainer;
    
    private Runnable resetFocus = new Runnable() {
        
        @Override
        public void run() {
            select(cards.first());
        }
    };
    
    private InputListener click = new InputListener(){
        @Override
        public void enter(InputEvent evt, float x, float y, int pointer, Actor fromActor){
            Card c = (Card)evt.getListenerActor();
            select(c);
        }
        
        @Override
        public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button){
            Card c = (Card)evt.getListenerActor();
            setTrainer(c, (Trainer)c.getUserObject());
            return true;
        }
    };
    
    private InputListener keys = new InputListener(){
        @Override
        public boolean keyDown(InputEvent evt, int keycode){
            if (!submenu.getGroup().isVisible() && menu.isVisible()){
                if (Input.LEFT.match(keycode)){
                    cardGroup.prev();
                    select((Card)cardGroup.getFocused());
                    return true;
                }
                if (Input.RIGHT.match(keycode)){
                    cardGroup.next();
                    select((Card)cardGroup.getFocused());
                    return true;
                }
                if (Input.ACCEPT.match(keycode)){
                    setTrainer(selectedCard, (Trainer)selectedCard.getUserObject());
                    return true;
                }
            }
            return false;
        }
    };
    
    public TrainMenu(Skin skin, SacrificeSubmenu sacrificeSubmenu){
        menu = new Group();
        menu.setWidth(700);
        menu.setHeight(350);
        
        cardGroup = new FocusGroup();
        //cards for displaying the description
        {
            cards = new Array<Card>();
            for (int i = 0; i < Stat.values().length; i++){
                Stat s = Stat.values()[i];
                Card card = new Card(skin, s.name());
                card.setPosition(getWidth()*.25f*i, 0, Align.left);
                card.setUserObject(new Trainer(s));
                card.addListener(click);
                card.setOrigin(Align.center);
                card.setHeight(350);
                card.setScale(.75f);
                menu.addActor(card);
                cards.add(card);
                cardGroup.add(card);
            }
        }

        submenu = sacrificeSubmenu;
        submenu.getGroup().setVisible(false);
        menu.addActor(submenu.getGroup());
        menu.addListener(keys);
        
        //start view as invisible
        menu.setColor(1f, 1f, 1f, 0f);
        menu.setOrigin(Align.center);
        menu.setTouchable(Touchable.disabled);
        menu.setVisible(false);
    }

    protected void select(Card a){
        if (a == selectedCard){
            return;
        }
        if (submenu.getGroup().isVisible()){
            return;
        }
        
        if (selectedCard != null){
            selectedCard.addAction(Actions.scaleTo(.75f, .75f, .15f));
        }
        
        a.clearActions();
        a.addAction(Actions.scaleTo(1f, 1f, .15f));
        selectedCard = a;
    }
    
    protected void setTrainer(Card card, Trainer trainer){
        if (this.trainer == null && trainer != null){
            for (Card c : cards){
                c.clearActions();
                c.addAction(Actions.parallel(Actions.scaleTo(.5f, .5f, .2f), Actions.alpha(0f, .2f)));
            }
            card.clearActions();
            card.addAction(Actions.moveToAligned(0, 0, Align.bottomLeft, .2f, Interpolation.circleOut));
            IPlayerContainer playerService = ServiceManager.getService(IPlayerContainer.class);
            Stats stats = Stats.Map.get(playerService.getPlayer());
            submenu.setPrompt("For " + trainer.calcPoints(stats) + " items you can improve your " + trainer.getTrainingType().name() 
                              + ".\n\nThe Aztec Gods of Gains say they'd really like some " + trainer.getBonus());
            
            InputHandler input = ServiceManager.getService(InputHandler.class);
            menu.addAction(
                Actions.sequence(
                    Actions.run(input.disableMe), 
                    submenu.show(), 
                    Actions.delay(.2f), 
                    Actions.run(input.enableMe)
                )
            );
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Focus, submenu.getGroup());
        }
        this.trainer = trainer;
    }
    
    public Trainer getTrainer(){
        return trainer;
    }
    
    float getWidth() {
        return menu.getWidth();
    }
    
    float getHeight() {
        return menu.getHeight();
    }
    
    /**
     * Show initial state with cards
     */
    public void show() {
        for (int i = 0; i < cards.size; i++){
            Card c = cards.get(i);
            c.clearActions();
            c.addAction(
                Actions.parallel(
                    Actions.moveToAligned(getWidth()*.25f*i, 0, Align.bottomLeft), 
                    Actions.alpha(1f),
                    Actions.scaleTo(.75f, .75f)
                )
            );
        }
        InputHandler input = ServiceManager.getService(InputHandler.class);
        menu.addAction(
            Actions.sequence(
                Actions.visible(true),
                Actions.scaleTo(.5f, .5f),
                Actions.alpha(0f),
                Actions.run(input.disableMe),
                Actions.addAction(
                    Actions.sequence(
                        Actions.parallel(
                                Actions.moveToAligned(170, getHeight()/2f, Align.left),
                                Actions.alpha(0f)
                        ),
                        Actions.visible(false)
                    ),
                    submenu.getGroup()
                ),
                Actions.parallel(
                        Actions.alpha(1f, .15f),
                        Actions.scaleTo(1f, 1f, .25f, Interpolation.circleOut)
                ),
                Actions.run(resetFocus),
                Actions.run(input.enableMe),
                Actions.touchable(Touchable.childrenOnly)
            )
        );
        submenu.getGroup().setTouchable(Touchable.disabled);
    }
    
    /**
     * Hide all elements
     */
    public void hide() {
        trainer = null;
        submenu.reset();
        InputHandler input = ServiceManager.getService(InputHandler.class);
        menu.addAction(
            Actions.sequence(
                Actions.touchable(Touchable.disabled),
                Actions.run(input.disableMe),
                Actions.parallel(
                        Actions.alpha(0f, .2f),
                        Actions.scaleTo(2f, 2f, .3f, Interpolation.circleOut)
                ),
                Actions.visible(false),
                Actions.addAction(Actions.visible(false), submenu.getGroup()),
                Actions.run(input.enableMe)
            )
        );
    }

    public Group getGroup() {
        return menu;
    }
}
