package scenes.dungeon;

import scenes.GameUI;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.SnapshotArray;
import com.esotericsoftware.tablelayout.Cell;

import components.Stats;
import core.common.SceneManager;
import core.common.Tracker;
import core.datatypes.Item;
import core.service.IDungeonContainer;
import core.service.IPlayerContainer;

public class WanderUI extends GameUI {

	Image fader;
	//logger
	ScrollPane logPane;
	Table log;
	
	Label message;
	Group dialog;
	
	Image goddess;
	
	//variables for sacrifice menu
	int index, menu;
	int healCost;
	private Group goddessDialog;
	private Label gMsg;
	
	private Table itemSubmenu;
	private Table lootList;
	private ObjectMap<Item, Integer> loot;
	private Table sacrificeList;
	private ObjectMap<Item, Integer> sacrifices;
	private ScrollPane lootPane;
	private ScrollPane sacrificePane;
	
	private IPlayerContainer playerService;
	private IDungeonContainer dungeonService;
	
	Table stats;
	Label enemyName;
	Label enemyHP;
	private boolean statsVis;
	float oldX, oldY;		//old mouse positions in case the player moves with keyboard the hover will update
	InputProcessor wanderControls;
	
	public WanderUI(AssetManager manager, IPlayerContainer playerService, IDungeonContainer dungeonService) {
		super(manager, playerService);
		
		this.playerService = playerService;
		this.dungeonService = dungeonService;
		
		loot = new ObjectMap<Item, Integer>();
		sacrifices = new ObjectMap<Item, Integer>();
		healCost = 1;
	}

	@Override
	protected void extend() {
		
		messageWindow.clear();
		
		log = new Table(skin);
		log.setWidth(messageWindow.getWidth());
		logPane = new ScrollPane(log, skin, "log");
		logPane.setSize(messageWindow.getWidth(), messageWindow.getHeight());
		messageWindow.addActor(logPane);
		
		dialog = makeWindow(skin, 400, 250, true);
		message = new Label("", skin, "promptsm");
		message.setWrap(true);
		message.setAlignment(Align.center);
		message.setPosition(40f, 40f);
		message.setWidth(320f);
		message.setHeight(170f);
		dialog.addActor(message);
		dialog.setVisible(false);
		
		dialog.setPosition(display.getWidth()/2-dialog.getWidth()/2, display.getHeight()/2-dialog.getHeight()/2);
		display.addActor(dialog);
		
		fader = new Image(skin.getRegion("fader"));
		fader.setScaling(Scaling.fill);
		fader.setSize(display.getWidth(), display.getHeight());
		fader.setPosition(0, 0);
		
		fader.addAction(Actions.alpha(0f));
		fader.act(0f);
		
		//loot List and buttons
		{
			itemSubmenu = new Table();
			itemSubmenu.setWidth(400f);
			itemSubmenu.setHeight(190f);
			itemSubmenu.setPosition(76f, 10f);
			
			lootList = new Table();
			lootList.top();
			lootList.setFillParent(true);
			lootList.pad(10f);
			
			lootPane = new ScrollPane(lootList, skin);
			lootPane.setScrollingDisabled(true, false);
			lootPane.setWidth(200f);
			lootList.setTouchable(Touchable.childrenOnly);
			itemSubmenu.add(lootPane).width(200f).expandY().fillY().pad(4f);
			
			sacrificeList = new Table();
			sacrificeList.top();
			sacrificeList.setFillParent(true);
			sacrificeList.pad(10f);
			sacrificePane = new ScrollPane(sacrificeList, skin);
			sacrificePane.setScrollingDisabled(true, false);
			sacrificePane.setWidth(200f);
			sacrificeList.setTouchable(Touchable.childrenOnly);
			itemSubmenu.add(sacrificePane).width(200f).expandY().fillY().pad(4f);
			
			itemSubmenu.addAction(Actions.alpha(0f));
			display.addActor(itemSubmenu);
		}
		
		//enemy stats
		{
			stats = new Table();
			enemyName = new Label("", skin, "promptsm");
			enemyName.setAlignment(Align.center);
			enemyHP = new Label("HP: 0/0", skin, "smaller");
			enemyHP.setAlignment(Align.center);
			
			float width = Math.max(enemyName.getPrefWidth(), enemyHP.getPrefWidth()) + 40;
			stats.setWidth(width);
			stats.add(enemyName).expandX().fillX().align(Align.center);
			stats.row();
			stats.add(enemyHP).expandX().fillX().align(Align.center);
			stats.addAction(Actions.alpha(0f));
			display.addActor(stats);
		}
		
		goddess = new Image(skin.getRegion("goddess"));
		goddess.setSize(128f, 128f);
		goddess.setScaling(Scaling.stretch);
		goddessDialog = makeWindow(skin, 500, 150, true);
		goddessDialog.setPosition(40f, display.getHeight()/2f-goddessDialog.getHeight()/2f);
		Table gMessage = new Table();
		gMessage.setFillParent(true);
		gMessage.pad(36f);
		gMsg = new Label("", skin, "small");
		gMsg.setWrap(true);
		gMessage.add(gMsg).expand().fill();
		goddessDialog.addActor(gMessage);
		
		display.addActor(goddess);
		display.addActor(goddessDialog);
		
		goddess.addAction(Actions.moveTo(display.getWidth(), display.getHeight()/2-64f));
		goddessDialog.addAction(Actions.alpha(0f));
		
		final InputListener displayControl = new InputListener(){
			public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button)
			{
				float dir = MathUtils.atan2(display.getHeight()/2-y, display.getWidth()/2-x);
				
				//LEFT
				if (dir > -MathUtils.PI/4 && dir < MathUtils.PI/4)
				{
					dungeonService.getCurrentFloor().getSystem(MovementSystem.class).movePlayer(MovementSystem.LEFT);
					return true;
				}
				//RIGHT
				else if (dir < -3*MathUtils.PI/4 || dir > 3*MathUtils.PI/4)
				{
					dungeonService.getCurrentFloor().getSystem(MovementSystem.class).movePlayer(MovementSystem.RIGHT);
					return true;
				}
				//UP
				else if (dir < -MathUtils.PI/4 && dir > -3*MathUtils.PI/4)
				{
					dungeonService.getCurrentFloor().getSystem(MovementSystem.class).movePlayer(MovementSystem.UP);
					return true;
				}
				//DOWN
				else if (dir > MathUtils.PI/4 && dir < 3*MathUtils.PI/4)
				{
					dungeonService.getCurrentFloor().getSystem(MovementSystem.class).movePlayer(MovementSystem.DOWN);
					return true;
				}
				return false;
			}
			
			@Override
			public boolean mouseMoved(InputEvent evt, float x, float y) { 
				Vector2 tile = dungeonService.getCurrentFloor().getSystem(RenderSystem.class).unproject(x, y, display.getWidth(), display.getHeight());
				dungeonService.getCurrentFloor().getSystem(MovementSystem.class).mouseMoved(tile);
				return true; 
			}
		};
		
		//mouse listener for moving the character by clicking within the display
		wanderControls = new InputProcessor() {
			@Override
			public boolean keyDown(int keycode) {
				boolean moved = false;
				
				//allow 2 turn dashing
				for (int i = 0; i < (Gdx.input.isKeyPressed(Keys.SHIFT_LEFT)?2:1); i++)
				{
					if (keycode == Keys.UP || keycode == Keys.W)
					{
						moved = dungeonService.getCurrentFloor().getSystem(MovementSystem.class).movePlayer(MovementSystem.UP);
					}
					if (keycode == Keys.DOWN || keycode == Keys.S)
					{
						moved = dungeonService.getCurrentFloor().getSystem(MovementSystem.class).movePlayer(MovementSystem.DOWN);
					}
					if (keycode == Keys.LEFT || keycode == Keys.A)
					{
						moved = dungeonService.getCurrentFloor().getSystem(MovementSystem.class).movePlayer(MovementSystem.LEFT);
					}
					if (keycode == Keys.RIGHT || keycode == Keys.D)
					{
						moved = dungeonService.getCurrentFloor().getSystem(MovementSystem.class).movePlayer(MovementSystem.RIGHT);
					}
				}
				if (moved)
				{
					displayControl.mouseMoved(null, oldX, oldY);
				}
				
				return moved;
			}

			@Override
			public boolean keyUp(int keycode) {return false;}

			@Override
			public boolean keyTyped(char character) {return false;}

			@Override
			public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false;	}

			@Override
			public boolean touchUp(int screenX, int screenY, int pointer, int button) {	return false; }

			@Override
			public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }

			@Override
			public boolean mouseMoved(int screenX, int screenY) { return false;	}

			@Override
			public boolean scrolled(int amount) { return false;	}
		};
		
		display.addListener(displayControl);
		
		index = 0;
	}
	
	@Override
	protected void externalRender(Rectangle view)
	{
		if (dungeonService.getCurrentFloor() != null)
		{
			dungeonService.getCurrentFloor().getSystem(RenderSystem.class).setView(getBatch(), getCamera());
			dungeonService.getCurrentFloor().getSystem(RenderSystem.class).process();
		}
	}

	@Override
	protected void triggerAction(int index) {
		if (this.index == 0)
		{
			this.index = 1;
			showGoddess("Hello there, what is it that you need?");
		}
		else if (this.index == 1)
		{
			
			if (index == 1)
			{
				showGoddess("So you'd like me to heal you?\nThat'll cost you " + (this.healCost) + " loot.");
				this.index = 2;
				menu = 1;
			}
			else if (index == 2)
			{
				showGoddess("Each floor deep you are costs another piece of loot.\nYou're currently " + dungeonService.getCurrentFloorNumber() + " floors deep.");
				this.index = 2;
				menu = 2;
			}
			
			if (index == 0)
			{
				this.index = 0;
				hideGoddess();
			}
			else 
			{
				showLoot();
			}
			
		}
		else if (this.index == 2)
		{
			if (index == 0)
			{
				hideGoddess();
				this.index = 0;
				this.menu = 0;
			}
			else
			{
				sacrifice();
			}
		}
		else if (this.index == -1)
		{
			if (index == 0)
			{
				display.addAction(
					Actions.sequence(
						Actions.alpha(0f, 3f),
						Actions.run(new Runnable(){
							@Override
							public void run(){
								SceneManager.switchToScene("town");
							}
						})
					)
				);
			}
		}
	}

	private void sacrifice()
	{
		if (playerService.getInventory().sacrifice(this.sacrifices, (menu == 1) ? this.healCost : dungeonService.getCurrentFloorNumber()))
		{
			hideGoddess();
			if (menu == 1)
			{
				Stats s = playerService.getPlayer();
				s.hp = s.maxhp;
				for (int i = 0; i < this.sacrifices.size; i++)
				{
					Tracker.NumberValues.Loot_Sacrificed.increment();
				}
				this.healCost++;
				this.index = 0;
				this.menu = 0;
			}
			else if (menu == 2)
			{
				leave();
			}
		}
		else
		{
			int desired = 0;
			if (menu == 1)
			{
				desired = this.healCost;
			}
			else
			{
				desired = dungeonService.getCurrentFloorNumber();
			}
			showGoddess("That's not enough!\nYou need to sacrifice " + desired + " items");
		}
	}

	private void showGoddess(String string) {
		hideStats();
		dungeonService.getCurrentFloor().getSystem(MovementSystem.class).inputEnabled(false);
		gMsg.setText(string);
		
		goddess.clearActions();
		goddess.addAction(Actions.moveTo(display.getWidth()-128f, display.getHeight()/2-64f, .3f));
		
		goddessDialog.clearActions();
		if (menu == 0)
		{
			goddessDialog.addAction(Actions.moveTo(40f, display.getHeight()/2 - goddessDialog.getHeight()/2));
		}
		goddessDialog.addAction(Actions.alpha(1f, .2f));
	}
	
	private void hideGoddess() {
		dungeonService.getCurrentFloor().getSystem(MovementSystem.class).inputEnabled(true);
		goddess.clearActions();
		goddessDialog.clearActions();
		goddess.addAction(Actions.moveTo(display.getWidth(), display.getHeight()/2-64f, .3f));
		goddessDialog.addAction(Actions.alpha(0f, .2f));
	
		itemSubmenu.addAction(Actions.alpha(0f, .2f));
	}
	
	private void showLoot() {
		itemSubmenu.clearActions();
		
		//make clone so we can work with it
		this.loot = new ObjectMap<Item, Integer>(playerService.getInventory().getLoot());
		
		sacrifices.clear();
		
		populateLoot();
		populateSacrifices();
		
		goddessDialog.clearActions();
		goddessDialog.addAction(Actions.moveTo(goddessDialog.getX(), display.getHeight()-goddessDialog.getHeight(), .2f));
		itemSubmenu.addAction(Actions.alpha(1f, .2f));
		
		lootPane.pack();
		sacrificePane.pack();
	}
	
	private void populateLoot() {
		lootList.clear();
		lootList.top().left();
		if (loot.keys().hasNext)
		{
			for (final Item item : loot.keys())
			{
				final TextButton l = new TextButton(item.toString(), skin);
				l.setDisabled(true);
				lootList.add(l).expandX().fillX();
				Label i = new Label(""+loot.get(item), skin, "smaller");
				i.setAlignment(Align.right);
				lootList.add(i).width(30f);
				lootList.row();
				
				l.addListener(new InputListener(){
					@Override
					public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button)
					{
						if (button == Buttons.LEFT)
						{
							Integer k = loot.get(item);
							
							if (k - 1 >= 0)
							{
								sacrifices.put(item, sacrifices.get(item, 0)+1);
								loot.put(item, k-1);
								populateSacrifices();
								populateLoot();
							}
							return true;
						}
						return false;
					}
					
					@Override
					public void enter(InputEvent evt, float x, float y, int pointer, Actor fromActor)
					{
						l.setChecked(true);
					}
					
					@Override
					public void exit(InputEvent evt, float x, float y, int pointer, Actor toActor)
					{
						l.setChecked(false);
					}
				});
			}
		}
		else
		{
			lootList.center();
			Label l = new Label("Looks like you don't have any loot!", skin, "smaller");
			l.setWrap(true);
			l.setAlignment(Align.center);
			lootList.add(l).expandX().fillX();
		}
		lootList.bottom();
		lootList.pack();
	}
	
	private void populateSacrifices()
	{
		sacrificeList.clear();
		sacrificeList.top().left();
		if (sacrifices.keys().hasNext)
		{
			for (final Item item : sacrifices.keys())
			{
				final TextButton l = new TextButton(item.toString(), skin);
				l.setDisabled(true);
				sacrificeList.add(l).expandX().fillX();
				Label i = new Label(""+sacrifices.get(item), skin, "smaller");
				i.setAlignment(Align.right);
				sacrificeList.add(i).width(30f);
				sacrificeList.row();
				
				l.addListener(new InputListener(){
					@Override
					public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button)
					{
						if (button == Buttons.LEFT)
						{
							Integer k = sacrifices.get(item);
							
							if (k - 1 >= 0)
							{
								loot.put(item, loot.get(item, 0)+1);
								sacrifices.put(item, k-1);
								if (k - 1 == 0)
								{
									sacrifices.remove(item);
								}
								populateSacrifices();
								populateLoot();
							}
							return true;
						}
						return false;
					}
					
					@Override
					public void enter(InputEvent evt, float x, float y, int pointer, Actor fromActor)
					{
						l.setChecked(true);
					}
					
					@Override
					public void exit(InputEvent evt, float x, float y, int pointer, Actor toActor)
					{
						l.setChecked(false);
					}
				});
			}
		}
		else
		{
			sacrificeList.center();
			Label l = new Label("Click on an item to add it to the list!", skin, "smaller");
			l.setWrap(true);
			l.setAlignment(Align.center);
			sacrificeList.add(l).expandX().fillX();
		}
		sacrificeList.pack();
	}

	@Override
	public String[] defineButtons() {
		if (index == 1)
		{
			return new String[]{"Return", "Heal Me", "Go Home"};
		}
		else if (index == 2)
		{
			return new String[]{"I've changed my mind", "Sacrifice Your Loot"};
		}
		else if (index == -1)
		{
			return new String[]{"Return Home"};
		}
		else
		{
			return new String[]{"Request Assistance", "Uh-Oh"};
		}
	}

	@Override
	protected Actor[] focusList() {
		if (this.index == 0)
		{
			return null;
		}
		return new Actor[]{dialog};
	}
	
	@Override
	public void setMessage(String msg)
	{
		//update the battle log
		SnapshotArray<Actor> children = log.getChildren();
		if (children.size > 10)
		{
			children.first().remove();
		}
		
		log.bottom();
		Label output = new Label(msg, skin, "smallest");
		output.setWrap(true);
		output.setAlignment(Align.left);
		log.add(output).expandX().fillX();
		log.row();
		
		log.act(0f);
		
		logPane.setScrollY(0);
	}

	/**
	 * Player is dead. drop loot and make fun of him
	 */
	protected void dead() {
		message.setText("You are dead.\n\nYou have dropped all your new loot.\nSucks to be you.");
		dialog.clearListeners();
		dialog.addListener(new InputListener(){
			@Override
			public boolean keyDown(InputEvent evt, int keycode)
			{
				if (keycode == Keys.ENTER || keycode == Keys.SPACE)
				{
					Gdx.input.setInputProcessor(null); //disable input
					
					//fade screen out and do stuff
					fader.addAction(
						Actions.sequence(
							Actions.alpha(0f, .5f)
						)
					);
					display.addAction(
						Actions.sequence(
							Actions.alpha(0f, 3f),
							Actions.run(new Runnable(){
								@Override
								public void run(){
									SceneManager.switchToScene("town");
								}
							})
						)
					);
					return true;
				}
				return false;
			}
		});
		dialog.setVisible(true);
		dialog.addAction(
			Actions.sequence(
				Actions.alpha(0f),
				Actions.alpha(1f, .2f)
			)
		);
		fader.addAction(
			Actions.sequence(
				Actions.alpha(0f),
				Actions.alpha(.5f, .2f)
			)
		);
		
		setFocus(dialog);
		index = -1;
		refreshButtons();
	}
	
	/**
	 * Player is dead. drop loot and make fun of him
	 */
	protected void leave() {
		message.setText("You decide to leave the dungeon.\nWhether that was smart of you or not, you got some sweet loot, and that's what matters.");
		dialog.clearListeners();
		dialog.addListener(new InputListener(){
			@Override
			public boolean keyDown(InputEvent evt, int keycode)
			{
				if (keycode == Keys.ENTER || keycode == Keys.SPACE)
				{
					Gdx.input.setInputProcessor(null); //disable input
					
					//fade screen out and do stuff
					display.addAction(
						Actions.sequence(
							Actions.alpha(0f, 3f),
							Actions.run(new Runnable(){
								@Override
								public void run(){
									SceneManager.switchToScene("town");
								}
							})
						)
					);
					return true;
				}
				return false;
			}
		});
		
		dialog.setVisible(true);
		dialog.addAction(
			Actions.sequence(
				Actions.alpha(0f),
				Actions.alpha(1f, .2f)
			)
		);
		fader.addAction(
			Actions.sequence(
				Actions.alpha(0f),
				Actions.alpha(.5f, .2f)
			)
		);
		
		setFocus(dialog);
		index = -1;
		refreshButtons();
	}
	
	public void showStats(float x, float y, String name, String hp)
	{
		if (statsVis && name.equals(enemyName.getText().toString())) {
			return;
		}
		
		statsVis = true;
		
		enemyName.setText(name);
		enemyHP.setText(hp);
		stats.pack();
		float width = Math.max(enemyName.getPrefWidth(), enemyHP.getPrefWidth()) + 40;
		stats.setWidth(width);
		stats.setBackground(skin.getDrawable("button_up"));
		Vector2 p = dungeonService.getCurrentFloor().getSystem(RenderSystem.class).project(x, y, display.getWidth(), display.getHeight());
		stats.addAction(Actions.sequence(
			Actions.alpha(0f),
			Actions.moveTo(p.x - stats.getPrefWidth()/2, p.y + dungeonService.getCurrentFloor().getSystem(RenderSystem.class).getScale()*.5f),
			Actions.parallel(
				Actions.alpha(1f, .3f),
				Actions.moveTo(p.x - stats.getPrefWidth()/2, p.y + dungeonService.getCurrentFloor().getSystem(RenderSystem.class).getScale()*1.25f, .3f)
				)
			)
		);
	}
	
	public void hideStats()
	{
		statsVis = false;
		stats.clearActions();
		stats.addAction(Actions.alpha(0f, .3f));
	}

	@Override
	protected void unhook() {
		playerService = null;
		dungeonService = null;
	}
}
