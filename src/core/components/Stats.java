package core.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Json.Serializable;
import com.badlogic.gdx.utils.JsonValue;

import core.datatypes.StatModifier;

/**
 * Component consisting of the stats that an entity may have, representing it
 * in the field.
 * @author nhydock
 *
 */
public class Stats extends Component implements Serializable {

    public static enum Stat {
        STRENGTH, DEFENSE, SPEED, VITALITY;
    }
    
    public static final ComponentMapper<Stats> Map = ComponentMapper.getFor(Stats.class);

    private int level;
    public int hp;
    public int maxhp;
    private int strength;
    private int defense;
    private int speed;
    private int timesTrained;
    public boolean hidden;

    private int[] baseStats;
    
    /**
     * Keep track of 
     */
    private Array<StatModifier> mods = new Array<StatModifier>();

    private int spells;
    
    public Stats() { hp = 1; maxhp = 1; }

    public Stats(int[] values, StatModifier[] mods) {
        level = 1;
        timesTrained = 0;
        baseStats = values;
        
        maxhp = baseStats[0];
        hp = maxhp;
        strength = baseStats[1];
        defense = baseStats[2];
        speed = baseStats[3];
        spells = getMaxSpells();
        
        this.mods.addAll(mods);
        recalculate(true);
    }

    /**
     * Recalculate the stat levels based on modifiers
     * @param fresh - when true, we calculate from the base stats, if false, we use the current values.
     *   This only applies to hp, which is flexible.
     */
    private void recalculate(boolean fresh) {
        maxhp = baseStats[0];
        strength = baseStats[1];
        defense = baseStats[2];
        speed = baseStats[3];
        
        if (mods.size == 0)
            return;
        
        float maxhpBonus = 1;
        float strengthBonus = 1;
        float defenseBonus = 1;
        float speedBonus = 1;
        
        for (StatModifier mod : mods) {
            maxhpBonus *= mod.hp;
            strengthBonus *= mod.str;
            defenseBonus *= mod.def;
            speedBonus *= mod.spd;
        }
        
        //do not allow negative hp
        maxhp = (int)Math.max(maxhp * maxhpBonus, 1);
        //do not allow attacks to heal
        strength = (int)Math.max(strength * strengthBonus, 0);
        defense *= defenseBonus;
        speed *= speedBonus;
        
        if (fresh) {
            hp = maxhp;
        } else {
            hp = (int)Math.min(hp, maxhp);
        }
    }

    public int getLevel() {
        return level;
    }

    public void levelUp(Stat stat){
        switch(stat){
            case STRENGTH:
                strength++; break;
            case VITALITY:
                maxhp += 2; hp = maxhp; break;
            case SPEED:
                speed++; break;
            case DEFENSE:
                defense++; break;
        }

        timesTrained++;
        if (timesTrained >= 5) {
            level++;
            timesTrained = 0;
        }
    }
    
    public int getStrength() {
        return strength;
    }

    public int getDefense() {
        return defense;
    }

    public float getSpeed() {
        return speed;
    }

    public int getEvasion() {
        return (int) (speed);
    }

    public int getVitality() {
        return maxhp / 2;
    }

    public void addModifier(StatModifier mod) {
        mods.add(mod);
        recalculate(false);
    }
    
    public void removeModifier(StatModifier mod) {
        mods.removeValue(mod, true);
        recalculate(false);
    }
    
    /**
     * Saves an entity's stats to a json file.
     * Used primarily for saving a player's stats to a save file.
     */
    @Override
    public void write(Json json) {
        json.writeValue("str", strength);
        json.writeValue("def", defense);
        json.writeValue("spd", speed);
        json.writeValue("mhp", maxhp);
        json.writeValue("hp", hp);
        json.writeValue("lvl", level);
        json.writeValue("trained", timesTrained);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        level = jsonData.getInt("lvl");
        strength = jsonData.getInt("str");
        defense = jsonData.getInt("def");
        speed = jsonData.getInt("spd");
        maxhp = jsonData.getInt("mhp");
        hp = jsonData.getInt("hp");
        timesTrained = jsonData.getInt("trained", 0);
        
        baseStats = new int[5];
        baseStats[0] = maxhp;
        baseStats[1] = strength;
        baseStats[2] = defense;
        baseStats[3] = speed;
        
        spells = getMaxSpells();
    }

    /**
     * Calculates the amount of spells the player should have dependent on their level
     * @param level
     * @return available amount of spells
     */
    public int getMaxSpells() {
        return (level / 5) + 3;
    }
    
    public int getSpells() {
        return spells;
    }
    
    /**
     * Uses a spell
     */
    public void castSpell() {
        spells = Math.max(spells - 1, 0);
    }
    
    /**
     * Recharges a spell slot
     */
    public void recharge() {
        spells = Math.min(spells + 1, getMaxSpells());
    }

    public boolean canCastSpell() {
        return spells > 0;
    }
}
