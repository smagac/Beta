package scenes.dungeon;

import scenes.GameUI;
import scenes.UI;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.LabeledTicker;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.SnapshotArray;

import components.Stats;
import core.DataDirs;
import core.common.SceneManager;
import core.common.Tracker;
import core.datatypes.Item;
import core.service.IDungeonContainer;
import core.service.IPlayerContainer;
import core.util.ScrollFocuser;

public class WanderUI extends GameUI {

	Image fader;
	
	//logger
	ScrollPane logPane;
	Table log;
	Label message;
	
	//header bar
	Label floorLabel;
	Label monsterLabel;
	Label lootLabel;
	
	//variables for sacrifice menu
	Group dialog;
	Image goddess;
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
	
	//level up dialog
	Group levelUpDialog;
	int points;
	LabeledTicker<Integer> strTicker;
	LabeledTicker<Integer> defTicker;
	LabeledTicker<Integer> spdTicker;
	LabeledTicker<Integer> vitTicker;
	
	//services
	private IPlayerContainer playerService;
	private IDungeonContainer dungeonService;
	
	float walkTimer;
	private boolean movementEnabled;

	public WanderUI(AssetManager manager, IPlayerContainer playerService, IDungeonContainer dungeonService) {
		super(manager, playerService);
		
		this.playerService = playerService;
		this.dungeonService = dungeonService;
		
		loot = new ObjectMap<Item, Integer>();
		sacrifices = new ObjectMap<Item, Integer>();
		healCost = 1;
		walkTimer = -1f;
	}

	@Override
	protected void extend() {
		
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
				
				//lootList.setFillParent(true);
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
				sacrificeList.setTouchable(Touchable.childrenOnly);
				sacrificePane.addListener(new ScrollFocuser(sacrificePane));
				//sacrificeList.setFillParent(true);
				itemSubmenu.add(sacrificePane).width(230f).expandY().fillY().pad(4f).padTop(0f);
				
				itemSubmenu.addAction(Actions.alpha(0f));
				display.addActor(itemSubmenu);
			}
			
			
			
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
			levelUpDialog = UI.makeWindow(skin, 500, 480, true);
			levelUpDialog.setPosition(getWidth()/2-levelUpDialog.getWidth()/2, getHeight());
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
					
					levelUpDialog.addAction(
						Actions.sequence(
							Actions.run(new Runnable(){

								@Override
								public void run() {
									levelUpDialog.setTouchable(Touchable.disabled);
									playerService.getPlayer().levelUp(new int[]{
											strTicker.getValue(),
											defTicker.getValue(),
											spdTicker.getValue(),
											vitTicker.getValue()
									});
									strTicker.setValue(0);
									defTicker.setValue(0);
									spdTicker.setValue(0);
									vitTicker.setValue(0);
									points = 0;
									manager.get(DataDirs.accept, Sound.class).play();
									movementEnabled = true;
								}
								
							}),
							Actions.moveTo(levelUpDialog.getX(), getHeight(), .3f)
						)
					);
					return true;
				}
			});
			levelUpDialog.addActor(accept);
			
			addActor(levelUpDialog);
			
			//levelUp();
		}
		
		final InputListener displayControl = new InputListener(){
			@Override
			public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button)
			{
				if (!movementEnabled)
					return false;
				
				Direction d = Direction.valueOf(x, y, display.getWidth(), display.getHeight());
				if (d != null)
				{
					dungeonService.getCurrentFloor().getSystem(MovementSystem.class).movePlayer(d);
					walkTimer = 0f;
					return true;
				}
				return false;
			}
			
			@Override
			public void touchUp(InputEvent evt, float x, float y, int pointer, int button)
			{
				walkTimer = -1f;
			}
		};
		display.addListener(displayControl);
		
		//mouse listener for moving the character by clicking within the display
		addListener(new InputListener(){
			@Override
			public boolean keyDown(InputEvent evt, int keycode) {
				if (!movementEnabled)
					return false;
				
				boolean moved = false;
				
				Direction to = Direction.valueOf(keycode);
				if (to != null)
				{
					moved = moved || dungeonService.getCurrentFloor().getSystem(MovementSystem.class).movePlayer(to);
				}
				
				if (moved)
				{
					walkTimer = 0f;
				}
				
				return moved;
			}
		});
		
		index = 0;
		movementEnabled = true;
	}
	
	@Override
	protected void externalRender()
	{
		if (dungeonService.getCurrentFloor() != null)
		{
			dungeonService.getCurrentFloor().getSystem(RenderSystem.class).process();
		}
	}
	
	private final Vector2 mousePos = new Vector2();
	
	@Override
	protected void extendAct(float delta)
	{
		if (dungeonService.getCurrentFloor() != null)
		{
			dungeonService.getCurrentFloor().setDelta(delta);
			
			if (walkTimer >= 0f)
			{
				walkTimer += delta;
				if (walkTimer > RenderSystem.MoveSpeed*2f)
				{
					Direction d = Direction.valueOf(Gdx.input);
					if (d == null && Gdx.input.isButtonPressed(Buttons.LEFT))
					{
						mousePos.set(Gdx.input.getX(), Gdx.input.getY());
						display.screenToLocalCoordinates(mousePos);
						d = Direction.valueOf(mousePos, display.getWidth(), display.getHeight());
					}
					if (d != null)
					{
						dungeonService.getCurrentFloor().getSystem(MovementSystem.class).movePlayer(d);
						walkTimer = 0f;
					}
					else
					{
						walkTimer = -1f;
					}
				}
			}
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
			for (int i = 0; i < this.sacrifices.size; i++)
			{
				Tracker.NumberValues.Loot_Sacrificed.increment();
			}
			
			if (menu == 1)
			{
				Stats s = playerService.getPlayer();
				s.hp = s.maxhp;
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
		movementEnabled = false;
		dungeonService.getCurrentFloor().getSystem(MovementSystem.class);
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
		movementEnabled = true;
		dungeonService.getCurrentFloor().getSystem(MovementSystem.class);
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
				lootList.add(l).width(190f);
				Label i = new Label(""+loot.get(item), skin, "smaller");
				i.setAlignment(Align.right);
				lootList.add(i).width(20f);
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
				sacrificeList.add(l).width(190f);
				Label i = new Label(""+sacrifices.get(item), skin, "smaller");
				i.setAlignment(Align.right);
				sacrificeList.add(i).width(20f);
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
			return new String[]{"Request Assistance"};
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
	
	public void levelUp()
	{
		points = 5;
		
		Stats s = playerService.getPlayer();
		Integer[] str = new Integer[6];
		Integer[] def = new Integer[6];
		Integer[] spd = new Integer[6];
		Integer[] vit = new Integer[6];
		for (int i = 0; i < points+1; i++)
		{
			str[i] = s.getStrength()+i;
			def[i] = s.getDefense()+i;
			spd[i] = s.getEvasion()+i;
			vit[i] = s.getVitality()+i;
		};
		
		strTicker.changeValues(str);
		defTicker.changeValues(def);
		spdTicker.changeValues(spd);
		vitTicker.changeValues(vit);
		
		levelUpDialog.addAction(Actions.moveTo(levelUpDialog.getX(), getHeight()/2-levelUpDialog.getHeight()/2, .3f));
		levelUpDialog.setTouchable(Touchable.enabled);
		movementEnabled = false;
	}

	/**
	 * Player is dead. drop loot and make fun of him
	 */
	protected void dead() {
		movementEnabled = false;
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
							Actions.alpha(1f, .5f)
						)
					);
					dialog.addAction(
						Actions.sequence(
							Actions.alpha(0f, .5f),
							Actions.delay(.5f),
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
				Actions.alpha(1f, .5f)
			)
		);
		fader.addAction(
			Actions.sequence(
				Actions.alpha(0f),
				Actions.alpha(.5f, .5f)
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
		movementEnabled = false;
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
					fader.addAction(
						Actions.sequence(
							Actions.alpha(1f, .5f)
						)
					);
					dialog.addAction(
						Actions.sequence(
							Actions.alpha(0f, .5f),
							Actions.delay(.5f),
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
				Actions.alpha(1f, .5f)
			)
		);
		fader.addAction(
			Actions.sequence(
				Actions.alpha(0f),
				Actions.alpha(.5f, .5f)
			)
		);
		
		setFocus(dialog);
		index = -1;
		refreshButtons();
	}
	
	@Override
	protected void unhook() {
		playerService = null;
		dungeonService = null;
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

	public Skin getSkin() {
		return skin;
	}
}
