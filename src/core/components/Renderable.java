package core.components;

import com.artemis.Component;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;

public class Renderable extends Component {

    private final TextureRegion sprite;
    private Actor actor;

    public Renderable(TextureRegion region) {
        sprite = region;
    }

    public TextureRegion getSprite() {
        return sprite;
    }

    public void setActor(Image sprite2) {
        actor = sprite2;
    }

    public void move(float f, float g) {
        actor.addAction(Actions.moveTo(f, g));
    }

    public Actor getActor() {
        return actor;
    }
}
