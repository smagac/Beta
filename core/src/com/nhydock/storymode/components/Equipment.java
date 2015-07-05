package com.nhydock.storymode.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.math.MathUtils;
import com.nhydock.storymode.scenes.Messages;

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

    abstract public static class Piece implements Comparable<Piece> {

        public static final int MAX_POWER = 14;
        public static final int MAX_DURABILITY = 20;
        
        protected int power;
        protected int maxDurability;
        protected int durability;
        
        public void decay() {
            if (durability > 0) {
                durability = Math.max(0, durability - 1);
                if (durability <= 0) {
                    MessageManager.getInstance().dispatchMessage(null, Messages.Interface.Notify, breakMsg());
                }
                MessageManager.getInstance().dispatchMessage(null, Messages.Player.Equipment, this);
            }
        }
        
        protected void replace(Piece p) {
            power = p.power;
            maxDurability = p.maxDurability;
            durability = p.maxDurability;
            MessageManager.getInstance().dispatchMessage(null, Messages.Interface.Notify, replaceMsg());
            
        }
        
        protected void repair(Piece p) {
            durability = Math.min(durability + p.maxDurability, maxDurability);
            MessageManager.getInstance().dispatchMessage(null, Messages.Interface.Notify, repairMsg());
            
        }
        
        protected void reset() {
            power = 0;
            durability = 0;
            maxDurability = 0;
        }
        
        public int getPower() {
            return (durability > 0 ) ? power : 0;
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
        
        @Override
        public boolean equals(Object o) {
            if (o instanceof Piece) {
                Piece p = (Piece)o;
                return p.power == this.power && 
                        p.durability == this.durability &&
                        p.maxDurability == this.maxDurability;
            }
            return false;
        }
        
        @Override
        public int compareTo(Piece o) {
            if (o.power > this.power) {
                return 1;
            }
            if (o.maxDurability > this.maxDurability) {
                return 1;
            }
            return 0;
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

        @Override
        public int compareTo(Piece o) {
            if (!(o instanceof Sword)) {
                return -1;
            }
            return super.compareTo(o);
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
        
        @Override
        public int compareTo(Piece o) {
            if (!(o instanceof Shield)) {
                return -1;
            }
            return super.compareTo(o);
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
        
        @Override
        public int compareTo(Piece o) {
            if (!(o instanceof Armor)) {
                return -1;
            }
            return super.compareTo(o);
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
        
        int worth = socket.compareTo(piece); 
        if (worth == 0) {
            socket.repair(piece);
        } else if (worth > 0) {
            socket.replace(piece);
        }
        
        MessageManager.getInstance().dispatchMessage(null, Messages.Player.Equipment, socket);
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
