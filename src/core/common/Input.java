package core.common;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;

public enum Input {
    ACCEPT(Keys.ENTER, Keys.SPACE, Keys.Q, Keys.NUMPAD_7),
    CANCEL(Keys.BACKSPACE, Keys.ESCAPE, Keys.E, Keys.NUMPAD_9),
    SWITCH(Keys.TAB, Keys.CONTROL_RIGHT, Keys.NUMPAD_5),
    UP(Keys.UP, Keys.W, Keys.NUMPAD_8),
    DOWN(Keys.DOWN, Keys.S, Keys.NUMPAD_2),
    LEFT(Keys.LEFT, Keys.A, Keys.NUMPAD_4),
    RIGHT(Keys.RIGHT, Keys.D, Keys.NUMPAD_6);
    
    int[] keys;
    
    Input(int... k) {
        keys = k;
    }
    
    /**
     * Iterate through for values
     * @param keycode
     * @return
     */
    public static Input valueOf(int keycode) {
        for (Input i : Input.values()) {
            for (int key : i.keys) {
                if (keycode == key) {
                    return i;
                }
            }
        }
        return null;
    }
    
    /**
     * Checks to see if a key code is registered as a matching value of this input list
     * @param keycode
     * @return
     */
    public boolean match(int keycode) {
        for (int key : keys) {
            if (key == keycode) {
                return true;
            }
        }
        return false;
    }

    public boolean isPressed() {
        boolean pressed = false;
        for (int key : keys) {
            if (Gdx.input.isKeyPressed(key)) {
                pressed = true;
                break;
            }
        }
        return pressed;
    }
}
