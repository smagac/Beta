package com.nhydock.gdx.scenes.scene2d.runnables;

import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.fsm.StateMachine;

@SuppressWarnings("rawtypes")
public class ChangeState implements Runnable {

    
    StateMachine machine;
    State state;
    
    public ChangeState(StateMachine machine, State state) {
        this.machine = machine;
        this.state = state;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        machine.changeState(state);
    }

}
