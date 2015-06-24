package scenes;

import com.badlogic.ashley.core.Entity;

import core.datatypes.Ailment;
import core.datatypes.Item;

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

        public static final int Focus = 0xfff4;
    }
    
    public static interface Player {

        public static final int Progress = 0xffe0;
        
        public static final int Stats = 0xffe1;
        
        public static final int Time = 0xffe2;
        
        //send this message when a new item(s) has been added to the player's inventory
        public static final int NewItem = 0xffe3;
        
        public static final int RemoveItem = 0xffe4;
        
        public static final int UpdateItem = 0xffe5;

        public static final int Equipment = 0xffe6;

        public static final int AddAilment = 0xffe7;

        public static final int RemoveAilment = 0xffe8;
        
        public static final int LevelUp = 0xffe9; //notification that we've leveled up
        
        
        public static final class ItemMsg {
            public Item item;
            public int amount;
        }
    }
    
    /*
     * TOWN (mainly just menu item index recognition)
     */
    public static interface Town {
        public static final int Home = 0;
        public static final int Explore = 1;
        public static final int Town = 2;
        
        public static final int Close = 0;
        public static final int Make = 1;
        public static final int Accept = 1;
        
        public static final int Random = 2;
        public static final int DailyDungeon = 3;

        public static final int CancelDownload = 0;
        
        public static final int CompleteQuest = 1;
        public static final int AcceptQuest = 1;
        
        public static final int Save = 0;
        public static final int Sleep = 1;
        public static final int PageFile = 2;
        
        public static final int Quest = 0;
        public static final int Craft = 1;
        public static final int Train = 2;
    }
    
    /*
     * DUNGEON
     */
    public static interface Dungeon {
        public static final int Movement = -1; //move the player
        public static final int Assist = 0x100B; //open up assist menu
        public static final int Heal = 0x100C;   //select assistance to open heal menu
        public static final int Leave = 0x100D;  //select assistance to open escape menu
    
        public static final int Sacrifice = 0x100F; //choose to sacrifice items
    
        public static final int Dead = 0x1000; //notification that we've died
        public static final int Exit = 0x1001; //notification that we've escaped the dungeon
        public static final int Refresh = 0x1002; //notification to the HUD to say that we need to update its display
        public static final int Proceed = 0x1003; //we've escaped the dungeon by going as deep as we can go
        
        public static final int FIGHT = 0x1004; //two entities are fighting
        public static final int KILLED = 0x1005; //an entity has been destroyed in combat
        
        public static final int Descend = 0x1006;
        public static final int Ascend = 0x1007;
        
        //different kind of notify, this goes in the bottom right box of the log instead of popups
        public static final int Notify = 0x1008;
        public static final int Action = 0x1009; 
        
        //zoom in and out with the camera;
        public static final int Target = 0x100A;
        public static final int Warp = 0x100E; 
        
        public static final class CombatNotify {
            public Entity attacker;
            public Entity opponent;
            public int dmg;
            public boolean critical;
            public Ailment cause;
        }
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
        
        /**
         * Update the enemy's stat display
         */
        public static final int Stats = 0x2009;
        
        public static final class VictoryResults {
            public Item reward;
            public Item bonus;
            public int bonusCount;
            public int exp;
        }
    }

    //messages that can be sent out by NPCs
    public static interface NPC {
        /**
         * Message used to identify interaction with a trainer npc
         */
        public static final int TRAINER = 0x3001;
    }
}
