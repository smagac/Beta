package com.nhydock.storymode.datatypes;

import com.badlogic.ashley.utils.ImmutableArray;
import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.badlogic.gdx.utils.ObjectIntMap.Keys;
import com.nhydock.storymode.scenes.Messages;

/**
 * Component keeping track of the general health of an entity
 * @author nhydock
 *
 */
public class Health {
    
    private ObjectIntMap<Ailment> status = new ObjectIntMap<Ailment>();
    private ImmutableArray<Ailment> ailments = new ImmutableArray<Ailment>(new Array<Ailment>());
    
    /**
     * Adds a new temporary ailment to this entity
     * @param effect
     * @return Ailment replaced if one existed
     */
    public boolean addAilment(Ailment effect) {
        Keys<Ailment> keys = status.keys();
        boolean add = true;
        while (keys.hasNext && add) {
            Ailment a = keys.next();
            if (a.equals(effect)) {
                if (a.getPriority() > effect.getPriority()) {
                    add = false;
                }
                else 
                {
                    break;
                }
            }
        }
        if (add) {
            status.put(effect, 0);    
            ailments = new ImmutableArray<Ailment>(status.keys().toArray());
            MessageManager.getInstance().dispatchMessage(null, Messages.Player.AddAilment, effect);
        }
        
        return true;
    }
    
    public ImmutableArray<Ailment> getAilments() {
        return ailments;
    }
    
    /**
     * Updates all ailments by 1 turn
     * @return list of ailments that have expired
     */
    public void update() {
        boolean changed = false;
        for (Ailment a : status.keys()) {
            if (!a.requiresMedicine()) {
                int age = status.get(a, 0) + 1;
                status.put(a, age);
                if (a.recovered(age)) {
                    status.remove(a, 0);
                    changed = true;
                    MessageManager.getInstance().dispatchMessage(null, Messages.Player.RemoveAilment, a);
                }
            }
        }
        if (changed) {
            ailments = new ImmutableArray<Ailment>(status.keys().toArray());
        }
        
    }
    
    /**
     * Remove all ailments
     */
    public void reset(){
       status.clear();
       ailments = new ImmutableArray<Ailment>(status.keys().toArray());
       MessageManager.getInstance().dispatchMessage(null, Messages.Player.RemoveAilment, null);
    }
}
