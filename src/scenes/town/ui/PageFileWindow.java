package scenes.town.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import core.datatypes.StatModifier;
import core.factories.AdjectiveFactory;
import core.factories.MonsterFactory.MonsterTemplate;
import core.service.implementations.PageFile;
import core.service.interfaces.IPlayerContainer;
import scene2d.ui.extras.TabbedPane;

public class PageFileWindow {

    Group pane;
    
    TabbedPane window;
    List<MonsterTemplate> monsterList;
    Group stats;
    
    ModifierPane modifierList;
    
    public PageFileWindow(final Skin skin, PageFile pf) {
        
        pane = new Group();
        pane.setSize(400, 300);
        
        modifierList = new ModifierPane(skin, pf.getDiscoveredModifiers());
        modifierList.window.setSize(400f, 300f);
        
        ButtonGroup<Button> tabs = new ButtonGroup<Button>();
        TextButton modTab = new TextButton("Modifiers", skin, "tab");
        modTab.setUserObject(modifierList.window);
        tabs.add(modTab);
        
        TextButton monTab = new TextButton("Monsters", skin, "tab");
        TextButton statTab = new TextButton("Status", skin, "tab");
        
        window = new TabbedPane(tabs, false);
        window.setSize(200, 300);
        pane.addActor(window);
    }
    
    public Actor getWindow(){
        return pane;
    }
    
    private class ModifierPane {
        String statFormat = "%10s %.2f%%";
        
        ScrollPane window;
        
        private Label adjective;
        private Label stats;
        
        ModifierPane(Skin skin, Array<String> modifiers) {
            Table table = new Table(skin);
            
            final String size = "smaller";
            final String fmt = "%.2f%%";
            
            table.setFillParent(true);
            table.top().left();
            table.row();
            //construct header
            table.add("Adjective", size).colspan(2);
            table.add("Type", size);
            table.add("Health", size);
            table.add("Strength", size);
            table.add("Defense", size);
            table.add("Speed", size);
            
            //construct table
            for (String adj : modifiers) {
                StatModifier mod = AdjectiveFactory.getModifier(adj);
                
                table.row();
                table.add(adj, size).colspan(2);
                table.add(mod.type, size);
                table.add(String.format(fmt, adjustValue(mod.hp)), size);
                table.add(String.format(fmt, adjustValue(mod.str)), size);
                table.add(String.format(fmt, adjustValue(mod.def)), size);
                table.add(String.format(fmt, adjustValue(mod.spd)), size);
            }
            
            window = new ScrollPane(table, skin);
            window.setFillParent(true);
        }
        
        float adjustValue(float stat) {
            return (stat - 1f) * 100f;
        }
        
        void setModifier(String adj, StatModifier mod) {
            adjective.setText(adj + "\n" + mod.type);
            stats.setText(
                String.format(statFormat, "HP:", adjustValue(mod.hp)) + "\n" +
                String.format(statFormat, "Strength:", adjustValue(mod.str)) + "\n" +
                String.format(statFormat, "Defense:", adjustValue(mod.def)) + "\n" + 
                String.format(statFormat, "Speed:", adjustValue(mod.spd))
            );
        }
    }
}
