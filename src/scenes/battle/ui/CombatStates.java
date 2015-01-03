package scenes.battle.ui;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;

public enum CombatStates implements State<BattleUI> {
    MAIN(){
        @Override
        public void enter(final BattleUI entity) {
            entity.boss.addAction(Actions.moveBy(-50f, 0f, .3f));
            entity.player.addAction(Actions.moveBy(50f, 0f, .3f));
            entity.player.addAction(Actions.addAction(Actions.run(new Runnable() {
                
                @Override
                public void run() {
                    entity.mainmenu.show();
                }
            }), entity.mainmenu));
        }
        
        @Override
        public void exit(final BattleUI entity) {
            entity.mainmenu.addAction(Actions.sequence(
                    Actions.alpha(0f, .2f),
                    Actions.run(new Runnable(){

                        @Override
                        public void run() {
                            entity.mainmenu.setVisible(false);
                        }
                        
                    })
            ));
        }
    },
    ATTACK(){
        @Override
        public void enter(BattleUI entity) {
            // TODO Auto-generated method stub
            
        }
    },
    MANUAL(){
        
    },
    AUTO(){
        
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
    @Override
    public boolean onMessage(BattleUI entity, Telegram telegram) {
        // TODO Auto-generated method stub
        return false;
    };
    
}
