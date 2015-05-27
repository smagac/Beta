package core.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.ai.fsm.StateMachine;

public class Combat extends Component {
    public static final ComponentMapper<Combat> Map = ComponentMapper.getFor(Combat.class);
    
    StateMachine<Entity> movementAI;
    String die;
    float moveChance;
    float agroMoveChance;
    boolean passive;

    public Combat(float norm, float agro, boolean passive, String deathMessage) {
        this.moveChance = norm;
        this.agroMoveChance = agro;
        this.die = deathMessage;
    }
    
    public void initAI(StateMachine<Entity> sm){
        this.movementAI = sm;
    }
    
    public StateMachine<Entity> getAI(){
        return movementAI;
    }

    /**
     * Get the chance that this enemy moves
     * 
     * @return
     */
    public float getMovementRate(boolean agro) {
        return (agro) ? agroMoveChance : moveChance;
    }

    public boolean isNaturallyPassive() {
        return passive;
    }

    public String getDeathMessage(String enemyName) {
        return String.format(die, enemyName);
    }
}
