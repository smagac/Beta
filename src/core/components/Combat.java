package core.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;

import core.components.Equipment.Piece;
import core.datatypes.Item;

public class Combat extends Component {
    public static final ComponentMapper<Combat> Map = ComponentMapper.getFor(Combat.class);
    
    String die;
    float moveChance;
    float agroMoveChance;
    boolean agro;
    boolean passive;

    public Combat(float norm, float agro, boolean passive, String deathMessage) {
        this.moveChance = norm;
        this.agroMoveChance = agro;
        this.die = deathMessage;
    }

    /**
     * Get the chance that this enemy moves
     * 
     * @return
     */
    public float getMovementRate() {
        return (agro) ? agroMoveChance : moveChance;
    }

    public boolean isPassive() {
        return passive;
    }

    /**
     * Forcibly aggress an enemy, 'causing it to drop its passive state
     */
    public void aggress() {
        passive = false;
        agro = true;
    }

    public boolean isAgro() {
        return agro;
    }

    public String getDeathMessage(String enemyName) {
        return String.format(die, enemyName);
    }

    public void calm() {
        agro = false;
    }
}
