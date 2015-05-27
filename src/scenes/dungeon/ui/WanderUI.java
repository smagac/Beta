package scenes.dungeon.ui;

import github.nhydock.ssm.Inject;
import scene2d.ui.ScrollOnChange;
import scene2d.ui.extras.Card;
import scene2d.ui.extras.FocusGroup;
import scene2d.ui.extras.ParticleActor;
import scene2d.ui.extras.ScrollFocuser;
import scene2d.ui.extras.ScrollFollower;
import scene2d.ui.extras.TabbedPane;
import scenes.GameUI;
import scenes.Messages;
import scenes.Messages.Dungeon.CombatNotify;
import scenes.UI;
import scenes.dungeon.Direction;
import scenes.dungeon.RenderSystem;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.SnapshotArray;

import core.DataDirs;
import core.common.Input;
import core.components.Combat;
import core.components.Equipment;
import core.components.Identifier;
import core.components.Stats;
import core.datatypes.Ailment;
import core.datatypes.dungeon.Progress;
import core.service.interfaces.IDungeonContainer;
import core.service.interfaces.IPlayerContainer;

@SuppressWarnings("unchecked")
public class WanderUI extends UI {

    protected StateMachine menu;
    
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

    SacrificeSubmenu sacrificeMenu;

    private Label enemyLabel;
    private Label lootLabel;

    Group messageWindow;

    private HUD hud;
    
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
            Messages.Dungeon.Zoom,
            Messages.Dungeon.Heal,
            Messages.Dungeon.Leave,
            Messages.Dungeon.LevelUp,
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
    
    public WanderUI(AssetManager manager) {
        super(manager);

        menu = new DefaultStateMachine<WanderUI>(this, WanderState.Wander);
    }
    
    @Override
    public void init() {
        this.clear();
        skin = shared.getResource(DataDirs.Home + "uiskin.json", Skin.class);

        //user hud
        hud = new HUD(skin);
        hud.updateStats(Stats.Map.get(playerService.getPlayer()));
        hud.updateProgress(dungeonService.getProgress());
        hud.getGroup().setPosition(getWidth()/2f, -8, Align.bottom);
        hud.updateAilments(null, false);
        addActor(hud.getGroup());
        
        //combat log
        {
            combatLog = new List<String>(skin);
            combatLog.setWidth(300f);
            combatLog.setHeight(100f);
            combatLog.setPosition(10, 50, Align.bottomLeft);
            
            combatLog.getItems().addAll("", "Test", "Test", "Test", "Test");
            addActor(combatLog);
            
            Image icon = new Image(skin, "ogre");
            enemyLabel = new Label("10/10", skin, "prompt"); 
            icon.setSize(32, 32);
            icon.setPosition(4, 4);
            enemyLabel.setPosition(42, 0, Align.bottomLeft);
            enemyLabel.setAlignment(Align.bottomLeft);
            
            addActor(icon);
            addActor(enemyLabel);
        }
        
        //event log
        {
            eventLog = new List<String>(skin);
            eventLog.setWidth(300f);
            eventLog.setHeight(100f);
            eventLog.setPosition(getWidth()-10, 50, Align.bottomRight);
            
            eventLog.getItems().addAll("", "Test", "Test", "Test", "Test");
            addActor(eventLog);
            
            Image icon = new Image(skin, "loot");
            lootLabel = new Label("10", skin, "prompt"); 
            icon.setSize(32, 32);
            icon.setPosition(getWidth()-4, 4, Align.bottomRight);
            lootLabel.setPosition(getWidth()-42, 0, Align.bottomRight);
            lootLabel.setAlignment(Align.bottomRight);
            
            addActor(icon);
            addActor(lootLabel);
        }
        
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
            addActor(group);
        }

        fader = new Image(skin.getRegion("wfill"));
        fader.setScaling(Scaling.fill);
        fader.setPosition(0, 0);

        fader.addAction(Actions.alpha(0f));
        fader.act(0f);
        fader.setFillParent(true);
        
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
            
            Button button = new TextButton("Return Home", skin, "bigpop");
            button.setWidth(150f);
            button.setHeight(48f);
            button.setPosition(messageWindow.getWidth()/2f, 0f, Align.center);
            messageWindow.addActor(button);
            
            addActor(messageWindow);
        }
        
        // goddess sacrifice view
        sacrificeMenu = new SacrificeSubmenu(skin, playerService, menu);
        sacrificeMenu.getGroup().setPosition(getWidth()/2f, getHeight()/2f, Align.center);
        addActor(sacrificeMenu.getGroup());

        getRoot().setName("root");
        setKeyboardFocus(getRoot());
        // key listener for moving the character by pressing the arrow keys or WASD
        addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent evt, int keycode) {
                if (menu.isInState(WanderState.Wander)) {
                    if (evt.getTarget() != getRoot())
                        return false;
                    
                    if (Input.ACCEPT.match(keycode)) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Assist);
                        return true;
                    }
                    
                    if (Input.CANCEL.match(keycode)) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Zoom);
                        return true;
                    }
                    
                    if (Input.ACTION.match(keycode)) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Action);
                        return true;
                    }
                    
                    Direction to = Direction.valueOf(keycode);
                    if (to != null) {
                        if (Input.ACTION.isPressed()){
                            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Action, to);
                        } else {
                            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Movement, to);
                        }
                        //Gdx.app.log("Wander", "moving player - " + to);
                        return true;
                    }
                }
                if (menu.isInState(WanderState.Dead) || menu.isInState(WanderState.Exit)) {
                    if (Input.ACCEPT.match(keycode) || Input.CANCEL.match(keycode)) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Close);
                        return true;
                    }
                }
                if (Input.CANCEL.match(keycode)) {
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Close);
                    return true;
                }
                
                return false;
            }

            @Override
            public boolean keyUp(InputEvent evt,  int keycode) {
                if (menu.isInState(WanderState.Wander)) {
                    Direction to = Direction.valueOf(keycode);
                    if (to != null) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Movement);
                    }
                    return true;
                }
                return false;
            }
        });
        
        addListener(new InputListener() {
           
            @Override
            public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                if (menu.isInState(WanderState.Wander)) {
                    if (button == Buttons.LEFT) {
                        Direction to = Direction.valueOf(x, y, getWidth(), getHeight());
                        if (Gdx.input.isButtonPressed(Buttons.RIGHT)) {
                            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Action, to);
                        } else {
                            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Movement, to);
                        }
                    } else if (button == Buttons.RIGHT){
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Action);
                    }
                    return true;
                }
                return false;
            }
            
            @Override
            public void touchUp(InputEvent evt, float x, float y, int pointer, int button) {
                if (menu.isInState(WanderState.Wander)) {
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Movement);
                }
            }
        });
    }

    @Override
    public void update(float delta) {
        if (dungeonService.getProgress().depth > 0) {
            dungeonService.getEngine().getSystem(RenderSystem.class).update(delta);
            menu.update();
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
            sacrificeMenu.lootList.updateLabel(msg.item, msg.amount);
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
        return menu.handleMessage(telegram);
    }

    @Override
    protected void load() {
        // TODO Auto-generated method stub
        
    }

    public void changeState(WanderState assist) {
        menu.changeState(assist);
    }
    
}
