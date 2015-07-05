package com.nhydock.storymode.datatypes.npc;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;

public class NPC extends Component {
    public static ComponentMapper<NPC> Map = ComponentMapper.getFor(NPC.class);
    
    final Behavior behavior;
    
    public NPC(Behavior b) {
        this.behavior = b;
    }
    
    public void interact(){
        behavior.run();
    }
    
    public Behavior getBehavior(){
        return behavior;
    }
    
    public static interface Behavior {
        public void run();
    }
}
