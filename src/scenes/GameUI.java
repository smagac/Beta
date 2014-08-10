package scenes;

import scene2d.ui.extras.FocusGroup;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
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

import components.Stats;
import core.DataDirs;
import core.datatypes.Inventory;
import core.service.IPlayerContainer;

/**
 * Base UI for adventuring in the town
 * </p>
 * Please don't ask about all the pixel specific values.  I used GIMP to make a mockup
 * of the UI and I'm just going by all that.  I used guides and everything, it makes more
 * sense when you open up the town_ui.xcf
 * @author nhydock
 *
 */
public abstract class GameUI extends UI {

	private static final String statFormat = "Crafting Completed %d/%d";
	private static final String levelFormat = "Level %d";
	private static final String hpFormat = "HP: %3d/%3d";
	private static final String expFormat = "EXP: %3d/%3d";
	private static final String timeFormat = "Time: %s";
	
	private Label craftingStats;
	private Label levelStats;
	private Label timeStats;
	
	private Group window;
	protected Group messageWindow;
	protected Group display;
	private Rectangle displayBounds;
	private Rectangle tmpBound;
	
	protected final HorizontalGroup buttonList;
	private ButtonGroup buttons;
	protected final ChangeListener focusListener;
	
	private Label hpStats;
	private Label expStats;

	private IPlayerContainer playerService;

	public GameUI(AssetManager manager, IPlayerContainer playerService)
	{
		super(manager);
		
		this.playerService = playerService;
		
		buttonList = new HorizontalGroup();
		buttons = new ButtonGroup();
		
		tmpBound = new Rectangle();
		displayBounds = new Rectangle();
		
		focusListener = new ChangeListener(){

			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (actor != focusList())
					return;
				
				if (focusList().getFocused() == buttonList)
				{
					hidePointer();
				}
				else
				{
					showPointer(focusList().getFocused(), Align.left, Align.top);
				}

				setFocus(focusList().getFocused());
			}
			
		};
	}
	
	@Override
	protected void load()
	{
		manager.load("data/uiskin.json", Skin.class);
		manager.load(DataDirs.accept, Sound.class);
		manager.load(DataDirs.tick, Sound.class);	
	}
	
	/**
	 * Initialize the ui after all assets have been loaded
	 */
	@Override
	public final void init()
	{
		skin = manager.get("data/uiskin.json", Skin.class);

		//stat frame
		{
			Group window = makeWindow(skin, 384, 108);
			window.setPosition(96f, 16f);
			
			craftingStats = new Label(String.format(statFormat, 0, 0), skin, "promptsm");
			levelStats = new Label(String.format(levelFormat, 99, 50, 90), skin, "promptsm");
			hpStats = new Label(String.format(levelFormat, 99, 50, 90), skin, "promptsm");
			expStats = new Label(String.format(levelFormat, 99, 50, 90), skin, "promptsm");
			timeStats = new Label(String.format(timeFormat, "000:00:00"), skin, "promptsm");
			
			craftingStats.setPosition(40f, 54f);
			timeStats.setPosition(344f-timeStats.getPrefWidth(), 54f);
			
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
		//message frame
		{
			Group window = makeWindow(skin, 384, 108);
			window.setPosition(480f, 16f);
			
			messageWindow = new Group();
			messageWindow.setPosition(32f, 32f);
			messageWindow.setSize(320f, 44f);

			window.addActor(messageWindow);
			
			addActor(window);
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
			window.addActor(display);
			window.addActor(frame);
			addActor(window);
			
			//populate the window frame
			extend();
			
		}
		
		String[] butt = defineButtons();
		if (butt != null)
		{
			window.addActor(buttonList);	
			
			buttonList.addListener(new InputListener(){
				@Override
				public boolean keyDown(InputEvent evt, int keycode)
				{
					if (!buttonList.isVisible())
						return false;
					
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
						manager.get(DataDirs.accept, Sound.class).play();
						triggerAction(getIndex());
						hidePointer();
						return true;
					}
					return false;
				}
			});
			
			refreshButtons();
		}
		
		//focus handler
		addListener(new InputListener(){
			
			@Override
			public boolean keyDown(InputEvent evt, int keycode)
			{
				if (focusList()==null)
				{
					setFocus(buttonList);
					return false;
				}
				
				if (keycode == Keys.TAB || keycode == Keys.CONTROL_RIGHT)
				{
					focusList().next(true);
					return true;
				}

				if (keycode == Keys.ESCAPE || keycode == Keys.BACKSPACE)
				{
					setFocus(buttonList);
					triggerAction(-1);
					hidePointer();
					return true;
				}
				return false;
			}
			
			@Override
			public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button)
			{
				if (button == Buttons.RIGHT)
				{
					setFocus(buttonList);
					triggerAction(-1);
					hidePointer();
					return true;
				}
				return false;
			}
		});
		
		addAction(Actions.alpha(0f));
		addAction(Actions.alpha(1f, .2f));
		calculateScissors(displayBounds, tmpBound);
		
		fader = new Image(skin.getDrawable("fader"));
		fader.setFillParent(true);
		fader.addAction(Actions.alpha(0f));
		fader.setTouchable(Touchable.disabled);
		addActor(fader);
		
		pointer = new Image(skin.getDrawable("pointer"));
		addActor(pointer);
		hidePointer();
		
		act(0);
	}
	
	/**
	 * Adds addition scene specific ui elements into the display
	 */
	protected abstract void extend();
	
	/**
	 * Allow rendering into the display things that aren't stage2d elements
	 */
	protected void externalRender(){ }
	
	/**
	 * Handles an action to be performed when a button in the menu is clicked
	 * @param index
	 */
	protected abstract void triggerAction(int index);
	
	protected abstract FocusGroup focusList();
	
	public abstract String[] defineButtons();
	
	protected final void refreshButtons()
	{
		setButtons(defineButtons());
	}
	
	private final void setButtons(final String... butt)
	{
		if (butt == null)
		{
			disableMenuInput();
			buttonList.clearChildren();
			buttons = null;
			return;
		}
		
		buttonList.clearChildren();
		
		buttons = new ButtonGroup();
		
		for (int i = 0; i < butt.length; i++)
		{
			final Button button = new TextButton(butt[i], skin);
			button.setName(butt[i]);
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
						manager.get(DataDirs.accept, Sound.class).play();
						triggerAction(getIndex());
					}
					return false;
				}
			});
			buttonList.addActor(button);
			buttons.add(button);
		}
		
		buttonList.setPosition(window.getWidth() / 2 - buttonList.getPrefWidth() / 2, 32f);
		
		setFocus(buttonList);
	}

	protected final void forceButtonFocus()
	{
		if (buttonList != null)
		{
			setFocus(buttonList);
			buttons.setChecked(buttons.getButtons().first().getName());
		}
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
	
	protected void extendAct(float delta){}
	
	@Override
	public final void act(float delta)
	{
		//update time
		timeStats.setText(String.format(timeFormat, playerService.getTimeElapsed()));
		
		//update stats
		Stats s = playerService.getPlayer();
		levelStats.setText(String.format(levelFormat, s.getLevel()));
		hpStats.setText(String.format(hpFormat, s.hp, s.maxhp));
		expStats.setText(String.format(expFormat, s.exp, s.nextExp));
		//update progress
		Inventory i = playerService.getInventory();
		craftingStats.setText(String.format(statFormat, i.getProgress(), i.getRequiredCrafts().size));
		
		//update animations
		super.act(delta);
		extendAct(delta);
		//display.act(delta);
	}
	
	@Override
	public final void draw()
	{
		Batch b = getBatch();
		
		b.setProjectionMatrix(getCamera().combined);
		b.begin();
		fill.draw(b, 1.0f);
		b.end();
		
		ScissorStack.pushScissors(tmpBound);
		externalRender();
		b.setProjectionMatrix(getCamera().combined);
		b.begin();
		display.draw(b, getRoot().getColor().a);
		b.end();
		ScissorStack.popScissors();
		
		//hide display during rendering of the stage
		display.setVisible(false);
		fill.setVisible(false);
		super.draw();

		//make sure it's set as visible so it accepts input between frames
		fill.setVisible(true);
		display.setVisible(true);
	}
	
	@Override
	public void resize(int width, int height){
		super.resize(width, height);
		calculateScissors(displayBounds, tmpBound);
	}
	
	protected final void setFocus(Actor a)
	{
		setKeyboardFocus(a);
		setScrollFocus(a);
	}
}
