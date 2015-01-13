package scenes;

/**
 * Collection of all UI messages that may be sent by the MessageDispatcher.
 * I know it's bad practice to put all constants in one file like this,
 * but with how many different message signatures we may send, this helps keep
 * everything in track.
 * @author nhydock
 *
 */
public interface Messages {
    public static interface Interface {
        // used to close the menu
        public static final int Close = 0xfff0;
    
        // used to popup a notification
        public static final int Notify = 0xfff1;
    
        // used when an item in a list is selected
        public static final int Selected = 0xfff2;
        
        // used when a button is highlighted
        public static final int Button = 0xfff3;
    }
    
    /*
     * TOWN (mainly just menu item index recognition)
     */
    public static interface Town {
        public static final int Sleep = 0;
        public static final int Explore = 1;
        public static final int Craft = 2;
        public static final int Quest = 3;
        public static final int Save = 4;

        public static final int Close = 0;
        public static final int Make = 1;
        public static final int Accept = 1;
        
        public static final int Random = 2;
        public static final int DailyDungeon = 3;

        public static final int CancelDownload = 0;
    }
    
    /*
     * DUNGEON
     */
    public static interface Dungeon {
        public static final int Movement = -1; //move the player
        public static final int Assist = 0; //open up assist menu
        public static final int Close = 0;  //close a prompt when it is present
        public static final int Heal = 1;   //select assistance to open heal menu
        public static final int Leave = 2;  //select assistance to open escape menu
    
        public static final int Sacrifice = 1; //choose to sacrifice items
    
        public static final int Dead = 0x1000; //notification that we've died
        public static final int Exit = 0x1001; //notification that we've escaped the dungeon
        public static final int LevelUp = 0x1002; //notification that we've leveled up
        public static final int Refresh = 0x1003; //notification to the HUD to say that we need to update its display
        
        public static final int FIGHT = 0x1004; //two entities are fighting
        public static final int KILLED = 0x1005; //an entity has been destroyed in combat
    }
    
    /*
     * BATTLE
     */
    public static interface Battle {
        /**
         * Advances to the next turn
         */
        public static final int ADVANCE = 0x2001;
        public static final int DAMAGE = 0X2002;
        
        /**
         * Message used to signify a target has been selected and who
         */
        public static final int TARGET = 0x2003;
        
        /**
         * Message used to signify a item has been used to modify a target
         */
        public static final int MODIFY = 0x2004;
        public static final int MODIFY_UPDATE = 0x2005;
        
        /**
         * Message used to signify that a modifier has expired.
         * Pack with which modifier to remove.
         */
        public static final int DEBUFF = 0x2006;
    
        public static final int VICTORY = 0x2007;
        public static final int DEFEAT = 0x2008;
    }
}