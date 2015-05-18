package core.datatypes;

/**
 * Enum of bad status effects that may be applied to an entity's health
 * @author nhydock
 *
 */
public final class Ailment {
    public static final Ailment POISON = new Ailment(10, 0b0001);
    public static final Ailment TOXIC = new Ailment(-1, 0b0001);    
    public static final Ailment CONFUSE = new Ailment(8, 0b0010);
    public static final Ailment SPRAIN = new Ailment(10, 0b0100);
    public static final Ailment ARTHRITIS = new Ailment(0, 0b0100);
    public static final Ailment BLIND = new Ailment(20, 0b1000);
    
    private final int turns;
    private final int bit;
    
    /**
     * Define a status effect
     * @param t
     *   the number of turns it takes to naturally wear off
     *   0 indicates that medicine is required to heal it
     */
    private Ailment(int t, int b){
        turns = t;
        bit = b;
    }
    
    @Override
    public int hashCode(){
        return bit;
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
}
