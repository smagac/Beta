package scenes.dungeon.ui;

import github.nhydock.ssm.Inject;
import scene2d.ui.extras.LabeledTicker;
import scene2d.ui.extras.ParticleActor;
import scene2d.ui.extras.Pointer;
import scenes.Messages;
import scenes.Messages.Dungeon.CombatNotify;
import scenes.Scene;
import scenes.UI;
import scenes.dungeon.RenderSystem;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.Scaling;

import core.DataDirs;
import core.components.Combat;
import core.components.Equipment;
import core.components.Identifier;
import core.components.Stats;
import core.datatypes.Ailment;
import core.datatypes.dungeon.Progress;
import core.service.interfaces.IDungeonContainer;
import core.service.interfaces.IPlayerContainer;

public class WanderUI extends UI {

    Group display;
    
    // logger
    Label message;

    // variables for sacrifice menu
    Image fader;
    
    ParticleActor weather;

    // services
    @Inject public IPlayerContainer playerService;
    @Inject public IDungeonContainer dungeonService;
    
    private EquipmentBar swordBar;
    private EquipmentBar shieldBar;
    private EquipmentBar armorBar;
    
    List<String> combatLog;
    List<String> eventLog;

    AssistMenu assistMenu;
    SacrificeSubmenu sacrificeMenu;
    
    private Label enemyLabel;
    private Label lootLabel;

    Group messageWindow;

    private HUD hud;

    Window floorSelect;

    TrainMenu trainingMenu;
    
    @Override
    protected void listenTo(IntSet messages) {
        super.listenTo(messages);
        messages.addAll(
            Messages.Dungeon.Movement, 
            Messages.Dungeon.Notify, 
            Messages.Dungeon.Dead,
            Messages.Dungeon.Exit,
            Messages.Dungeon.Leave,
            Messages.Dungeon.Refresh,
            Messages.Dungeon.Action,
            Messages.Dungeon.Assist,
            Messages.Dungeon.Target,
            Messages.Dungeon.Heal,
            Messages.Dungeon.Leave,
            Messages.Dungeon.Warp,
            Messages.Dungeon.Sacrifice,
            Messages.NPC.TRAINER,
            Messages.Player.LevelUp,
            Messages.Player.UpdateItem,
            Messages.Player.NewItem,
            Messages.Player.Equipment,
            Messages.Player.Progress,
            Messages.Player.Stats,
            Messages.Player.Time,
            Messages.Player.AddAilment,
            Messages.Player.RemoveAilment,
            Messages.Interface.Close,
            Messages.Interface.Notify,
            Messages.Interface.Button
        );
    }
    
    public WanderUI(Scene scene, AssetManager manager) {
        super(scene, manager);
        
        stateMachine = new DefaultStateMachine<WanderUI>(this);
        stateMachine.setGlobalState(WanderState.Global);
    }
    
    @Override
    public void init() {
        this.clear();
        final WanderUI self = this;
        skin = shared.getResource(DataDirs.Home + "uiskin.json", Skin.class);

        display = new Group();
        
        //user hud
        hud = new HUD(skin);
        hud.updateStats(Stats.Map.get(playerService.getPlayer()));
        hud.updateProgress(dungeonService.getProgress());
        hud.getGroup().setPosition(getWidth()/2f, -8, Align.bottom);
        hud.updateAilments(null, false);
        display.addActor(hud.getGroup());
        
        //combat log
        {
            combatLog = new List<String>(skin);
            combatLog.setWidth(300f);
            combatLog.setHeight(100f);
            combatLog.setPosition(10, 50, Align.bottomLeft);
            combatLog.setTouchable(Touchable.disabled);
            
            combatLog.getItems().addAll("", "", "", "", "");
            addActor(combatLog);
            
            Image icon = new Image(skin, "ogre");
            enemyLabel = new Label("10/10", skin, "prompt"); 
            icon.setSize(32, 32);
            icon.setPosition(4, 4);
            enemyLabel.setPosition(42, 0, Align.bottomLeft);
            enemyLabel.setAlignment(Align.bottomLeft);
            
            display.addActor(icon);
            display.addActor(enemyLabel);
        }
        display.addActor(combatLog);
        
        //event log
        {
            eventLog = new List<String>(skin);
            eventLog.setWidth(300f);
            eventLog.setHeight(100f);
            eventLog.setPosition(getWidth()-10, 50, Align.bottomRight);
            eventLog.setTouchable(Touchable.disabled);
            eventLog.getItems().addAll("", "", "", "", "");
            addActor(eventLog);
            
            Image icon = new Image(skin, "loot");
            lootLabel = new Label("10", skin, "prompt"); 
            icon.setSize(32, 32);
            icon.setPosition(getWidth()-4, 4, Align.bottomRight);
            lootLabel.setPosition(getWidth()-42, 0, Align.bottomRight);
            lootLabel.setAlignment(Align.bottomRight);
            
            display.addActor(icon);
            display.addActor(lootLabel);
        }
        display.addActor(eventLog);
        
        //create equipment hud
        {
            Group group = new Group();
            
            swordBar = new EquipmentBar(Equipment.Sword.class, skin);
            group.addActor(swordBar.getActor());
            
            shieldBar = new EquipmentBar(Equipment.Shield.class, skin);
            shieldBar.getActor().setPosition(64, 0);
            group.addActor(shieldBar.getActor());
            
            armorBar = new EquipmentBar(Equipment.Armor.class, skin);
            armorBar.getActor().setPosition(128, 0);
            group.addActor(armorBar.getActor());
            
            group.setSize(176, swordBar.getActor().getHeight());
            group.setPosition(getWidth()/2f, getHeight()-50f, Align.bottom);
            display.addActor(group);
        }
        
        //add goddess menu button
        {
            Image sacrificeIcon = new Image(skin, playerService.getWorship());
            sacrificeIcon.setSize(48, 48);
            sacrificeIcon.setPosition(getWidth()-10, getHeight()-10, Align.topRight);
            sacrificeIcon.addListener(new InputListener(){
                @Override
                public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                    if (stateMachine.isInState(WanderState.Wander)) {
                        if (button == Buttons.LEFT) {
                            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Assist);
                            return true;
                        }
                    }
                    return false;
                }
            });
            display.addActor(sacrificeIcon);
        }
        
        // goddess sacrifice view
        sacrificeMenu = new SacrificeSubmenu(skin, playerService, this);
        assistMenu = new AssistMenu(skin, sacrificeMenu);
        assistMenu.getGroup().setPosition(getWidth()/2f, getHeight()/2f, Align.center);
        display.addActor(assistMenu.getGroup());
        
        trainingMenu = new TrainMenu(skin, sacrificeMenu);
        trainingMenu.getGroup().setPosition(getWidth()/2f, getHeight()/2f, Align.center);
        display.addActor(trainingMenu.getGroup());
        
        addActor(display);
        
        fader = new Image(skin.getRegion("wfill"));
        fader.setScaling(Scaling.fill);
        fader.setPosition(0, 0);

        fader.addAction(Actions.alpha(0f));
        fader.act(0f);
        fader.setFillParent(true);
        fader.setTouchable(Touchable.disabled);
        
        addActor(fader);

        //prompts
        {
            messageWindow = new Group();
            
            Window window = new Window("", skin, "thick");
            window.setSize(550, 300);
            
            message = new Label("", skin, "prompt");
            message.setSize(490, 280);
            message.setAlignment(Align.center);
            message.setPosition(275, 150, Align.center);
            message.setWrap(true);
            window.addActor(message);
            
            messageWindow.setSize(550, 300);
            messageWindow.setOrigin(Align.center);
            messageWindow.setPosition(getWidth()/2f, getHeight()/2f, Align.center);
            messageWindow.setColor(1,1,1,0);
            messageWindow.addActor(window);
            messageWindow.setTouchable(Touchable.disabled);
            Button button = new TextButton("Return Home", skin, "bigpop");
            button.setWidth(150f);
            button.setHeight(48f);
            button.setPosition(messageWindow.getWidth()/2f, 0f, Align.center);
            messageWindow.addActor(button);
            
            addActor(messageWindow);
        }
        
        //floor select
        {
            floorSelect = new Window("", skin, "square");
            floorSelect.setSize(380, 400);
            floorSelect.setPosition(getWidth()/2f, getHeight()/2f, Align.center);
            floorSelect.setOrigin(Align.center);
            
            Label prompt = new Label("This dungeon feels oddly familiar to you.\n \nDo you wish to immediately descend deeper into it?", skin, "prompt");
            prompt.setSize(360, 250);
            prompt.setWrap(true);
            prompt.setAlignment(Align.center);
            prompt.setPosition(190, 260, Align.center);
            floorSelect.addActor(prompt);
            int deepest = dungeonService.getDungeon().getDeepestTraversal();
            Integer[] vals = new Integer[deepest];
            for (int n = 0, i = 1; i <= deepest; n++, i++) {
                vals[n] = i;
            }
            LabeledTicker<Integer> ticker = new LabeledTicker<Integer>(null, vals, skin);
            ticker.setPosition(190, 110, Align.center);
            ticker.setName("ticker");
            floorSelect.addActor(ticker);
            
            TextButton button = new TextButton("Warp", skin);
            button.setSize(150, 48);
            button.setPosition(190, 20, Align.bottom);
            button.addListener(new InputListener(){
                @Override
                public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                    if (button == Buttons.LEFT) {
                        LabeledTicker<Integer> ticker = floorSelect.findActor("ticker");
                        System.out.println(ticker.getValue());
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Warp, ticker.getValue());
                    }
                    return false;
                }
            });
            floorSelect.addActor(button);
            
            floorSelect.setTouchable(Touchable.disabled);
            floorSelect.setColor(1, 1, 1, 0);
            
            addActor(floorSelect);
        }
        
        pointer = new Pointer(skin);
        addActor(pointer);
        pointer.setVisible(false);

        getRoot().setName("root");
        setKeyboardFocus(getRoot());
        // key listener for moving the character by pressing the arrow keys or WASD
        addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent evt, int keycode) {
                return (evt.isHandled())?true:((WanderState)stateMachine.getCurrentState()).keyDown(self, keycode);
            }

            @Override
            public boolean keyUp(InputEvent evt,  int keycode) {
                return (evt.isHandled())?true:((WanderState)stateMachine.getCurrentState()).keyUp(self, keycode);
            }
            
            @Override
            public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                return (evt.isHandled())?true:((WanderState)stateMachine.getCurrentState()).touchDown(self, x, y, button);
            }
            
            @Override
            public void touchUp(InputEvent evt, float x, float y, int pointer, int button) {
                if (!evt.isHandled()){
                    ((WanderState)stateMachine.getCurrentState()).touchUp(self, x, y, button);
                }
                
            }
            
            @Override
            public boolean mouseMoved(InputEvent evt, float x, float y){
                if (!evt.isHandled()){
                    Vector2 v = stageToScreenCoordinates(new Vector2(x, y));
                    ((WanderState)stateMachine.getCurrentState()).mouseMoved(self, v);
                }
                return false;
            }
        });
        
        if (dungeonService.getDungeon().getDeepestTraversal() > 1) {
            stateMachine.changeState(WanderState.SelectFloor);
        } else {
            stateMachine.changeState(WanderState.Wander);
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Warp, 1);
        }
        
        display.setColor(1,1,1,0);
    }

    @Override
    public void update(float delta) {
        if (dungeonService.getProgress().depth > 0) {
            dungeonService.getEngine().getSystem(RenderSystem.class).update(delta);
            stateMachine.update();
        }
    }
    
    /**
     * Refreshes the HUD at the top of the screen to display the proper current
     * progress of the dungeon
     */
    void refresh(Progress progress) {
        enemyLabel.setText(progress.monstersKilled + "/" + progress.monstersTotal);
        lootLabel.setText(String.valueOf(progress.lootFound));
    }

    public void fadeOut(Runnable cmd) {
        fader.addAction(
            Actions.sequence(
                Actions.alpha(0f), 
                Actions.alpha(1f, .3f), 
                Actions.run(cmd)
            )
        );
    }
    
    public void fadeIn() {
        fader.addAction(
            Actions.alpha(0f, .3f)
        );
    }
    
    public void addCombatMessage(String msg) {
        // update the battle log
        Array<String> items = combatLog.getItems();
        items.add(msg);
        if (items.size > 5) {
            items.removeIndex(0);
        }
    }
    
    public void addEventMessage(String msg) {
        // update the battle log
        Array<String> items = eventLog.getItems();
        items.add(msg);
        if (items.size > 5) {
            items.removeIndex(0);
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        if (dungeonService.getProgress().depth > 0) {
            dungeonService.getEngine().getSystem(RenderSystem.class).resize(width, height);
        }
    }

    @Override
    public boolean handleMessage(Telegram telegram) {
        if (telegram.message == Messages.Dungeon.Notify && telegram.extraInfo instanceof String) {
            addEventMessage((String)telegram.extraInfo);
            return true;
        }
        else if (telegram.message == Messages.Dungeon.Notify && telegram.extraInfo instanceof CombatNotify) {
            CombatNotify notification = (CombatNotify)telegram.extraInfo;
            audio.playSfx(DataDirs.Sounds.hit);
            int dmg = notification.dmg;
            if (dmg == -1) {
                if (notification.attacker == playerService.getPlayer()) {
                    Identifier id = Identifier.Map.get(notification.opponent);
                    String name = id.toString();
                    addCombatMessage(String.format("You attacked %s, but missed!", name));
                }
                else {
                    Identifier id = Identifier.Map.get(notification.attacker);
                    String name = id.toString();
                    addCombatMessage(String.format("%s attacked you, but missed!", name));
                }
            } else if (notification.attacker == playerService.getPlayer()) {
                Identifier id = Identifier.Map.get(notification.opponent);
                String name = id.toString();
                if (dmg == 0) {
                    addCombatMessage(String.format("%s blocked your attack!", name));
                }
                else {
                    if (notification.critical) {
                        addCombatMessage("CRITICAL HIT!");
                    }
                    addCombatMessage(String.format("You attacked %s for %d damage", name, notification.dmg));
                }
            }
            //status ailment damage
            else if (notification.cause != null) {
                if (notification.cause == Ailment.POISON) {
                    addCombatMessage(String.format("The poison damaged you for %d points", notification.dmg));
                }
                if (notification.cause == Ailment.TOXIC) {
                    addCombatMessage(String.format("The toxin damaged you for %d points", notification.dmg));
                }
                if (notification.cause == Ailment.SPRAIN) {
                    addCombatMessage(String.format("Your sprained leg ached for %d damage", notification.dmg));
                }
                if (notification.cause == Ailment.ARTHRITIS) {
                    addCombatMessage(String.format("Your joints ache for %d damage", notification.dmg));
                }
                
            }
            else {
                Identifier id = Identifier.Map.get(notification.attacker);
                String name = id.toString();
                if (dmg == 0) {
                    addCombatMessage(String.format("You blocked %s's attack", name));
                }
                else {
                    addCombatMessage(String.format("%s attacked you for %d damage", name, dmg));
                }
            }
            return true;
        }
        if (telegram.message == Messages.Dungeon.Dead && telegram.extraInfo != playerService.getPlayer()) {
            Entity opponent = (Entity)telegram.extraInfo;
            Combat combat = Combat.Map.get(opponent);
            addCombatMessage(combat.getDeathMessage(Identifier.Map.get(opponent).toString()));
            
            return true;
        }
        if (telegram.message == Messages.Dungeon.Refresh) {
            refresh((Progress) telegram.extraInfo);
            hud.updateProgress((Progress) telegram.extraInfo);
            return true;
        }
        if (telegram.message == Messages.Player.UpdateItem || 
            telegram.message == Messages.Player.NewItem) {
            Messages.Player.ItemMsg msg = (Messages.Player.ItemMsg)telegram.extraInfo;
            sacrificeMenu.updateLabel(msg.item, msg.amount);
            return true;
        }
        if (telegram.message == Messages.Player.Equipment) {
            Equipment.Piece piece = (Equipment.Piece)telegram.extraInfo;
            if (piece instanceof Equipment.Sword) {
                swordBar.setPower(piece.getPower());
                swordBar.updateDurability(piece.getDurability(), piece.getMaxDurability());
            }
            else if (piece instanceof Equipment.Shield) {
                shieldBar.setPower(piece.getPower());
                shieldBar.updateDurability(piece.getDurability(), piece.getMaxDurability());
            }
            else {
                armorBar.setPower(piece.getPower());
                armorBar.updateDurability(piece.getDurability(), piece.getMaxDurability());
            }

            return true;
        }
        if (telegram.message == Messages.Player.AddAilment) {
            Ailment ailment = (Ailment)telegram.extraInfo;
            hud.updateAilments(ailment, true);
            
            if (ailment == Ailment.BLIND) {
                dungeonService.getEngine().getSystem(RenderSystem.class).updateFOV();
            }
            
            return true;
        }
        if (telegram.message == Messages.Player.RemoveAilment) {
            Ailment ailment = (Ailment)telegram.extraInfo;
            hud.updateAilments(ailment, false);
            
            if (ailment == Ailment.BLIND || ailment == null) {
                dungeonService.getEngine().getSystem(RenderSystem.class).updateFOV();
            }
            
            return true;
        }
        if (telegram.message == Messages.Player.Stats) {
            Stats stats = playerService.getPlayer().getComponent(Stats.class);
            hud.updateStats(stats);
            return true;
        }
        return stateMachine.handleMessage(telegram);
    }

    @Override
    protected void load() {
        // TODO Auto-generated method stub
        
    }
}
