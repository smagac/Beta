package scenes.battle.ui;

import github.nhydock.ssm.SceneManager;
import github.nhydock.ssm.ServiceManager;
import scene2d.InputDisabler;
import scene2d.PlayBGM;
import scenes.Messages;
import scenes.battle.ui.CombatHandler.Combatant;
import scenes.battle.ui.CombatHandler.Turn;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.utils.Align;

import core.DataDirs;
import core.components.Combat;
import core.components.Stats;
import core.datatypes.Inventory;
import core.datatypes.Item;
import core.factories.AdjectiveFactory;
import core.service.interfaces.IDungeonContainer;

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
            entity.resetFocus();
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
                    if (entity.getCurrentState() == VICTORY) {
                        return;
                    }
                    
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
                    if (entity.getCurrentState() == VICTORY) {
                        return;
                    }
                    
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
    MODIFY(){

        @Override
        public boolean onMessage(final BattleUI entity, Telegram telegram) {
            if (telegram.message == Messages.Interface.Close) {
                entity.changeState(MAIN);
                return true;
            }
            if (telegram.message == Messages.Interface.Button) {
                exit(entity);
                final String adj = entity.inventory.getSelected().descriptor();
                entity.playSacrificeAnimation(new Runnable() {
                    
                    @Override
                    public void run() {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Battle.TARGET, entity.battleService.getBoss());
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Battle.MODIFY, adj);
                        entity.changeState(MAIN);
                    }
                });
                return true;
            }
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
                        Actions.moveToAligned(entity.getDisplayWidth(), 20f, Align.bottomRight, .2f, Interpolation.circleOut)
                    )
            );
            
            entity.resetFocus();
        }
        
        @Override
        public void exit(BattleUI entity) {
            entity.sacrificePromptWindow.addAction(
                Actions.parallel(
                    Actions.alpha(0f, .1f),
                    Actions.moveTo(20, 240, .2f)
                )
            );
            entity.itemPane.addAction(Actions.moveBy(entity.itemPane.getWidth(), 0, .2f, Interpolation.circleOut));
            entity.sacrificeButton.addAction(
                    Actions.sequence(
                        Actions.delay(.2f),
                        Actions.moveToAligned(entity.getDisplayWidth(), 20f, Align.bottomLeft, .2f, Interpolation.circleOut)
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
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Battle.ADVANCE);
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
        
    }, 
    VICTORY(){

        @Override
        public void enter(final BattleUI entity) {
            entity.addAction(
                Actions.sequence(
                    Actions.delay(10f),
                    Actions.run(new PlayBGM(entity.getManager().get(DataDirs.Audio + "victory.mp3", Music.class))),
                    Actions.delay(10f),
                    Actions.run(new Runnable(){

                        @Override
                        public void run() {
                            SceneManager.switchToScene("dungeon");
                        }
                        
                    })
                )
            );
        }

        @Override
        public boolean onMessage(BattleUI entity, Telegram telegram) {
            // TODO Auto-generated method stub
            return false;
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
