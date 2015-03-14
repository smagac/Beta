package core.components;

import scenes.Messages;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.MathUtils;

public class Equipment extends Component {

    public static final ComponentMapper<Equipment> Map = ComponentMapper.getFor(Equipment.class); 
    
    public static Piece getRandomPiece(int depth) {
        Piece p;
        float r = MathUtils.random();
        if (r < .4) {
            p = new Sword();
        }
        else if (r < .6) {
            p = new Shield();
        }
        else {
            p = new Armor();
        }
        
        p.power = (int)MathUtils.random(1, 1+(depth/50f * Piece.MAX_POWER));
        p.maxDurability = (int)MathUtils.random(1, 1+(depth/50f * Piece.MAX_DURABILITY));
        p.durability = p.maxDurability;
        
        return p;
    }

    abstract public static class Piece {

        public static final int MAX_POWER = 14;
        public static final int MAX_DURABILITY = 20;
        
        protected int power;
        protected int maxDurability;
        protected int durability;
        
        public void decay() {
            if (durability > 0) {
                durability = Math.max(0, durability - 1);
                if (durability <= 0) {
                    power = 0;
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Notify, breakMsg());
                }
                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Player.Equipment, this);
            }
        }
        
        protected void replace(Piece p) {
            power = p.power;
            maxDurability = p.maxDurability;
            durability = p.maxDurability;
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Notify, replaceMsg());
            
        }
        
        protected void repair(Piece p) {
            durability = Math.max(durability + p.maxDurability, maxDurability);
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Notify, repairMsg());
            
        }
        
        protected void reset() {
            power = 0;
            durability = 0;
            maxDurability = 0;
        }
        
        public int getPower() {
            return power;
        }
        
        abstract String breakMsg();
        
        abstract String replaceMsg();
        
        abstract String repairMsg();

        public int getDurability() {
            return durability;
        }
        
        public int getMaxDurability() {
            return maxDurability;
        }
    }
    
    public static class Sword extends Piece {

        @Override
        String breakMsg() {
            return "Your sword broke!";
        }

        @Override
        String replaceMsg() {
            return "You equipped a sword of +" + power;
        }

        @Override
        String repairMsg() {
            return "You repaired your sword";
        }
        
    }
    
    public static class Shield extends Piece {
        @Override
        String breakMsg() {
            return "Your shield broke!";
        }

        @Override
        String replaceMsg() {
            return "You equipped a shield of +" + power;
        }

        @Override
        String repairMsg() {
            return "You repaired your shield";
        }
        
    }
    
    public static class Armor extends Piece {
        @Override
        String breakMsg() {
            return "Your armor broke!";
        }

        @Override
        String replaceMsg() {
            return "You equipped armor of +" + power;
        }

        @Override
        String repairMsg() {
            return "You repaired your armor";
        }
        
    }
    
    private Piece sword = new Sword();
    private Piece shield = new Shield();
    private Piece armor = new Armor();
    
    public Equipment(){}
    
    public Piece getSword(){
        return sword;
    }
    
    public Piece getShield(){
        return shield;
    }
    
    public Piece getArmor(){
        return armor;
    }
    
    /**
     * Pickup a piece of equipment.  If the item's max durability
     * is more than the current equipped piece, then the item will replace
     * if the power is equal to or more than.  Else it will recover
     * the durability of the current piece of equipment by that much.
     * @param piece
     */
    public void pickup(Piece piece) {
        Piece socket;
        if (piece instanceof Sword) {
            socket = sword;
        } else if (piece instanceof Shield) {
            socket = shield;
        } else {
            socket = armor;
        }
        
        if (piece.maxDurability > socket.maxDurability) {
            if (piece.power >= socket.power) {
                socket.replace(piece);
            } else {
                socket.repair(piece);
            }
        }
        
        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Player.Equipment, socket);
    }
    
    /**
     * Reset the stat values of all of the equipment
     */
    public void reset() {
        sword.reset();
        shield.reset();
        armor.reset();
    }
}
