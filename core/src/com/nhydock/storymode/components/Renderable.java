package com.nhydock.storymode.components;

import com.badlogic.ashley.core.Component;
import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class Renderable extends Component {
    public static final ComponentMapper<Renderable> Map = ComponentMapper.getFor(Renderable.class);

    private Image actor;
    private String spriteName;
    private float density;
    private boolean changed;

    public Renderable(String type) {
        this.spriteName = type;
    }
    
    public void setSpriteName(String type) {
        this.spriteName = type;
        changed = true;
    }
    
    public boolean hasChanged() {
        return changed;
    }
    
    public void loadImage(TextureAtlas atlas) {
        if (this.actor == null){
            this.actor = new Image(atlas.findRegion(spriteName));
        } else {
            this.actor.setDrawable(new TextureRegionDrawable(atlas.findRegion(spriteName)));
        }
        changed = false;
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
        if (this.actor == null) {
            this.actor = new Image(skin, spriteName);
        } else {
            this.actor.setDrawable(skin, spriteName);
        }
        changed = false;
    }
}
