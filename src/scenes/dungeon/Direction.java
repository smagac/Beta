package scenes.dungeon;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public enum Direction {
    Up(Keys.UP, Keys.W), Down(Keys.DOWN, Keys.S), Left(Keys.LEFT, Keys.A), Right(Keys.RIGHT, Keys.D);

    private int[] keys;

    /**
     * @param key
     *            - Acceptable keys that mask to the direction
     */
    Direction(int... key) {
        keys = key;
    }

    /**
     * Get the direction from a keycode of a key press
     * 
     * @param keycode
     * @return
     */
    public static Direction valueOf(int keycode) {
        for (int i = 0; i < Direction.values().length; i++) {
            Direction d = Direction.values()[i];
            for (int key : d.keys) {
                if (keycode == key)
                    return d;
            }
        }
        return null;
    }

    /**
     * Calculate a direction from the relative location of the mouse from the
     * center of a viewport
     * 
     * @param pos
     * @param vw
     * @param vh
     * @return
     */
    public static Direction valueOf(float x, float y, float vw, float vh) {
        float dir = MathUtils.atan2(vh / 2 - y, vw / 2 - x);
        if (dir > -MathUtils.PI / 4 && dir < MathUtils.PI / 4) {
            return Left;
        }
        // RIGHT
        else if (dir < -3 * MathUtils.PI / 4 || dir > 3 * MathUtils.PI / 4) {
            return Right;
        }
        // UP
        else if (dir < -MathUtils.PI / 4 && dir > -3 * MathUtils.PI / 4) {
            return Up;
        }
        // DOWN
        else if (dir > MathUtils.PI / 4 && dir < 3 * MathUtils.PI / 4) {
            return Down;
        }
        return null;
    }

    /**
     * Calculate a direction from the relative location of the mouse from the
     * center of a viewport
     * 
     * @param pos
     * @param vw
     * @param vh
     * @return
     */
    public static Direction valueOf(Vector2 pos, float vw, float vh) {
        float dir = MathUtils.atan2(vh / 2 - pos.y, vw / 2 - pos.x);
        if (dir > -MathUtils.PI / 4 && dir < MathUtils.PI / 4) {
            return Left;
        }
        // RIGHT
        else if (dir < -3 * MathUtils.PI / 4 || dir > 3 * MathUtils.PI / 4) {
            return Right;
        }
        // UP
        else if (dir < -MathUtils.PI / 4 && dir > -3 * MathUtils.PI / 4) {
            return Up;
        }
        // DOWN
        else if (dir > MathUtils.PI / 4 && dir < 3 * MathUtils.PI / 4) {
            return Down;
        }
        return null;
    }

    /**
     * Get a direction by checking the current key input from a GDX input
     * instance
     * 
     * @param input
     * @return
     */
    public static Direction valueOf(Input input) {
        for (int i = 0; i < Direction.values().length; i++) {
            Direction d = Direction.values()[i];
            for (int key : d.keys) {
                if (input.isKeyPressed(key))
                    return d;
            }
        }
        return null;
    }
}
