package core.service.interfaces;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.ai.Agent;

/**
 * Service for handling boss battle data containment and message dispatching
 * @author nhydock
 *
 */
public interface IBattleContainer extends Agent {

    public void setBoss(Entity bossEntity);
    
}
