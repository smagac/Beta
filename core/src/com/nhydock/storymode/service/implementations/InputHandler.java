package com.nhydock.storymode.service.implementations;

import github.nhydock.ssm.Service;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;

public class InputHandler extends InputMultiplexer implements Service {
    
    public Runnable disableMe = new Runnable() {

        @Override
        public void run() {
            disable();
        }
        
    };

    public Runnable enableMe = new Runnable() {

        @Override
        public void run() {
            enable();
        }
        
    };
    
    
    private boolean disabled;

    @Override
    public boolean keyDown(int keycode){
        if (!disabled) {
            return super.keyDown(keycode);
        }
        return false;
    }
    
    @Override
    public boolean keyUp(int keycode){
        if (!disabled) {
            return super.keyUp(keycode);
        }
        return false;
    }
    
    @Override
    public boolean keyTyped(char character){
        if (!disabled) {
            return super.keyTyped(character);
        }
        return false;
    }
    
    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        if (!disabled) {
            return super.touchDown(x, y, pointer, button);
        }
        return false;
    }
    
    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {
        if (!disabled) {
            return super.touchUp(x, y, pointer, button);
        }
        return false;
    }
    
    @Override
    public boolean touchDragged(int x, int y, int pointer) {
        if (!disabled) {
            return super.touchDragged(x, y, pointer);
        }
        return false;
    }
    
    @Override
    public boolean mouseMoved(int x, int y) {
        if (!disabled) {
            return super.mouseMoved(x, y);
        }
        return false;
    }
    
    public void toggleDisable(){
        disabled = !disabled;
    }
    
    public void disable() {
        disabled = true;
    }
    
    public void enable() {
        disabled = false;
    }
    
    public boolean isDisabled() {
        return disabled;
    }
    @Override
    public void onRegister() {
        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void onUnregister() {
        
    }

}
