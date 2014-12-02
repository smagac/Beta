package core.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class Renderable extends Component {

    private Actor actor;
    private String spriteName;

    public Renderable(String type) {
        this.spriteName = type;
    }
    
    public void loadImage(TextureAtlas atlas) {
        this.actor = new Image(atlas.findRegion(spriteName));
    }

    public String getSpriteName() {
        return spriteName;
    }

    public void move(float f, float g) {
        actor.addAction(Actions.moveTo(f, g));
    }

    public Actor getActor() {
        return actor;
    }

    public void loadImage(Skin skin) {
        this.actor = new Image(skin, spriteName);
    }
}
