package scenes.dungeon.ui;

import scene2d.ui.extras.SimpleWindow;

import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entries;
import com.badlogic.gdx.utils.ObjectMap.Entry;

import core.components.Stats;
import core.datatypes.Ailment;
import core.datatypes.dungeon.Progress;

public class HUD {

    private Group container;
    private Label hpStats;
    private ProgressBar hpBar;
    
    private Label keys;
    private Label depth;
    
    private Group ailmentList;
    private ObjectMap<String, Icon> ailments;
    
    private Label spells;
    
    private class Icon {
        Image icon;
        boolean visible;
    }
    
    public HUD(Skin skin) {
    
        container = new Group();
        container.setSize(480, 160);
        
        //left component
        {
            SimpleWindow window = new SimpleWindow(skin, "round");
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
            SimpleWindow window = new SimpleWindow(skin, "round");
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
            SimpleWindow window = new SimpleWindow(skin, "round");
            window.setSize(300, 72);
            window.setPosition(90, 0);
            
            //hp
            {
                Label label = new Label("HP:", skin, "prompt");
                label.setPosition(12, 30, Align.bottomLeft);
                window.addActor(label);
                
                hpStats = new Label("10/10", skin, "prompt");
                hpStats.setPosition(240, 30, Align.bottomRight);
                hpStats.setAlignment(Align.bottomRight);
                window.addActor(hpStats);
                
                hpBar = new ProgressBar(0.0f, 1.0f, .01f, false, skin, "simple");
                hpBar.setSize(240, 20);
                hpBar.setPosition(10, 10);
                hpBar.setValue(1f);
                window.addActor(hpBar);
            }
            
            //spells
            {
                Image image = new Image(skin, "magic");
                image.setSize(32, 32);
                image.setPosition(274, 30, Align.bottom);
                window.addActor(image);
                
                Label label = spells = new Label("0", skin, "promptsm");
                label.setPosition(274, 10, Align.bottom);
                label.setAlignment(Align.bottom);
                window.addActor(label);
            }
            
            container.addActor(window);
        }
        
        //status ailment icons
        {
            ailmentList = new Group();
            ailments = new ObjectMap<String, Icon>();
        
            
            Object[] pos = {Ailment.POISON, 0, Ailment.TOXIC, 0, Ailment.SPRAIN, 40, Ailment.ARTHRITIS, 40, Ailment.CONFUSE, 80, Ailment.BLIND, 120};
            
            for (int i = 0, n = 1; i < pos.length; i += 2, n += 2){
                Ailment a = (Ailment)pos[i];
                int x = (int)pos[n];
                Image image = new Image(skin, "status_" + a.toString());
                image.setSize(32f, 32f);
                image.setPosition(x, 0f);
                image.setAlign(Align.center);
                image.setColor(1,1,1,0);
                Icon icon = new Icon();
                icon.icon = image;
                ailments.put(a.toString(), icon);
                ailmentList.addActor(image);
            }
            
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
        hpBar.setValue(s.hp/(float)s.maxhp);
        
        spells.setText(String.valueOf(s.getSpells()));
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
            Icon icon = ailments.get(name);
            if (!icon.visible) {
                popInAilment(icon.icon);
                icon.visible = true;
            }
        } else {
            if (ailment == null) {
                Entries<String, Icon> e = ailments.entries();
                while (e.hasNext) {
                    Entry<String, Icon> entry = e.next();
                    Image image = entry.value.icon;
                    if (entry.value.visible) {
                        popOutAilment(image);
                        entry.value.visible = false;
                    }
                }
            }
            else {
                String name = ailment.toString();
                Icon icon = ailments.get(name);
                if (icon.visible) {
                    popOutAilment(icon.icon);
                    icon.visible = false;
                }
            }
        }
    }
}
