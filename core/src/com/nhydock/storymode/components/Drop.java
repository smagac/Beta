package com.nhydock.storymode.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.nhydock.storymode.components.Equipment.Piece;
import com.nhydock.storymode.datatypes.Item;

public class Drop extends Component {

    public static final ComponentMapper<Drop> Map = ComponentMapper.getFor(Drop.class);
    
    public final Object reward;
    
    public Drop(Item reward) {
        this.reward = reward;
    }
    
    public Drop(Piece reward) {
        this.reward = reward;
    }
}
