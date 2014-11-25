package core.components;

import com.artemis.Component;

import core.datatypes.Item;

public class Combat extends Component {

    String die;
    float moveChance;
    float agroMoveChance;
    boolean agro;
    boolean passive;
    Item itemDrop;

    public Combat(float norm, float agro, boolean passive, Item drop, String deathMessage) {
        this.moveChance = norm;
        this.agroMoveChance = agro;
        this.die = deathMessage;
        this.itemDrop = drop;
    }

    /**
     * Get the chance that this enemy moves
     * 
     * @return
     */
    public float getMovementRate() {
        return (agro) ? agroMoveChance : moveChance;
    }

    /**
     * @return get the loot dropped when the entity is killed
     */
    public Item getDrop() {
        return itemDrop;
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
