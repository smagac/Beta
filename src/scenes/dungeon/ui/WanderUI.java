package scenes.dungeon.ui;

import github.nhydock.ssm.Inject;
import scene2d.ui.ScrollOnChange;
import scene2d.ui.extras.FocusGroup;
import scene2d.ui.extras.ParticleActor;
import scene2d.ui.extras.ScrollFocuser;
import scenes.GameUI;
import scenes.Messages;
import scenes.Messages.Dungeon.CombatNotify;
import scenes.dungeon.Direction;
import scenes.dungeon.RenderSystem;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
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
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.SnapshotArray;

import core.DataDirs;
import core.common.Input;
import core.components.Combat;
import core.components.Drop;
import core.components.Equipment;
import core.components.Equipment.Piece;
import core.components.Identifier;
import core.datatypes.Item;
import core.datatypes.dungeon.Progress;
import core.service.interfaces.IDungeonContainer;
import core.service.interfaces.IPlayerContainer;

@SuppressWarnings("unchecked")
public class WanderUI extends GameUI {

    // logger
    private ScrollPane logPane;
    private Table log;
    Label message;

    // header bar
    private Label floorLabel;
    private Label monsterLabel;
    private Label lootLabel;
    private Label keyLabel;
    
    // variables for sacrifice menu
    Group dialog;
    Image goddess;
    Image fader;
    
    private Group goddessDialog;
    private Label gMsg;
    private Table itemSubmenu;
    
    ItemList lootList;
    private ScrollPane lootPane;
    ItemList sacrificeList;
    private ScrollPane sacrificePane;
    
    private FocusGroup sacrificeGroup;
    
    ParticleActor weather;

    // services
    @Inject public IPlayerContainer playerService;
    @Inject public IDungeonContainer dungeonService;
    private FocusGroup defaultGroup;
    private EquipmentBar swordBar;
    private EquipmentBar shieldBar;
    private EquipmentBar armorBar;
    
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
            Messages.Dungeon.Zoom,
            Messages.Player.UpdateItem,
            Messages.Player.NewItem,
            Messages.Player.Equipment
        );
    }
    
    public WanderUI(AssetManager manager) {
        super(manager);

        menu = new DefaultStateMachine<WanderUI>(this, WanderState.Wander);
    }
    
    @Override
    protected void extend() {

        messageWindow.clear();

        // header bar
        {
            Table table = new Table();
            TextureAtlas atlas = shared.getResource(DataDirs.Home + "dungeon.atlas", TextureAtlas.class);
            table.setBackground(new TextureRegionDrawable(atlas.findRegion("header")));
            table.setHeight(32f);
            table.setWidth(display.getWidth());

            // monsters
            {
                Table set = new Table();
                Image icon = new Image(atlas.findRegion("ogre"));
                Label label = monsterLabel = new Label("0/0", skin, "prompt");

                set.add(icon).size(32f).padRight(10f);
                set.add(label).expandX().fillX();
                table.add(set).expandX().align(Align.left).colspan(1).padLeft(10f);
            }

            // treasure
            {
                Table set = new Table();
                Image icon = new Image(atlas.findRegion("loot"));
                Label label = lootLabel = new Label("0", skin, "prompt");

                set.add(icon).size(32f).padRight(10f);
                set.add(label).expandX().fillX();
                table.add(set).expandX().colspan(1);
            }
            // keys
            {
                Table set = new Table();
                Image icon = new Image(atlas.findRegion("key"));
                Label label = keyLabel = new Label("0", skin, "prompt");

                set.add(icon).size(32f).padRight(10f);
                set.add(label).expandX().fillX();
                table.add(set).expandX().colspan(1);
            }
                        
            // floors
            {
                Table set = new Table();
                Image icon = new Image(atlas.findRegion("up"));
                Label label = floorLabel = new Label("Floor 0/0", skin, "prompt");

                set.add(icon).size(32f).padRight(10f);
                set.add(label).expandX().fillX();
                table.add(set).expandX().align(Align.right).colspan(1).padRight(10f);
            }
            table.setPosition(0, display.getHeight() - 32f);
            display.addActor(table);
        }

        //create equipment hud
        {
            Group hud = new Group();
            
            swordBar = new EquipmentBar(Equipment.Sword.class, skin);
            hud.addActor(swordBar.getActor());
            
            shieldBar = new EquipmentBar(Equipment.Shield.class, skin);
            shieldBar.getActor().setPosition(0, 48);
            hud.addActor(shieldBar.getActor());
            
            armorBar = new EquipmentBar(Equipment.Armor.class, skin);
            armorBar.getActor().setPosition(0, 96);
            hud.addActor(armorBar.getActor());
            
            hud.setSize(48, swordBar.getActor().getHeight());
            hud.setPosition(getDisplayWidth()-10, 10, Align.bottomRight);
            display.addActor(hud);
        }

        fader = new Image(skin.getRegion("wfill"));
        fader.setScaling(Scaling.fill);
        fader.setPosition(0, 0);

        fader.addAction(Actions.alpha(0f));
        fader.act(0f);
        fader.setFillParent(true);
        display.addActor(fader);

        // combat log
        {
            log = new Table(skin);
            log.setWidth(messageWindow.getWidth());
            logPane = new ScrollPane(log, skin, "log");
            logPane.setSize(messageWindow.getWidth(), messageWindow.getHeight());
            logPane.addListener(new ScrollFocuser(logPane));
            messageWindow.addActor(logPane);
        }

        // goddess sacrifice view
        {
            dialog = makeWindow(skin, 400, 250, true);
            message = new Label("", skin, "promptsm");
            message.setWrap(true);
            message.setAlignment(Align.center);
            message.setPosition(40f, 40f);
            message.setWidth(320f);
            message.setHeight(170f);
            dialog.addActor(message);
            dialog.setVisible(false);

            dialog.setPosition(display.getWidth() / 2 - dialog.getWidth() / 2,
                    display.getHeight() / 2 - dialog.getHeight() / 2);
            display.addActor(dialog);

            // loot List and buttons
            {
                itemSubmenu = new Table();
                itemSubmenu.setWidth(460f);
                itemSubmenu.setHeight(200f);
                itemSubmenu.setPosition(66f, 10f);

                Label lootLabel = new Label("My Loot", skin, "header");
                lootLabel.setAlignment(Align.center);
                itemSubmenu.top().add(lootLabel).width(230f).pad(4f).padBottom(0f);
                Label sacrificeLabel = new Label("Sacrifice", skin, "header");
                sacrificeLabel.setAlignment(Align.center);
                itemSubmenu.top().add(sacrificeLabel).width(230f).pad(4f).padBottom(0f);
                itemSubmenu.row();

                lootList = new ItemList(skin);
                lootList.list.setTouchable(Touchable.childrenOnly);
                lootList.setItems(playerService.getInventory().getLoot());
                
                lootPane = new ScrollPane(lootList.list, skin);
                lootPane.setScrollingDisabled(true, false);
                lootPane.setScrollbarsOnTop(false);
                lootPane.setScrollBarPositions(true, false);
                lootPane.setFadeScrollBars(false);
                lootPane.addListener(new ScrollFocuser(lootPane));
                
                itemSubmenu.add(lootPane).width(230f).expandY().fillY().pad(4f).padTop(0f);

                sacrificeList = new ItemList(skin);
                sacrificeList.list.setTouchable(Touchable.childrenOnly);
                
                lootList.setSwapList(sacrificeList);
                sacrificeList.setSwapList(lootList);
                
                sacrificePane = new ScrollPane(sacrificeList.list, skin);
                sacrificePane.setScrollingDisabled(true, false);
                sacrificePane.setScrollbarsOnTop(false);
                sacrificePane.setScrollBarPositions(true, false);
                sacrificePane.setFadeScrollBars(false);
                sacrificePane.addListener(new ScrollFocuser(sacrificePane));

                itemSubmenu.add(sacrificePane).width(230f).expandY().fillY().pad(4f).padTop(0f);

                itemSubmenu.addAction(Actions.alpha(0f));
                display.addActor(itemSubmenu);

            }

            sacrificeGroup = new FocusGroup(buttonList, lootList.list, sacrificeList.list);
            sacrificeGroup.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (focusList() == null)
                        return;

                    Actor a = focusList().getFocused();

                    if (a == lootList.list) {
                        showPointer(lootPane, Align.left, Align.top);
                    }
                    else if (a == sacrificeList.list) {
                        showPointer(sacrificePane, Align.right, Align.top);
                    }
                    else {
                        hidePointer();
                    }

                    setFocus(a);
                }
            });

            ScrollOnChange lootPaneScroller = new ScrollOnChange(lootPane);
            ScrollOnChange sacrificePaneScroller = new ScrollOnChange(sacrificePane);
            lootList.list.addListener(lootPaneScroller);
            sacrificeList.list.addListener(sacrificePaneScroller);
            
            goddess = new Image(skin.getRegion(playerService.getWorship()));
            goddess.setSize(128f, 128f);
            goddess.setScaling(Scaling.stretch);
            goddessDialog = makeWindow(skin, 500, 150, true);
            goddessDialog.setPosition(40f, display.getHeight() / 2f - goddessDialog.getHeight() / 2f);
            Table gMessage = new Table();
            gMessage.setFillParent(true);
            gMessage.pad(36f);
            gMsg = new Label("", skin, "small");
            gMsg.setWrap(true);
            gMessage.add(gMsg).expand().fill();
            goddessDialog.addActor(gMessage);

            display.addActor(goddess);
            display.addActor(goddessDialog);

            goddess.addAction(Actions.moveTo(display.getWidth(), display.getHeight() / 2 - 64f));
            goddessDialog.addAction(Actions.alpha(0f));
        }
        
        


        // key listener for moving the character by pressing the arrow keys or
        // WASD
        addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent evt, int keycode) {
                if (menu.isInState(WanderState.Wander)) {
                    Direction to = Direction.valueOf(keycode);
                    if (to != null) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Movement, to);
                        return true;
                    }
                    
                    if (Input.SWITCH.match(keycode)) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Action);
                        return true;
                    }
                    
                    if (Input.CANCEL.match(keycode)) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Zoom);
                        return true;
                    }
                }
                return false;
            }

            @Override
            public boolean keyUp(InputEvent evt,  int keycode) {
                if (menu.isInState(WanderState.Wander)) {
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Movement);
                    return true;
                }
                return false;
            }
        });
        
        display.addListener(new InputListener() {
           
            @Override
            public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                if (menu.isInState(WanderState.Wander)) {
                    Direction to = Direction.valueOf(x, y, display.getWidth(), display.getHeight());
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Dungeon.Movement, to);
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
        
        defaultGroup = new FocusGroup(buttonList);
        defaultGroup.addListener(focusListener);
    }

    @Override
    protected void preRender() {
        if (dungeonService.getProgress().depth > 0) {
            dungeonService.getEngine().getSystem(RenderSystem.class).update(Gdx.graphics.getDeltaTime());
        }
    }

    @Override
    protected void extendAct(float delta) {
        if (dungeonService.getProgress().depth > 0) {
            menu.update();
        }
    }
    
    /**
     * Refreshes the HUD at the top of the screen to display the proper current
     * progress of the dungeon
     */
    void refresh(Progress progress) {
        floorLabel.setText(String.format("Floor %d/%d", progress.depth, progress.floors));
        lootLabel.setText(String.format("%d", progress.totalLootFound));
        keyLabel.setText(String.format("%d", progress.keys));
        monsterLabel.setText(String.format("%d/%d", progress.monstersKilled, progress.monstersTotal));
    }

    void showGoddess(String string) {
        gMsg.setText(string);

        goddess.clearActions();
        goddess.addAction(Actions.moveTo(display.getWidth() - 128f, display.getHeight() / 2 - 64f, .3f));

        goddessDialog.clearActions();
        if (menu.isInState(WanderState.Assist)) {
            goddessDialog.addAction(Actions.moveTo(40f, display.getHeight() / 2 - goddessDialog.getHeight() / 2));
        }
        goddessDialog.addAction(Actions.alpha(1f, .2f));
    }

    void hideGoddess() {
        goddess.clearActions();
        goddessDialog.clearActions();
        goddess.addAction(Actions.moveTo(display.getWidth(), display.getHeight() / 2 - 64f, .3f));
        goddessDialog.addAction(Actions.alpha(0f, .2f));

        itemSubmenu.addAction(Actions.alpha(0f, .2f));
    }

    void showLoot() {
        itemSubmenu.clearActions();

        // make clone so we can work with it
        goddessDialog.clearActions();
        goddessDialog.addAction(Actions.moveTo(goddessDialog.getX(), display.getHeight() - goddessDialog.getHeight(),
                .2f));
        itemSubmenu.addAction(Actions.alpha(1f, .2f));

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
    
    @Override
    public String[] defineButtons() {
        return ((UIState) menu.getCurrentState()).defineButtons();
    }

    @Override
    protected FocusGroup focusList() {
        if (menu.isInState(WanderState.Sacrifice_Heal) || menu.isInState(WanderState.Sacrifice_Leave)) {
            return sacrificeGroup;
        }
        return defaultGroup;
    }

    @Override
    public void setMessage(String msg) {
        // update the battle log
        SnapshotArray<Actor> children = log.getChildren();
        if (children.size > 10) {
            children.first().remove();
        }

        log.row();
        log.bottom().left();
        Label l = new Label(msg, skin, "smallest");
        l.setWrap(true);
        l.setAlignment(Align.left);
        log.add(l).expandX().fillX();

        log.pack();
        log.act(0f);
        logPane.validate();
        logPane.act(0f);

        logPane.setScrollPercentY(1.0f);
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
            setMessage((String)telegram.extraInfo);
        }
        else if (telegram.message == Messages.Dungeon.Notify && telegram.extraInfo instanceof CombatNotify) {
            CombatNotify notification = (CombatNotify)telegram.extraInfo;
            audio.playSfx(DataDirs.Sounds.hit);
            int dmg = notification.dmg;
            if (dmg == -1) {
                if (notification.attacker == playerService.getPlayer()) {
                    Identifier id = Identifier.Map.get(notification.opponent);
                    String name = id.toString();
                    setMessage(String.format("You attacked %s, but missed!", name));
                }
                else {
                    Identifier id = Identifier.Map.get(notification.attacker);
                    String name = id.toString();
                    setMessage(String.format("%s attacked you, but missed!", name));
                }
            } else if (notification.attacker == playerService.getPlayer()) {
                Identifier id = Identifier.Map.get(notification.opponent);
                String name = id.toString();
                if (dmg == 0) {
                    setMessage(String.format("%s blocked your attack!", name));
                }
                else {
                    if (notification.critical) {
                        setMessage("CRITICAL HIT!");
                    }
                    setMessage(String.format("You attacked %s for %d damage", name, notification.dmg));
                }
            }
            else {
                Identifier id = Identifier.Map.get(notification.attacker);
                String name = id.toString();
                if (dmg == 0) {
                    setMessage(String.format("You blocked %s's attack", name));
                }
                else {
                    setMessage(String.format("%s attacked you for %d damage", name, dmg));
                }
            }
            return true;
        }
        if (telegram.message == Messages.Dungeon.Dead && telegram.extraInfo != playerService.getPlayer()) {
            Entity opponent = (Entity)telegram.extraInfo;
            Combat combat = Combat.Map.get(opponent);
            setMessage(combat.getDeathMessage(Identifier.Map.get(opponent).toString()));
            
            return true;
        }
        if (telegram.message == Messages.Dungeon.Refresh) {
            refresh((Progress) telegram.extraInfo);
            return true;
        }
        if (telegram.message == Messages.Player.UpdateItem || 
            telegram.message == Messages.Player.NewItem) {
            Messages.Player.ItemMsg msg = (Messages.Player.ItemMsg)telegram.extraInfo;
            lootList.updateLabel(msg.item, msg.amount);
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
        return super.handleMessage(telegram);
    }
    
}
