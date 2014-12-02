package scenes.dungeon;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.Telegram;

public enum GameState implements State<Scene> {
    Wander(){
        @Override
        public void enter(Scene entity) {
            entity.setUI(entity.wanderUI);
            Gdx.input.setInputProcessor(entity.wanderUI);
        }
        
        @Override
        public boolean onMessage(Scene entity, Telegram telegram) {
            if (telegram.message == Messages.FIGHT) {
                Entity opponent = (Entity)telegram.extraInfo;
                entity.transition.setEnemy(opponent);
                entity.statemachine.changeState(Battle);
                return true;
            }
            return false;
        }
    }, 
    Battle() {
        
        @Override
        public void enter(final Scene entity) {
            Gdx.input.setInputProcessor(null);
            entity.setUI(entity.transition);
            entity.transition.playAnimation(new Runnable(){

                @Override
                public void run() {
                    entity.statemachine.changeState(Wander);
                }
               
            });
        }
        
        @Override
        public boolean onMessage(Scene entity, Telegram telegram) {
            return false;
        }
    };
    
    @Override
    public void enter(Scene entity) {
        // TODO Auto-generated method stub

    }

    @Override
    public void update(Scene entity) {
        
    }

    @Override
    public void exit(Scene entity) {
        // TODO Auto-generated method stub

    }

    public class Messages {
        public static final int FIGHT = 0x3001;
        public static final int KILLED = 0x4001;
    }

}
