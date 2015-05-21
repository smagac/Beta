package scenes.dungeon.ui;

import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Align;

import scenes.Messages;
import core.components.Stats;
import core.service.interfaces.IPlayerContainer;

public class HUD {

    Group container;
    Label hpStats;
    ProgressBar hpBar;
    
    Label expStats;
    ProgressBar expBar;
    
    Label keys;
    Label depth;
    
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
    }
    
    public Group getGroup(){
        return container;
    }
    
    public void update(IPlayerContainer playerService) {
        // update stats
        Stats s = playerService.getPlayer().getComponent(Stats.class);
        hpStats.setText(String.format("%d/%d", s.hp, s.maxhp));
        expStats.setText(String.format("%d/%d", s.exp, s.nextExp));
    }

}
