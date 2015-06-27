package scenes;

import github.nhydock.ssm.Inject;
import github.nhydock.ssm.ServiceManager;
import scene2d.ui.extras.Pointer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.IntSet.IntSetIterator;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import core.DataDirs;
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
    
    protected void listenTo(IntSet messages){}
    
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

    @Override
    public boolean handleMessage(Telegram msg) {
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
}
