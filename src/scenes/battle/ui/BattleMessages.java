package scenes.battle.ui;

/**
 * List of messages available for sending to the message dispatcher involved with boss battles
 * @author nhydock
 *
 */
public interface BattleMessages {

    /**
     * Advances to the next turn
     */
    public static final int ADVANCE = 0x0001;
    
    //cross menu selections
    public static final int ATTACK = 0x1001;
    public static final int DEFEND = 0x1002;
    public static final int ITEM = 0x1003;
    
    /**
     * Tells the system that the attack will be performed using a manual attack method
     */
    public static final int MANUAL = 0x2001;
    /**
     * Tells the system that the attack by the player will be executed automatically
     */
    public static final int AUTO = 0x2002;
    
    public static final int DAMAGE = 0X2003;
    
    /**
     * Message used to signify a target has been selected and who
     */
    public static final int TARGET = 0x3001;
    
    /**
     * Message used to signify a item has been used to modify a target
     */
    public static final int MODIFY = 0x4001;
    
    /**
     * Message used to signify that a modifier has expired.
     * Pack with which modifier to remove.
     */
    public static final int DEBUFF = 0x4002;
    
}
