package core.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public class Renderable extends Component {
    public static final ComponentMapper<Renderable> Map = ComponentMapper.getFor(Renderable.class);

    private Actor actor;
    private String spriteName;
    private float density;

    public Renderable(String type) {
        this.spriteName = type;
    }
    
    public void loadImage(TextureAtlas atlas) {
        this.actor = new Image(atlas.findRegion(spriteName));
    }
    
    public void setDensity(float d) {
        density = d;
    }
    
    public float getDensity() {
        return density;
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
