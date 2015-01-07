package scenes.battle.ui;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.utils.Array;

import core.common.Input;
import core.util.ListPointer;

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
    
    Array<Actor> main;
    ListPointer<Actor> mainSet;
    Image attack;
	Image attackManual;
	Image attackAuto;
    
	Array<Actor> attackMenu;
    ListPointer<Actor> attackSet;
    
	Image item;
	Image itemForce;
	Image itemModify;
	
    Image defend;
	
    private StateMachine<CrossMenu> sm;
	
	/**
	 * Generate the cross menu
	 * @param skin
	 */
	public CrossMenu(Skin skin, final BattleUI ui) {
		super();
		
		sm = new DefaultStateMachine<CrossMenu>(this);
		this.ui = ui;
		
		main = new Array<Actor>();
		mainSet = new ListPointer<Actor>(main, true);
		attackMenu = new Array<Actor>();
        attackSet = new ListPointer<Actor>(attackMenu, true);
        
		attack = new Image(skin, "attack");
		item = new Image(skin, "item");
		defend = new Image(skin, "defend");
		attack.setName("attack");
		item.setName("item");
		defend.setName("defend");
		
		main.add(attack);
		main.add(item);
		main.add(defend);
		
		for (int i = 0, y = 48; i < main.size; i++, y -= 48) {
		    Actor a = main.get(i);
		    a.setOrigin(Align.left);
		    a.setScale(1f);
		    a.setY(y);
		    a.addListener(new ButtonInput(a, this));
		    addActor(a);
		}
		
		attackAuto = new Image(skin, "auto");
		attackManual = new Image(skin, "manual");
        attackMenu.add(attackAuto);
        attackMenu.add(attackManual);
        for (int i = 0, y = 16; i < attackMenu.size; i++, y -= 32) {
            Actor a = attackMenu.get(i);
            a.setOrigin(Align.left);
            a.setScale(1f);
            a.setPosition(0, y, Align.bottomLeft);
            a.addAction(Actions.alpha(0f));
            a.addListener(new ButtonInput(a, this));
            addActor(a);
        }
        
		
		setSize(ui.getDisplayWidth(), ui.getDisplayHeight());
		act(1f);
		
		addListener(new InputListener(){
		   
		    @Override
		    public boolean keyDown(InputEvent evt, int keycode) {
		        ListPointer<Actor> set = mainSet;
		        if (sm.getCurrentState() == MenuState.ATTACK) {
		            set = attackSet;
		        }
		        
		        if (Input.UP.match(keycode)) {
		            MessageDispatcher.getInstance().dispatchMessage(0, null, sm, Messages.Select, set.peekPrev());
		            return true;
		        }
		        if (Input.DOWN.match(keycode)) {
		            MessageDispatcher.getInstance().dispatchMessage(0, null, sm, Messages.Select, set.peekNext());
		            return true;
		        }
		        if (Input.ACCEPT.match(keycode)) {
		            MessageDispatcher.getInstance().dispatchMessage(0, null, sm, Messages.Next, set.get());
		            return true;
		        }
		        if (Input.CANCEL.match(keycode)) {
		            MessageDispatcher.getInstance().dispatchMessage(0, null, sm, Messages.Prev);
                    return true;
                }
		        return false;
		    }
		});
	}
	
	/**
	 * Opens the menu and enables input
	 */
	public void show(){
	    setTouchable(Touchable.enabled);
        sm.changeState(MenuState.MAIN);
    }
	
	/**
	 * Closes the menu and prevents further input
	 */
	public void hide(){
	    attack.addAction(
            Actions.sequence(
                Actions.parallel(
                    Actions.moveToAligned(0, attack.getY(), Align.bottomLeft, .2f),
                    Actions.alpha(1f, .1f)
                )
            )
        );
        
	    attackAuto.addAction(
            Actions.sequence(
                Actions.parallel(
                    Actions.moveToAligned(0, attackAuto.getY(), Align.bottomLeft, .2f),
                    Actions.alpha(1f, .1f)
                )
            )
        );
    
	    attackManual.addAction(
            Actions.sequence(
                Actions.parallel(
                    Actions.moveToAligned(0, attackManual.getY(), Align.bottomLeft, .2f),
                    Actions.alpha(1f, .1f)
                )
            )
        );
        item.addAction(
            Actions.sequence(
                Actions.delay(.1f),
                Actions.parallel(
                    Actions.moveToAligned(0, item.getY(), Align.bottomLeft, .2f),
                    Actions.alpha(1f, .1f)
                )
            )
        );
        defend.addAction(
            Actions.sequence(
                Actions.delay(.2f),
                Actions.parallel(
                    Actions.moveToAligned(0, defend.getY(), Align.bottomLeft, .2f),
                    Actions.alpha(1f, .1f)
                )
            )
        );
	    
	    setTouchable(Touchable.disabled);
	}
	
	private static interface Messages {
	    static final int Select = 0x0001;
	    static final int Next   = 0x0002;
	    static final int Prev   = 0x0003;
	}
	
	private enum MenuState implements State<CrossMenu> {
	    MAIN(){
	        @Override
	        public void enter(CrossMenu entity) {
	            entity.mainSet.select(entity.attack);
	            entity.attack.addAction(
                    Actions.sequence(
                        Actions.delay(.7f),
                        Actions.parallel(
                            Actions.moveTo(-96f, entity.attack.getY(), .3f, Interpolation.circleOut),
                            Actions.alpha(1f, .2f)
                        )
                    )
                );
	            
	            entity.item.addAction(
                    Actions.sequence(
                        Actions.delay(.8f),
                        Actions.parallel(
                            Actions.moveTo(-32f, entity.item.getY(), .3f, Interpolation.circleOut),
                            Actions.alpha(1f, .2f)
                        )
                    )
                );
	            
	            entity.defend.addAction(
                    Actions.sequence(
                        Actions.delay(.9f),
                        Actions.parallel(
                            Actions.moveTo(-32f, entity.defend.getY(), .3f, Interpolation.circleOut),
                            Actions.alpha(1f, .2f)
                        )
                    )
                );
	        }
	        
	        @Override
	        public boolean onMessage(CrossMenu entity, Telegram telegram) {
	            if (telegram.message == Messages.Select) {
	                if (telegram.extraInfo == entity.attack ||
	                    telegram.extraInfo == entity.defend ||
	                    telegram.extraInfo == entity.item) {
	                    Actor old = entity.mainSet.get();
	                    old.addAction(Actions.moveTo(-32f, old.getY(), .2f, Interpolation.circleOut));
	                    Actor selected = entity.mainSet.select((Actor)telegram.extraInfo);
	                    selected.addAction(Actions.moveTo(-96f, selected.getY(), .2f, Interpolation.circleOut));
	                }
	                return true;
	            }
	            if (telegram.message == Messages.Next) {
	                if (entity.mainSet.get() == telegram.extraInfo) {
	                    if (telegram.extraInfo == entity.attack) {
	                        entity.sm.changeState(ATTACK);
	                    }
	                    if (telegram.extraInfo == entity.item) {
	                        entity.sm.changeState(ITEM);
	                    }
	                    return true;
	                }
	            }
	            return false;
	        }
	    },
	    ATTACK(){
	        @Override
            public void enter(CrossMenu entity) {
	            entity.item.addAction(
                    Actions.sequence(
                        Actions.parallel(
                            Actions.moveTo(0f, entity.item.getY(), .3f, Interpolation.circleOut),
                            Actions.alpha(0f, .2f)
                        )
                    )
                );
                
                entity.defend.addAction(
                    Actions.sequence(
                        Actions.delay(.1f),
                        Actions.parallel(
                            Actions.moveTo(0f, entity.defend.getY(), .3f, Interpolation.circleOut),
                            Actions.alpha(0f, .2f)
                        )
                    )
                );
                
                entity.attackAuto.addAction(
                    Actions.sequence(
                        Actions.delay(.3f),
                        Actions.parallel(
                            Actions.moveTo(-entity.attackAuto.getWidth() - 16, entity.attackAuto.getY(), .3f, Interpolation.circleOut),
                            Actions.alpha(1f, .2f)
                        )
                    )
                );
                
                entity.attackManual.addAction(
                    Actions.sequence(
                        Actions.delay(.4f),
                        Actions.parallel(
                            Actions.moveTo(-entity.attackManual.getWidth(), entity.attackManual.getY(), .3f, Interpolation.circleOut),
                            Actions.alpha(1f, .2f)
                        )
                    )
                );
                
                entity.attackSet.select(entity.attackAuto);
            }
	        
	        @Override
            public void exit(CrossMenu entity) {  
	            entity.attackAuto.addAction(
                    Actions.sequence(
                        Actions.delay(0f),
                        Actions.parallel(
                            Actions.moveTo(0, entity.attackAuto.getY(), .3f, Interpolation.circleOut),
                            Actions.alpha(0f, .2f)
                        )
                    )
                );
                
                entity.attackManual.addAction(
                    Actions.sequence(
                        Actions.delay(.1f),
                        Actions.parallel(
                            Actions.moveTo(0, entity.attackManual.getY(), .3f, Interpolation.circleOut),
                            Actions.alpha(0f, .2f)
                        )
                    )
                );
            }

            @Override
            public boolean onMessage(CrossMenu entity, Telegram telegram) {
                if (telegram.message == Messages.Prev ||
                    (telegram.message == Messages.Next && telegram.extraInfo == entity.attack)) {
                    entity.sm.changeState(MAIN);
                    return true;
                }
                if (telegram.message == Messages.Select) {
                    if (telegram.extraInfo == entity.attackAuto ||
                        telegram.extraInfo == entity.attackManual) {
                        Actor old = entity.attackSet.get();
                        if (old != telegram.extraInfo) {
                            old.addAction(Actions.moveTo(-old.getWidth(), old.getY(), .2f, Interpolation.circleOut));
                        }
                        Actor selected = entity.attackSet.select((Actor)telegram.extraInfo);
                        selected.addAction(Actions.moveTo(-(old.getWidth() + 16), selected.getY(), .2f, Interpolation.circleOut));
                        return true;
                    }
                    if (telegram.extraInfo == entity.attack) {
                        Actor old = entity.attackSet.get();
                        old.addAction(Actions.moveTo(-old.getWidth(), old.getY(), .2f, Interpolation.circleOut));
                        return true;
                    }
                }
                if (telegram.message == Messages.Next && telegram.extraInfo == entity.attackAuto) {
                    entity.ui.changeState(CombatStates.AUTO);
                    return true;
                }
                
                return false;
            }
	    },
	    ITEM(){
	        @Override
            public void enter(final CrossMenu entity) {
	            entity.hide();
                entity.ui.playSacrificeAnimation(new Runnable() {
                    
                    @Override
                    public void run() {
                        entity.show();
                    }
                });
            }
	        
	        @Override
	        public void exit(CrossMenu entity) {  
	            
	        }

            @Override
            public boolean onMessage(CrossMenu entity, Telegram telegram) {
                // TODO Auto-generated method stub
                return false;
            }
	    };

        

        @Override
        public void update(CrossMenu entity) { }

        @Override
        public void exit(CrossMenu entity) {  }
	    
	}
	
    private class ButtonInput extends InputListener {
        Actor button;
        CrossMenu menu;
        
        public ButtonInput(Actor focus, CrossMenu menu) {
            this.button = focus;
            this.menu = menu;
        }
        
        @Override
        public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
            MessageDispatcher.getInstance().dispatchMessage(0f, null, menu.sm, Messages.Next, this.button);
            return true;
        }
        
        @Override
        public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
            MessageDispatcher.getInstance().dispatchMessage(0f, null, menu.sm, Messages.Select, this.button);
        }
    }
}
