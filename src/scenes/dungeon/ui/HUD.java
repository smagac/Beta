package scenes.dungeon.ui;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectMap;

import scenes.Messages;
import core.components.Stats;
import core.datatypes.Ailment;
import core.datatypes.Health;
import core.datatypes.dungeon.Progress;
import core.service.interfaces.IPlayerContainer;

public class HUD {

    private Group container;
    private Label hpStats;
    private ProgressBar hpBar;
    
    private Label expStats;
    private ProgressBar expBar;
    
    private Label keys;
    private Label depth;
    
    private Group ailmentList;
    private ObjectMap<String, Image> ailments;
    
    public HUD(Skin skin) {
    
        container = new Group();
        container.setSize(480, 160);
        
        //left component
        {
            Window window = new Window("", skin, "round");
            window.setSize(100, 48);
            window.setPosition(0, 0);
            
            Image icon = new Image(skin, "key");
            icon.setSize(32, 32);
            icon.setPosition(8, 10);
            window.addActor(icon);
            
            keys = new Label("0", skin, "prompt");
            keys.setAlignment(Align.bottomRight);
            keys.setPosition(80, 10, Align.bottomRight);
            window.addActor(keys);
            
            container.addActor(window);
        }
        
        //right component
        {
            Window window = new Window("", skin, "round");
            window.setSize(100, 48);
            window.setPosition(380, 0);
            
            Image icon = new Image(skin, "up");
            icon.setSize(32, 32);
            icon.setPosition(12, 10);
            window.addActor(icon);
            
            depth = new Label("0", skin, "prompt");
            depth.setAlignment(Align.bottomRight);
            depth.setPosition(85, 10, Align.bottomRight);
            window.addActor(depth);
            
            container.addActor(window);
        }
        
        //central hud
        {
            Window window = new Window("", skin, "round");
            window.setSize(300, 72);
            window.setPosition(90, 0);
            
            //hp
            {
                Label label = new Label("HP:", skin, "promptsm");
                label.setPosition(10, 45, Align.bottomLeft);
                window.addActor(label);
                
                hpStats = new Label("10/10", skin, "promptsm");
                hpStats.setPosition(290, 45, Align.bottomRight);
                hpStats.setAlignment(Align.bottomRight);
                window.addActor(hpStats);
                
                hpBar = new ProgressBar(0.0f, 1.0f, .01f, false, skin, "simple");
                hpBar.setSize(280, 10);
                hpBar.setPosition(10, 36);
                hpBar.setValue(1f);
                window.addActor(hpBar);
            }
            
            //exp
            {
                Label label = new Label("exp:", skin, "smaller");
                label.setPosition(10, 20, Align.bottomLeft);
                window.addActor(label);
                
                expStats = new Label("0/10", skin, "smaller");
                expStats.setPosition(290, 20, Align.bottomRight);
                expStats.setAlignment(Align.bottomRight);
                window.addActor(expStats);
                
                expBar = new ProgressBar(0.0f, 1.0f, .01f, false, skin, "simple");
                expBar.setSize(280, 6);
                expBar.setPosition(10, 12);
                expBar.setValue(0f);
                window.addActor(expBar);
            }
            
            container.addActor(window);
        }
        
        //status ailment icons
        {
            ailmentList = new Group();
            ailments = new ObjectMap<String, Image>();
        
            Image image;
            Ailment a;
            
            a = Ailment.POISON;
            image = new Image(skin, "status_" + a.toString());
            image.setSize(32f, 32f);
            image.setPosition(0f, 0f);
            image.setAlign(Align.center);
            ailments.put(a.toString(), image);
            ailmentList.addActor(image);
        
            a = Ailment.TOXIC;
            image = new Image(skin, "status_" + a.toString());
            image.setSize(32f, 32f);
            image.setPosition(0f, 0f);
            image.setAlign(Align.center);
            ailments.put(a.toString(), image);
            ailmentList.addActor(image);
            
            a = Ailment.SPRAIN;
            image = new Image(skin, "status_" + a.toString());
            image.setSize(32f, 32f);
            image.setPosition(40f, 0f);
            image.setAlign(Align.center);
            ailments.put(a.toString(), image);
            ailmentList.addActor(image);
            
            a = Ailment.ARTHRITIS;
            image = new Image(skin, "status_" + a.toString());
            image.setSize(32f, 32f);
            image.setPosition(40f, 0f);
            image.setAlign(Align.center);
            ailments.put(a.toString(), image);
            ailmentList.addActor(image);
            
            a = Ailment.CONFUSE;
            image = new Image(skin, "status_" + a.toString());
            image.setSize(32f, 32f);
            image.setPosition(80f, 0f);
            image.setAlign(Align.center);
            ailments.put(a.toString(), image);
            ailmentList.addActor(image);
            
            a = Ailment.BLIND;
            image = new Image(skin, "status_" + a.toString());
            image.setSize(32f, 32f);
            image.setPosition(120f, 0f);
            image.setAlign(Align.center);
            ailments.put(a.toString(), image);
            ailmentList.addActor(image);
            
            ailmentList.setWidth(152f);
            ailmentList.setHeight(32f);
            ailmentList.setPosition(240, 80, Align.bottom);
            container.addActor(ailmentList);
        }
    }
    
    public Group getGroup(){
        return container;
    }
    
    public void updateStats(Stats s) {
        // update stats
        hpStats.setText(String.format("%d/%d", s.hp, s.maxhp));
        expStats.setText(String.format("%d/%d", s.exp, s.nextExp));
        
        hpBar.setValue(s.hp/(float)s.maxhp);
        expBar.setValue(s.getExp()/s.nextExp);
    }
    
    public void updateProgress(Progress progress) {
        keys.setText(String.valueOf(progress.keys));
        depth.setText(String.valueOf(progress.depth));
    }

    /**
     * Perform the animation for popping in an ailment icon
     * @param icon
     */
    private void popInAilment(Image icon) {
        icon.clearActions();
        icon.addAction(
            Actions.sequence(
                Actions.alpha(0f),
                Actions.scaleTo(.2f, .2f),
                Actions.parallel(
                    Actions.alpha(1f, .1f),
                    Actions.scaleTo(1, 1, .14f, Interpolation.circleOut)
                )
            )
        );
    }

    /**
     * Perform the animation for popping in an ailment icon
     * @param icon
     */
    private void popOutAilment(Image icon) {
        icon.clearActions();
        icon.addAction(
            Actions.sequence(
                Actions.alpha(1f),
                Actions.scaleTo(1f, 1f),
                Actions.parallel(
                    Actions.alpha(0f, .1f),
                    Actions.scaleTo(.2f, .2f, .14f, Interpolation.circleOut)
                )
            )
        );
    }
    
    /**
     * Updates the visible icons for ailments
     * @param ailment
     * @param added
     *  true if the ailment is new
     */
    public void updateAilments(Ailment ailment, boolean added) {
        if (added) {
            String name = ailment.toString();
            Image icon = ailments.get(name);
            popInAilment(icon);
        } else {
            if (ailment == null) {
                for (Image i : ailments.values()) {
                    popOutAilment(i);
                }
            }
            else {
                String name = ailment.toString();
                Image icon = ailments.get(name);
                popOutAilment(icon);
            }
        }
    }
}
