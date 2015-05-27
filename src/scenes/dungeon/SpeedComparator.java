package scenes.dungeon;

import java.util.Comparator;

import com.badlogic.ashley.core.Entity;

import core.components.Stats;

/**
 * Comparator used to sort the action order of entities in the system
 * 
 * @author nhydock
 *
 */
class SpeedComparator implements Comparator<Entity> {

    static final SpeedComparator instance = new SpeedComparator();

    private SpeedComparator() {
    };

    @Override
    public int compare(Entity o1, Entity o2) {
        Stats s1 = Stats.Map.get(o1);
        Stats s2 = Stats.Map.get(o2);

        Float spd1 = s1.getSpeed();
        Float spd2 = s2.getSpeed();

        return spd1.compareTo(spd2);
    }

}