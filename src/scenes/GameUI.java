package scenes;

import java.nio.CharBuffer;

import github.nhydock.ssm.Inject;
import scene2d.InputDisabler;
import scene2d.ui.extras.FocusGroup;
import scene2d.ui.extras.LabeledTicker;
import scene2d.ui.extras.TabbedPane;
import scenes.dungeon.ui.WanderState;
import scenes.dungeon.ui.WanderUI;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.IntSet;

import core.DataDirs;
import core.common.Input;
import core.components.Stats;
import core.datatypes.Inventory;
import core.datatypes.quests.Quest;
import core.service.interfaces.IPlayerContainer;

/**
 * Base UI for adventuring in the town </p> Please don't ask about all the pixel
 * specific values. I used GIMP to make a mockup of the UI and I'm just going by
 * all that. I used guides and everything, it makes more sense when you open up
 * the town_ui.xcf
 * 
 * @author nhydock
 *
 */
@SuppressWarnings("rawtypes")
public abstract class GameUI extends UI {

    private static final String statFormat = "Crafting Completed %d/%d";
    private static final String levelFormat = "Level %d";
    private static final String hpFormat = "HP: %3d/%3d";
    private static final String expFormat = "EXP: %3d/%3d";

    private Label craftingStats;
    private Label levelStats;
    private Label timeStats;

    private Group window;
    protected Group messageWindow;
    protected Group display;
    private Rectangle displayBounds;
    private Rectangle tmpBound;

    protected final HorizontalGroup buttonList;
    private ButtonGroup<Button> buttons;
    protected final ChangeListener focusListener;

    private Label hpStats;
    private Label expStats;

    protected Table notificationStack;

    // level up dialog
    Group levelUpDialog;
    int points;
    private Label pointLabel;
    LabeledTicker<Integer> strTicker;
    LabeledTicker<Integer> defTicker;
    LabeledTicker<Integer> spdTicker;
    LabeledTicker<Integer> vitTicker;
    FocusGroup levelUpGroup;
    
    @Inject
    public IPlayerContainer playerService;

    protected StateMachine menu;
    
    private int seconds = 0;
    private String time = "Time: %03d:%02d:%02d";
    
    //prepare these beforehand
    @Override
    protected void listenTo(IntSet messages)
    {
        messages.addAll(
            Messages.Interface.Notify, 
            Messages.Interface.Selected, 
            Messages.Interface.Close,   
            Messages.Interface.Button,
            TabbedPane.Messages.ChangeTabs,
            Messages.Dungeon.LevelUp,
            Messages.Player.Progress,
            Messages.Player.Stats,
            Messages.Player.Time,
            Quest.Actions.Notify
        );
    }

    public GameUI(AssetManager manager) {
        super(manager);

        buttonList = new HorizontalGroup();
        buttons = new ButtonGroup<Button>();

        tmpBound = new Rectangle();
        displayBounds = new Rectangle();

        focusListener = new ChangeListener() {

            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (focus() == null)
                    return;

                if (focus().getFocused() == buttonList || focus().getFocused() == null) {
                    hidePointer();
                }
                else {
                    showPointer(focus().getFocused(), Align.left, Align.top);
                }

                setFocus(focus().getFocused());
            }

        };
    }

    @Override
    protected void load() {}

    /**
     * Initialize the ui after all assets have been loaded
     */
    @Override
    public final void init() {
        skin = shared.getResource(DataDirs.Home + "uiskin.json", Skin.class);

        // stat frame
        {
            Group window = makeWindow(skin, 384, 108);
            window.setPosition(96f, 16f);

            craftingStats = new Label(statFormat, skin, "promptsm");
            levelStats = new Label(levelFormat, skin, "promptsm");
            hpStats = new Label(hpFormat, skin, "promptsm");
            expStats = new Label(expFormat, skin, "promptsm");
            timeStats = new Label("Time: 000:00:00", skin, "promptsm");

            craftingStats.setPosition(40f, 54f);
            timeStats.setPosition(344f - timeStats.getPrefWidth(), 54f);

            levelStats.setAlignment(Align.left);
            hpStats.setAlignment(Align.center);
            expStats.setAlignment(Align.right);

            Table group = new Table();
            group.pad(10f);
            group.row().bottom().left();
            group.add(levelStats).expandX().fillX();
            group.add(hpStats).expandX().fillX();
            group.add(expStats).expandX().fillX();
            group.setWidth(320f);
            group.setHeight(20f);
            group.setPosition(32f, 32f);

            window.addActor(craftingStats);
            window.addActor(timeStats);
            window.addActor(group);

            addActor(window);
        }
        // message frame
        {
            Group window = makeWindow(skin, 384, 108);
            window.setPosition(480f, 16f);

            messageWindow = new Group();
            messageWindow.setPosition(32f, 32f);
            messageWindow.setSize(320f, 44f);

            window.addActor(messageWindow);

            addActor(window);
        }

        // window frame
        {
            Group frame = makeWindow(skin, 832, 432);
            window = new Group();
            window.setSize(832f, 432f);
            window.setPosition(64f, 92f);

            display = new Group();
            display.setSize(window.getWidth() - 64f, window.getHeight() - 64f);
            display.setPosition(32f, 32f);
            displayBounds = new Rectangle(window.getX() + display.getX(), window.getY() + display.getY(),
                    display.getWidth(), display.getHeight());
            window.addActor(display);
            window.addActor(frame);
            addActor(window);

            // populate the window frame
            extend();

            buildLevelUpDialog();
        }

        // notification area
        {

            // quest notification bubble pane
            notificationStack = new Table(skin);
            notificationStack.bottom();
            notificationStack.setWidth(200f);
            display.addActor(notificationStack);

        }

        String[] butt = defineButtons();
        if (butt != null) {
            window.addActor(buttonList);

            buttonList.addListener(new InputListener() {
                @Override
                public boolean keyDown(InputEvent evt, int keycode) {
                    if (!buttonList.isVisible())
                        return false;

                    if (Input.LEFT.match(keycode)) {
                        setIndex(getIndex() - 1);
                        return true;
                    }
                    if (Input.RIGHT.match(keycode)) {
                        setIndex(getIndex() + 1);
                        return true;
                    }
                    if (Input.ACCEPT.match(keycode)) {
                        audio.playSfx(DataDirs.Sounds.accept);
                        triggerAction(getIndex());
                        hidePointer();
                        return true;
                    }
                    return false;
                }
            });

            refreshButtons();
        }

        // focus handler
        addListener(new InputListener() {

            @Override
            public boolean keyDown(InputEvent evt, int keycode) {

                if (Input.CANCEL.match(keycode)) {
                    triggerAction(-1);
                    hidePointer();
                    return true;
                }
                if (focus() == null) {
                    if (buttonList != null) {
                        setFocus(buttonList);
                    }
                    return false;
                }

                if (Input.SWITCH.match(keycode)) {
                    focus().next(true);
                    return true;
                }
                return false;
            }

            @Override
            public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                if (button == Buttons.RIGHT) {
                    triggerAction(-1);
                    hidePointer();
                    return true;
                }
                return false;
            }
        });
        
        calculateScissors(displayBounds, tmpBound);

        if (fader == null) {
            fader = new Image(skin.getDrawable("fader"));
            fader.setFillParent(true);
            fader.addAction(Actions.alpha(0f));
            fader.setTouchable(Touchable.disabled);
            addActor(fader);
        }

        pointer = new Image(skin.getDrawable("pointer"));
        addActor(pointer);
        hidePointer();

        act(0);
        
        resetFocus();
            
        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Player.Progress);
        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Player.Stats);
        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Player.Time);
    }
    

    private void setPoints(int points) {
        this.points = points;
        this.pointLabel.setText(String.format("Points %d", points));
    }
    
    @SuppressWarnings("unchecked")
    private void buildLevelUpDialog(){

        levelUpGroup = new FocusGroup();
        levelUpGroup.setVisible(false);

        levelUpDialog = UI.makeWindow(skin, 500, 480, true);
        levelUpDialog.setPosition(getWidth() / 2 - levelUpDialog.getWidth() / 2, getHeight());
        levelUpDialog.setVisible(false);

        final Table window = new Table();
        window.setFillParent(true);
        window.center().top().pack();

        Label prompt = new Label("You've Leveled Up!", skin, "prompt");
        prompt.setAlignment(Align.center);
        window.add(prompt).expandX().fillX().padBottom(20).colspan(3);
        window.row();

        pointLabel = new Label("Points 0", skin, "prompt");
        pointLabel.setAlignment(Align.center);

        LabeledTicker[] tickers = new LabeledTicker[4];
        tickers[0] = strTicker = new LabeledTicker<Integer>("Strength", new Integer[] { 0 }, skin);
        tickers[1] = defTicker = new LabeledTicker<Integer>("Defense", new Integer[] { 0 }, skin);
        tickers[2] = spdTicker = new LabeledTicker<Integer>("Speed", new Integer[] { 0 }, skin);
        tickers[3] = vitTicker = new LabeledTicker<Integer>("Vitality", new Integer[] { 0 }, skin);
        
        for (final LabeledTicker<Integer> ticker : tickers) {
            ticker.setLeftAction(new Runnable() {

                @Override
                public void run() {
                    audio.playSfx(DataDirs.Sounds.tick);
                    if (ticker.getValueIndex() > 0) {
                        ticker.defaultLeftClick.run();
                        setPoints(points + 1);
                    }
                }

            });
            ticker.setRightAction(new Runnable() {

                @Override
                public void run() {
                    audio.playSfx(DataDirs.Sounds.tick);
                    if (ticker.getValueIndex() < ticker.length() && points > 0) {
                        ticker.defaultRightClick.run();
                        setPoints(points - 1);
                    }
                }

            });

            window.center();
            window.add(ticker).expandX().fillX().pad(0, 50f, 10f, 50f).colspan(3);
            window.row();

            levelUpGroup.add(ticker);
        }

        window.add(pointLabel).expandX().fillX().colspan(3);
        levelUpDialog.addActor(window);

        final TextButton accept = new TextButton("START", skin);
        accept.align(Align.center);
        accept.setSize(80, 32);
        accept.pad(5);
        accept.setPosition(levelUpDialog.getWidth() / 2 - accept.getWidth() / 2, 10f);

        accept.addListener(new InputListener() {
            @Override
            public void enter(InputEvent evt, float x, float y, int pointer, Actor fromActor) {
                accept.setChecked(true);
            }

            @Override
            public void exit(InputEvent evt, float x, float y, int pointer, Actor fromActor) {
                accept.setChecked(false);
            }

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                if (points > 0) {
                    audio.playSfx(DataDirs.Sounds.accept);
                    return false;
                }

                MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Close);
                return true;
            }
        });
        levelUpDialog.addActor(accept);
        levelUpDialog.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent evt, int keycode) {
                if (Input.DOWN.match(keycode)) {
                    levelUpGroup.next();
                    return true;
                }
                if (Input.UP.match(keycode)) {
                    levelUpGroup.prev();
                    return true;
                }
                if (Input.ACCEPT.match(keycode)) {
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Close);
                    return true;
                }
                return false;
            }
        });
        levelUpGroup.addListener(focusListener);

        addActor(levelUpDialog);
        addActor(levelUpGroup);
    }

    /**
     * Adds addition scene specific ui elements into the display
     */
    protected abstract void extend();

    /**
     * Allow rendering into the display things that aren't stage2d elements.
     * This method is called before things in the display are rendered.
     */
    protected void preRender() {}

    /**
     * Allow rendering into the display things that aren't stage2d elements.
     * This method is called after things in the display are rendered.
     */
    protected void postRender() {}
    
    /**
     * Handles an action to be performed when a button in the menu is clicked
     * 
     * @param index
     */
    protected void triggerAction(int index) {
        if (index == -1) {
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Close);
        }
        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Button, index);
        menu.update();
        refreshButtons();
    }
    
    /**
     * Primary focus handler for GameUI.  If the subclass requires
     * more options for areas of focus, implement the focusList method
     * @return the current active focus group
     */
    private final FocusGroup focus() {
        if (levelUpDialog.isVisible()) {
            return levelUpGroup;
        }
        else {
            return focusList();
        }
    }
    
    protected abstract FocusGroup focusList();

    /**
     * @return Names of all the button options available at the bottom of the screen
     */
    public abstract String[] defineButtons();

    /**
     * Resets the buttons for the GameUI to be whatever is currently defined
     */
    public final void refreshButtons() {
        setButtons(defineButtons());
    }
    
    /**
     * Resets the keyboard focus to the first actor of the currently active focus group
     */
    public final void resetFocus() {
        if (focus() != null) {
            focus().setFocus(focus().getActors().first());
            setFocus(focus().getFocused());
        }
    }

    /**
     * Rebuilds the button list at the bottom of the UI
     * @param butt
     */
    private final void setButtons(final String... butt) {
        if (butt == null) {
            disableMenuInput();
            buttonList.clearChildren();
            buttons = null;
            return;
        }

        buttonList.clearChildren();

        buttons = new ButtonGroup<Button>();

        for (int i = 0; i < butt.length; i++) {
            final Button button = new TextButton(butt[i], skin);
            final int index = i;
            button.setName(butt[i]);
            button.pad(4f, 10f, 4f, 10f);
            button.addListener(new InputListener() {
                @Override
                public void enter(InputEvent evt, float x, float y, int pointer, Actor fromActor) {
                    setIndex(index);
                }

                @Override
                public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                    if (button == Buttons.LEFT) {
                        audio.playSfx(DataDirs.Sounds.accept);
                        triggerAction(index);
                    }
                    return false;
                }
            });
            buttonList.addActor(button);
            buttons.add(button);
        }

        buttonList.setPosition(window.getWidth() / 2 - buttonList.getPrefWidth() / 2, 32f);
    }

    /**
     * Forcibly changes the focus of the UI onto its button bar
     */
    protected final void forceButtonFocus() {
        if (buttonList != null) {
            setFocus(buttonList);
            buttons.setChecked(buttons.getButtons().first().getName());
        }
    }

    /**
     * @return the index of the currently selected button
     */
    protected final int getIndex() {
        return buttons.getButtons().indexOf(buttons.getChecked(), true);
    }

    /**
     * Forcibly set the button menu index from outside the button listeners
     */
    protected final void setIndex(int i) {
        if (i < 0) {
            i = 0;
        }
        if (i >= buttons.getButtons().size) {
            i = buttons.getButtons().size - 1;
        }
        buttons.getButtons().get(i).setChecked(true);
    }

    /**
     * Disables the button menu input. Useful if you have a submenu present, or
     * an animation playing. Will also hide the button menu.
     */
    protected final void disableMenuInput() {
        buttonList.setVisible(false);
    }

    protected final void enableMenuInput() {
        buttonList.setVisible(true);
    }

    /**
     * Sets the message in the bottom right corner
     */
    public void setMessage(String s) {
        messageWindow.clear();

        Label message = new Label("", skin, "promptsm");
        message.setPosition(8f, 12f);

        message.setText(s);

        messageWindow.addActor(message);
    }

    /**
     * Adds a new notification bubble into the bottom left corner of the display
     * 
     * @param s
     */
    public void pushNotification(String notification) {
        // make notification label popup
        final Label popup = new Label(notification, skin, "smaller");
        popup.setWrap(true);

        final Table label = new Table();
        label.add(popup).pad(10f).align(Align.left).expandX().row();
        label.setBackground(skin.getDrawable("button_up"));
        notificationStack.add(label).expandX().fillX().row();

        label.addAction(
            Actions.sequence(
                Actions.alpha(0), 
                Actions.fadeIn(.3f), 
                Actions.delay(5f),
                Actions.fadeOut(.3f), 
                Actions.run(new Runnable() {
                    @Override
                    public void run() {
                        label.remove();
                    }
                })
            )
        );
    }

    protected void extendAct(float delta) {
    }

    @Override
    public final void update(float delta) {
        // update time
        int[] t = playerService.getTimeElapsed();
        if (t[2] != seconds) {
            //timeStats.setText(String.format(time, t[0], t[1], t[2]));
            seconds = t[2];
        }
        
        // update animations
        extendAct(delta);
        // display.act(delta);
    }

    @Override
    public final void draw() {
        Batch b = getBatch();

        b.setProjectionMatrix(getCamera().combined);
        b.begin();
        fill.draw(b, 1.0f);
        b.end();

        ScissorStack.pushScissors(tmpBound);
        b.setProjectionMatrix(getCamera().combined);
        preRender();
        b.begin();
        display.draw(b, getRoot().getColor().a);
        b.end();
        postRender();
        ScissorStack.popScissors();

        // hide display during rendering of the stage
        display.setVisible(false);
        fill.setVisible(false);
        super.draw();

        // make sure it's set as visible so it accepts input between frames
        fill.setVisible(true);
        display.setVisible(true);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        calculateScissors(displayBounds, tmpBound);
    }

    public final void setFocus(Actor a) {
        setKeyboardFocus(a);
        setScrollFocus(a);
    }

    public final Actor getButtonList() {
        return buttonList;
    }

    @Override
    public boolean handleMessage(Telegram msg) {
        if (msg.message == Messages.Interface.Notify) {
            pushNotification((String) msg.extraInfo);
        }
        if (msg.message == Messages.Dungeon.LevelUp) {
            LevelUpState.enter(this);
            return true;
        }
        if (msg.message == Messages.Player.Stats) {
            // update stats
            Stats s = playerService.getPlayer().getComponent(Stats.class);
            levelStats.setText(String.format(levelFormat, s.getLevel()));
            hpStats.setText(String.format(hpFormat, s.hp, s.maxhp));
            expStats.setText(String.format(expFormat, s.exp, s.nextExp));
            return true;
        }
        if (msg.message == Messages.Player.Progress) {
            // update progress
            Inventory i = playerService.getInventory();
            craftingStats.setText(String.format(statFormat, i.getProgress(), i.getRequiredCrafts().size));
            return true;
        }
        if (levelUpDialog.isVisible()) {
            return LevelUpState.onMessage(this, msg);
        }
        return menu.handleMessage(msg);
    }

    /**
     * Allow changing the state of the UI
     * 
     * @param state
     */
    @SuppressWarnings("unchecked")
    public final void changeState(State state) {
        menu.changeState(state);
    }
    
    /**
     * Fetch the currently active state of the UI
     * @return
     */
    public final State getCurrentState() {
        return menu.getCurrentState();
    }

    /**
     * @return the actor containing the display
     */
    public final Actor getDisplay() {
        return display;
    }

    public final float getDisplayWidth() {
        return display.getWidth();
    }

    public final float getDisplayHeight() {
        return display.getHeight();
    }
    
    public final float getDisplayCenterX() {
        return display.getWidth() / 2f;
    }
    
    public final float getDisplayCenterY() {
        return display.getHeight() /2f;
    }
    
    /**
     * Private level up state for GameUI.  Provides a shared way
     * of for any GameUI to be capable of providing the level up dialog
     * @author nhydock
     *
     */
    private static class LevelUpState {
        private static final int POINTS_REWARDED = 5;

        public static void enter(final GameUI entity) {
            entity.setPoints(POINTS_REWARDED);

            Stats s = entity.playerService.getPlayer().getComponent(Stats.class);
            Integer[] str = new Integer[POINTS_REWARDED + 1];
            Integer[] def = new Integer[POINTS_REWARDED + 1];
            Integer[] spd = new Integer[POINTS_REWARDED + 1];
            Integer[] vit = new Integer[POINTS_REWARDED + 1];
            for (int i = 0; i < POINTS_REWARDED + 1; i++) {
                str[i] = s.getStrength() + i;
                def[i] = s.getDefense() + i;
                spd[i] = s.getEvasion() + i;
                vit[i] = s.getVitality() + i;
            }
            ;

            entity.strTicker.changeValues(str);
            entity.defTicker.changeValues(def);
            entity.spdTicker.changeValues(spd);
            entity.vitTicker.changeValues(vit);

            InputDisabler.swap();
            entity.levelUpDialog.setVisible(true);
            entity.levelUpGroup.setVisible(true);
            entity.levelUpDialog.addAction(
                    Actions.sequence(
                            Actions.moveTo(entity.levelUpDialog.getX(), entity.getHeight() / 2 - entity.levelUpDialog.getHeight() / 2, .3f),
                    Actions.run(new Runnable() {
                        @Override
                        public void run() {

                            entity.resetFocus();
                            InputDisabler.swap();
                        }
                    })
                )
            );
            entity.levelUpDialog.setTouchable(Touchable.enabled);
        }

        public static void exit(final GameUI entity) {
            entity.hidePointer();
            InputDisabler.swap();
            entity.levelUpDialog.addAction(
                Actions.sequence(
                    Actions.moveTo(entity.levelUpDialog.getX(), entity.getHeight(), .3f), 
                    Actions.run(new Runnable() {

                        @Override
                        public void run() {
                            entity.levelUpDialog.setVisible(false);
                            entity.levelUpGroup.setFocus(null);
                            entity.resetFocus();
                            InputDisabler.swap();
                        }
        
                    })
                )
            );
            entity.levelUpDialog.setTouchable(Touchable.disabled);
            entity.playerService.getPlayer().getComponent(Stats.class).levelUp(
                    new int[] { entity.strTicker.getValue(), entity.defTicker.getValue(), entity.spdTicker.getValue(),
                            entity.vitTicker.getValue() });
            entity.strTicker.setValue(0);
            entity.defTicker.setValue(0);
            entity.spdTicker.setValue(0);
            entity.vitTicker.setValue(0);
            entity.points = 0;
            entity.audio.playSfx(DataDirs.Sounds.accept);
            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Player.Stats);
        }
        
        public static boolean onMessage(GameUI entity, Telegram telegram) {
            if (telegram.message == Messages.Interface.Close) {
                if (entity.points > 0) {
                    entity.audio.playSfx(DataDirs.Sounds.tick);
                    return false;
                }
                else {
                    exit(entity);
                    return true;
                }
            }
            return false;
        }
    }
}
