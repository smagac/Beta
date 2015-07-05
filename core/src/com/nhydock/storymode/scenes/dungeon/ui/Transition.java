package com.nhydock.storymode.scenes.dungeon.ui;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.nhydock.gdx.scenes.scene2d.runnables.PlaySound;
import com.nhydock.storymode.DataDirs;
import com.nhydock.storymode.scenes.Scene;
import com.nhydock.storymode.scenes.UI;

/**
 * Simple UI/Stage for doing a transition effect to introduce
 * the boss for the battle
 * @author nhydock
 *
 */
public class Transition extends UI {

    Group slides;
    
    TextureRegion captured;

    public Transition(Scene scene, AssetManager manager) {
        super(scene, manager);
    }
    
    @Override
    protected void listenTo(IntSet messages){
        //do nothing
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
        setViewport(new ScalingViewport(Scaling.fill, width, height));
        
        slides = new Group();
        
        for (int i = 0, x = 0, skip = (int)(width / 16f); i < 16; x += skip, i++) {
            TextureRegion slice = new TextureRegion(tx, x, 0, skip, -height);
            
            Image image = new Image(slice);
            image.setPosition(x, 0);
            slides.addActor(image);
        }
        
        getViewport().update(width, height, true);
        slides.setPosition(0, 0);
        
        
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
        
        audio.fadeOut();
        addAction(
            Actions.sequence(
                Actions.run(getScene().getInput().disableMe),
                Actions.run(new PlaySound(DataDirs.Sounds.transition)), 
                Actions.delay(delay + .3f),
                Actions.run(after)
            )
        );
    }
}
