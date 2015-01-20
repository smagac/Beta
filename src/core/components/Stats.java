package core.components;

import com.badlogic.ashley.core.Component;
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
    private int level;
    public int hp;
    public int maxhp;
    private int strength;
    private int defense;
    private int speed;
    public int exp;
    public int nextExp;
    public boolean hidden;

    private int[] baseStats;
    
    /**
     * Keep track of 
     */
    private Array<StatModifier> mods = new Array<StatModifier>();
    
    public Stats() { }

    public Stats(int[] values, StatModifier[] mods) {
        level = 1;
        nextExp = 10;
        baseStats = values;
        
        maxhp = baseStats[0];
        hp = maxhp;
        strength = baseStats[1];
        defense = baseStats[2];
        speed = baseStats[3];
        exp = baseStats[4];
        
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

    /**
     * The amount of experience points the entity currently has
     * @return if it's a player, it returns their current amount, if the entity is an enemy, it returns the amount
     *         that they are going to reward the player with.
     */
    public float getExp() {
        return exp;
    }

    /**
     * @return true if the entity has enough experience points accumulated to level up
     */
    public boolean canLevelUp() {
        return exp >= nextExp;
    }

    /**
     * Level up an entity, setting its stats to the provided list
     * @param stats - new stats of the entity after level up
     */
    public void levelUp(int[] stats) {
        level++;
        strength = stats[0];
        defense = stats[1];
        speed = stats[2];
        maxhp = stats[3] * 2;
        exp = 0;
        nextExp = level * 10;
        
        hp = maxhp;
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
        json.writeValue("exp", exp);
        json.writeValue("lvl", level);
    }

    @Override
    public void read(Json json, JsonValue jsonData) {
        level = jsonData.getInt("lvl");
        nextExp = level * 10;
        strength = jsonData.getInt("str");
        defense = jsonData.getInt("def");
        speed = jsonData.getInt("spd");
        maxhp = jsonData.getInt("mhp");
        hp = jsonData.getInt("hp");
        exp = jsonData.getInt("exp");
        
        baseStats = new int[5];
        baseStats[0] = maxhp;
        baseStats[1] = strength;
        baseStats[2] = defense;
        baseStats[3] = speed;
        baseStats[4] = exp;
        
    }
}
