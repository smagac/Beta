package core.components;

import static org.junit.Assert.*;

import org.junit.Test;

import core.components.Equipment.Piece;
import core.components.Equipment.Sword;

public class EquipmentTest {

    @Test
    public void testGetRandomPiece() {
        Piece p = Equipment.getRandomPiece(1);
        assertNotNull(p);
    }

    @Test
    public void testEquipment() {
        Equipment e = new Equipment();
        assertNotNull(e);
        assertNotNull(e.getShield());
        assertNotNull(e.getArmor());
        assertNotNull(e.getSword());
    }

    @Test
    public void testPickup() {
        Sword s = new Equipment.Sword();
        s.power = 1;
        s.durability = 10;
        s.maxDurability = 10;
        
        Equipment e = new Equipment();
        
        //test replacing with stronger equipment
        assertTrue(s != e.getSword());
        assertNotEquals(s.power, e.getSword().power);
        e.pickup(s);
        assertTrue(s.equals(e.getSword()));
        
        //test repairing
        e.getSword().durability = 5;
        
        Sword s2 = new Equipment.Sword();
        s2.power = 1;
        s2.durability = 10;
        s2.maxDurability = 10;
        
        assertEquals(0, e.getSword().compareTo(s2));
        
        e.pickup(s2);
        
        assertEquals(e.getSword().maxDurability, e.getSword().durability);
        
        //test replace with more durable weapon
        s2.power = 1;
        s2.durability = 15;
        s2.maxDurability = 15;
        
        e.pickup(s2);
        assertTrue(e.getSword().equals(s2));
        
        //test replace more durable weapon with more powerful weapon
        s2.power = 2;
        s2.durability = 5;
        s2.maxDurability = 5;
        
        assertEquals(15, e.getSword().maxDurability);
        assertEquals(1, e.getSword().power);
        
        e.pickup(s2);
        
        assertEquals(5, e.getSword().durability);
        assertEquals(2, e.getSword().power);
    }

    @Test
    public void testReset() {
        Piece p = new Sword();
        assertEquals(0, p.power);
        assertEquals(0, p.durability);
        assertEquals(0, p.maxDurability);
        
        p.power = 5;
        p.durability = 7;
        p.maxDurability = 8;
        
        assertEquals(5, p.power);
        assertEquals(7, p.durability);
        assertEquals(8, p.maxDurability);
        
        p.reset();
        
        assertEquals(0, p.power);
        assertEquals(0, p.durability);
        assertEquals(0, p.maxDurability);
    }

}
