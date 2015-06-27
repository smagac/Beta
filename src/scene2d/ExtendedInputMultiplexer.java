package scene2d;

import com.badlogic.gdx.InputMultiplexer;

public class ExtendedInputMultiplexer extends InputMultiplexer {

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

    public boolean keyDown(int keycode){
        if (!disabled) {
            return super.keyDown(keycode);
        }
        return false;
    }
    
    public boolean keyUp(int keycode){
        if (!disabled) {
            return super.keyUp(keycode);
        }
        return false;
    }
    
    public boolean keyTyped(char character){
        if (!disabled) {
            return super.keyTyped(character);
        }
        return false;
    }
    
    public boolean touchDown(int x, int y, int pointer, int button) {
        if (!disabled) {
            return super.touchDown(x, y, pointer, button);
        }
        return false;
    }
    
    public boolean touchUp(int x, int y, int pointer, int button) {
        if (!disabled) {
            return super.touchUp(x, y, pointer, button);
        }
        return false;
    }
    
    public boolean touchDragged(int x, int y, int pointer) {
        if (!disabled) {
            return super.touchDragged(x, y, pointer);
        }
        return false;
    }
    
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
}
