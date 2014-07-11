package scenes.town;

import java.io.File;
import java.util.Iterator;

import scenes.GameUI;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Scaling;

import core.DataDirs;
import core.common.SceneManager;
import core.datatypes.Craftable;
import core.datatypes.FileType;
import core.datatypes.Item;
import core.service.IPlayerContainer;
import core.util.FileSort;
import core.util.PathSort;

public class TownUI extends GameUI {

	private Image sleepImg;
	private Group exploreImg;
	private Image craftImg;
	private Image character;

	private Table exploreSubmenu;
	private List<String> fileList;
	private Array<FileHandle> directoryList;
	private FileHandle directory;
	private Table fileDetails;
	
	private Table craftSubmenu;
	private Table lootSubmenu;
	private List<Craftable> craftList;
	private ScrollPane lootPane;
	private ScrollPane craftPane;
	private Table lootList;
	private Table requirementList;
	
	private Image goddess;
	private Group goddessDialog;
	private Label gMsg;
	
	private boolean changeDir;
	private int lastIndex = -1;
	private ButtonGroup craftTabs;
	private FileHandle queueDir;
	
	private IPlayerContainer playerService;
	
	private int menu = -1;
	private static final int GODDESS = 3;
	private static final int CRAFT = 2;
	private static final int EXPLORE = 1;
	private static final int SLEEP = 0;
	private Iterator<String> dialog;
	private boolean over;
	
	public TownUI(AssetManager manager, IPlayerContainer player) {
		super(manager, player);
		this.playerService = player;
	}
	
	@Override
	protected void unhook()
	{
		this.playerService = null;
	}
	
	@Override
	public void extend()
	{
		//explore icon
		{
			exploreImg = new Group();
			Image back = new Image(skin.getRegion("explore_back"));
			Image front = new Image(skin.getRegion("explore"));
			exploreImg.addActor(back);
			exploreImg.addActor(front);
			front.addAction(
					Actions.forever(
						Actions.sequence(
							Actions.moveTo(0, 0),
							Actions.moveTo(0, 10f, 2f),
							Actions.moveTo(0, 0, 2f)
						)
					)
				);
			front.setTouchable(Touchable.disabled);
			back.setTouchable(Touchable.disabled);
			
			exploreImg.setSize(front.getWidth(), front.getHeight());
			exploreImg.setPosition(display.getWidth()/2-exploreImg.getWidth()/2, display.getHeight()-exploreImg.getHeight());
			exploreImg.setTouchable(Touchable.enabled);
			exploreImg.addListener(new InputListener(){
				@Override
				public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button)
				{
					if (button == Buttons.LEFT)
					{
						if (menu == -1)
						{
							triggerAction(1);
							manager.get(DataDirs.accept, Sound.class).play();
							refreshButtons();
						}
						return true;
					}
					return false;
				}
			});
			
			display.addActor(exploreImg);
		}

		//sleep icon
		{
			sleepImg = new Image(skin.getRegion("sleep"));
			sleepImg.setPosition(0f, 0f);
			sleepImg.addListener(new InputListener(){
				@Override
				public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button)
				{
					if (button == Buttons.LEFT)
					{
						if (menu == -1)
						{
							triggerAction(0);
							manager.get(DataDirs.accept, Sound.class).play();
							refreshButtons();
						}
						return true;
					}
					return false;
				}
			});
			display.addActor(sleepImg);
		}
		
		//craft icon
		{
			craftImg = new Image(skin.getRegion("craft"));
			craftImg.setPosition(display.getWidth()-craftImg.getWidth(), 0);
			craftImg.addListener(new InputListener(){
				@Override
				public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button)
				{
					if (button == Buttons.LEFT)
					{
						if (menu == -1)
						{
							triggerAction(2);
							manager.get(DataDirs.accept, Sound.class).play();
							refreshButtons();
						}
						return true;
					}
					return false;
				}
			});
			display.addActor(craftImg);
		}
		
		//draw you
		{
			character = new Image(skin.getRegion("character"));
			character.setSize(96f, 96f);
			character.setPosition(display.getWidth()/2-character.getWidth()/2, 18f);
			display.addActor(character);
		}
		
		//create craft submenu layout
		{
			craftSubmenu = new Table();
			craftSubmenu.setWidth(250f);
			craftSubmenu.setHeight(display.getHeight());
			
			
			Group tabs = new HorizontalGroup();
			craftTabs = new ButtonGroup();
			final TextButton myButton = new TextButton("My List", skin);
			tabs.addActor(myButton);
			myButton.setName("required");
			craftTabs.add(myButton);
			myButton.addListener(new ChangeListener(){

				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (actor == myButton)
					{
						if (myButton.isChecked())
						{
							manager.get(DataDirs.tick, Sound.class).play();
							craftList.setItems(playerService.getInventory().getRequiredCrafts());
							craftList.setSelectedIndex(0);
						}
					}
				}
			});
			
			final TextButton todayButton = new TextButton("Today's Special", skin);
			todayButton.setName("extra");
			tabs.addActor(todayButton);
			craftTabs.add(todayButton);
			todayButton.addListener(new ChangeListener(){

				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (actor == todayButton)
					{
						if (todayButton.isChecked())
						{
							craftList.setItems(playerService.getInventory().getTodaysCrafts());
							craftList.setSelectedIndex(0);
						}
					}
				}
			});
			
			craftSubmenu.add(tabs).fillX().expandX().padLeft(2f).padBottom(0f);
			craftSubmenu.row();
			
			//list of required crafts
			craftList = new List<Craftable>(skin);
			craftList.setItems(playerService.getInventory().getRequiredCrafts());
			craftList.addListener(new ChangeListener(){

				@Override
				public void changed(ChangeEvent event, Actor actor) {
					requirementList.clear();
					manager.get(DataDirs.tick, Sound.class).play();
					
					//build requirements list
					Craftable c = craftList.getSelected();
					ObjectMap<String, Integer> items = c.getRequirements();
					for (String name : items.keys())
					{
						Label l = new Label(name, skin, "smallest");
						l.setAlignment(Align.left);
						requirementList.add(l).expandX().fillX();
						Label i = new Label(""+items.get(name), skin, "smallest");
						i.setAlignment(Align.right);
						requirementList.add(i).width(30f);
						requirementList.row();
					}
					
					requirementList.pack();
				}

			});
			
			craftPane = new ScrollPane(craftList, skin);
			craftPane.setFadeScrollBars(false);
			
			craftSubmenu.top().add(craftPane).expand().fill().height(display.getHeight()/2-20).pad(2f).padTop(0f);
			
			craftSubmenu.row();
			//current highlighted craft item requirements
			requirementList = new Table();
			requirementList.clear();
			requirementList.row();
			requirementList.pad(10);
			requirementList.top().left();
			
			ScrollPane pane2 = new ScrollPane(requirementList, skin);
			pane2.setFadeScrollBars(false);
			craftSubmenu.bottom().add(pane2).expand().fill().height(display.getHeight()/2-20).pad(2f);
			craftSubmenu.pad(10f);
			craftSubmenu.setPosition(display.getWidth(), 0);
			display.addActor(craftSubmenu);
			
			lootSubmenu = new Table();
			lootSubmenu.setWidth(250f);
			lootSubmenu.setHeight(display.getHeight());
			lootSubmenu.setPosition(-lootSubmenu.getWidth(), 0);
			
			Label lootLabel = new Label("My Loot", skin, "header");
			lootLabel.setAlignment(Align.center);
			lootSubmenu.top().add(lootLabel).expandX().fillX().pad(10f).padBottom(0f);
			lootSubmenu.row();
			
			lootList = new Table();
			lootPane = new ScrollPane(lootList, skin);
			lootPane.setHeight(display.getHeight()/2);
			lootPane.setScrollingDisabled(true, false);
			lootPane.setScrollBarPositions(true, false);
			lootPane.setFadeScrollBars(false);
			lootPane.setScrollbarsOnTop(true);
			lootSubmenu.add(lootPane).expand().fill().pad(10f).padTop(0f);
			
			lootPane.addListener(new InputListener(){
				@Override
				public boolean keyDown(InputEvent evt, int keycode)
				{
					if (keycode == Keys.DOWN || keycode == Keys.S)
					{
						lootPane.fling(.4f, 0, -64f/.4f);
					}
					if (keycode == Keys.UP || keycode == Keys.W)
					{
						lootPane.fling(.4f, 0, 64f/.4f);
					}
					return false;
				}
			});
			craftPane.addListener(new InputListener(){
				@Override
				public boolean keyDown(InputEvent evt, int keycode)
				{
					if (keycode == Keys.DOWN || keycode == Keys.S)
					{
						craftList.setSelectedIndex(Math.min(craftList.getItems().size-1, craftList.getSelectedIndex()+1));
						float y = Math.max(0, (craftList.getSelectedIndex() * craftList.getItemHeight()) + craftPane.getHeight()/2);
						craftPane.scrollTo(0, craftList.getHeight()-y, craftPane.getWidth(), craftPane.getHeight());
					}
					if (keycode == Keys.UP || keycode == Keys.W)
					{
						craftList.setSelectedIndex(Math.max(0, craftList.getSelectedIndex()-1));
						float y = Math.max(0, (craftList.getSelectedIndex() * craftList.getItemHeight()) + craftPane.getHeight()/2);
						craftPane.scrollTo(0, craftList.getHeight()-y, craftPane.getWidth(), craftPane.getHeight());
					}
					if (keycode == Keys.LEFT || keycode == Keys.A)
					{
						myButton.setChecked(true);
					}
					if (keycode == Keys.RIGHT || keycode == Keys.D )
					{
						todayButton.setChecked(true);
					}
					
					return false;
				}
			});
			
			display.addActor(lootSubmenu);
		}
		

		//create explore submenu layout
		{
			exploreSubmenu = new Table();
			exploreSubmenu.setWidth(250f);
			exploreSubmenu.setHeight(display.getHeight());
			exploreSubmenu.setPosition(-exploreSubmenu.getWidth(), 0);
			
			//list of required crafts
			fileList = new List<String>(skin);
			
			loadDir(Gdx.files.absolute(Gdx.files.external(".").file().getAbsolutePath()));
			changeDir = false;

			fileList.addListener(new ChangeListener(){

				@Override
				public void changed(ChangeEvent event, Actor actor) {
					if (changeDir)
					{
						changeDir = false;
						event.cancel();
						return;
					}
					
					fileDetails.addAction(Actions.moveTo(display.getWidth(), 0, .3f));
					
					int listIndex;
					final FileHandle selected;
					try
					{
						listIndex = fileList.getSelectedIndex();
						selected = directoryList.get(listIndex);
					}
					catch (java.lang.IndexOutOfBoundsException e)
					{
						listIndex = 0;
						System.out.println("file loader derp");
						return;
					}
					
					if (selected == null && lastIndex == listIndex)
					{
						//go to parent directory
						queueDir = directory.parent();
						manager.get(DataDirs.tick, Sound.class).play();
						return;
					}
					else if (selected != null)
					{
						if (selected.isDirectory())
						{
							fileDetails.clearActions();
							fileDetails.addAction(Actions.moveTo(display.getWidth(), 0, .3f));
							
							if (lastIndex == listIndex)
							{
								changeDir = true;
								fileList.setItems();
								fileList.addAction(Actions.sequence(
									Actions.moveTo(-fileList.getWidth(), 0, .3f),
									Actions.run(new Runnable(){

										@Override
										public void run() {
											queueDir = selected;
										}
										
									}),
									Actions.moveTo(0, 0, .3f)
								));
								return;
							}
						}
						else
						{
							fileDetails.clear();
							
							//generate a details panel
							Table contents = new Table();
							
							FileType ext = FileType.getType(selected.extension());
							Image icon = new Image(skin.getRegion(ext.toString()));
							Label type = new Label("File Type: " + ext, skin);
							Label size = new Label("File Size: " + (selected.length()/1000f) + " kb", skin);
							Label diff = new Label("Difficulty: " + new String(new char[ext.difficulty(selected.length())]).replace('\0', '*'), skin);
							
							icon.setAlign(Align.center);
							icon.setSize(96f, 96f);
							icon.setScaling(Scaling.fit);
							
							contents.pad(10f);
							contents.add(icon).size(96f, 96f).expandX();
							contents.row();
							contents.add(type).expandX().fillX();
							contents.row();
							contents.add(size).expandX().fillX();
							contents.row();
							contents.add(diff).expandX().fillX();
							
							ScrollPane pane2 = new ScrollPane(contents, skin);
							pane2.setScrollingDisabled(true, true);
							fileDetails.add(pane2).expand().fill();
							fileDetails.addAction(Actions.moveTo(display.getWidth()-fileDetails.getWidth(), 0, .3f));
						}
					}
					
					if (lastIndex != -1)
					{
						manager.get(DataDirs.tick, Sound.class).play();
					}
					lastIndex = listIndex;
				}
			});
			
			final ScrollPane pane = new ScrollPane(fileList, skin);
			pane.setHeight(display.getHeight());
			pane.setScrollingDisabled(true, false);
			pane.setFadeScrollBars(false);
			pane.setScrollBarPositions(true, false);
			pane.setScrollbarsOnTop(false);
			
			exploreSubmenu.add(pane).expand().fill().pad(10f);
			
			fileList.addListener(new InputListener(){
				@Override
				public boolean keyDown(InputEvent evt, int keycode)
				{
					if (keycode == Keys.DOWN || keycode == Keys.S)
					{
						fileList.setSelectedIndex(Math.min(fileList.getItems().size-1, fileList.getSelectedIndex()+1));
					}
					if (keycode == Keys.UP || keycode == Keys.W)
					{
						fileList.setSelectedIndex(Math.max(0, fileList.getSelectedIndex()-1));
					}

					float y = Math.max(0, (fileList.getSelectedIndex() * fileList.getItemHeight()) + pane.getHeight()/2);
					pane.scrollTo(0, fileList.getHeight()-y, pane.getWidth(), pane.getHeight());
					
					return false;
				}
			});
			
			fileDetails = new Table();
			fileDetails.setSize(250f, display.getHeight());
			fileDetails.setPosition(display.getWidth(), 0);
			
			display.addActor(fileDetails);
			display.addActor(exploreSubmenu);
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
		goddessDialog.setVisible(false);
		
		setMessage("What're we doing next?");
	}
	
	protected void endGame()
	{
		InputMultiplexer input = (InputMultiplexer)Gdx.input.getInputProcessor();
		input.removeProcessor(this);
		
		over = true;
		restore();
		getRoot().clearListeners();
		getRoot().addAction(
			Actions.sequence(
				Actions.delay(3f),
				Actions.forever(
					Actions.sequence(
						Actions.moveTo(5, 0, .1f, Interpolation.bounce),
						Actions.moveTo(-5, 0, .1f, Interpolation.bounce)
					)
				)
			)
		);
		fader.addAction(
			Actions.sequence(
				Actions.delay(3f),
				Actions.alpha(1f, 5f),
				Actions.run(new Runnable(){

					@Override
					public void run() {
						SceneManager.switchToScene("endgame");
					}
					
				})
			)
		);
	}
	
	private void loadDir(FileHandle external) {
		lastIndex = -1;
		//disable input while loading directory
		InputProcessor input = Gdx.input.getInputProcessor();
		Gdx.input.setInputProcessor(null);
		
		FileHandle[] handles = external.list();
		Array<FileHandle> acceptable = new Array<FileHandle>();
		Array<String> paths = new Array<String>();
		
		for (FileHandle handle : handles)
		{
			File f = handle.file();
			String path = handle.name();
			//make sure hidden files are not shown
			if (f.isHidden())
			{
				continue;
			}
			if (f.isDirectory())
			{
				path += "/";
			}
			
			acceptable.add(handle);
			paths.add(path);
		}
		
		paths.sort(new PathSort());
		acceptable.sort(new FileSort());
		
		if (external.parent() != null || !external.path().equals(external.parent().path()))
		{
			paths.insert(0, "..");
			acceptable.insert(0, null);
		}
		
		this.fileList.setItems(paths);
		this.fileList.setSelectedIndex(0);
		this.directoryList = acceptable;
		this.directory = external;
		this.fileList.act(0f);
		
		Gdx.input.setInputProcessor(input);
	}

	@Override
	protected void externalRender (Rectangle r) {
		
		if (queueDir != null)
		{
			loadDir(queueDir);
			queueDir = null;
		}
	}
	
	@Override
	protected void triggerAction(int index)
	{
		if (menu == GODDESS)
		{
			if (dialog.hasNext())
			{
				gMsg.setText(dialog.next());
			}
			else
			{
				dialog = null;
				hideGoddess();
				menu = -1;
			}
		}
		else if (menu == CRAFT)
		{
			if (index == 1)
			{
				Craftable c = craftList.getSelected();
				int count = playerService.getInventory().getProgress();
				if (c != null)
				{
					boolean made = playerService.getInventory().makeItem(c);
					setMessage((made)?"Crafted an item!":"Not enough materials");
					populateLoot();
					
					if (playerService.getInventory().getProgressPercentage() >= 1.0f)
					{
						endGame();
					}
					//show helpful tip after first item has been made!
					else if (count == 0 && made)
					{
						int hint = MathUtils.random(3);
						if (hint == 0)
						{
							showGoddess(
								"Hooray~  You crafted something for me!",
								"For actually going through with all this and actually helping me, let me tell you a secret!",
								"If you press any button between 0-9 you can change the color of the world!",
								"And if you press + or - you can change the contrast of those colors~",
								"You can do this at any point in time in the world of StoryMode, even during the intro scene!",
								"Hope you have fun with this secret!  Goodbye~"
							);
						}
						else if (hint == 1)
						{
							showGoddess(
									"Hooray~  You crafted something for me!",
									"For actually going through with all this and actually helping me, let me tell you a secret!",
									"The great developer of your reality kind of forgot to remove debug keys from your world",
									"Pressing F5 will soft reset your world back to the introduction sequence.",
									"This can be helpful if your current difficulty is too hard or you want to start over to get a good score",
									"You can do this at any point in time in the world of StoryMode!",
									"Though I don't know why you'd want to use it before you've even really started the game.",
									"Hope you have fun with this secret!  Goodbye~"
								);
						}
						else if (hint == 2)
						{
							showGoddess(
									"Hooray~  You crafted something for me!",
									"For actually going through with all this and actually helping me, let me tell you a secret!",
									"The great developer of your reality kind of forgot to remove debug keys from your world",
									"Pressing F9 will immediately kill the application",
									"He calls it \"The Boss Key\" and told me once it's a homage to DOS Games. Whatever that means.",
									"You can do this at any point in time in the world of StoryMode!",
									"Be warned as it's a bit dangerous to use!  Hopefully you won't accidentally bump the key at any point in time~",
									"Hope you have fun with this secret!  Goodbye~"
								);
						}
						else if (hint == 3)
						{
							showGoddess(
									"Hooray~  You crafted something for me!",
									"For actually going through with all this and actually helping me, let me tell you a secret!",
									"The great developer of your reality kind of forgot to remove debug keys from your world",
									"Pressing F6 will immediately throw you into a game session of level 3 difficulty.",
									"It's great if you want to skip the cool cinematic and my blabbering mouth at the beginning of the game.",
									"Though I don't know why you'd ever want to do that!",
									"Like most other secrets, you can use this key at any point in the world of StoryMode!",
									"Hope you have fun with this secret!  Goodbye~"
								);
						}
					}
					
				}
			}
			else
			{
				restore();
			}
		}
		else if (menu == EXPLORE)
		{
			if (index == 1 || index == 2)
			{
				if (playerService.getPlayer().hp <= 0)
				{
					setMessage("You need to rest first!");
				}
				else
				{
					FileType ext = FileType.Other;
					int diff = 1;
					
					scenes.dungeon.Scene dungeon = (scenes.dungeon.Scene)SceneManager.create("dungeon");
					//load selected file dungeon
					if (index == 1)
					{
						FileHandle f = directoryList.get(fileList.getSelectedIndex());
						if (f != null && !f.isDirectory())
						{
							diff = ext.difficulty(f.length());
							dungeon.setDungeon(f, diff);
						}
						else
						{
							return;
						}
					}
					//random dungeonas
					else
					{
						ext = FileType.values()[MathUtils.random(FileType.values().length-1)];
						diff = MathUtils.random(1, 5);
						dungeon.setDungeon(ext, diff);
					}
				
					SceneManager.switchToScene(dungeon);
					
				}
			}
			else
			{
				restore();
			}
		}
		else if (menu == SLEEP)
		{
			//ignore input
		}
		else
		{
			//craft menu
			if (index == 2)
			{
				showCraftSubmenu();
			}
			else if (index == 1)
			{
				showExploreSubmenu();
			}
			else if (index == 0)
			{
				sleep();
			}
			else
			{
				restore();
			}
		}
	}
	
	private void populateLoot()
	{
		lootList.clear();
		lootList.top().left();
		
		ObjectMap<Item, Integer> loot = playerService.getInventory().getLoot();
		if (loot.keys().hasNext)
		{
			lootList.setWidth(lootPane.getWidth());
			lootList.pad(10f);
			for (Item item : loot.keys())
			{
				Label l = new Label(item.toString(), skin, "smaller");
				l.setAlignment(Align.left);
				lootList.add(l).expandX().fillX();
				Label i = new Label(""+loot.get(item), skin, "smaller");
				i.setAlignment(Align.right);
				lootList.add(i).width(30f);
				lootList.row();
				
			}
			lootList.setTouchable(Touchable.disabled);
		}
		else
		{
			lootList.center();
			Label l = new Label("Looks like you don't have any loot!  You should go exploring", skin);
			l.setWrap(true);
			l.setAlignment(Align.center);
			lootList.add(l).expandX().fillX();
		}
		lootList.pack();
	}
	
	private void showCraftSubmenu()
	{
		menu = CRAFT;
		//populate the submenu's data
		craftList.setItems(playerService.getInventory().getRequiredCrafts());	
		requirementList.clear();
		craftTabs.setChecked(craftTabs.getButtons().first().getName());
		//create loot menu
		populateLoot();
	
		sleepImg.addAction(Actions.moveTo(-sleepImg.getWidth(), 0, 1.5f));
		exploreImg.addAction(Actions.moveTo(-exploreImg.getWidth(), 118f, 1.5f));
		craftImg.addAction(
			Actions.sequence(
				Actions.moveTo(display.getWidth()-craftImg.getWidth(), display.getHeight() - craftImg.getHeight(), .5f),
				Actions.moveTo(display.getWidth()/2-craftImg.getWidth()/2, 118f, 1f)
			)
		);
	
		craftSubmenu.addAction(Actions.sequence(
			Actions.moveTo(display.getWidth(), 0),
			Actions.delay(1.5f),
			Actions.moveTo(display.getWidth()-craftSubmenu.getWidth(), 0, .3f)
		));
		
		lootSubmenu.addAction(Actions.sequence(
			Actions.moveTo(-lootSubmenu.getWidth(), 0),
			Actions.delay(1.5f),
			Actions.moveTo(0, 0, .3f)
		));
		
		setMessage("Tink Tink");
	}
	
	private void showExploreSubmenu()
	{
		menu = EXPLORE;
		sleepImg.addAction(Actions.moveTo(-sleepImg.getWidth(), 0, 1.5f));
		craftImg.addAction(Actions.moveTo(display.getWidth(), 0, 1.5f));
	
		exploreSubmenu.addAction(Actions.sequence(
			Actions.moveTo(-exploreSubmenu.getWidth(), 0),
			Actions.delay(1.5f),
			Actions.moveTo(0, 0, .3f)
		));
		
		setMessage("Where to?");
	}
	
	private void sleep()
	{
		menu = SLEEP;
		disableMenuInput();
		setMessage("Good night!");
		sleepImg.addAction(Actions.sequence(
			Actions.moveTo(32f, display.getHeight()/2 - sleepImg.getHeight()/2, .5f),
			Actions.moveTo(display.getWidth()/2 - sleepImg.getWidth()/2, display.getHeight()/2 - sleepImg.getHeight()/2, 1f)
		));
		exploreImg.addAction(Actions.moveTo(display.getWidth(), 118f, 1.5f));
		craftImg.addAction(Actions.moveTo(display.getWidth(), 0f, 1.5f));
		
		display.addAction(Actions.sequence(
			Actions.delay(2f),
			Actions.alpha(0f, 2f),
			Actions.delay(3f),
			Actions.run(new Runnable(){

				@Override
				public void run() {
					playerService.rest();
					
					//new crafts appear each day!
					playerService.getInventory().refreshCrafts();
				}
				
			}),
			Actions.alpha(1f, 2f),
			Actions.delay(.3f),
			Actions.run(new Runnable() {
				
				@Override
				public void run()
				{
					restore();
				}
				
			})
		));
	}
	
	/**
	 * restores the original positions of all the images
	 */
	private void restore()
	{
		menu = -1;
		
		exploreImg.clearActions();
		sleepImg.clearActions();
		craftImg.clearActions();
		character.clearActions();
		lootSubmenu.clearActions();
		craftSubmenu.clearActions();
		exploreSubmenu.clearActions();
		fileDetails.clearActions();
		
		exploreImg.addAction(Actions.moveTo(display.getWidth()/2-exploreImg.getWidth()/2, 118f, 1.5f));
		sleepImg.addAction(Actions.moveTo(0, 0, 1.5f));
		craftImg.addAction(Actions.moveTo(display.getWidth()-craftImg.getWidth(), 0, 1.5f));
		character.addAction(Actions.moveTo(display.getWidth()/2-character.getWidth()/2, 18f, 1.5f));
		lootSubmenu.addAction(Actions.moveTo(-lootSubmenu.getWidth(), 0, .3f));
		craftSubmenu.addAction(Actions.moveTo(display.getWidth(), 0, .3f));
		exploreSubmenu.addAction(Actions.moveTo(-exploreSubmenu.getWidth(), 0, .3f));
		fileDetails.addAction(Actions.moveTo(display.getWidth(), 0, .3f));
		setMessage("What're we doing next?");
		
		enableMenuInput();
		refreshButtons();
	}

	@Override
	public String[] defineButtons() {
		if (menu == GODDESS)
		{
			if (dialog.hasNext())
			{
				return new String[]{"Continue"};
			}
			else
			{
				return new String[]{"Good-bye"};
			}
		}
		else if (menu == CRAFT)
		{
			return new String[]{"Return", "Make Item"};
		}
		else if (menu == EXPLORE)
		{
			return new String[]{"Return", "Explore Dungeon", "Random Dungeon"};
		}
		else if (menu == SLEEP || over)
		{
			return null;
		}
		return new String[]{"sleep", "explore", "craft"};
	}

	@Override
	protected Actor[] focusList() {
		if (menu == CRAFT)
		{
			return new Actor[]{lootPane, craftPane};
		}
		else if (menu == EXPLORE)
		{
			return new Actor[]{fileList};
		}
		return null;
	}


	private void showGoddess(String... text) {
		dialog = new Array<String>(text).iterator();
		gMsg.setText(dialog.next());
		
		goddess.clearActions();
		goddess.addAction(Actions.moveTo(display.getWidth()-128f, display.getHeight()/2-64f, .3f));
		
		goddessDialog.clearActions();
		goddessDialog.addAction(Actions.alpha(1f, .2f));
		goddessDialog.setVisible(true);
		restore();
		
		menu = GODDESS;
		refreshButtons();
	}
	
	private void hideGoddess() {
		goddess.clearActions();
		goddessDialog.clearActions();
		goddess.addAction(Actions.moveTo(display.getWidth(), display.getHeight()/2-64f, .3f));
		goddessDialog.addAction(Actions.sequence(Actions.alpha(0f, .2f), Actions.run(new Runnable(){
			@Override
			public void run() {
				goddessDialog.setVisible(false);
			}
		})));
	}
}
