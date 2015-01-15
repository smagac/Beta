package scene2d.ui.extras;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pools;

/**
 * Provides a simple single-focus grouping of actors.
 * <p/>
 * Actors added to the focus group will switch focus when hovered over. What
 * this means is determined by the ui.
 * 
 * @author nhydock
 *
 */
public class FocusGroup extends Actor {

    Array<Actor> actors;
    Array<InputListener> inputs;

    Actor focus;
    int focusIndex;

    public FocusGroup() {
        actors = new Array<Actor>();
        inputs = new Array<InputListener>();
        focus = null;
    }

    public FocusGroup(Actor... list) {
        actors = new Array<Actor>();
        inputs = new Array<InputListener>();

        focus = null;

        for (int i = 0; i < list.length; i++) {
            add(list[i]);
        }
    }

    /**
     * Adds a new actor to the focus group and registers them with a listener
     * 
     * @param a
     */
    public void add(final Actor a) {
        InputListener input = new InputListener() {
            @Override
            public void enter(InputEvent evt, float x, float y, int pointer, Actor fromActor) {
                setFocus(a);
            }
        };

        actors.add(a);
        inputs.add(input);
        a.addListener(input);
    }

    /**
     * Removes a single actor from the group and unhooks its focus listener
     * 
     * @param a
     *            - reference to the actor in the group to remove
     */
    public void remove(final Actor a) {
        int i = actors.indexOf(a, true);
        remove(i);
    }

    /**
     * Removes a single actor from the group and unhooks its focus listener
     * 
     * @param i
     *            - index of the actor in the group
     */
    public void remove(int i) {
        Actor a = actors.removeIndex(i);
        InputListener input = inputs.removeIndex(i);

        a.removeListener(input);
    }

    /**
     * Remove all actors from the group and removes all the focus listeners from
     * those actors
     */
    @Override
    public void clear() {
        for (int i = 0; i < actors.size; i++) {
            InputListener input = inputs.get(i);
            Actor a = actors.get(i);
            a.removeListener(input);
        }
        actors.clear();
        inputs.clear();

        super.clear();
    }

    /**
     * @return Actor that is currently focused on in the group
     */
    public Actor getFocused() {
        return focus;
    }

    /**
     * @return index of the currently focused on element
     */
    public int getFocusedIndex() {
        return focusIndex;
    }

    /**
     * @return list of all the actors contained in this focus group
     */
    public Array<Actor> getActors() {
        return actors;
    }

    /**
     * Focuses on the next actor in the list
     */
    public void next() {
        next(false);
    }

    /**
     * Focuses on the next actor in the list
     * 
     * @param wrap
     *            - tells the focus group to wrap the index back to the
     *            beginning of the list if next is past the end
     */
    public void next(boolean wrap) {
        focusIndex++;
        if (focusIndex >= actors.size) {
            if (wrap) {
                focusIndex = 0;
            }
            else {
                focusIndex = actors.size - 1;
            }
        }
        focus = actors.get(focusIndex);
        ChangeEvent changeEvent = Pools.obtain(ChangeEvent.class);
        fire(changeEvent);
        Pools.free(changeEvent);
    }

    /**
     * Focuses on the previous actor in the list
     */
    public void prev() {
        prev(false);
    }

    /**
     * Focuses on the previous actor in the list
     * 
     * @param wrap
     *            - tells the focus group to wrap the index back to the end of
     *            the list if prev is past the beginning
     */
    public void prev(boolean wrap) {
        focusIndex--;
        if (focusIndex < 0) {
            if (wrap) {
                focusIndex = actors.size - 1;
            }
            else {
                focusIndex = 0;
            }
        }
        focus = actors.get(focusIndex);
        ChangeEvent changeEvent = Pools.obtain(ChangeEvent.class);
        fire(changeEvent);
        Pools.free(changeEvent);
    }

    /**
     * Removes focus from everything
     */
    public void unfocus() {
        focus = null;
        focusIndex = -1;
        ChangeEvent changeEvent = Pools.obtain(ChangeEvent.class);
        fire(changeEvent);
        Pools.free(changeEvent);
    }

    /**
     * Switches the focus of the group onto the specified actor
     * 
     * @param a
     */
    public void setFocus(Actor a) {
        if (!actors.contains(a, true)) {
            //Gdx.app.log("FocusGroup", "not a valid actor");
            return;
        }
        if (focus != a) {
            focus = a;
            focusIndex = actors.indexOf(a, true);
            ChangeEvent changeEvent = Pools.obtain(ChangeEvent.class);
            fire(changeEvent);
            Pools.free(changeEvent);
        }
    }
}
