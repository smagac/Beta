package core.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectMap;

import core.datatypes.Ailment;

/**
 * Component keeping track of the general health of an entity
 * @author nhydock
 *
 */
public class Health extends Component {
    
    public static ComponentMapper<Health> Map = ComponentMapper.getFor(Health.class);
    
    private ObjectIntMap<Ailment> status = new ObjectIntMap<Ailment>();
    private Array<Ailment> expiredList = new Array<Ailment>();
    private ImmutableArray<Ailment> expiredReturn = new ImmutableArray<Ailment>(expiredList);
    private ImmutableArray<Ailment> ailments = new ImmutableArray<Ailment>(new Array<Ailment>());
    /**
     * Adds a new temporary ailment to this entity
     * @param effect
     */
    public void addAilment(Ailment effect) {
        status.put(effect, 0);
    }
    
    public ImmutableArray<Ailment> getAilments() {
        return ailments;
    }
    
    /**
     * Updates all ailments by 1 turn
     * @return list of ailments that have expired
     */
    public ImmutableArray<Ailment> update() {
        expiredList.clear();
        boolean changed = false;
        for (Ailment a : status.keys()) {
            int age = status.get(a, 0) + 1;
            status.put(a, age);
            if (a.recovered(age)) {
                status.remove(a, 0);
                expiredList.add(a);
                changed = true;
            }
        }
        if (changed) {
            ailments = new ImmutableArray<Ailment>(status.keys().toArray());
        }
        return expiredReturn;
    }
    
    /**
     * Remove all ailments
     */
    public void reset(){
       status.clear();
       expiredList.clear();
    }
}
