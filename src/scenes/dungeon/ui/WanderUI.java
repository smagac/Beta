package scenes.dungeon.ui;

import github.nhydock.ssm.Inject;
import scene2d.ui.extras.FocusGroup;
import scene2d.ui.extras.LabeledTicker;
import scene2d.ui.extras.ScrollFocuser;
import scenes.GameUI;
import scenes.UI;
import scenes.dungeon.Direction;
import scenes.dungeon.MovementSystem;
import scenes.dungeon.Progress;
import scenes.dungeon.RenderSystem;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.SnapshotArray;

import core.DataDirs;
import core.datatypes.Item;
import core.datatypes.quests.Quest;
import core.service.interfaces.IDungeonContainer;
import core.service.interfaces.IPlayerContainer;

import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.msg.MessageDispatcher;

@SuppressWarnings("unchecked")
public class WanderUI extends GameUI {

	//logger
	private ScrollPane logPane;
	private Table log;
	Label message;
	
	//header bar
	private Label floorLabel;
	private Label monsterLabel;
	private Label lootLabel;
	
	//variables for sacrifice menu
	Group dialog;
	Image goddess;
	
	int healCost;
	private Group goddessDialog;
	private Label gMsg;
	private Table itemSubmenu;
	private Table lootList;
	private ObjectMap<Item, Integer> loot;
	private ButtonGroup lootButtons;
	private Table sacrificeList;
	ObjectMap<Item, Integer> sacrifices;
	private ButtonGroup sacrificeButtons;
	
	private ScrollPane lootPane;
	private ScrollPane sacrificePane;
	private FocusGroup sacrificeGroup;
	
	//level up dialog
	Group levelUpDialog;
	int points;
	LabeledTicker<Integer> strTicker;
	LabeledTicker<Integer> defTicker;
	LabeledTicker<Integer> spdTicker;
	LabeledTicker<Integer> vitTicker;
	FocusGroup levelUpGroup;
	
	//services
	@Inject public IPlayerContainer playerService;
	@Inject public  IDungeonContainer dungeonService;

	public WanderUI(AssetManager manager) {
		super(manager);

		loot = new ObjectMap<Item, Integer>();
		sacrifices = new ObjectMap<Item, Integer>();
		healCost = 1;
		
		menu = new DefaultStateMachine<WanderUI>(this, WanderState.Wander);
	}
	
	@Override
	protected void load()
	{
		super.load();
		manager.load("data/dungeon.atlas", TextureAtlas.class);
		manager.load("data/null.png", Texture.class);
		manager.load(DataDirs.hit, Sound.class);
		manager.load(DataDirs.dead, Sound.class);
	}

	@Override
	protected void extend() {
		
		final WanderUI ui = this;
		
		messageWindow.clear();
		
		//header bar
		{
			Table table = new Table();
			TextureAtlas atlas = manager.get("data/dungeon.atlas", TextureAtlas.class);
			table.setBackground(new TextureRegionDrawable(atlas.findRegion("floor")));
			table.setHeight(32f);
			table.setWidth(display.getWidth());
			
			//monsters
			{
				Table set = new Table();
				Image icon = new Image(atlas.findRegion("ogre"));
				Label label = monsterLabel = new Label("0/0", skin, "prompt");
				
				set.add(icon).size(32f).padRight(10f);
				set.add(label).expandX().fillX();
				table.add(set).expandX().align(Align.left).colspan(1).padLeft(10f);
			}
			
			//treasure
			{
				Table set = new Table();
				Image icon = new Image(atlas.findRegion("loot"));
				Label label = lootLabel = new Label("0", skin, "prompt");
				
				set.add(icon).size(32f).padRight(10f);
				set.add(label).expandX().fillX();
				table.add(set).expandX().colspan(1);
			}
			//floors
			{
				Table set = new Table();
				Image icon = new Image(atlas.findRegion("up"));
				Label label = floorLabel = new Label("Floor 0/0", skin, "prompt");
				
				set.add(icon).size(32f).padRight(10f);
				set.add(label).expandX().fillX();
				table.add(set).expandX().align(Align.right).colspan(1).padRight(10f);
			}
			table.setPosition(0, display.getHeight()-32f);
			display.addActor(table);
		}
		
		
		fader = new Image(skin.getRegion("fader"));
		fader.setScaling(Scaling.fill);
		fader.setPosition(0, 0);
		
		fader.addAction(Actions.alpha(0f));
		fader.act(0f);
		fader.setFillParent(true);
		display.addActor(fader);
		
		//combat log
		{
			log = new Table(skin);
			log.setWidth(messageWindow.getWidth());
			logPane = new ScrollPane(log, skin, "log");
			logPane.setSize(messageWindow.getWidth(), messageWindow.getHeight());
			logPane.addListener(new ScrollFocuser(logPane));
			messageWindow.addActor(logPane);
		}
		
		//goddess sacrifice view
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
			
			dialog.setPosition(display.getWidth()/2-dialog.getWidth()/2, display.getHeight()/2-dialog.getHeight()/2);
			display.addActor(dialog);
			
			//loot List and buttons
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
				
				lootList = new Table();
				lootList.top();
				lootList.pad(0f);
				
				lootPane = new ScrollPane(lootList, skin);
				lootPane.setScrollingDisabled(true, false);
				lootPane.setScrollbarsOnTop(false);
				lootPane.setScrollBarPositions(true, false);
				lootPane.setFadeScrollBars(false);
				lootPane.addListener(new ScrollFocuser(lootPane));
				
				lootList.setTouchable(Touchable.childrenOnly);
				itemSubmenu.add(lootPane).width(230f).expandY().fillY().pad(4f).padTop(0f);
				
				sacrificeList = new Table();
				sacrificeList.bottom();
				sacrificeList.pad(4f).padRight(10f);
				
				sacrificePane = new ScrollPane(sacrificeList, skin);
				sacrificePane.setScrollingDisabled(true, false);
				sacrificePane.setScrollbarsOnTop(false);
				sacrificePane.setScrollBarPositions(true, false);
				sacrificePane.setFadeScrollBars(false);
				sacrificePane.addListener(new ScrollFocuser(sacrificePane));
				
				sacrificeList.setTouchable(Touchable.childrenOnly);
				itemSubmenu.add(sacrificePane).width(230f).expandY().fillY().pad(4f).padTop(0f);
				
				itemSubmenu.addAction(Actions.alpha(0f));
				display.addActor(itemSubmenu);
				
				lootPane.addListener(new InputListener(){
					@Override
					public boolean keyDown(InputEvent evt, int keycode)
					{
						if (keycode == Keys.RIGHT || keycode == Keys.D)
						{
							sacrificeGroup.setFocus(sacrificePane);
							return true;
						}
						
						if (loot.size <= 0)
						{
							return false;
						}
						
						if (keycode == Keys.ENTER || keycode == Keys.SPACE)
						{
							Item item = (Item)lootButtons.getChecked().getUserObject();
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
						
						if (keycode == Keys.DOWN || keycode == Keys.S)
						{
							if (lootButtons.getChecked() == null)
							{
								Button next = lootButtons.getButtons().first();
								if (next != null)
								{
									next.setChecked(true);
								}
							}
							else
							{
								int selected = lootButtons.getButtons().indexOf(lootButtons.getChecked(), true);
								
								if (selected + 1 >= lootButtons.getButtons().size)
								{
									return false;
								}
								else
								{
									Button next = lootButtons.getButtons().get(selected+1);
									next.setChecked(true);
								}
							}
							return true;
						}
						if (keycode == Keys.UP || keycode == Keys.W)
						{
							if (lootButtons.getChecked() == null)
							{
								Button next = lootButtons.getButtons().first();
								if (next != null)
								{
									next.setChecked(true);
								}
							}
							else
							{
								int selected = lootButtons.getButtons().indexOf(lootButtons.getChecked(), true);
								if (selected - 1 < 0)
								{
									return false;
								}
								else
								{
									Button next = lootButtons.getButtons().get(selected-1);
									next.setChecked(true);
								}
							}
							return true;
						}
						
						return false;
					}
				});
				
				sacrificePane.addListener(new InputListener(){
					@Override
					public boolean keyDown(InputEvent evt, int keycode)
					{
						if (keycode == Keys.LEFT || keycode == Keys.A)
						{
							sacrificeGroup.setFocus(lootPane);
							return true;
						}
						
						if (sacrifices.size <= 0)
						{
							return false;
						}
						
						if (keycode == Keys.ENTER || keycode == Keys.SPACE)
						{
							Item item = (Item)sacrificeButtons.getChecked().getUserObject();
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
						
						if (keycode == Keys.DOWN || keycode == Keys.S)
						{
							if (sacrificeButtons.getChecked() == null)
							{
								Button next = sacrificeButtons.getButtons().first();
								if (next != null)
								{
									next.setChecked(true);
								}
							}
							else
							{
								int selected = sacrificeButtons.getButtons().indexOf(sacrificeButtons.getChecked(), true);
								
								if (selected + 1 >= sacrificeButtons.getButtons().size)
								{
									return false;
								}
								else
								{
									Button next = sacrificeButtons.getButtons().get(selected+1);
									next.setChecked(true);
								}
							}
							return true;
						}
						if (keycode == Keys.UP || keycode == Keys.W)
						{
							if (sacrificeButtons.getChecked() == null)
							{
								Button next = sacrificeButtons.getButtons().first();
								if (next != null)
								{
									next.setChecked(true);
								}
							}
							else
							{
								int selected = sacrificeButtons.getButtons().indexOf(sacrificeButtons.getChecked(), true);
								if (selected - 1 < 0)
								{
									return false;
								}
								else
								{
									Button next = sacrificeButtons.getButtons().get(selected-1);
									next.setChecked(true);
								}
							}
							return true;
						}
						
						return false;
					}
				});
			}
			
			sacrificeGroup = new FocusGroup(buttonList, lootPane, sacrificePane);
			sacrificeGroup.addListener(new ChangeListener(){
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (focusList() == null)
						return;
					
					Actor a = focusList().getFocused();
					
					if (a == lootPane)
					{
						showPointer(a, Align.left, Align.top);
					}
					else if (a == sacrificePane)
					{
						showPointer(a, Align.right, Align.top);
					}
					else
					{
						hidePointer();
					}
					
					setFocus(a);
				}
			});
			lootButtons = new ButtonGroup();
			sacrificeButtons = new ButtonGroup();
			
			goddess = new Image(skin.getRegion(playerService.getWorship()));
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
		}
		
		//level up dialog
		{

			levelUpGroup = new FocusGroup();
			levelUpGroup.setVisible(false);
			
			levelUpDialog = UI.makeWindow(skin, 500, 480, true);
			levelUpDialog.setPosition(getWidth()/2-levelUpDialog.getWidth()/2, getHeight());
			levelUpDialog.setVisible(false);
			
			final Table window = new Table();
			window.setFillParent(true);
			window.center().top().pack();
			
			Label prompt = new Label("You've Leveled Up!", skin, "prompt");
			prompt.setAlignment(Align.center);
			window.add(prompt).expandX().fillX().padBottom(20).colspan(3);
			window.row();
			
			final Label pointLabel = new Label("Points 0", skin, "prompt");
			pointLabel.setAlignment(Align.center);
			
			@SuppressWarnings("rawtypes")
			LabeledTicker[] tickers = new LabeledTicker[4];
			tickers[0] = strTicker = new LabeledTicker<Integer>("Strength", new Integer[]{0}, skin);
			tickers[1] = defTicker = new LabeledTicker<Integer>("Defense", new Integer[]{0}, skin);
			tickers[2] = spdTicker = new LabeledTicker<Integer>("Speed", new Integer[]{0}, skin);
			tickers[3] = vitTicker = new LabeledTicker<Integer>("Vitality", new Integer[]{0}, skin);
			for (final LabeledTicker<Integer> ticker : tickers)
			{
				ticker.setLeftAction(new Runnable(){

					@Override
					public void run() {
						manager.get(DataDirs.tick, Sound.class).play();
						if (ticker.getValueIndex() > 0)
						{
							ticker.defaultLeftClick.run();
							points++;
							pointLabel.setText(String.format("Points %d", points));
						}
					}
					
				});
				ticker.setRightAction(new Runnable(){
					
					@Override
					public void run() {
						manager.get(DataDirs.tick, Sound.class).play();
						if (ticker.getValueIndex() < ticker.length() && points > 0)
						{
							ticker.defaultRightClick.run();
							points--;
							pointLabel.setText(String.format("Points %d", points));
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
			accept.setPosition(levelUpDialog.getWidth()/2-accept.getWidth()/2, 10f);
			
			accept.addListener(new InputListener(){
				@Override
				public void enter(InputEvent evt, float x, float y, int pointer, Actor fromActor)
				{
					accept.setChecked(true);
				}
				
				@Override
				public void exit(InputEvent evt, float x, float y, int pointer, Actor fromActor)
				{
					accept.setChecked(false);
				}
				@Override
				public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
					if (points > 0)
					{
						manager.get(DataDirs.tick, Sound.class).play();
						return false;
					}
					
					MessageDispatcher.getInstance().dispatchMessage(0, ui, ui, GameUI.Messages.Close);
					return true;
				}
			});
			levelUpDialog.addActor(accept);
			levelUpDialog.addListener(new InputListener(){
				@Override
				public boolean keyDown(InputEvent evt, int keycode)
				{
					if (keycode == Keys.DOWN || keycode == Keys.S)
					{
						levelUpGroup.next();
						return true;
					}
					if (keycode == Keys.UP || keycode == Keys.W)
					{
						levelUpGroup.prev();
						return true;
					}
					if (keycode == Keys.ENTER || keycode == Keys.SPACE)
					{
						MessageDispatcher.getInstance().dispatchMessage(0, ui, ui, GameUI.Messages.Close);
						return true;
					}
					return false;
				}
			});
			levelUpGroup.addListener(new ChangeListener(){
				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (focusList() == null)
						return;
					
					Actor a = focusList().getFocused();
					setFocus(a);
					
					showPointer(a, Align.left, Align.center);
				}
			});
			
			addActor(levelUpDialog);
			addActor(levelUpGroup);
			
			MessageDispatcher.getInstance().addListener(Quest.Actions.Notify, this);
		}
		
		//key listener for moving the character by pressing the arrow keys or WASD
		addListener(new InputListener(){
			@Override
			public boolean keyDown(InputEvent evt, int keycode) {
				if (menu.isInState(WanderState.Wander))
				{
					Direction to = Direction.valueOf(keycode);
					if (to != null)
					{
						MessageDispatcher.getInstance().dispatchMessage(0f, ui, ui, MenuMessage.Movement, to);
						return true;
					}
				}
				return false;
			}
			
			@Override
			public void touchUp(InputEvent evt, float x, float y, int pointer, int button)
			{
				if (menu.isInState(WanderState.Wander))
				{
					MessageDispatcher.getInstance().dispatchMessage(0f, ui, ui, MenuMessage.Movement);
				}
			}
		});
	}
	
	@Override
	protected void externalRender()
	{
		if (dungeonService.getCurrentFloor() != null)
		{
			dungeonService.getCurrentFloor().getSystem(RenderSystem.class).process();
		}
	}
	
	@Override
	protected void extendAct(float delta)
	{
		if (dungeonService.getCurrentFloor() != null)
		{
			dungeonService.getCurrentFloor().setDelta(delta);
			menu.update();
		}
	}

	@Override
	protected void triggerAction(int index) {
		MessageDispatcher.getInstance().dispatchMessage(0f, this, this, index);
		menu.update();
		refreshButtons();
	}

	void refresh(Progress progress)
	{
		floorLabel.setText(String.format("Floor %d/%d", progress.depth, progress.floors));
		lootLabel.setText(String.format("%d", progress.totalLootFound));
		monsterLabel.setText(String.format("%d/%d", progress.monstersKilled, progress.monstersTotal));
	}
	
	void showGoddess(String string) {
		dungeonService.getCurrentFloor().getSystem(MovementSystem.class);
		gMsg.setText(string);
		
		goddess.clearActions();
		goddess.addAction(Actions.moveTo(display.getWidth()-128f, display.getHeight()/2-64f, .3f));
		
		goddessDialog.clearActions();
		if (menu.isInState(WanderState.Assist))
		{
			goddessDialog.addAction(Actions.moveTo(40f, display.getHeight()/2 - goddessDialog.getHeight()/2));
		}
		goddessDialog.addAction(Actions.alpha(1f, .2f));
	}
	
	void hideGoddess() {
		dungeonService.getCurrentFloor().getSystem(MovementSystem.class);
		goddess.clearActions();
		goddessDialog.clearActions();
		goddess.addAction(Actions.moveTo(display.getWidth(), display.getHeight()/2-64f, .3f));
		goddessDialog.addAction(Actions.alpha(0f, .2f));
	
		itemSubmenu.addAction(Actions.alpha(0f, .2f));
	}
	
	void showLoot() {
		itemSubmenu.clearActions();
		
		//make clone so we can work with it
		this.loot = new ObjectMap<Item, Integer>(playerService.getInventory().getLoot());
		
		sacrifices.clear();
		
		populateLoot();
		populateSacrifices();
		
		if (loot.size > 0)
		{
			lootButtons.getButtons().first().setChecked(true);
		}
		if (sacrifices.size > 0)
		{
			sacrificeButtons.getButtons().first().setChecked(true);
		}
		
		goddessDialog.clearActions();
		goddessDialog.addAction(Actions.moveTo(goddessDialog.getX(), display.getHeight()-goddessDialog.getHeight(), .2f));
		itemSubmenu.addAction(Actions.alpha(1f, .2f));
		
		lootPane.pack();
		sacrificePane.pack();
	}
	
	private void populateLoot() {
		lootList.clear();
		lootList.top().left();
		lootButtons.getAllChecked().clear();
		lootButtons.getButtons().clear();
		
		if (loot.keys().hasNext)
		{
			for (final Item item : loot.keys())
			{
				final TextButton l = new TextButton(item.toString(), skin);
				l.setName(item.toString());
				l.setUserObject(item);
				l.setDisabled(true);
				lootList.add(l).width(190f);
				Label i = new Label(""+loot.get(item), skin, "smaller");
				i.setAlignment(Align.right);
				lootList.add(i).width(20f);
				lootList.row();
				lootButtons.add(l);
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
		lootList.pack();
	}
	
	private void populateSacrifices()
	{
		sacrificeList.clear();
		sacrificeList.top().left();
		sacrificeButtons.getAllChecked().clear();
		sacrificeButtons.getButtons().clear();
		
		if (sacrifices.keys().hasNext)
		{
			for (final Item item : sacrifices.keys())
			{
				final TextButton l = new TextButton(item.toString(), skin);
				l.setName(item.toString());
				l.setUserObject(item);
				l.setDisabled(true);
				sacrificeList.add(l).width(190f);
				Label i = new Label(""+sacrifices.get(item), skin, "smaller");
				i.setAlignment(Align.right);
				sacrificeList.add(i).width(20f);
				sacrificeList.row();
				sacrificeButtons.add(l);
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

	public void fade(Runnable cmd)
	{
		fader.addAction(
			Actions.sequence(
				Actions.alpha(0f),
				Actions.alpha(1f, .3f),
				Actions.run(cmd),
				Actions.alpha(0f, .3f)
			)
		);
	}
	
	@Override
	public String[] defineButtons() {
		return ((UIState)menu.getCurrentState()).defineButtons();
	}

	@Override
	protected FocusGroup focusList() {
		if (levelUpDialog.isVisible())
		{
			return levelUpGroup;
		}
		if (menu.isInState(WanderState.Sacrifice_Heal) || menu.isInState(WanderState.Sacrifice_Leave))
		{
			return sacrificeGroup;
		}
		return null;
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
	public void resize(int width, int height)
	{
		super.resize(width, height);
		if (dungeonService.getCurrentFloor() != null)
		{
			dungeonService.getCurrentFloor().getSystem(RenderSystem.class).getStage().getViewport().update(width, height, true);
		}
	}
	
}
