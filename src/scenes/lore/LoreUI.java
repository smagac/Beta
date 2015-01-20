package scenes.lore;

import github.nhydock.ssm.Inject;
import scene2d.runnables.GotoScene;
import scene2d.runnables.PlayBGM;
import scenes.UI;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;

import core.DataDirs;
import core.common.Input;
import core.service.interfaces.IPlayerContainer;

public class LoreUI extends UI {
	
	protected static final float NORMAL = .8f;
    protected static final float FAST = 3f;
    protected static final float FASTER = 5f;
    protected static final float FASTEST = 10f;
    
    
    Scene parent;
	@Inject public IPlayerContainer player;
	
	private Image fader;
	
	//here's the table that holds all of the lore
	// we're going to pan it up accross the screen
	Label lore;
	String text;
	Rectangle crop;
	Rectangle view;
	private StateMachine<LoreUI> sm;
	
	boolean done;
	boolean ready;
	float scrollY;
	float scrollRate = NORMAL;
	
	public LoreUI(Scene scene, AssetManager manager) {
		super(manager);
		parent = scene;
		
		sm = new DefaultStateMachine<LoreUI>(this);
		crop = new Rectangle();
		crop.x = 40;
		crop.y = 40;
		crop.width = getWidth() - 80;
		crop.height = getHeight() - 80;
		
		view = new Rectangle();
	}
	
	@Override
	protected void load() {

        //get a random bit of lore
        text = Gdx.files.internal(DataDirs.getChildren(Gdx.files.internal(DataDirs.Lore)).random()).readString();
        
	}

	@Override
	public void init() {
		skin = shared.getResource(DataDirs.Home + "uiskin.json", Skin.class);
		
		Image dargon = new Image(skin, "dargon");
		dargon.setScale(5f);
		dargon.setColor(1,1,1,0f);
		dargon.setPosition(40f, 130f);
		dargon.addAction(
		        Actions.sequence(
		            Actions.delay(2f),
		            Actions.parallel(
		                    Actions.moveTo(40f, 120f, 4f),
		                    Actions.alpha(1f, 6f)
		            )
		        )
		);
		addActor(dargon);
		
		Image you = new Image(skin, player.getGender());
		you.setScale(4f);
		you.setPosition(getWidth() * .8f - you.getWidth(), 80f);
		you.setColor(1, 1, 1, 0f);
		you.addAction(Actions.alpha(1f, 2f));
		addActor(you);
		
		fader = new Image(skin, "fill");
		fader.setSize(getWidth(), getHeight());
		fader.setColor(1, 1, 1, 0f);
		
		Label l = lore = new Label(text, skin, "prompt");
		l.setWidth(crop.width);
        l.setWrap(true);
		l.setAlignment(Align.left);
		l.setVisible(false);
		
		fader.addAction(Actions.sequence(
                Actions.delay(10f),
                Actions.alpha(.8f, 2f),
                Actions.run(new Runnable(){

                    @Override
                    public void run() {
                        ready = true;
                    }
                    
                })
        ));
        
		addActor(fader);
		addActor(l);
		
		addListener(new InputListener(){
		   @Override
		   public boolean keyDown(InputEvent evt, int keycode)
		   {
		       if (Input.ACCEPT.match(keycode)) {
		           if (Input.CANCEL.isPressed()) {
		               scrollRate = FASTEST;
		           } else {
	                   scrollRate = FAST;   
		           }
		           return true;
		       }
		       if (Input.CANCEL.match(keycode)) {
		           if (Input.ACCEPT.isPressed()) {
		               scrollRate = FASTEST;
		           } else {
		               scrollRate = FASTER;
		           }
		           return true;
		       }
		       return false;
 		   }
		   
		   @Override
		   public boolean keyUp(InputEvent evt, int keycode) {
		       if (Input.ACCEPT.match(keycode)) {
		           if (Input.CANCEL.isPressed()) {
		               scrollRate = FASTER;
		           } else {
		               scrollRate = NORMAL;
		           }
                   return true;
               }
		       if (Input.CANCEL.match(keycode)) {
		           if (Input.ACCEPT.isPressed()) {
		               scrollRate = FAST;
		           } else {
		               scrollRate = NORMAL;
		           }
		       }
		       return false;
		   }
		});
	}

	@Override
	public void update(float delta) { 
	    if (ready)
	    {
	        if (scrollY > view.y + lore.getPrefHeight() + lore.getStyle().font.getLineHeight()) {
                fader.addAction(
                    Actions.sequence(
                        Actions.run(PlayBGM.fadeOut),
                        Actions.alpha(1f, 2f),
                        Actions.run(new GotoScene("town"))      
                    )
                );
                ready = false;
                done = true;
            }
	        else {
	            scrollY += lore.getStyle().font.getLineHeight() * scrollRate * delta;
	        }
	    } 
	    else if (!done) {
	        scrollY = view.y - view.height - (lore.getStyle().font.getLineHeight() * NORMAL);
	    }
	    lore.setPosition(view.x, scrollY, Align.topLeft);
	}

	@Override
	public void draw() {
	    super.draw();
	    
	    super.calculateScissors(crop, view);
	    if (ready)
	    {
	        ScissorStack.pushScissors(view);
            getBatch().begin();
            lore.draw(getBatch(), 1.0f);
            getBatch().end();
            ScissorStack.popScissors();
	    }
	}
	
	@Override
	public boolean handleMessage(Telegram msg) {
		return sm.handleMessage(msg);
	}
}
