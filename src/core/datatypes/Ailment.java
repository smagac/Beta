package core.datatypes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.ObjectFloatMap;

/**
 * Enum of bad status effects that may be applied to an entity's health
 * @author nhydock
 *
 */
public final class Ailment {
    public static final Ailment POISON = new Ailment(10, 0b0001, "poison", 1);
    public static final Ailment TOXIC = new Ailment(-1, 0b0001, "toxic", 2);    
    public static final Ailment CONFUSE = new Ailment(8, 0b0010, "confuse", 1);
    public static final Ailment SPRAIN = new Ailment(10, 0b0100, "sprain", 1);
    public static final Ailment ARTHRITIS = new Ailment(0, 0b0100, "arthritis", 2);
    public static final Ailment BLIND = new Ailment(20, 0b1000, "blind", 1);
    
    public static final Ailment[] ALL = {POISON, TOXIC, CONFUSE, SPRAIN, ARTHRITIS, BLIND};
    
    private final int turns;
    private final int bit;
    private final int priority;
    private final String name;
    
    /**
     * Define a status effect
     * @param t
     *   the number of turns it takes to naturally wear off
     *   0 indicates that medicine is required to heal it
     */
    private Ailment(int t, int b, String n, int p){
        turns = t;
        bit = b;
        name = n;
        priority = p;
    }
    
    @Override
    public int hashCode(){
        return bit;
    }
    
    public int getPriority(){
        return priority;
    }
    
    /**
     * @param age - length of time the entity has had this ailment
     * @return true if recovered from natural healing over time
     */
    public boolean recovered(int age) {
        if (!requiresMedicine()) {
            return age > turns;
        }
        return false;
    }
    
    /**
     * @return true if the ailment requires medicine in order to recover fully
     */
    public boolean requiresMedicine() {
        return turns <= -1;
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    public static class AilmentModifier {
        ObjectFloatMap<Ailment> chance;
        
        public AilmentModifier() {
            chance = new ObjectFloatMap<Ailment>();
        }
        
        public void addAilment(Ailment a, float rate) {
            chance.put(a, rate);
        }
        
        public float getChance(Ailment a) {
            return chance.get(a, 0f);
        }
        
        /**
         * Merges another ailment modifier with this one, selecting the strongest
         * values from both
         * @param a
         */
        public void merge(AilmentModifier a) {
            ObjectFloatMap.Entries<Ailment> e = a.chance.entries();
            while (e.hasNext) {
                ObjectFloatMap.Entry<Ailment> entry = e.next();
                float val = this.chance.get(entry.key, entry.value);
                if (val <= entry.value) {
                    chance.put(entry.key, entry.value);
                }
            }
        }
    }
}
