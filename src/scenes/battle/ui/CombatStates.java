package scenes.battle.ui;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
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
        
    },
    AUTO(){
        
        @Override
        public void enter(final BattleUI entity) {
            /*
             * Pretty much everything can be done as soon as we enter
             */
            
            /*
             * First we roll the dice, then show the values 
             */
            int bossRoll = MathUtils.random(1, 3);
            int playerRoll = MathUtils.random(1,3);
            
            Runnable after = new Runnable() {

                @Override
                public void run() {
                    int bossRoll = MathUtils.random(1, 3);
                    int playerRoll = MathUtils.random(1,3);
                    
                    Runnable after = new Runnable(){

                        @Override
                        public void run() {
                            entity.changeState(MAIN);
                        }
                        
                    };
                    entity.playFightAnimation(false, playerRoll, bossRoll, after);
                }
                
            };
            
            entity.playFightAnimation(true, playerRoll, bossRoll, after);
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
