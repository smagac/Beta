package scenes.dungeon.ui;

import github.nhydock.ssm.Inject;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;

import core.DataDirs;
import core.components.Identifier;
import core.components.Renderable;
import core.service.interfaces.IPlayerContainer;
import scenes.UI;

/**
 * Simple UI/Stage for doing a transition effect to introduce
 * the boss for the battle
 * @author nhydock
 *
 */
public class Transition extends UI {

    Group slides;
    
    TextureRegion captured;

    public Transition(AssetManager manager) {
        super(manager);
    }

    @Override
    protected void load() {
    }

    @Override
    public void init() {
        clear();
        TextureRegion tx = captured = ScreenUtils.getFrameBufferTexture();
    
        int width = tx.getRegionWidth();
        int height = tx.getRegionHeight();
        
        slides = new Group();
        
        for (int i = 0, x = 0, skip = (int)(width / 16f); i < 16; x += skip, i++) {
            TextureRegion slice = new TextureRegion(tx, x, 0, skip, -height);
            
            Image image = new Image(slice);
            image.setPosition(x, 0);
            slides.addActor(image);
        }
        
        slides.setPosition(0, 0);
        slides.setSize(width, height);
        
        addActor(slides);
    }
    
    public void playAnimation(Runnable after) {
        getRoot().clearActions();
        slides.clearActions();
        
        float delay = .1f;
        for (Actor a : slides.getChildren()) {
            a.addAction(
                Actions.sequence(
                    Actions.delay(delay),
                    Actions.moveBy(0, captured.getRegionHeight(), .2f, Interpolation.circleOut)
                )
            );
            delay += .1f;
        }
        
        addAction(Actions.sequence(Actions.delay(delay + .3f),Actions.run(after)));
    }
}
