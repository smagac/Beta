package core.common;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Keys;

public enum Input {
    ACCEPT(Keys.ENTER, Keys.SPACE, Keys.Q, Keys.U, Keys.NUMPAD_7),
    CANCEL(Keys.BACKSPACE, Keys.ESCAPE, Keys.E, Keys.O, Keys.NUMPAD_9),
    ACTION(Keys.TAB, Keys.BACKSLASH, Keys.NUMPAD_5),
    UP(Keys.UP, Keys.W, Keys.I, Keys.NUMPAD_8),
    DOWN(Keys.DOWN, Keys.S, Keys.K, Keys.NUMPAD_2),
    LEFT(Keys.LEFT, Keys.A, Keys.J, Keys.NUMPAD_4),
    RIGHT(Keys.RIGHT, Keys.D, Keys.L, Keys.NUMPAD_6);
    
    public final int[] keys;
    
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
            if (i.match(keycode)) {
                return i;
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
        return isPressed(Gdx.input);
    }
    
    public boolean isPressed(com.badlogic.gdx.Input input) {
        boolean pressed = false;
        for (int key : keys) {
            if (input.isKeyPressed(key)) {
                pressed = true;
                break;
            }
        }
        return pressed;
    }
}
