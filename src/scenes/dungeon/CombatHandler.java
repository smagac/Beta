package scenes.dungeon;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.MathUtils;

import core.components.Combat;
import core.components.Equipment;
import core.components.Lock;
import core.components.Renderable;
import core.components.Stats;
import core.components.Groups.Monster;

/**
 * Standard bump combat handling of calculations
 * @author nhydock
 *
 */
class CombatHandler {

    static class Turn {
        public Turn(Entity a, Entity o) {
            attacker = a;
            opponent = o;
        }
        final Entity attacker;
        final Entity opponent;
    }
    
    static class Result {
        int damage;
        int exp;
        boolean killed;
        boolean critical;
    }
    
    /**
     * Make two entities fight
     * 
     * @param actor
     * @param opponent
     */
    protected static Result fight(Turn turn, Entity player) {
        Result result = new Result();
        Stats aStats = Stats.Map.get(turn.attacker);
        Stats bStats = Stats.Map.get(turn.opponent);
        Equipment equipment = Equipment.Map.get(player);
        
        final float MULT = (turn.attacker == player) ? 2 : 1.25f;

        if (MathUtils.randomBoolean(1f - (MathUtils.random(.8f, MULT) * bStats.getSpeed()) / 100f)) {
            float chance = MathUtils.random(.8f, MULT);
            int str = aStats.getStrength();
            int def = bStats.getDefense();
            if (turn.attacker == player) {
                str += equipment.getSword().getPower();
                if (!Monster.isObject(turn.opponent)) {
                    equipment.getSword().decay();
                }
            }
            else {
                //shield provides chance to block
                float pow = equipment.getShield().getPower(); 
                if (MathUtils.randomBoolean(pow / Equipment.Piece.MAX_POWER)) {
                    equipment.getShield().decay();
                    def += Integer.MAX_VALUE;    
                }
                //armor lessens damage if the shield doesn't blog
                else {
                    def += equipment.getArmor().getPower();
                    equipment.getArmor().decay();    
                }
            }
            result.damage = (int)Math.max(0, (chance * str) - def);

            if (turn.attacker == player) {
                result.critical = chance > MULT * .8f && result.damage > 0;   
            }
            
            bStats.hp -= result.damage;
        }
        else {
            result.damage = -1;
        }
        
        if (bStats.hp <= 0) {
            result.killed = true;
            result.exp = bStats.exp;
        }
        
        return result;
    }
    
    public static void markDead(Entity actor) {
        Renderable r = Renderable.Map.get(actor);
        r.setSpriteName("dead");
        r.setDensity(0);   
    }

    /**
     * Detects if an entity is "dead"/should be ignored
     * @param opponent
     * @return
     */
    public static boolean isDead(Entity opponent, Entity player) {
        
        Stats bStats = Stats.Map.get(opponent);

        // ignore if target died at some point along the way        
        if (bStats.hp <= 0 || (opponent != player && !Combat.Map.has(opponent))) {
            return true;
        }

        return false;
    }
    
    static void unlockDoor(Entity door) {
        Lock lock = Lock.Map.get(door);
        lock.unlocked = true;
        Renderable.Map.get(door).setSpriteName("opened");
        Renderable.Map.get(door).setDensity(0);
    }
}
