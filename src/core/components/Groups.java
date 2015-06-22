package core.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.Entity;
import com.badlogic.ashley.core.Family;

import core.datatypes.npc.NPC;
import core.datatypes.npc.Trainer;

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
    public static final Family npcType = Family.all(NPC.class).get();
    
    public static class Monster extends Component {

        public static final String Loot = "treasure chest";
        public static final String Door = "door";
        public static final String Key = "key";
        public static final String Mimic = "mimic";
        public static final String DoorMimic = "domimic";
        
        public static boolean isLoot(Entity e) {
            Identifier id = Identifier.Map.get(e);
            String name = id.toString();
            return name.endsWith(Monster.Loot);
        }
        
        public static boolean isDoor(Entity e) {
            Identifier id = Identifier.Map.get(e);
            String name = id.toString();
            return name.endsWith(Monster.Door);
        }
        
        public static boolean isKey(Entity e) {
            Identifier id = Identifier.Map.get(e);
            String name = id.toString();
            return name.endsWith(Monster.Key);
        }
        
        public static boolean isObject(Entity e) {
            Identifier id = Identifier.Map.get(e);
            String name = id.toString();
            return name.endsWith(Monster.Loot) || name.endsWith(Monster.Key) || name.endsWith(Monster.Door);
        }
        
        public static boolean isMimic(Entity e) {
            Identifier id = Identifier.Map.get(e);
            String name = id.toString();
            return name.endsWith(Monster.Mimic) || name.endsWith(Monster.DoorMimic);
        }
    }

    public static class Boss extends Component {
        
    }
    
    public static class Player extends Component {}
}