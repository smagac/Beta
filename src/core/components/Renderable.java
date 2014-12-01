package core.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;

public class Renderable extends Component {

    private final TextureRegion sprite;
    private Actor actor;
    private String spriteName;

    public Renderable(String type, TextureRegion region) {
        sprite = region;
        this.spriteName = type;
        this.actor = new Image(region);
        this.actor.setPosition(-1, -1);
    }

    public String getSpriteName() {
        return spriteName;
    }
    
    public TextureRegion getSprite() {
        return sprite;
    }

    public void move(float f, float g) {
        actor.addAction(Actions.moveTo(f, g));
    }

    public Actor getActor() {
        return actor;
    }
}
