package scenes.battle.ui;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;

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
            if (telegram.message == BattleMessages.ITEM) {
                //TODO fix up to go to the item menu
                entity.changeState(ATTACK);
            }
            
            // TODO Auto-generated method stub
            return false;
        }
    },
    ATTACK(){
        @Override
        public void enter(final BattleUI entity) {
            entity.addAction(Actions.sequence(Actions.delay(.8f), Actions.run(new Runnable(){

                @Override
                public void run() {
                    entity.playSacrificeAnimation(new Runnable() {
                        
                        @Override
                        public void run() {
                            entity.changeState(MAIN);
                        }
                    });
                }
                
            })));
            
        }

        @Override
        public boolean onMessage(BattleUI entity, Telegram telegram) {
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
        
    },
    AUTO(){

        @Override
        public boolean onMessage(BattleUI entity, Telegram telegram) {
            // TODO Auto-generated method stub
            return false;
        }
        
    },
    FORCE(){

        @Override
        public boolean onMessage(BattleUI entity, Telegram telegram) {
            // TODO Auto-generated method stub
            return false;
        }
        
    },
    MODIFY(){

        @Override
        public boolean onMessage(BattleUI entity, Telegram telegram) {
            // TODO Auto-generated method stub
            return false;
        }
        
    };
    
    @Override
    public void enter(BattleUI entity) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void update(BattleUI entity) {
        // TODO Auto-generated method stub
        
    }
    @Override
    public void exit(BattleUI entity) {
        // TODO Auto-generated method stub
        
    }
    
}
