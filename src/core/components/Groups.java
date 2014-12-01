package core.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Family;

/**
 * Simple grouping identifier for monsters
 * 
 * @author nhydock
 *
 */
public class Groups {

    public static final Family monsterType = Family.all(Monster.class).get();
    public static final Family bossType = Family.all(Boss.class).get();
    public static final Family playerType = Family.all(Player.class).get();
    
    
    public static class Monster extends Component {

        public static final String Loot = "treasure chest";
    
    }

    public static class Boss extends Component {
        
    }
    
    public static class Player extends Component {}
}