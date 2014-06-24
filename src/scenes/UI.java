package scenes;

import GenericComponents.Stats;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TiledDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import core.common.Storymode;
import core.datatypes.Inventory;

/**
 * Base UI for adventuring in the town
 * </p>
 * Please don't ask about all the pixel specific values.  I used GIMP to make a mockup
 * of the UI and I'm just going by all that.  I used guides and everything, it makes more
 * sense when you open up the town_ui.xcf
 * @author nhydock
 *
 */
public abstract class UI {

	
	private static final String statFormat = "Crafting Completed %d/%d";
	private static final String levelFormat = "Level %d  HP: %3d/%3d";
	private static final String timeFormat = "Time: %s";
	
	private Label craftingStats;
	private Label levelStats;
	private Label timeStats;
	
	protected Scene<? extends UI> parent;
	private AssetManager manager;
	private Stage stage;
	private Group window;
	protected Group messageWindow;
	protected Group display;
	private Rectangle displayBounds;
	private Rectangle tmpBound;
	
	protected Skin skin;
	
	private HorizontalGroup buttonList;
	private ButtonGroup buttons;
	private Storymode service;
	
	public UI(Scene<? extends UI> scene, AssetManager manager)
	{
		this.parent = scene;
		this.service = scene.getService();
		this.manager = manager;
		Viewport view = new ScalingViewport(Scaling.fit, 960, 540);
		stage = new Stage(view);
		buttonList = new HorizontalGroup();
		buttons = new ButtonGroup();
		
		tmpBound = new Rectangle();
		displayBounds = new Rectangle();
		
		manager.load("data/uiskin.json", Skin.class);
	}
	
	/**
	 * Custom required method to create complex actors that are recognized
	 * as a single window in order to provide tiling of the ninepatch 
	 */
	public static Group makeWindow(Skin skin, int width, int height)
	{
		Group group = new Group();
		group.setSize(width, height);
		group.setTouchable(Touchable.childrenOnly);

		TextureRegion p = skin.getRegion("window");
		TextureRegion[] split = {
				new TextureRegion(p, 0, 0, 32, 32),   //tl
				new TextureRegion(p, 32, 0, 32, 32),  //tc
				new TextureRegion(p, 64, 0, 32, 32),  //tr
				new TextureRegion(p, 0, 32, 32, 32),  //ml
				new TextureRegion(p, 32, 32, 32, 32), //mc
				new TextureRegion(p, 64, 32, 32, 32), //mr
				new TextureRegion(p, 0, 64, 32, 32),  //bl
				new TextureRegion(p, 32, 64, 32, 32), //bc
				new TextureRegion(p, 64, 64, 32, 32)  //br
		};
		
		//setup corners
		Image tl = new Image(new TextureRegionDrawable(split[0]));
		tl.setPosition(0, height-32);
		
		Image tr = new Image(new TextureRegionDrawable(split[2]));
		tr.setPosition(width-32, height-32);
		
		Image bl = new Image(new TextureRegionDrawable(split[6]));
		bl.setPosition(0, 0);
		
		Image br = new Image(new TextureRegionDrawable(split[8]));
		br.setPosition(width-32, 0);
		
		group.addActor(tl);
		group.addActor(tr);
		group.addActor(bl);
		group.addActor(br);
		
		//setup sides
		Image t = new Image(new TiledDrawable(split[1]));
		t.setPosition(32, height-32);
		t.setWidth(width-64);
		group.addActor(t);
		
		Image l = new Image(new TiledDrawable(split[3]));
		l.setPosition(0, 32);
		l.setHeight(height-64);
		group.addActor(l);
		
		Image r = new Image(new TiledDrawable(split[5]));
		r.setPosition(width-32, 32);
		r.setHeight(height-64);
		group.addActor(r);
		
		Image b = new Image(new TiledDrawable(split[7]));
		b.setPosition(32, 0);
		b.setWidth(width-64);
		group.addActor(b);

		//setup center
//		Image c = new Image(new TiledDrawable(split[4]));
//		c.setPosition(32, 32);
//		c.setSize(width-64, height-64);
//		group.addActor(c);
		

		return group;
	}
	
	/**
	 * Initialize the ui after all assets have been loaded
	 */
	public void init()
	{
		skin = manager.get("data/uiskin.json", Skin.class);

		//make sure the stage is empty
		stage.clear();
		
		//stat frame
		{
			Group window = makeWindow(skin, 384, 108);
			window.setPosition(96f, 16f);
			
			craftingStats = new Label(String.format(statFormat, 0, 0), skin, "small");
			levelStats = new Label(String.format(levelFormat, 99, 50, 90), skin, "small");
			timeStats = new Label(String.format(timeFormat, "000:00:00"), skin, "small");
			
			craftingStats.setPosition(40f, 54f);
			levelStats.setPosition(40f, 32f);
			timeStats.setPosition(344f-timeStats.getPrefWidth(), 32f);
			
			window.addActor(craftingStats);
			window.addActor(levelStats);
			window.addActor(timeStats);
			
			
			stage.addActor(window);
		}
		//message frame
		{
			Group window = makeWindow(skin, 384, 108);
			window.setPosition(480f, 16f);
			
			messageWindow = new Group();
			messageWindow.setPosition(32f, 32f);
			messageWindow.setSize(320f, 44f);
			
			window.addActor(messageWindow);
			stage.addActor(window);
		}
		
		//window frame
		{
			Group frame = makeWindow(skin, 832, 432);
			window = new Group();
			window.setSize(832f,  432f);
			window.setPosition(64f, 92f);
			
			display = new Group();
			display.setSize(window.getWidth()-64f, window.getHeight()-64f);
			display.setPosition(32f, 32f);
			displayBounds = new Rectangle(window.getX()+display.getX(), window.getY()+display.getY(), display.getWidth(), display.getHeight());
			
			//populate the window frame
			extend();
			
			window.addActor(display);
			window.addActor(frame);
			stage.addActor(window);
		}
		
		String[] butt = defineButtons();
		if (butt != null)
		{
			buttonList = new HorizontalGroup();
			
			setButtons(butt);
			
			window.addActor(buttonList);	
			
			buttonList.addListener(new InputListener(){
				@Override
				public boolean keyDown(InputEvent evt, int keycode)
				{
					if (keycode == Keys.LEFT || keycode == Keys.A)
					{
						setIndex(getIndex()-1);
						return true;
					}
					if (keycode == Keys.RIGHT || keycode == Keys.D)
					{
						setIndex(getIndex()+1);
						return true;
					}
					if (keycode == Keys.ENTER || keycode == Keys.SPACE)
					{
						triggerAction(getIndex());
						setButtons(defineButtons());
						return true;
					}
					if (keycode == Keys.ESCAPE || keycode == Keys.BACKSPACE)
					{
						triggerAction(-1);
						setButtons(defineButtons());
						return true;
					}
					return false;
				}
			});
			
			setFocus(buttonList);
		}
		
		if (buttonList == null)
		{
			stage.setKeyboardFocus(focusList()[0]);
		}
		
		//focus handler
		stage.addListener(new InputListener(){
			
			int focus = -1;
			
			@Override
			public boolean keyDown(InputEvent evt, int keycode)
			{
				if (focusList()==null)
					return false;
				
				if (stage.getKeyboardFocus() == buttonList && buttonList != null)
				{
					if (keycode == Keys.UP || keycode == Keys.W || keycode == Keys.TAB)
					{
						buttons.uncheckAll();
						focus = 0;
						setFocus(focusList()[0]);
					}
				}
				else
				{
					if (keycode == Keys.ESCAPE || keycode == Keys.BACKSPACE)
					{
						focus = -1;
						setFocus(buttonList);
					}
					else if (keycode == Keys.TAB)
					{
						focus++;
						if (focus >= focusList().length)
						{
							if (buttonList != null)
							{
								focus = -1;
								setFocus(buttonList);
								buttons.getButtons().get(0).setChecked(true);
							}
							else
							{
								focus = 0;
								setFocus(focusList()[focus]);
							}
						}
						else
						{
							setFocus(focusList()[focus]);
						}
					}
				}

				return false;
			}
		});
		
		stage.addAction(Actions.alpha(0f));
		stage.addAction(Actions.alpha(1f, .2f));
		stage.calculateScissors(displayBounds, tmpBound);
	}
	
	/**
	 * Adds addition scene specific ui elements into the display
	 */
	protected abstract void extend();
	
	/**
	 * Allow rendering into the display things that aren't stage2d elements
	 */
	protected void externalRender(){
		
	}
	
	/**
	 * Handles an action to be performed when a button in the menu is clicked
	 * @param index
	 */
	protected abstract void triggerAction(int index);
	
	protected abstract Actor[] focusList();
	
	public abstract String[] defineButtons();
	
	private final void setButtons(final String... butt)
	{
		if (butt == null)
		{
			disableMenuInput();
			return;
		}
		
		buttonList.clearChildren();
		
		buttons = new ButtonGroup();
		
		for (int i = 0; i < butt.length; i++)
		{
			final Button button = new TextButton(butt[i], skin);
			button.pad(4f, 10f, 4f, 10f);
			button.addListener(new InputListener(){
				@Override
				public void enter(InputEvent evt, float x, float y, int pointer, Actor fromActor)
				{
					button.setChecked(true);
				}
				
				@Override
				public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button)
				{
					if (button == Buttons.LEFT)
					{
						triggerAction(getIndex());
						setButtons(defineButtons());
						return true;
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
	 * @return the index of the currently selected button
	 */
	protected final int getIndex()
	{
		return buttons.getButtons().indexOf(buttons.getChecked(), true);
	}
	
	/**
	 * Forcibly set the button menu index from outside the button listeners
	 */
	protected final void setIndex(int i)
	{
		if (i < 0)
		{
			i = 0;
		}
		if (i >= buttons.getButtons().size)
		{
			i = buttons.getButtons().size - 1;
		}
		buttons.getButtons().get(i).setChecked(true);
	}
	
	/**
	 * Disables the button menu input.  Useful if you have a submenu present, or an animation playing.
	 * Will also hide the button menu.
	 */
	protected final void disableMenuInput()
	{
		buttonList.setVisible(false);
	}
	
	protected final void enableMenuInput()
	{
		buttonList.setVisible(true);
	}
	
	/**
	 * Sets the message in the bottom right corner
	 */
	public void setMessage(String s)
	{
		messageWindow.clear();
		
		Label message = new Label("", skin, "promptsm");
		message.setPosition(8f, 12f);
		
		message.setText(s);
		
		messageWindow.addActor(message);
	}
	
	public final void update(float delta)
	{
		//update time
		timeStats.setText(String.format(timeFormat, getService().getTimeElapsed()));
		
		//update stats
		Stats s = getService().getPlayer();
		levelStats.setText(String.format(levelFormat, 1, s.hp, s.maxhp));
		
		//update progress
		Inventory i = getService().getInventory();
		craftingStats.setText(String.format(statFormat, i.getProgress(), i.getRequiredCrafts().size));
		
		//update animations
		stage.act(delta);
		display.act(delta);
	}
	
	public final void draw()
	{
		ScissorStack.pushScissors(tmpBound);
		Batch b = stage.getBatch();
		b.begin();
		display.draw(b, stage.getRoot().getColor().a);
		b.end();
		externalRender();
		ScissorStack.popScissors();
		
		//hide display during rendering of the stage
		display.setVisible(false);
		stage.draw();
		
		//make sure it's set as visible so it accepts input between frames
		display.setVisible(true);
	}
	
	public final void resize(int width, int height){
		stage.getViewport().update(width, height);
		stage.calculateScissors(displayBounds, tmpBound);
	}
	
	public final void addToInput(InputMultiplexer input)
	{
		input.addProcessor(stage);
	}
	
	protected final Storymode getService()
	{
		return service;
	}
	
	private final void setFocus(Actor a)
	{
		stage.setKeyboardFocus(a);
		stage.setScrollFocus(a);
	}
	
	protected Batch getBatch()
	{
		return stage.getBatch();
	}
	
	protected Camera getCamera()
	{
		return stage.getViewport().getCamera();
	}
}
