package scene2d.ui.extras;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;

public class ScrollFocuser extends InputListener {

    Actor a;

    public ScrollFocuser(Actor a) {
        this.a = a;
    }

    @Override
    public final void enter(InputEvent evt, float x, float y, int pointer, Actor fromActor) {
        a.getStage().setScrollFocus(a);
    }

    @Override
    public final void exit(InputEvent evt, float x, float y, int pointer, Actor toActor) {
        a.getStage().setScrollFocus(null);
    }
}
