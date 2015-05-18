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
import core.datatypes.dungeon.Progress;
import core.service.interfaces.IDungeonContainer;
import core.service.interfaces.IPlayerContainer;

@SuppressWarnings("unchecked")
public class WanderUI extends UI {

    protected StateMachine menu;
    
    // logger
    Label message;

    // variables for sacrifice menu
    Image goddess;
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
            Messages.Player.UpdateItem,
            Messages.Player.NewItem,
            Messages.Player.Equipment,
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
        {
            Group hud = new Group();
            Image pane = new Image(skin, "window3");
            pane.setSize(280, 100);
            hud.addActor(pane);
            
            hud.setSize(280, 100);
            hud.setPosition(getWidth()/2f, -32f, Align.bottom);
            addActor(hud);
            //TODO info
            Label hpLabel = new Label("HP: 10/10", skin, "prompt");
            hpLabel.setPosition(8, 100, Align.topLeft);
            hud.addActor(hpLabel);
            //TODO level label
            Label floorLabel = new Label("Level: 1/68", skin, "small");
            floorLabel.setPosition(272, 40, Align.bottomRight);
            hud.addActor(floorLabel);
            //TODO keys label
            Label keyLabel = new Label("Keys: 10", skin, "small");
            keyLabel.setPosition(8, 40, Align.bottomLeft);
            hud.addActor(keyLabel);
        }
        
        //combat log
        {
            combatLog = new List<String>(skin);
            combatLog.setWidth(300f);
            combatLog.setHeight(100f);
            combatLog.setPosition(10, 40, Align.bottomLeft);
            
            combatLog.getItems().addAll("", "Test", "Test", "Test", "Test");
            addActor(combatLog);
        }
        
        //event log
        {
            eventLog = new List<String>(skin);
            eventLog.setWidth(300f);
            eventLog.setHeight(100f);
            eventLog.setPosition(getWidth()-10, 40, Align.bottomRight);
            
            eventLog.getItems().addAll("", "Test", "Test", "Test", "Test");
            addActor(eventLog);
        }
        
        //create equipment hud
        {
            Group hud = new Group();
            
            swordBar = new EquipmentBar(Equipment.Sword.class, skin);
            hud.addActor(swordBar.getActor());
            
            shieldBar = new EquipmentBar(Equipment.Shield.class, skin);
            shieldBar.getActor().setPosition(64, 0);
            hud.addActor(shieldBar.getActor());
            
            armorBar = new EquipmentBar(Equipment.Armor.class, skin);
            armorBar.getActor().setPosition(128, 0);
            hud.addActor(armorBar.getActor());
            
            hud.setSize(176, swordBar.getActor().getHeight());
            hud.setPosition(getWidth()/2f, getHeight()-50f, Align.bottom);
            addActor(hud);
        }

        fader = new Image(skin.getRegion("wfill"));
        fader.setScaling(Scaling.fill);
        fader.setPosition(0, 0);

        fader.addAction(Actions.alpha(0f));
        fader.act(0f);
        fader.setFillParent(true);
        
        addActor(fader);

        // goddess sacrifice view
        sacrificeMenu = new SacrificeSubmenu(skin, playerService);
        sacrificeMenu.getGroup().setPosition(getWidth()/2f, getHeight()/2f, Align.center);
        addActor(sacrificeMenu.getGroup());
        
        goddess = new Image(skin.getRegion(playerService.getWorship()));
        goddess.setSize(128f, 128f);
        goddess.setScaling(Scaling.stretch);
        goddess.addAction(Actions.moveTo(getWidth(), getHeight() / 2 - 64f));
        
        addActor(goddess);


        // key listener for moving the character by pressing the arrow keys or WASD
        addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent evt, int keycode) {
                if (menu.isInState(WanderState.Wander)) {
                    
                    if (Input.ACTION.match(keycode)) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Action);
                        return true;
                    }
                    
                    if (Input.ACCEPT.match(keycode)) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Assist);
                        return true;
                    }
                    
                    if (Input.CANCEL.match(keycode)) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Zoom);
                        return true;
                    }
                    
                    Direction to = Direction.valueOf(keycode);
                    if (to != null) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Movement, to);
                        //Gdx.app.log("Wander", "moving player - " + to);
                        return true;
                    }
                }
                if (menu.isInState(WanderState.Assist)) {
                    if (Input.CANCEL.match(keycode)) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Close);
                        return true;
                    }
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
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Movement, to);
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
    }

    void showGoddess(String string) {

        goddess.clearActions();
        goddess.addAction(Actions.moveTo(getWidth() - 128f, getHeight() / 2 - 64f, .3f));

    }

    void hideGoddess() {
        goddess.clearActions();
        goddess.addAction(Actions.moveTo(getWidth(), getHeight() / 2 - 64f, .3f));
    }

    void showLoot() {

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
