package com.nhydock.storymode.service.interfaces;

import github.nhydock.ssm.Service;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.ai.msg.Telegraph;

/**
 * Service for handling boss battle data containment and message dispatching
 * @author nhydock
 *
 */
public interface IBattleContainer extends Telegraph, Service {

    public void setBoss(Entity bossEntity);
    public void setPlayer(Entity playerEntity);
    public Entity getTarget();
    public Entity getBoss();
    public Entity getPlayer();
    
}
