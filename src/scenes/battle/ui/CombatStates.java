package scenes.battle.ui;

import scene2d.InputDisabler;
import scenes.Messages;
import scenes.battle.ui.CombatHandler.Combatant;
import scenes.battle.ui.CombatHandler.Turn;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.utils.Align;

public enum CombatStates implements State<BattleUI> {
    MAIN(){
        @Override
        public void enter(final BattleUI entity) {
            entity.boss.addAction(Actions.moveBy(-80f, 0f, .3f));
            entity.player.addAction(Actions.moveBy(80f, 0f, .3f));
            entity.player.addAction(Actions.addAction(Actions.run(new Runnable() {
                
                @Override
                public void run() {
                    entity.mainmenu.show();
                }
            }), entity.mainmenu));
            entity.setFocus(entity.mainmenu);
        }
        
        @Override
        public void exit(final BattleUI entity) {
            entity.mainmenu.hide();
            entity.boss.addAction(Actions.moveBy(80f, 0f, .3f));
            entity.player.addAction(Actions.moveBy(-80f, 0f, .3f));
        }

        @Override
        public boolean onMessage(final BattleUI entity, Telegram telegram) {
            // TODO Auto-generated method stub
            return false;
        }
    },
    MANUAL(){

        @Override
        public boolean onMessage(BattleUI entity, Telegram telegram) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void enter(final BattleUI entity) {
            final Runnable returnToState = new Runnable(){

                @Override
                public void run() {
                    //advance the battle once both sides have attacked
                    MessageDispatcher.getInstance().dispatchMessage(null, null, Messages.Battle.ADVANCE);
                    entity.changeState(MAIN);
                }
                
            };
            
            /*
             * Pretty much everything can be done as soon as we enter
             */
            InputDisabler.swap();
            
            /*
             * First we roll the dice, then show the values 
             */
            final Turn t = entity.combat.manualFightRoll(Combatant.Player);
            
            Runnable after = new Runnable() {

                @Override
                public void run() {
                    entity.setFocus(entity.timeline);
                    InputDisabler.swap();
                    
                    entity.buildTimeline(t, returnToState);
                }
                
            };
            
            entity.playRollAnimation(t, Actions.run(after));
        }
        
    },
    AUTO(){
        
        @Override
        public void enter(final BattleUI entity) {
            final Runnable returnToState = new Runnable(){

                @Override
                public void run() {
                    //advance the battle once both sides have attacked
                    MessageDispatcher.getInstance().dispatchMessage(null, null, Messages.Battle.ADVANCE);
                    InputDisabler.swap();
                    entity.changeState(MAIN);
                }
                
            };
            
            /*
             * Pretty much everything can be done as soon as we enter
             */
            InputDisabler.swap();
            
            /*
             * First we roll the dice, then show the values 
             */
            final Turn t = entity.combat.fightRoll(Combatant.Player);
            
            Runnable after = new Runnable() {

                @Override
                public void run() {
                    entity.combat.fight(t);
                    
                    if (entity.combat.isFoeStunned()) {
                        entity.addAction(Actions.sequence(Actions.delay(2f), Actions.run(returnToState)));
                    }
                    else
                    {
                        entity.addAction(
                            Actions.sequence(
                                Actions.delay(1f), 
                                Actions.run(
                                    new Runnable() {
                                        @Override
                                        public void run() {
                                            final Turn t2 = entity.combat.fightRoll(Combatant.Enemy);
                                            entity.playFightAnimation(t2, 
                                                new Runnable() {
            
                                                    @Override
                                                    public void run() {
                                                        entity.combat.fight(t2);
                
                                                        entity.addAction(
                                                            Actions.sequence(
                                                                Actions.delay(2f),
                                                                Actions.run(returnToState)
                                                            )
                                                        );
                                                    }
                                                }
                                            );
                                        }
                                    }
                                )
                            )
                        );
                    }
                }
            };
            
            entity.playFightAnimation(t, after);
        }
        
        @Override
        public boolean onMessage(BattleUI entity, Telegram telegram) {
            
            
            return false;
        }
        
    },
    FORCE(){

        @Override
        public boolean onMessage(BattleUI entity, Telegram telegram) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void enter(BattleUI entity) {
            // TODO Auto-generated method stub
            
        }
        
    },
    MODIFY(){

        @Override
        public boolean onMessage(BattleUI entity, Telegram telegram) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void enter(BattleUI entity) {
            entity.sacrificePrompt.setText("By sacrificing an item, you can compound its modifier's effects onto the boss for a limited amount of time.");
            entity.sacrificePromptWindow.addAction(
                Actions.parallel(
                    Actions.alpha(1f, .1f),
                    Actions.moveTo(20, 220, .2f)
                )
            );
            entity.itemPane.addAction(Actions.moveBy(-entity.itemPane.getWidth(), 0, .2f, Interpolation.circleOut));
            entity.sacrificeButton.addAction(
                    Actions.sequence(
                        Actions.delay(.2f),
                        Actions.moveToAligned(entity.getDisplayWidth() - 20, 20f, Align.bottomRight, .2f, Interpolation.circleOut)
                    )
            );
        }
    },
    DEFEND(){

        @Override
        public boolean onMessage(BattleUI entity, Telegram telegram) {
            return false;
        }

        @Override
        public void enter(final BattleUI entity) {
            InputDisabler.swap();
            final Turn t = entity.combat.defendRoll();
            final Runnable returnToState = new Runnable(){

                @Override
                public void run() {
                    //advance the battle once both sides have attacked
                    MessageDispatcher.getInstance().dispatchMessage(null, null, Messages.Battle.ADVANCE);
                    InputDisabler.swap();
                    entity.changeState(MAIN);
                }
                
            };
            
            Runnable after = new Runnable() {

                @Override
                public void run() {
                    entity.combat.defend(t);
                    
                    entity.addAction(
                        Actions.sequence(
                            Actions.delay(1f), 
                            Actions.run(returnToState)
                        )
                    );
                }
                
            };
            
            entity.playDefenseAnimation(t, after);
        }
        
    };
    
    @Override
    public void update(BattleUI entity) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void exit(BattleUI entity) {
        // TODO Auto-generated method stub
        
    }
    
}
