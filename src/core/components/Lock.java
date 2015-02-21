package core.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;

public class Lock extends Component {
    public static final ComponentMapper<Lock> Map = ComponentMapper.getFor(Lock.class);

    public boolean unlocked;
    public boolean open;
    
}
