package scene2d.ui;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

/**
 * Utility class that assists in scrolling a pane to focus on an actor that's changed.
 * Created primarily to assist with menus that use tables to represent lists of data.
 * @author nhydock
 *
 */
public class ScrollOnChange extends ChangeListener {

    ScrollPane pane;
    Rectangle bounds;
    Vector2 pos;
    public ScrollOnChange(ScrollPane pane) {
        this.pane = pane;
        bounds = new Rectangle();
        pos = new Vector2();
    }
    
    @Override
    public void changed(ChangeEvent event, Actor actor) {
        //ignore the actor if it's a button that's no longer checked
        if (actor instanceof Button) {
            if (!((Button)actor).isChecked()) {
                return;
            }
        }
        
        pos.x = actor.getX();
        pos.y = actor.getY();
        actor.localToStageCoordinates(pos);
        pane.stageToLocalCoordinates(pos);
        this.pane.scrollTo(pos.x, pos.y, actor.getWidth(), actor.getHeight(), false, false);
    }

}
