package scenes.town;

import github.nhydock.ssm.SceneManager;

import java.io.File;
import java.util.Iterator;

import scene2d.ui.extras.FocusGroup;
import scene2d.ui.extras.ScrollFocuser;
import scene2d.ui.extras.ScrollFollower;
import scene2d.ui.extras.TabbedPane;
import scenes.GameUI;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.fsm.StateMachine;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
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
import core.datatypes.Craftable;
import core.datatypes.FileType;
import core.datatypes.Item;
import core.service.IPlayerContainer;
import core.util.FileSort;
import core.util.PathSort;

public class TownUI extends GameUI {

	public static void clearHistory()
	{
		historyPaths.clear();
		history.clear();
	}
	
	private static Array<String> historyPaths = new Array<String>();
	private static Array<FileHandle> history = new Array<FileHandle>();
	private static FileHandle directory;

	private Image sleepImg;
	private Group exploreImg;
	private Image craftImg;
	private Image character;

	private FocusGroup exploreGroup;
	private ButtonGroup exploreTabs;
	private Table exploreSubmenu;
	private List<String> fileList;
	private List<String> recentFileList;
	private Array<FileHandle> directoryList;
	private Array<String> paths;
	private Table fileDetails;
	private ScrollPane fileDetailsPane;
	private Table fileDetailsContent;
	
	private FocusGroup craftGroup;
	private Table craftSubmenu;
	private Table lootSubmenu;
	private TabbedPane craftMenu;
	private List<Craftable> craftList;
	private List<Craftable> todayList;
	private ScrollPane lootPane;
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
	
	private StateMachine<TownUI> menu;

	public TownUI(AssetManager manager, IPlayerContainer player) {
		super(manager, player);
		this.playerService = player;
		
		menu = new DefaultStateMachine<TownUI>(this, TownState.Main);
	}
	
	@Override
	protected void unhook()
	{
		this.playerService = null;
	}
	
	@Override
	public void extend()
	{	
		final TownUI ui = this;
		
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
						if (menu.isInState(TownState.Main))
						{
							MessageDispatcher
								.getInstance()
								.dispatchMessage(0f, ui, ui, TownState.MenuMessage.Explore);
							manager.get(DataDirs.accept, Sound.class).play();
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
			sleepImg.addListener(
				new InputListener(){
					@Override
					public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button)
					{
						if (button == Buttons.LEFT)
						{
							
							if (menu.isInState(TownState.Main))
							{
								triggerAction(TownState.MenuMessage.Sleep);
								manager.get(DataDirs.accept, Sound.class).play();
							}
							return true;
						}
						return false;
					}
				}
			);
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
						if (menu.isInState(TownState.Main))
						{
							menu.changeState(TownState.Craft);
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
			character = new Image(skin.getRegion(playerService.getGender()));
			character.setSize(96f, 96f);
			character.setPosition(display.getWidth()/2-character.getWidth()/2, 18f);
			display.addActor(character);
		}
		
		//create craft submenu layout
		{
			craftSubmenu = new Table();
			craftSubmenu.setWidth(250f);
			craftSubmenu.setHeight(display.getHeight());
			
			
			craftTabs = new ButtonGroup();
			final TextButton myButton = new TextButton("My List", skin);
			myButton.setName("required");
			craftTabs.add(myButton);
			
			final TextButton todayButton = new TextButton("Today's Special", skin);
			todayButton.setName("extra");
			craftTabs.add(todayButton);
			
			//list of required crafts
			{
				final List<Craftable> list = craftList = new List<Craftable>(skin);
				list.setItems(playerService.getInventory().getRequiredCrafts());
	
				final ScrollPane p = new ScrollPane(list, skin);
				p.addListener(new ScrollFocuser(p));
				p.setFadeScrollBars(false);
				list.addListener(new ChangeListener(){
					
					@Override
					public void changed(ChangeEvent event, Actor actor) {
						float y = Math.max(0, (list.getSelectedIndex() * list.getItemHeight()) + p.getHeight()/2);
						p.scrollTo(0, list.getHeight()-y, p.getWidth(), p.getHeight());
						requirementList.clear();
						
						//build requirements list
						MessageDispatcher.getInstance().dispatchMessage(0, ui, ui, TownState.MenuMessage.Refresh, list.getSelected());
						manager.get(DataDirs.tick, Sound.class).play();
					}
				});
				p.addListener(new InputListener(){
					@Override
					public boolean keyDown(InputEvent evt, int keycode)
					{
						if (keycode == Keys.DOWN || keycode == Keys.S)
						{
							list.setSelectedIndex(Math.min(list.getItems().size-1, list.getSelectedIndex()+1));
							float y = Math.max(0, (list.getSelectedIndex() * list.getItemHeight()) + p.getHeight()/2);
							p.scrollTo(0, list.getHeight()-y, p.getWidth(), p.getHeight());
							return true;
						}
						if (keycode == Keys.UP || keycode == Keys.W)
						{
							list.setSelectedIndex(Math.max(0, list.getSelectedIndex()-1));
							float y = Math.max(0, (list.getSelectedIndex() * list.getItemHeight()) + p.getHeight()/2);
							p.scrollTo(0, list.getHeight()-y, p.getWidth(), p.getHeight());
							return true;
						}
						
						return false;
					}
				});
				myButton.setUserObject(p);
			}
			
			//list of today's crafts
			{
				final List<Craftable> list = todayList = new List<Craftable>(skin);
				list.setItems(playerService.getInventory().getTodaysCrafts());
				
				final ScrollPane p = new ScrollPane(list, skin);
				p.addListener(new ScrollFocuser(p));
				p.setFadeScrollBars(false);
				
				list.addListener(new ChangeListener(){
					
					@Override
					public void changed(ChangeEvent event, Actor actor) {
						float y = Math.max(0, (list.getSelectedIndex() * list.getItemHeight()) + p.getHeight()/2);
						p.scrollTo(0, list.getHeight()-y, p.getWidth(), p.getHeight());
						MessageDispatcher.getInstance().dispatchMessage(0, ui, ui, TownState.MenuMessage.Refresh, list.getSelected());
						manager.get(DataDirs.tick, Sound.class).play();
					}
				});
				
				p.addListener(new InputListener(){
					@Override
					public boolean keyDown(InputEvent evt, int keycode)
					{
						if (keycode == Keys.DOWN || keycode == Keys.S)
						{
							list.setSelectedIndex(Math.min(list.getItems().size-1, list.getSelectedIndex()+1));
							float y = Math.max(0, (list.getSelectedIndex() * list.getItemHeight()) + p.getHeight()/2);
							p.scrollTo(0, list.getHeight()-y, p.getWidth(), p.getHeight());
							return true;
						}
						if (keycode == Keys.UP || keycode == Keys.W)
						{
							list.setSelectedIndex(Math.max(0, list.getSelectedIndex()-1));
							float y = Math.max(0, (list.getSelectedIndex() * list.getItemHeight()) + p.getHeight()/2);
							p.scrollTo(0, list.getHeight()-y, p.getWidth(), p.getHeight());
							return true;
						}
						
						return false;
					}
				});
				todayButton.setUserObject(p);
			}
			
			
			craftMenu = new TabbedPane(craftTabs, false);
			
			craftMenu.setTabAction(new Runnable(){

				@Override
				public void run() {
					craftList.setSelectedIndex(0);
					todayList.setSelectedIndex(0);
					
					if (craftMenu.getOpenTabIndex() == 0)
					{
						MessageDispatcher.getInstance().dispatchMessage(0, ui, ui, TownState.MenuMessage.Refresh, craftList.getSelected());
					}
					else
					{
						MessageDispatcher.getInstance().dispatchMessage(0, ui, ui, TownState.MenuMessage.Refresh, todayList.getSelected());
					}
				}
				
			});
			
			craftMenu.addListener(new InputListener(){
				@Override
				public boolean keyDown(InputEvent evt, int keycode)
				{
					List<?> l;
					if (craftMenu.getOpenTabIndex() == 0)
					{
						l = craftList;
					}
					else
					{
						l = todayList;
					}
					
					if (keycode == Keys.DOWN || keycode == Keys.S)
					{
						l.setSelectedIndex(Math.min(l.getItems().size-1, l.getSelectedIndex()+1));
					}
					if (keycode == Keys.UP || keycode == Keys.W)
					{
						l.setSelectedIndex(Math.max(0, l.getSelectedIndex()-1));
					}

					return false;
				}
			});
			
			craftSubmenu.top().add(craftMenu).expand().fill().height(display.getHeight()/2-10).pad(2f).padTop(0f);
			
			craftSubmenu.row();
			
			//current highlighted craft item requirements
			requirementList = new Table();
			requirementList.row();
			requirementList.pad(10);
			requirementList.top().left();
			
			final ScrollPane pane2 = new ScrollPane(requirementList, skin);
			pane2.setFadeScrollBars(false);
			pane2.addListener(new ScrollFocuser(pane2));

			craftSubmenu.bottom().add(pane2).expand().fill().height(display.getHeight()/2-10).pad(2f);
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
			lootPane.addListener(new ScrollFocuser(lootPane));

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
			
			display.addActor(lootSubmenu);
			
			craftGroup = new FocusGroup(buttonList, lootPane, craftMenu);
			craftGroup.addListener(focusListener);
		}
		

		//create explore submenu layout
		{
			exploreSubmenu = new Table();
			exploreSubmenu.setWidth(250f);
			exploreSubmenu.setHeight(display.getHeight());
			exploreSubmenu.setPosition(-exploreSubmenu.getWidth(), 0);
			
			//pane for showing details about the selected file
			fileDetails = new Table();
			fileDetails.setSize(250f, display.getHeight());
			fileDetails.setPosition(display.getWidth(), 0);
			
			fileDetailsContent = new Table();
			fileDetailsPane = new ScrollPane(fileDetailsContent, skin);
			fileDetailsPane.setScrollingDisabled(true, true);
			fileDetails.add(fileDetailsPane).expand().fill();
			
			display.addActor(fileDetails);
			
			//list of required crafts
			fileList = new List<String>(skin);
			recentFileList = new List<String>(skin);
			recentFileList.setItems(historyPaths);
			
			//prep file browsing
			if (directory == null)
			{
				loadDir(Gdx.files.absolute(Gdx.files.external(".").file().getAbsolutePath()).parent());	
			}
			else
			{
				loadDir(directory);
			}
			changeDir = false;
			
			exploreTabs = new ButtonGroup();
			
			{
				final TextButton browseButton = new TextButton("Browse", skin);
				browseButton.setName("browse");
				exploreTabs.add(browseButton);
				
				final ScrollPane pane = new ScrollPane(fileList, skin);
				pane.setWidth(250f);
				pane.setHeight(display.getHeight());
				pane.setScrollingDisabled(true, false);
				pane.setFadeScrollBars(false);
				pane.setScrollBarPositions(true, false);
				pane.setScrollbarsOnTop(false);
				pane.addListener(new ScrollFocuser(pane));
				
				browseButton.setUserObject(pane);
				
				fileList.addListener(new ScrollFollower(pane, fileList));
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
								MessageDispatcher.getInstance().dispatchMessage(0f, ui, ui, TownState.MenuMessage.Selected, selected);
							}
						}
						
						if (lastIndex != -1)
						{
							manager.get(DataDirs.tick, Sound.class).play();
						}
						lastIndex = listIndex;
					}
				});
			}
		
			{
				final TextButton recentButton = new TextButton("Recent Files", skin);
				recentButton.setName("history");
				exploreTabs.add(recentButton);
				
				recentFileList.addListener(new ChangeListener(){

					@Override
					public void changed(ChangeEvent event, Actor actor) {
						FileHandle selected = history.get(fileList.getSelectedIndex());
						MessageDispatcher.getInstance().dispatchMessage(0f, ui, ui, TownState.MenuMessage.Selected, selected);
					}
				
				});
				
				final ScrollPane pane = new ScrollPane(recentFileList, skin);
				pane.setWidth(250f);
				pane.setHeight(display.getHeight());
				pane.setScrollingDisabled(true, false);
				pane.setFadeScrollBars(false);
				pane.setScrollBarPositions(true, false);
				pane.setScrollbarsOnTop(false);
				pane.addListener(new ScrollFocuser(pane));
				
				recentButton.setUserObject(pane);
				recentFileList.addListener(new ScrollFollower(pane, recentFileList));
			}

			final TabbedPane exploreMenu = new TabbedPane(exploreTabs, false);
			
			exploreSubmenu.add(exploreMenu).fill().expand();
			
			exploreMenu.addListener(new InputListener(){
				@Override
				public boolean keyDown(InputEvent evt, int keycode)
				{
				
					if ((keycode == Keys.ENTER || keycode == Keys.SPACE) && exploreMenu.getOpenTabIndex() == 0)
					{
						int listIndex = fileList.getSelectedIndex();
						final FileHandle selected = directoryList.get(listIndex);
						if (selected == null)
						{
							//go to parent directory
							queueDir = directory.parent();
							manager.get(DataDirs.tick, Sound.class).play();
							return true;
						}
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
								return true;
							}
						}
					}
					
					return false;
				}
			});

			exploreMenu.addListener(new InputListener(){
				@Override
				public boolean keyDown(InputEvent evt, int keycode)
				{
					List<?> l;
					if (exploreMenu.getOpenTabIndex() == 0)
					{
						l = fileList;
					}
					else
					{
						l = recentFileList;
					}
					
					if (keycode == Keys.DOWN || keycode == Keys.S)
					{
						l.setSelectedIndex(Math.min(l.getItems().size-1, l.getSelectedIndex()+1));
					}
					if (keycode == Keys.UP || keycode == Keys.W)
					{
						l.setSelectedIndex(Math.max(0, l.getSelectedIndex()-1));
					}

					return false;
				}
			});
			
			display.addActor(exploreSubmenu);
			
			exploreGroup = new FocusGroup(buttonList, exploreMenu);
			exploreGroup.addListener(focusListener);
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
		goddessDialog.setVisible(false);
		
		setMessage("What're we doing next?");
	}
	
	private void loadDir(FileHandle external) {
		lastIndex = -1;
		//disable input while loading directory
		InputProcessor input = Gdx.input.getInputProcessor();
		Gdx.input.setInputProcessor(null);
		
		FileHandle[] handles = external.list();
		Array<FileHandle> acceptable = new Array<FileHandle>();
		paths = new Array<String>();
		
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
		
		if (external.parent() != null && !external.path().equals(external.parent().path()))
		{
			paths.insert(0, "..");
			acceptable.insert(0, null);
		}
		
		this.fileList.setItems(paths);
		this.fileList.setSelectedIndex(0);
		this.directoryList = acceptable;
		directory = external;
		this.fileList.act(0f);
		
		Gdx.input.setInputProcessor(input);
	}	

	@Override
	protected void extendAct (float delta) {
		
		if (queueDir != null)
		{
			loadDir(queueDir);
			queueDir = null;
		}
	}
	
	@Override
	protected void triggerAction(int index)
	{
		MessageDispatcher.getInstance().dispatchMessage(0, this, this, index);
		menu.update();
	}
	
	/**
	 * 
	 * @param c
	 */
	
	/**
	 * restores the original positions of all the images
	 */
	private void restore()
	{
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
		
		hidePointer();
	}

	@Override
	public String[] defineButtons() {
		return ((UIState)menu.getCurrentState()).defineButtons();
	}

	@Override
	protected FocusGroup focusList() {
		if (menu.isInState(TownState.Craft))
		{
			return craftGroup;
		}
		else if (menu.isInState(TownState.Explore))
		{
			return exploreGroup;
		}
		return null;
	}

	@Override
	public void update(float delta) {
		// Do nothing
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		return menu.handleMessage(msg);
	}
	
	/**
	 * Wrapped state for wander ui, forcing define buttons
	 * @author nhydock
	 *
	 */
	private interface UIState extends State<TownUI> {
		public String[] defineButtons();
	}
	
	/**
	 * Handles state based ui menu logic and switching
	 * @author nhydock
	 *
	 */
	private enum TownState implements UIState {	
		Main(){

			@Override
			public void enter(TownUI entity) {
				entity.restore();
				entity.refreshButtons();
			}
			
			@Override
			public String[] defineButtons(){
				return new String[]{"sleep", "explore", "craft"};
			}
			
			/**
			 * Use on message to switch between menus based on button index
			 */
			@Override
			public boolean onMessage(TownUI entity, Telegram t)
			{
				if (t.message == MenuMessage.Sleep)
				{
					entity.menu.changeState(Sleep);
				}
				else if (t.message == MenuMessage.Explore)
				{
					entity.menu.changeState(Explore);
				}
				else if (t.message == MenuMessage.Craft)
				{
					entity.menu.changeState(Craft);
				}
				
				return false;
			}
		},
		Craft(){

			private void populateLoot(TownUI ui)
			{
				ui.lootList.clear();
				ui.lootList.top().left();
				
				ObjectMap<Item, Integer> loot = ui.playerService.getInventory().getLoot();
				if (loot.keys().hasNext)
				{
					ui.lootList.setWidth(ui.lootPane.getWidth());
					ui.lootList.pad(10f);
					for (Item item : loot.keys())
					{
						Label l = new Label(item.toString(), ui.skin, "smaller");
						l.setAlignment(Align.left);
						ui.lootList.add(l).expandX().fillX();
						Label i = new Label(""+loot.get(item), ui.skin, "smaller");
						i.setAlignment(Align.right);
						ui.lootList.add(i).width(30f);
						ui.lootList.row();
						
					}
					ui.lootList.setTouchable(Touchable.disabled);
				}
				else
				{
					ui.lootList.center();
					Label l = new Label("Looks like you don't have any loot!  You should go exploring", ui.skin);
					l.setWrap(true);
					l.setAlignment(Align.center);
					ui.lootList.add(l).expandX().fillX();
				}
				ui.lootList.pack();
			}
			
			private void refreshRequirements(Craftable c, TownUI entity)
			{
				if (c == null)
				{
					throw new NullPointerException("Craftable object can not be null");
				}
				
				//build requirements list
				entity.requirementList.clear();
				
				ObjectMap<String, Integer> items = c.getRequirements();
				for (String name : items.keys())
				{
					Label l = new Label(name, entity.skin, "smallest");
					l.setAlignment(Align.left);
					entity.requirementList.add(l).expandX().fillX();
					Label i = new Label(entity.playerService.getInventory().genericCount(name)+"/"+items.get(name), entity.skin, "smallest");
					i.setAlignment(Align.right);
					entity.requirementList.add(i).width(30f);
					entity.requirementList.row();
				}	
				entity.requirementList.pack();
			}
			
			@Override
			public void enter(TownUI ui) {
				//populate the submenu's data
				ui.craftMenu.showTab(0);
				//create loot menu
				populateLoot(ui);
			
				ui.sleepImg.addAction(Actions.moveTo(-ui.sleepImg.getWidth(), 0, 1.5f));
				ui.exploreImg.addAction(Actions.moveTo(-ui.exploreImg.getWidth(), 118f, 1.5f));
				ui.craftImg.addAction(
					Actions.sequence(
						Actions.moveTo(ui.display.getWidth()-ui.craftImg.getWidth(), ui.display.getHeight() - ui.craftImg.getHeight(), .5f),
						Actions.moveTo(ui.display.getWidth()/2-ui.craftImg.getWidth()/2, 118f, 1f)
					)
				);
			
				ui.craftSubmenu.addAction(Actions.sequence(
					Actions.moveTo(ui.display.getWidth(), 0),
					Actions.delay(1.5f),
					Actions.moveTo(ui.display.getWidth()-ui.craftSubmenu.getWidth(), 0, .3f)
				));
				
				ui.lootSubmenu.addAction(Actions.sequence(
					Actions.moveTo(-ui.lootSubmenu.getWidth(), 0),
					Actions.delay(1.5f),
					Actions.moveTo(0, 0, .3f)
				));
				
				ui.setMessage("Tink Tink");
				ui.refreshButtons();
			}
			
			@Override
			public String[] defineButtons(){
				return new String[]{"Return", "Make Item"};
			}
			
			@Override
			public boolean onMessage(TownUI ui, Telegram t)
			{
				if (t.message == MenuMessage.Make)
				{				
					Craftable c;
					if (ui.craftMenu.getOpenTabIndex() == 0)
					{
						Gdx.app.log("Crafting", "making required");
						c = ui.craftList.getSelected();
					}
					else
					{
						c = ui.todayList.getSelected();
					}
					if (c != null)
					{
						int count = ui.playerService.getInventory().getProgress();
						boolean made = ui.playerService.getInventory().makeItem(c);
						ui.setMessage((made)?"Crafted an item!":"Not enough materials");
						populateLoot(ui);
						
						if (made)
						{
							refreshRequirements(c, ui);
						}
						
						if (ui.playerService.getInventory().getProgressPercentage() >= 1.0f)
						{
							ui.menu.changeState(Over);
						}
						//show helpful tip after first item has been made!
						else if (count == 0 && made)
						{
							int hint = MathUtils.random(3);
							String[] text = null;
							if (hint == 0)
							{
								text = new String[]{
											"Hooray~  You crafted something for me!",
											"For actually going through with all this and actually helping me, let me tell you a secret!",
											"If you press any button between 0-9 you can change the color of the world!",
											"And if you press + or - you can change the contrast of those colors~",
											"You can do this at any point in time in the world of StoryMode, even during the intro scene!",
											"Hope you have fun with this secret!  Goodbye~"
										};
							}
							else if (hint == 1)
							{
								text = new String[]{
										"Hooray~  You crafted something for me!",
										"For actually going through with all this and actually helping me, let me tell you a secret!",
										"The great developer of your reality kind of forgot to remove debug keys from your world",
										"Pressing F5 will soft reset your world back to the introduction sequence.",
										"This can be helpful if your current difficulty is too hard or you want to start over to get a good score",
										"You can do this at any point in time in the world of StoryMode!",
										"Though I don't know why you'd want to use it before you've even really started the game.",
										"Hope you have fun with this secret!  Goodbye~"
								};
							}
							else if (hint == 2)
							{
								text = new String[]{
										"Hooray~  You crafted something for me!",
										"For actually going through with all this and actually helping me, let me tell you a secret!",
										"The great developer of your reality kind of forgot to remove debug keys from your world",
										"Pressing F9 will immediately kill the application",
										"He calls it \"The Boss Key\" and told me once it's a homage to DOS Games. Whatever that means.",
										"You can do this at any point in time in the world of StoryMode!",
										"Be warned as it's a bit dangerous to use!  Hopefully you won't accidentally bump the key at any point in time~",
										"Hope you have fun with this secret!  Goodbye~"
								};
							}
							else
							{
								text = new String[]{
										"Hooray~  You crafted something for me!",
										"For actually going through with all this and actually helping me, let me tell you a secret!",
										"The great developer of your reality kind of forgot to remove debug keys from your world",
										"Pressing F6 will immediately throw you into a game session of level 3 difficulty.",
										"It's great if you want to skip the cool cinematic and my blabbering mouth at the beginning of the game.",
										"Though I don't know why you'd ever want to do that!",
										"Like most other secrets, you can use this key at any point in the world of StoryMode!",
										"Hope you have fun with this secret!  Goodbye~"
								};
							}
							
							//modify telegram to include text
							Object info = t.extraInfo;
							t.extraInfo = text;
							Goddess.onMessage(ui, t);
							t.extraInfo = info;
							
							ui.menu.changeState(Goddess);
						}
						return true;
					}
				}
				else if (t.message == MenuMessage.Refresh)
				{
					Craftable c = (Craftable)t.extraInfo;
					refreshRequirements(c, ui);
				}
				else
				{
					ui.menu.changeState(Main);
				}
				return false;
			}
		},
		Explore(){

			@Override
			public void enter(TownUI entity) {
				entity.sleepImg.addAction(Actions.moveTo(-entity.sleepImg.getWidth(), 0, 1.5f));
				entity.craftImg.addAction(Actions.moveTo(entity.display.getWidth(), 0, 1.5f));
			
				entity.exploreSubmenu.addAction(Actions.sequence(
					Actions.moveTo(-entity.exploreSubmenu.getWidth(), 0),
					Actions.delay(1.5f),
					Actions.moveTo(0, 0, .3f)
				));
				
				entity.setMessage("Where to?");
				entity.refreshButtons();
			}
		
			@Override
			public String[] defineButtons(){
				return new String[]{"Return", "Explore Dungeon", "Random Dungeon"};
			}
			
			@Override
			public boolean onMessage(final TownUI entity, final Telegram t)
			{
				if (t.message == MenuMessage.Explore || t.message == MenuMessage.Random)
				{
					if (entity.playerService.getPlayer().hp <= 0)
					{
						entity.setMessage("You need to rest first!");
					}
					else
					{
						FileType ext = FileType.Other;
						int diff = 1;
						
						scenes.dungeon.Scene dungeon = (scenes.dungeon.Scene)SceneManager.create("dungeon");
						//load selected file dungeon
						if (t.message == MenuMessage.Explore)
						{
							if (entity.exploreTabs.getChecked().getName().equals("history"))
							{
								FileHandle f = history.get(entity.fileList.getSelectedIndex());
								ext = FileType.getType(f.extension());
								diff = ext.difficulty(f.length());
								dungeon.setDungeon(f, diff);
							}
							else
							{
								FileHandle f = entity.directoryList.get(entity.fileList.getSelectedIndex());
								if (f != null && !f.isDirectory())
								{
									ext = FileType.getType(f.extension());
									diff = ext.difficulty(f.length());
									dungeon.setDungeon(f, diff);
									history.add(f);
									historyPaths.add(f.name());
								}
								else
								{
									return false;
								}
							}
						}
						//random dungeons
						else
						{
							ext = FileType.values()[MathUtils.random(FileType.values().length-1)];
							diff = MathUtils.random(1, 5);
							dungeon.setDungeon(ext, diff);
							directory = null;
						}
					
						SceneManager.switchToScene(dungeon);
						return true;
					}
				}
				/**
				 * Updates the information in the right side file panel to reflect the metadata of the specified file
				 */
				else if (t.message == MenuMessage.Selected)
				{
					final FileHandle selected = (FileHandle)t.extraInfo;
					
					entity.fileDetails.addAction(
						Actions.sequence(
							Actions.moveTo(entity.display.getWidth(),  0, .3f),
							Actions.run(new Runnable(){
								
								@Override
								public void run() {
									//generate a details panel
									Table contents = entity.fileDetailsContent;
									contents.clear();
									
									FileType ext = FileType.getType(selected.extension());
									Image icon = new Image(entity.skin.getRegion(ext.toString()));
									Label type = new Label("File Type: " + ext, entity.skin);
									Label size = new Label("File Size: " + (selected.length()/1000f) + " kb", entity.skin);
									Label diff = new Label("Difficulty: " + new String(new char[ext.difficulty(selected.length())]).replace('\0', '*'), entity.skin);	
									
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
									
									contents.pack();
									entity.fileDetailsPane.pack();
								}
							}),
							Actions.moveTo(entity.display.getWidth()-entity.fileDetails.getWidth(), 0, .3f)
						)
					);
				}
				else
				{
					entity.menu.changeState(Main);
					return true;
				}
				
				return false;
			}
		},
		Sleep(){

			@Override
			public void enter(final TownUI entity) {
				entity.setMessage("Good night!");
				entity.sleepImg.addAction(Actions.sequence(
					Actions.moveTo(32f, entity.display.getHeight()/2 - entity.sleepImg.getHeight()/2, .5f),
					Actions.moveTo(entity.display.getWidth()/2 - entity.sleepImg.getWidth()/2, entity.display.getHeight()/2 - entity.sleepImg.getHeight()/2, 1f)
				));
				entity.exploreImg.addAction(Actions.moveTo(entity.display.getWidth(), 118f, 1.5f));
				entity.craftImg.addAction(Actions.moveTo(entity.display.getWidth(), 0f, 1.5f));
				
				entity.display.addAction(Actions.sequence(
					Actions.delay(2f),
					Actions.alpha(0f, 2f),
					Actions.delay(3f),
					Actions.run(new Runnable(){

						@Override
						public void run() {
							entity.playerService.rest();
							
							//new crafts appear each day!
							entity.playerService.getInventory().refreshCrafts();
							
							entity.todayList.setItems(entity.playerService.getInventory().getTodaysCrafts());
						}
						
					}),
					Actions.alpha(1f, 2f),
					Actions.delay(.3f),
					Actions.run(new Runnable() {
						
						@Override
						public void run()
						{
							entity.menu.changeState(TownState.Main);
						}
						
					})
				));
				entity.refreshButtons();
			}

			@Override
			public String[] defineButtons() {
				return null;
			}

			@Override
			public boolean onMessage(TownUI entity, Telegram telegram) { return false; }
			
		},
		Goddess(){

			public Iterator<String> dialog;
			
			@Override
			public void enter(TownUI entity) {
				entity.goddess.clearActions();
				entity.goddess.addAction(Actions.moveTo(entity.display.getWidth()-128f, entity.display.getHeight()/2-64f, .3f));
				
				entity.goddessDialog.clearActions();
				entity.goddessDialog.addAction(Actions.alpha(1f, .2f));
				entity.goddessDialog.setVisible(true);
				entity.restore();
				
				entity.refreshButtons();
			}
			
			@Override
			public void exit(final TownUI entity) {
				entity.goddess.clearActions();
				entity.goddessDialog.clearActions();
				entity.goddess.addAction(Actions.moveTo(entity.display.getWidth(), entity.display.getHeight()/2-64f, .3f));
				entity.goddessDialog.addAction(Actions.sequence(Actions.alpha(0f, .2f), Actions.run(new Runnable(){
					@Override
					public void run() {
						entity.goddessDialog.setVisible(false);
					}
				})));
			}

			@Override
			public void update(TownUI entity) {
				if (dialog.hasNext())
				{
					entity.gMsg.setText(dialog.next());
				}
				else
				{
					dialog = null;
					entity.menu.changeState(Main);
				}
			}
			
			@Override
			public String[] defineButtons()
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
			
			@Override
			public boolean onMessage(TownUI entity, Telegram t)
			{
				if (t.extraInfo instanceof String[])
				{
					dialog = new Array<String>((String[])t.extraInfo).iterator();
					entity.gMsg.setText(dialog.next());
					return true;
				}
				return false;
			}
		},
		Over(){

			@Override
			public String[] defineButtons() {
				return null;
			}

			@Override
			public void enter(TownUI entity) {
				InputMultiplexer input = (InputMultiplexer)Gdx.input.getInputProcessor();
				input.removeProcessor(entity);
				
				entity.restore();
				entity.getRoot().clearListeners();
				entity.getRoot().addAction(
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
				entity.fader.addAction(
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

			@Override
			public boolean onMessage(TownUI entity, Telegram telegram) {
				return false;
			}
		},
		Save(){

			@Override
			public String[] defineButtons() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void enter(TownUI entity) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public boolean onMessage(TownUI entity, Telegram telegram) {
				return false;
			}
			
			private void saveFile(TownUI entity, int slot)
			{
				entity.playerService.save(slot);
			}
			
		};
		
		/**
		 * Message types for the main state
		 * @author nhydock
		 *
		 */
		private static class MenuMessage
		{
			static final int Sleep = 0;
			static final int Explore = 1;
			static final int Craft = 2;
			
			static final int Make = 1;
			static final int Refresh = 2;
			
			static final int Random = 2;
			
			//used when an item in a list is selected
			static final int Selected = -1;
		}

		@Override
		public void exit(TownUI ui) {}

		@Override
		public void update(TownUI ui){}
	}
}
