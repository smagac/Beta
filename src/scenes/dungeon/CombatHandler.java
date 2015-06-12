package scenes.dungeon;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;

import core.components.Combat;
import core.components.Equipment;
import core.components.Groups.Monster;
import core.components.Lock;
import core.components.Renderable;
import core.components.Stats;
import core.datatypes.Ailment;
import core.datatypes.Ailment.AilmentModifier;

/**
 * Standard bump combat handling of calculations
 * @author nhydock
 *
 */
class CombatHandler {

    static class Result {
        int damage;
        int exp;
        boolean killed;
        boolean critical;
        Array<Ailment> inflicted;
    }
    
    /**
     * Make two entities fight
     * 
     * @param actor
     * @param opponent
     */
    protected static Result fight(final Entity attacker, final Entity opponent, Entity player) {
        Result result = new Result();
        Stats aStats = Stats.Map.get(attacker);
        Stats bStats = Stats.Map.get(opponent);
        Equipment equipment = Equipment.Map.get(player);
        
        final float MULT = (attacker == player) ? 2 : 1.25f;

        if (MathUtils.randomBoolean(1f - (MathUtils.random(.8f, MULT) * bStats.getSpeed()) / 100f)) {
            float chance = MathUtils.random(.8f, MULT);
            int str = aStats.getStrength();
            int def = bStats.getDefense();
            if (attacker == player) {
                str += equipment.getSword().getPower();
                if (!Monster.isObject(opponent)) {
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

            if (attacker == player) {
                result.critical = chance > MULT * .8f && result.damage > 0;   
            }
            
            //inflict status effects 
            if (result.damage > 0 && attacker != player) {
                Combat combat = Combat.Map.get(attacker);
                result.inflicted = new Array<Ailment>();
                AilmentModifier am = combat.getAilments();
                for (Ailment a : Ailment.ALL) {
                    if (MathUtils.randomBoolean(am.getChance(a))) {
                        result.inflicted.add(a);
                    }
                }
            }
            
            bStats.hp = Math.max(0, bStats.hp - result.damage);
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
    
    /**
     * Spell casting attack.  Spells deal with raw strength, so equipment has no effect.  They are
     * unblockable and ignore the opponent's defense.
     * @param attacker
     * @param opponent
     * @param player
     * @return
     */
    protected static Result magic(final Entity attacker, final Entity opponent, Entity player) {
        Result result = new Result();
        Stats aStats = Stats.Map.get(attacker);
        Stats bStats = Stats.Map.get(opponent);
        
        final float MULT = (attacker == player) ? 2 : 1.25f;

        float chance = MathUtils.random(.8f, MULT);
        int str = aStats.getStrength();
        result.damage = (int)Math.max(0, (chance * str));

        if (attacker == player) {
            result.critical = chance > MULT * .8f && result.damage > 0;   
        }
        
        bStats.hp = Math.max(0, bStats.hp - result.damage);
        
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
