package scenes.newgame;

import github.nhydock.ssm.Inject;
import scene2d.ui.extras.Card;
import scene2d.ui.extras.FocusGroup;
import scene2d.ui.extras.LabeledTicker;
import scene2d.ui.extras.Pointer;
import scene2d.ui.extras.SimpleWindow;
import scenes.Messages;
import scenes.UI;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;

import core.DataDirs;
import core.common.Input;
import core.service.interfaces.IPlayerContainer;
import core.service.interfaces.IPlayerContainer.SaveSummary;

public class NewUI extends UI {

    Table textTable;
    Label text;
    Image goddess;
    Image you;

    SimpleWindow createFrame;
    FocusGroup createFocus;

    Group slots;
    private Button newGameButton;
    private FocusGroup slotFocus;

    Scene parent;
    ButtonGroup<Button> gender;
    LabeledTicker<Integer> number;
    CheckBox hardcore;

    @Inject public IPlayerContainer player;

    StateMachine<NewUI> sm;

    public NewUI(Scene scene, AssetManager manager) {
        super(scene, manager);
        parent = scene;

        sm = new DefaultStateMachine<NewUI>(this);
    }

    @Override
    protected void load() {
        
    }

    @Override
    public void init() {
        skin = shared.getResource(DataDirs.Home + "uiskin.json", Skin.class);

        final NewUI ui = this;

        // create load data dialog
        {
            slots = new Group();
            slots.setSize(620, 500);
            slots.setPosition(getWidth()/2f, getHeight()/2f, Align.center);
            slots.setColor(1,1,1,0);

            //new game button
            {
                Button button = newGameButton = new TextButton("New Game", skin, "huge");
                button.setSize(620f, 80);
                button.setPosition(0, 500, Align.topLeft);
                button.addListener(new InputListener() {
                    @Override
                    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor){
                        newGameButton.setChecked(true);
                        slotFocus.unfocus();
                    }
                    
                    @Override
                    public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                        if (button == Buttons.LEFT) {
                            MessageDispatcher.getInstance().dispatchMessage(null, ui, Messages.Interface.Selected, 0);
                            return true;
                        }
                        return false;
                    }
                    
                });
                slots.addActor(button);
            }            
            
            
            FocusGroup focus = slotFocus = new FocusGroup();

            for (int x = 0, i = 1; i <= 3; i++, x += 210) {
                Card card;
                final SaveSummary s = player.summary(i);
                if (s != null) {
                    String diff = (new String(new char[s.diff])).replace("\0", "*");
                    String data = String.format(
                            " \n \n \n Time: %s\nCrafting Completed: %s\nDifficulty: %s\n \nLast Played:\n%s\n \n%s", 
                            s.time, s.progress, diff, s.date, ((s.hardcore)?"HARDCORE":" ") );
                    
                    card = new Card(skin, "Slot " + i, data, s.gender);
                    card.setUserObject(s);
                } else {
                    card = new Card(skin, "Slot " + i);
                }
                card.setName("slot " + i);
                card.setPosition(x, 0);
                final int index = i;
                card.addListener(new InputListener() {

                    @Override
                    public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                        if (button == Buttons.LEFT) {
                            MessageDispatcher.getInstance().dispatchMessage(null, ui, Messages.Interface.Selected, index);
                            return true;
                        }
                        return false;
                    }
                    
                });
                slots.addActor(card);
                focus.add(card);
            }
            
            slots.addListener(
                new InputListener() {
                    @Override
                    public boolean keyDown(InputEvent evt, int keycode) {
                        if (Input.RIGHT.match(keycode)) {
                            slotFocus.next(true);
                            return true;
                        }
                        if (Input.LEFT.match(keycode)) {
                            slotFocus.prev(true);
                            return true;
                        }
                        if (Input.UP.match(keycode)) {
                            newGameButton.setChecked(true);
                            slotFocus.unfocus();
                            return true;
                        }
                        if (Input.ACCEPT.match(keycode)) {
                            MessageDispatcher.getInstance().dispatchMessage(null, ui, Messages.Interface.Selected, slotFocus.getFocusedIndex() + 1);
                            return true;
                        }

                        return false;
                    }
                }
            );
            slotFocus.addListener(new ChangeListener(){

                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    for (int i = 1; i <= 3; i++) {
                        Actor card = slots.findActor("slot " + i);
                        card.clearActions();
                        if (card == slotFocus.getFocused()) {
                            card.addAction(Actions.moveTo(card.getX(), 20, .15f, Interpolation.circleOut));
                            newGameButton.setChecked(false);
                        } else {
                            card.addAction(Actions.moveTo(card.getX(), 0, .15f, Interpolation.circleOut));
                        }
                    }
                }
                
            });
            
            slotFocus.setFocus(slotFocus.getActors().first());
            
            addActor(slots);
        }

        createFrame = new SimpleWindow(skin, "round");
        createFrame.setSize(580, 270);
        createFrame.setPosition(getWidth()/2, getHeight()/2, Align.center);
        createFrame.setTouchable(Touchable.disabled);
        
        final Table window = new Table(skin);
        window.setFillParent(true);
        window.center().top().pad(40f).pack();

        Label prompt = new Label("Please create a character", skin, "prompt");
        prompt.setAlignment(Align.center);

        window.add(prompt).expandX().fillX().padBottom(20);
        window.row();

        createFocus = new FocusGroup();
        // Difficulty
        {
            Integer[] values = { 1, 2, 3, 4, 5 };
            number = new LabeledTicker<Integer>("Difficulty", values, skin);
            number.setLeftAction(new Runnable() {

                @Override
                public void run() {
                    audio.playSfx(DataDirs.Sounds.tick);
                    number.defaultLeftClick.run();
                }

            });

            number.setRightAction(new Runnable() {

                @Override
                public void run() {
                    audio.playSfx(DataDirs.Sounds.tick);
                    number.defaultRightClick.run();
                }

            });
            window.add(number).expandX().fillX().pad(0, 50f, 10f, 50f);
            createFocus.add(number);
        }
        window.row();

        // Gender
        {
            final Table table = new Table();
            prompt = new Label("Gender", skin, "prompt");
            prompt.setAlignment(Align.left);
            table.add(prompt).expandX().fillX();

            final TextButton left = new TextButton("Male", skin, "big");
            left.pad(10);
            left.setChecked(true);

            final TextButton right = new TextButton("Female", skin, "big");
            right.pad(10);

            gender = new ButtonGroup<Button>(left, right);

            window.center();
            table.add(left).width(80f).right().padRight(10f);
            table.add(right).width(80f).right();
            window.add(table).expandX().fillX().pad(0, 50f, 10f, 50f);
            createFocus.add(table);

            table.addListener(new InputListener() {
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor){
                    createFocus.setFocus(table);
                }
                
                @Override
                public boolean keyDown(InputEvent evt, int keycode) {
                    boolean hit = false;

                    if (Input.LEFT.match(keycode)) {
                        hit = true;
                        left.setChecked(true);
                    }
                    if (Input.RIGHT.match(keycode)) {
                        hit = true;
                        right.setChecked(true);
                    }
                    return hit;
                }
            });
        }
        window.row();
        
        //hardcore
        {
            hardcore = new CheckBox("Hardcore Mode", skin, "default");
            window.add(hardcore).expandX().fillX().align(Align.left).pad(10);
            createFocus.add(hardcore);
            hardcore.addListener(new InputListener(){
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor){
                    createFocus.setFocus(hardcore);
                }
                
                @Override
                public boolean keyDown(InputEvent event, int keycode) {
                    if (Input.ACCEPT.match(keycode)) {
                        hardcore.toggle();
                        return true;
                    }
                    return false;
                };
            });
        }
        window.row();

        //accept button
        {
            final TextButton accept = new TextButton("START", skin);
            accept.align(Align.center);
            accept.setSize(80, 32);
            accept.pad(5);
            accept.setPosition(createFrame.getWidth() / 2 - accept.getWidth() / 2, 10f);
            window.add(accept).expandX().width(80f).height(32);
            accept.addListener(new InputListener() {
    
                @Override
                public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                    accept.setChecked(true);
                    createFocus.setFocus(accept);
                }
                
                @Override
                public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                    accept.setChecked(false);
                }
                
                @Override
                public boolean keyDown(InputEvent event, int keycode) {
                    if (Input.ACCEPT.match(keycode)) {
                        accept.setChecked(true);
                        MessageDispatcher.getInstance().dispatchMessage(null, ui, Messages.Interface.Button);
                        return true;
                    }
                    return false;
                }
                
                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                    if (button == Buttons.LEFT) {
                        MessageDispatcher.getInstance().dispatchMessage(null, ui, Messages.Interface.Button);
                        return true;
                    }
                    return true;
                }
            });
            createFocus.add(accept);
        }
        
        createFrame.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent evt, int keycode) {

                if (Input.DOWN.match(keycode)) {
                    createFocus.next();
                }
                if (Input.UP.match(keycode)) {
                    createFocus.prev();
                }
                return false;
            }
        });

        createFrame.addActor(window);
        createFrame.setColor(1.0f, 1.0f, 1.0f, 0.0f);
        addActor(createFrame);

        createFocus.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Actor a = createFocus.getFocused();
                setKeyboardFocus(a);

                pointer.setPosition(a, Align.left);
                pointer.setVisible(true);
            }
        });
        
        goddess = new Image();
        goddess.setScaling(Scaling.stretch);
        goddess.setSize(128f, 128f);
        goddess.setPosition(getWidth() * .6f, 48f);
        goddess.setOrigin(64f, 64f);

        you = new Image();
        you.setScaling(Scaling.stretch);
        you.setSize(64f, 64f);
        you.setPosition(getWidth() * .4f, 48f);

        textTable = new Table();
        textTable.center();
        textTable.setWidth(getWidth());
        textTable.setFillParent(true);

        // text
        text = new Label("", skin, "prompt");
        text.setAlignment(Align.center);
        text.setWrap(true);

        textTable.add(text).center().expandX().fillX().pad(60f);

        Label down = new Label("More", skin, "promptsm");
        textTable.row();
        textTable.add(down).right().padRight(80f);
        textTable.pack();
        textTable.addAction(Actions.alpha(0f));
        textTable.setTouchable(Touchable.disabled);
        textTable.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent evt, int keycode) {
                if (Input.CANCEL.match(keycode)) {
                    MessageDispatcher.getInstance().dispatchMessage(null, ui, Messages.Interface.Close);
                    return true;
                }
                if (Input.ACCEPT.match(keycode)) {
                    MessageDispatcher.getInstance().dispatchMessage(null, ui, Messages.Interface.Notify);
                    return true;
                }
                
                return false;
            }
        });
        textTable.setTouchable(Touchable.disabled);

        act();

        pointer = new Pointer(skin);
        pointer.setVisible(false);
        addActor(pointer);
        addListener(new InputListener(){
            @Override
            public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                if (button == Buttons.LEFT && sm.getCurrentState() == UIState.Story) {
                    MessageDispatcher.getInstance().dispatchMessage(null, ui, Messages.Interface.Notify);
                }
                else if (button == Buttons.RIGHT && sm.getCurrentState() == UIState.Story) {
                    MessageDispatcher.getInstance().dispatchMessage(null, ui, Messages.Interface.Close);
                }
                return false;
            }
        });
        sm.changeState(UIState.Choose);
    }

    public int getDifficulty() {
        return number.getValue();
    }

    public boolean isDone() {
        return sm.isInState(UIState.Over);
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    public boolean getGender() {
        return gender.getButtons().get(0).isChecked();
    }

    @Override
    public void update(float delta) {
    }

    @Override
    public boolean handleMessage(Telegram msg) {
        return sm.handleMessage(msg);
    }

    

    public boolean isHardcore() {
        return hardcore.isChecked();
    }
}
