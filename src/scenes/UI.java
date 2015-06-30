package scenes;

import github.nhydock.ssm.Inject;
import github.nhydock.ssm.ServiceManager;
import scene2d.ui.extras.Pointer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.IntSet.IntSetIterator;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import core.DataDirs;
import core.common.Input;
import core.common.Storymode;
import core.service.interfaces.IAudioManager;
import core.service.interfaces.ISharedResources;

public abstract class UI extends Stage implements Telegraph {

    public static final Viewport viewport = new ScalingViewport(Scaling.fit, Storymode.InternalRes[0], Storymode.InternalRes[1]);
    
    protected final Scene parent;
    protected Skin skin;
    protected AssetManager manager;
    protected Pointer pointer;
    protected final Image fill;

    @Inject public ISharedResources shared;
    @Inject public IAudioManager audio;

    private IntSet messages;
    
    protected StateMachine stateMachine;

    private ScrollPane readmeView;
    
    public UI(Scene parent, AssetManager manager) {
        super(viewport);
        this.manager = manager;
        this.parent = parent;
        fill = new Image(new Texture(Gdx.files.internal(DataDirs.Home + "fill.png")));

        fill.setFillParent(true);
        this.addActor(fill);

        load();
        ServiceManager.inject(this);
        
        messages = new IntSet();
        listenTo(messages);
        listen();
    }
    
    public final StateMachine getStateMachine(){
        return stateMachine;
    }
    
    protected void listenTo(IntSet messages){
        messages.addAll(
            Messages.Readme.Open,
            Messages.Readme.Close,
            Messages.Interface.Focus
        );
    }
    
    /**
     * Add this UI as a listener of all of its message types
     */
    private final void listen(){
        IntSetIterator iter = messages.iterator();
        iter.reset();
        while (iter.hasNext) {
            MessageDispatcher.getInstance().addListener(this, iter.next());
        }
    }
    
    /**
     * Remove this UI as a listener to any of its messages
     */
    private final void deafen(){
        IntSetIterator iter = messages.iterator();
        iter.reset();
        while (iter.hasNext) {
            MessageDispatcher.getInstance().removeListener(this, iter.next());
        }
    }

    /**
     * Load any resources needed by this ui
     */
    protected abstract void load();

    /**
     * Initializes all elements of the ui after the manager has finished loading
     */
    public abstract void init();

    public static Group makeWindow(Skin skin, int width, int height) {
        return makeWindow(skin, width, height, false);
    }

    /**
     * Custom required method to create complex actors that are recognized as a
     * single window in order to provide tiling of the ninepatch
     */
    public static Group makeWindow(Skin skin, int width, int height, boolean filled) {
        Group group = new Group();
        group.setSize(width, height);
        group.setTouchable(Touchable.childrenOnly);

        TextureRegion p = skin.getRegion("window");
        TextureRegion[] split = { new TextureRegion(p, 0, 0, 32, 32), // tl
                new TextureRegion(p, 32, 0, 32, 32), // tc
                new TextureRegion(p, 64, 0, 32, 32), // tr
                new TextureRegion(p, 0, 32, 32, 32), // ml
                new TextureRegion(p, 32, 32, 32, 32), // mc
                new TextureRegion(p, 64, 32, 32, 32), // mr
                new TextureRegion(p, 0, 64, 32, 32), // bl
                new TextureRegion(p, 32, 64, 32, 32), // bc
                new TextureRegion(p, 64, 64, 32, 32) // br
        };

        // setup corners
        Image tl = new Image(new TextureRegionDrawable(split[0]));
        tl.setPosition(0, height - 32);

        Image tr = new Image(new TextureRegionDrawable(split[2]));
        tr.setPosition(width - 32, height - 32);

        Image bl = new Image(new TextureRegionDrawable(split[6]));
        bl.setPosition(0, 0);

        Image br = new Image(new TextureRegionDrawable(split[8]));
        br.setPosition(width - 32, 0);

        group.addActor(tl);
        group.addActor(tr);
        group.addActor(bl);
        group.addActor(br);

        // setup sides
        Image t = new Image(new TiledDrawable(split[1]));
        t.setPosition(32, height - 32);
        t.setWidth(width - 64);
        group.addActor(t);

        Image l = new Image(new TiledDrawable(split[3]));
        l.setPosition(0, 32);
        l.setHeight(height - 64);
        group.addActor(l);

        Image r = new Image(new TiledDrawable(split[5]));
        r.setPosition(width - 32, 32);
        r.setHeight(height - 64);
        group.addActor(r);

        Image b = new Image(new TiledDrawable(split[7]));
        b.setPosition(32, 0);
        b.setWidth(width - 64);
        group.addActor(b);

        // setup center
        if (filled) {
            Image c = new Image(new TiledDrawable(split[4]));
            c.setPosition(32, 32);
            c.setSize(width - 64, height - 64);
            group.addActor(c);
        }

        return group;
    }

    public void resize(int width, int height) {
        getViewport().update(width, height, true);
    }


    @Override
    public void dispose() {
        super.dispose();
        ServiceManager.unhook(this);
        deafen();
    }

    /**
     * @return the skin object in use by this UI
     */
    public final Skin getSkin() {
        return skin;
    }

    /**
     * @return the main asset manager used to load resources
     */
    public final AssetManager getManager() {
        return manager;
    }

    public void update(float delta) { /* do nothing */ }

    @SuppressWarnings("unchecked")
    @Override
    public boolean handleMessage(Telegram msg) {
        if (msg.message == Messages.Readme.Open && readmeView != null) {
            getStateMachine().setGlobalState(ReadmeState.instance);
            return true;
        }
        if (msg.message == Messages.Interface.Focus) {
            setKeyboardFocus((Actor)msg.extraInfo);
            return true;
        }
        if (getStateMachine() != null) {
            return getStateMachine().handleMessage(msg);
        }
        return false;
    }

    @Override
    public final void act(float delta) {
        update(delta);
        super.act(delta);
    }
    
    public Pointer getPointer(){
        return pointer;
    }

    /**
     * Allow changing the state of the UI
     * 
     * @param state
     */
    @SuppressWarnings("unchecked")
    public final void changeState(State state) {
        stateMachine.changeState(state);
    }
    
    /**
     * Fetch the currently active state of the UI
     * @return
     */
    public final State getCurrentState() {
        return stateMachine.getCurrentState();
    }

    public final Scene getScene() {
        return parent;
    }
    
    protected void loadReadme(String name) {
        //parse and load readme file if it exists
        FileHandle readmeFile = Gdx.files.internal(DataDirs.GameData + "readme/" + name);
        if (readmeFile.exists())
        {
            final TextButton readme = new TextButton("Help!", skin);
            readme.addListener(new InputListener(){
                @Override
                public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                    readme.setChecked(false);
                    if (getStateMachine().getGlobalState() != ReadmeState.instance) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Readme.Open);
                        return true;
                    } else {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Readme.Close);
                        return true;
                    }
                }
                @Override
                public void enter(InputEvent evt, float x, float y, int pointer, Actor fromActor) {
                    readme.setChecked(true);
                }
                @Override
                public void exit(InputEvent evt, float x, float y, int pointer, Actor toActor) {
                    readme.setChecked(false);
                }
            });
            readme.setSize(90, 24);
            readme.setPosition(20, getHeight(), Align.topLeft);
            addActor(readme);
            
            Table text = new Table();
            text.pad(20f);
            for (String line : readmeFile.readString().split("(\r\n|\n)"))
            {
                Label l;
                if (line.startsWith("##"))
                {
                    l = new Label(line.substring(2), skin, "prompt");
                }
                else if (line.startsWith("#"))
                {
                    l = new Label(line.substring(1), skin, "promptsm");    
                }
                else if (line.length() == 0)
                {
                    l = new Label(" ", skin);
                }
                else
                {
                    l = new Label(line, skin, "smaller");
                }
                l.setWrap(true);
                text.add(l).expandX().fillX().row();
            }
            
            readmeView = new ScrollPane(text, skin, "prompt");
            readmeView.setSize(480, getHeight() - 40);
            readmeView.setPosition(getWidth()/2f, getHeight(), Align.bottom);
            readmeView.setFadeScrollBars(false);
            
            readmeView.addListener(new InputListener() {
                @Override
                public boolean keyDown(InputEvent evt, int keycode) {
                    if (getStateMachine().getGlobalState() != ReadmeState.instance) {
                        if (keycode == Keys.F1) {
                            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Readme.Open);
                            return true;
                        }
                    } else {
                        if (Input.CANCEL.match(keycode) || keycode == Keys.F1) {
                            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Readme.Close);
                            return true;
                        }
                        if (Input.UP.match(keycode)) {
                            readmeView.setScrollY(readmeView.getScrollY() - 32);
                            return true;
                        }
                        if (Input.DOWN.match(keycode)) {
                            readmeView.setScrollY(readmeView.getScrollY() + 32);
                            return true;
                        }
                    }
                    return false;
                }
            });
            addActor(readmeView);
        }
    }

    public Actor getReadme() {
        return readmeView;
    }
}
