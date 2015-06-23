package scenes.town.ui;

import github.nhydock.ssm.ServiceManager;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;

import core.common.Input;
import core.components.Stats;
import core.datatypes.StatModifier;
import core.factories.AdjectiveFactory;
import core.factories.MonsterFactory.MonsterTemplate;
import core.service.implementations.PageFile;
import core.service.implementations.PageFile.NumberValues;
import core.service.implementations.PageFile.StringValues;
import core.service.interfaces.IPlayerContainer;
import scene2d.ui.extras.ScrollFollower;
import scene2d.ui.extras.TabbedPane;
import scenes.Messages;

public class PageFileWindow {

    Group pane;
    
    TabbedPane window;
    Group stats;
    
    ModifierPane modifierList;
    MonsterPane monsterList;
    StatusPane statusPane;
    
    public PageFileWindow(final Skin skin, IPlayerContainer player, PageFile pf) {
        
        pane = new Group();
        pane.setSize(600, 300);
        
        modifierList = new ModifierPane(skin, pf.getDiscoveredModifiers(), pf);
        monsterList = new MonsterPane(skin, pf.getDiscoveredMonsters());
        statusPane = new StatusPane(skin, player, pf);
        
        ButtonGroup<Button> tabs = new ButtonGroup<Button>();
        TextButton modTab = new TextButton("Modifiers", skin, "tab");
        modTab.setUserObject(modifierList.pane);
        tabs.add(modTab);
        
        TextButton monTab = new TextButton("Monsters", skin, "tab");
        monTab.setUserObject(monsterList.window);
        tabs.add(monTab);
        
        TextButton statTab = new TextButton("Status", skin, "tab");
        statTab.setUserObject(statusPane.window);
        tabs.add(statTab);
        
        window = new TabbedPane(tabs, false);
        window.setSize(600, 300);
        pane.addActor(window);
        
        pane.addListener(new InputListener(){
            @Override
            public boolean keyDown(InputEvent evt, int keycode){
                if (Input.LEFT.match(keycode)){
                    window.prevTab();
                    return true;
                }
                if (Input.RIGHT.match(keycode)){
                    window.nextTab();
                    return true;
                }
                if (Input.UP.match(keycode)){
                    if (window.getOpenTab() == modifierList.pane){
                        float y = modifierList.frame.getScrollY();
                        y = Math.max(0, y-20);
                        modifierList.frame.setScrollY(y);
                    } else if (window.getOpenTab() == monsterList.window){
                        if (monsterList.monsterList.getItems().size > 0) {
                            int selected = monsterList.monsterList.getSelectedIndex();
                            selected = Math.max(0, selected-1);
                            monsterList.monsterList.setSelectedIndex(selected);
                        }
                    }
                    return true;
                }
                if (Input.DOWN.match(keycode)){
                    if (window.getOpenTab() == modifierList.pane){
                        float y = modifierList.frame.getScrollY();
                        y = Math.min(modifierList.frame.getMaxY(), y+20);
                        modifierList.frame.setScrollY(y);
                    } else if (window.getOpenTab() == monsterList.window){
                        if (monsterList.monsterList.getItems().size > 0) {
                            int selected = monsterList.monsterList.getSelectedIndex();
                            selected = Math.min(monsterList.monsterList.getItems().size-1, selected+1);
                            monsterList.monsterList.setSelectedIndex(selected);
                        }
                    }
                    return true;
                }
                if (Input.CANCEL.match(keycode)){
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Close);
                    return true;
                }
                return false;
            }
        });
    }
    
    public Actor getWindow(){
        return pane;
    }
    
    private static class ModifierPane {
        Group pane;
        ScrollPane frame;
        
        private static final String fmt = "%.2f%%";
        
        ModifierPane(Skin skin, Array<String> modifiers, PageFile pf) {
            final String size = "list";
            
            Table header = new Table(skin);
            header.setBackground(skin.getTiledDrawable("fill"));
            header.top().left();
            header.pad(10f).padTop(1f).padRight(16f);
            header.row();
            //construct header
            header.add("Adjective", size).width(100f).align(Align.center).expandX().fillX();
            header.add("Type", size).width(75f).align(Align.center).expandX().fillX();
            header.add("Health", size).align(Align.left).expandX().fillX();
            header.add("Strength", size).align(Align.left).expandX().fillX();
            header.add("Defense", size).align(Align.left).expandX().fillX();
            header.add("Speed", size).align(Align.left).expandX().fillX();
            
            Table table = new Table(skin);
            table.pad(10f);
            table.padTop(20f);
            table.top().left();
            table.row();
            
            //construct table
            for (String adj : modifiers) {
                StatModifier mod = AdjectiveFactory.getModifier(adj);
                table.row();
                table.add(adj, size).width(100f).align(Align.left).expandX().fillX();
                
                if (pf.hasUnlocked(adj)){
                    table.add(mod.type, size).width(75f).expandX().fillX();
                    table.add(adjustValue(mod.hp), size).expandX().fillX();
                    table.add(adjustValue(mod.str), size).expandX().fillX();
                    table.add(adjustValue(mod.def), size).expandX().fillX();
                    table.add(adjustValue(mod.spd), size).expandX().fillX();
                } else {
                    table.add("???", size).width(75f).expandX().fillX();
                    table.add("???", size).expandX().fillX();
                    table.add("???", size).expandX().fillX();
                    table.add("???", size).expandX().fillX();
                    table.add("???", size).expandX().fillX();
                }
            }
            
            frame = new ScrollPane(table, skin);
            frame.setScrollbarsOnTop(false);
            frame.setFadeScrollBars(false);
            frame.setScrollBarPositions(false, true);
            frame.setFillParent(true);
            
            pane = new Group();
           
            pane.addActor(frame);
            pane.addActor(header);
            header.setWidth(584f);
            header.setHeight(24f);
            header.setPosition(1, 270f, Align.topLeft);
            
        }
        
        String adjustValue(float stat) {
            float val = (stat - 1f) * 100f;
            return (val == 0f)?"---":((val > 0)?"+":"")+String.format(fmt, val);
        }
    }

    private static class MonsterPane {
        Group window;
        private Skin skin;
        private List<MonsterTemplate> monsterList;
        
        private Image sprite;
        private Label name;
        private Label type;
        private Label statsA, statsB;
        
        MonsterPane(Skin skin, Array<MonsterTemplate> monsters) {
            this.skin = skin;
            
            window = new Window("", skin, "pane");
            window.setSize(600f, 270f);
            
            monsterList = new List<MonsterTemplate>(skin);
            monsterList.setItems(monsters);
            monsterList.addListener(new ChangeListener(){

                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    MonsterTemplate temp = monsterList.getSelected();
                    if (temp != null)
                    {
                        setMonster(temp);
                    }
                }
                
            });
            
            ScrollPane pane = new ScrollPane(monsterList, skin, "log");
            pane.setScrollBarPositions(false, true);
            pane.setFadeScrollBars(false);
            pane.setScrollbarsOnTop(false);
            pane.setPosition(4, 8, Align.bottomLeft);
            pane.setHeight(260f);
            pane.setWidth(190f);
            pane.addListener(new ScrollFollower(pane, monsterList));
            window.addActor(pane);
            
            Window details = new Window("", skin, "pane");
            details.setSize(400f, 270f);
            details.setPosition(200f, 0f);
            
            sprite = new Image();
            sprite.setSize(64f, 64f);
            sprite.setPosition(40f, 250f, Align.topLeft);
            details.addActor(sprite);
            
            name = new Label("", skin, "prompt");
            name.setAlignment(Align.topLeft);
            name.setPosition(130f, 250f, Align.topLeft);
            details.addActor(name);
            
            type = new Label("", skin, "promptsm");
            type.setAlignment(Align.topLeft);
            type.setPosition(130f, 218f, Align.topLeft);
            details.addActor(type);
            
            statsA = new Label("", skin, "small");
            statsA.setAlignment(Align.topLeft);
            statsA.setPosition(40f, 180f, Align.topLeft);
            details.addActor(statsA);
            
            statsB = new Label("", skin, "small");
            statsB.setAlignment(Align.topLeft);
            statsB.setPosition(220f, 180f, Align.topLeft);
            details.addActor(statsB);
            
            window.addActor(details);
            
        }
        
        public void setMonster(MonsterTemplate monster){
            int depth = ServiceManager.getService(PageFile.class).get(monster);
            sprite.setDrawable(skin, monster.getType());
            name.setText(monster.toString());
            type.setText(monster.getType());
            
            statsA.setText(
                "Level 1\n \n" +
                "Health: "    + monster.getHp(1) + "\n" +
                "Strength:  " + monster.getStr(1) + "\n" +
                "Defense:   " + monster.getDef(1) + "\n" +
                "Speed:     " + monster.getSpd(1) + "\n"
            );
            
            statsB.setText(
                "Level " + depth + "\n \n" +
                "Health:    " + monster.getHp(depth) + "\n" +
                "Strength:  " + monster.getStr(depth) + "\n" +
                "Defense:   " + monster.getDef(depth) + "\n" +
                "Speed:     " + monster.getSpd(depth) + "\n"
            );
        }
    }

    private static class StatusPane {
        Window window;
        
        Label strengthLabel;
        Label defenseLabel;
        Label speedLabel;
        Label vitalityLabel;
        
        StatusPane(Skin skin, IPlayerContainer service, PageFile tracker){

            window = new Window("", skin, "pane");
            window.setSize(600f, 270f);
            
            Image sprite = new Image(skin, service.getGender());
            sprite.setSize(64, 64);
            sprite.setPosition(20, 260, Align.topLeft);
            window.addActor(sprite);
            
            Stats stats = Stats.Map.get(service.getPlayer());
            Label level = new Label("Level: " + stats.getLevel(), skin, "prompt");
            level.setAlignment(Align.topLeft);
            level.setPosition(100, 260, Align.topLeft);
            window.addActor(level);
            
            //stats
            {
                Table statTable = new Table(skin);
                statTable.pad(8f).padTop(50f).padBottom(10f).padRight(16f);
                statTable.top();
                
                //strength
                {
                    Label title = new Label("Strength", skin, "small");
                    Label value = strengthLabel = new Label(String.valueOf(stats.getStrength()), skin, "small");

                    title.setAlignment(Align.left);
                    value.setAlignment(Align.right);
                    statTable.add(title).expandX().fillX();
                    statTable.add(value).expandX().fillX();
                    statTable.row();
                }

                //defense
                {
                    Label title = new Label("Defense", skin, "small");
                    Label value = defenseLabel = new Label(String.valueOf(stats.getDefense()), skin, "small");

                    title.setAlignment(Align.left);
                    value.setAlignment(Align.right);
                    statTable.add(title).expandX().fillX();
                    statTable.add(value).expandX().fillX();
                    statTable.row();
                }

                //Vitality
                {
                    Label title = new Label("Vitality", skin, "small");
                    Label value = vitalityLabel = new Label(String.valueOf(stats.getVitality()), skin, "small");

                    title.setAlignment(Align.left);
                    value.setAlignment(Align.right);
                    statTable.add(title).expandX().fillX();
                    statTable.add(value).expandX().fillX();
                    statTable.row();
                }

                //Speed
                {
                    Label title = new Label("Speed", skin, "small");
                    Label value = speedLabel = new Label(String.valueOf((int)stats.getSpeed()), skin, "small");

                    title.setAlignment(Align.left);
                    value.setAlignment(Align.right);
                    statTable.add(title).expandX().fillX();
                    statTable.add(value).expandX().fillX();
                    statTable.row();
                }
                
                statTable.setSize(220f, 80f);
                statTable.setPosition(150f, 220f, Align.top);
                window.addActor(statTable);
            }
            
            {
                Table header = new Table(skin);

                Label score = new Label(String.format("Score: %09d", tracker.score()), skin, "small");
                score.setAlignment(Align.center);
                header.top();
                header.add(score).expandX().fillX();
                header.row();
                Label rank = new Label(tracker.rank(), skin, "promptsm");
                rank.setAlignment(Align.center);
                header.add(rank).expandX().fillX();

                header.setSize(300f, 50f);
                header.setPosition(300f, 260f, Align.topLeft);
                
                Table scoring = new Table(skin);
                scoring.setSize(290f, 210f);
                scoring.pad(8f).padTop(50f).padBottom(10f);
                scoring.bottom();
                for (NumberValues val : PageFile.NumberValues.values()) {
                    Label title = new Label(val.toString(), skin, "smaller");
                    Label value = new Label(tracker.toString(val), skin, "smaller");

                    scoring.row().expandX();
                    title.setAlignment(Align.left);
                    value.setAlignment(Align.right);
                    scoring.add(title).width(200f);
                    scoring.add(value).expandX().fillX();
                }

                scoring.add().expandX().height(16f);
                scoring.row();

                scoring.setPosition(310f, 10f, Align.bottomLeft);

                Table stringScoring = new Table(skin);
                stringScoring.setSize(290f, 120f);
                stringScoring.pad(8f).padBottom(10f);
                stringScoring.bottom();
                for (StringValues val : PageFile.StringValues.values()) {
                    Label title = new Label(val.toString(), skin, "smaller");
                    Label value = new Label(String.format("%s", tracker.max(val)), skin, "smaller");

                    stringScoring.row();
                    title.setAlignment(Align.left);
                    value.setAlignment(Align.right);
                    stringScoring.add(title).width(200f);
                    stringScoring.add(value).expandX().fillX();
                }

                stringScoring.setPosition(10f, 10f, Align.bottomLeft);
                
                window.addActor(header);
                window.addActor(scoring);
                window.addActor(stringScoring);
            }
        }
        
        public void updateStats(Stats s){
            strengthLabel.setText(String.valueOf(s.getStrength()));
            defenseLabel.setText(String.valueOf(s.getDefense()));
            vitalityLabel.setText(String.valueOf(s.getVitality()));
            speedLabel.setText(String.valueOf((int)s.getSpeed()));
        }
    }

    public void updateStats(Stats s) {
        statusPane.updateStats(s);
    }
}
