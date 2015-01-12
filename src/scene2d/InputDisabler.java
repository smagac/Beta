package scene2d;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;

/**
 * Simple run-twice runnable.  First run grabs the current input and
 * removes it from GDX.  Run again to reset the input processor to
 * the one pulled out.  This is good if you want animations that
 * want to pause input completely while happening.
 * @author nhydock
 *
 */
public class InputDisabler implements Runnable {

    public static InputDisabler instance = new InputDisabler();
    public static void swap() {
        instance.run();
    }
    InputProcessor saved;
    
    @Override
    public void run() {
        if (saved == null) {
            saved = Gdx.input.getInputProcessor();
            Gdx.input.setInputProcessor(null);
        }
        else {
            Gdx.input.setInputProcessor(saved);
            saved = null;
        }
        
    }

}
