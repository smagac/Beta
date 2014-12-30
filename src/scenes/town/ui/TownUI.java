package scenes.town.ui;

import github.nhydock.ssm.Inject;

import java.io.File;

import scene2d.ui.extras.FocusGroup;
import scene2d.ui.extras.ScrollFocuser;
import scene2d.ui.extras.ScrollFollower;
import scene2d.ui.extras.TabbedPane;
import scenes.GameUI;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.Input.Keys;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Interpolation;
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
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;

import core.DataDirs;
import core.datatypes.Craftable;
import core.datatypes.quests.Quest;
import core.service.interfaces.IPlayerContainer;
import core.service.interfaces.IPlayerContainer.SaveSummary;
import core.util.FileSort;
import core.util.PathSort;

@SuppressWarnings("unchecked")
public class TownUI extends GameUI {

    public static void clearHistory() {
        historyPaths.clear();
        history.clear();
    }

    static Array<String> historyPaths = new Array<String>();
    static Array<FileHandle> history = new Array<FileHandle>();
    static FileHandle directory;

    Image sleepImg;
    Group exploreImg;
    Image craftImg;
    Image character;

    // explore display
    private FocusGroup exploreGroup;
    ButtonGroup<Button> exploreTabs;
    Table exploreSubmenu;
    List<String> fileList;
    List<String> recentFileList;
    Array<FileHandle> directoryList;
    private Array<String> paths;
    Table fileDetails;
    ScrollPane fileDetailsPane;
    Table fileDetailsContent;

    // craft display
    private FocusGroup craftGroup;
    Table craftSubmenu;
    Table lootSubmenu;
    TabbedPane craftMenu;
    List<Craftable> craftList;
    List<Craftable> todayList;
    ScrollPane lootPane;
    Table lootList;
    Table requirementList;

    // quest display
    private FocusGroup questGroup;
    TabbedPane questMenu;
    Table questSubmenu;
    Table questDetails;
    ScrollPane questDetailsPane;
    Table questDetailsContent;
    List<Quest> availableQuests;
    List<Quest> acceptedQuests;

    // download display
    Group downloadWindow;

    Image goddess;
    Group goddessDialog;
    Label gMsg;

    private boolean changeDir;
    private ButtonGroup<Button> craftTabs;
    private FileHandle queueDir;

    @Inject
    public IPlayerContainer playerService;

    Group saveWindow;
    Array<Table> saveSlots;
    FocusGroup formFocus;

    public TownUI(AssetManager manager) {
        super(manager);

        menu = new DefaultStateMachine<TownUI>(this, TownState.Main);
    }

    private void makeMain() {
        final TownUI ui = this;

        // explore icon
        {
            exploreImg = new Group();
            Image back = new Image(skin.getRegion("explore_back"));
            Image front = new Image(skin.getRegion("explore"));
            exploreImg.addActor(back);
            exploreImg.addActor(front);
            front.addAction(Actions.forever(Actions.sequence(Actions.moveTo(0, 0), Actions.moveTo(0, 10f, 1.5f),
                    Actions.moveTo(0, 0, 1.5f))));
            front.setTouchable(Touchable.disabled);
            back.setTouchable(Touchable.disabled);

            exploreImg.setSize(front.getWidth(), front.getHeight());
            exploreImg.setPosition(display.getWidth() / 2 - exploreImg.getWidth() / 2,
                    display.getHeight() - exploreImg.getHeight());
            exploreImg.setTouchable(Touchable.enabled);
            exploreImg.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                    if (button == Buttons.LEFT) {
                        if (menu.isInState(TownState.Main)) {
                            MessageDispatcher.getInstance().dispatchMessage(0f, ui, ui, MenuMessage.Explore);
                            audio.playSfx(shared.getResource(DataDirs.accept, Sound.class));
                        }
                        return true;
                    }
                    return false;
                }
            });

            display.addActor(exploreImg);
        }

        // sleep icon
        {
            sleepImg = new Image(skin.getRegion("sleep"));
            sleepImg.setPosition(0f, 0f);
            sleepImg.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                    if (button == Buttons.LEFT) {

                        if (menu.isInState(TownState.Main)) {
                            triggerAction(MenuMessage.Sleep);
                            audio.playSfx(shared.getResource(DataDirs.accept, Sound.class));
                        }
                        return true;
                    }
                    return false;
                }
            });
            display.addActor(sleepImg);
        }

        // craft icon
        {
            craftImg = new Image(skin.getRegion("craft"));
            craftImg.setPosition(display.getWidth() - craftImg.getWidth(), 0);
            craftImg.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                    if (button == Buttons.LEFT) {
                        if (menu.isInState(TownState.Main)) {
                            menu.changeState(TownState.Craft);
                            audio.playSfx(shared.getResource(DataDirs.accept, Sound.class));
                            refreshButtons();
                        }
                        return true;
                    }
                    return false;
                }
            });
            display.addActor(craftImg);
        }

        // draw you
        {
            character = new Image(skin.getRegion(playerService.getGender()));
            character.setSize(96f, 96f);
            character.setPosition(display.getWidth() / 2 - character.getWidth() / 2, 18f);
            display.addActor(character);
        }
    }

    /**
     * create craft submenu layout
     */
    private void makeCraft() {
        final TownUI ui = this;

        craftSubmenu = new Table();
        craftSubmenu.setWidth(250f);
        craftSubmenu.setHeight(display.getHeight());

        craftTabs = new ButtonGroup<Button>();
        final TextButton myButton = new TextButton("My List", skin);
        myButton.setName("required");
        craftTabs.add(myButton);

        final TextButton todayButton = new TextButton("Today's Special", skin);
        todayButton.setName("extra");
        craftTabs.add(todayButton);

        // list of required crafts
        {
            final List<Craftable> list = craftList = new List<Craftable>(skin);
            list.setItems(playerService.getInventory().getRequiredCrafts());

            final ScrollPane p = new ScrollPane(list, skin);
            p.addListener(new ScrollFocuser(p));
            p.setFadeScrollBars(false);
            list.addListener(new ChangeListener() {

                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    float y = Math.max(0, (list.getSelectedIndex() * list.getItemHeight()) + p.getHeight() / 2);
                    p.scrollTo(0, list.getHeight() - y, p.getWidth(), p.getHeight());
                    requirementList.clear();

                    // build requirements list
                    MessageDispatcher.getInstance().dispatchMessage(0, ui, ui, MenuMessage.Refresh, list.getSelected());
                    audio.playSfx(shared.getResource(DataDirs.tick, Sound.class));
                }
            });
            p.addListener(new InputListener() {
                @Override
                public boolean keyDown(InputEvent evt, int keycode) {
                    if (keycode == Keys.DOWN || keycode == Keys.S) {
                        list.setSelectedIndex(Math.min(list.getItems().size - 1, list.getSelectedIndex() + 1));
                        float y = Math.max(0, (list.getSelectedIndex() * list.getItemHeight()) + p.getHeight() / 2);
                        p.scrollTo(0, list.getHeight() - y, p.getWidth(), p.getHeight());
                        return true;
                    }
                    if (keycode == Keys.UP || keycode == Keys.W) {
                        list.setSelectedIndex(Math.max(0, list.getSelectedIndex() - 1));
                        float y = Math.max(0, (list.getSelectedIndex() * list.getItemHeight()) + p.getHeight() / 2);
                        p.scrollTo(0, list.getHeight() - y, p.getWidth(), p.getHeight());
                        return true;
                    }

                    return false;
                }
            });
            myButton.setUserObject(p);
        }

        // list of today's crafts
        {
            final List<Craftable> list = todayList = new List<Craftable>(skin);
            list.setItems(playerService.getInventory().getTodaysCrafts());

            final ScrollPane p = new ScrollPane(list, skin);
            p.addListener(new ScrollFocuser(p));
            p.setFadeScrollBars(false);

            list.addListener(new ChangeListener() {

                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    float y = Math.max(0, (list.getSelectedIndex() * list.getItemHeight()) + p.getHeight() / 2);
                    p.scrollTo(0, list.getHeight() - y, p.getWidth(), p.getHeight());
                    MessageDispatcher.getInstance().dispatchMessage(0, ui, ui, MenuMessage.Refresh, list.getSelected());
                    audio.playSfx(shared.getResource(DataDirs.tick, Sound.class));
                }
            });

            p.addListener(new InputListener() {
                @Override
                public boolean keyDown(InputEvent evt, int keycode) {
                    if (keycode == Keys.DOWN || keycode == Keys.S) {
                        list.setSelectedIndex(Math.min(list.getItems().size - 1, list.getSelectedIndex() + 1));
                        float y = Math.max(0, (list.getSelectedIndex() * list.getItemHeight()) + p.getHeight() / 2);
                        p.scrollTo(0, list.getHeight() - y, p.getWidth(), p.getHeight());
                        return true;
                    }
                    if (keycode == Keys.UP || keycode == Keys.W) {
                        list.setSelectedIndex(Math.max(0, list.getSelectedIndex() - 1));
                        float y = Math.max(0, (list.getSelectedIndex() * list.getItemHeight()) + p.getHeight() / 2);
                        p.scrollTo(0, list.getHeight() - y, p.getWidth(), p.getHeight());
                        return true;
                    }

                    return false;
                }
            });
            todayButton.setUserObject(p);
        }

        craftMenu = new TabbedPane(craftTabs, false);

        craftMenu.setTabAction(new Runnable() {

            @Override
            public void run() {
                craftList.setSelectedIndex(0);
                todayList.setSelectedIndex(0);

                if (craftMenu.getOpenTabIndex() == 0) {
                    MessageDispatcher.getInstance().dispatchMessage(0, ui, ui, MenuMessage.Refresh,
                            craftList.getSelected());
                }
                else {
                    MessageDispatcher.getInstance().dispatchMessage(0, ui, ui, MenuMessage.Refresh,
                            todayList.getSelected());
                }
            }

        });

        craftMenu.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent evt, int keycode) {
                List<?> l;
                if (craftMenu.getOpenTabIndex() == 0) {
                    l = craftList;
                }
                else {
                    l = todayList;
                }

                if (keycode == Keys.DOWN || keycode == Keys.S) {
                    l.setSelectedIndex(Math.min(l.getItems().size - 1, l.getSelectedIndex() + 1));
                }
                if (keycode == Keys.UP || keycode == Keys.W) {
                    l.setSelectedIndex(Math.max(0, l.getSelectedIndex() - 1));
                }

                return false;
            }
        });

        craftSubmenu.top().add(craftMenu).expand().fill().height(display.getHeight() / 2 - 10).pad(2f).padTop(0f);

        craftSubmenu.row();

        // current highlighted craft item requirements
        requirementList = new Table();
        requirementList.row();
        requirementList.pad(10);
        requirementList.top().left();

        final ScrollPane pane2 = new ScrollPane(requirementList, skin);
        pane2.setFadeScrollBars(false);
        pane2.addListener(new ScrollFocuser(pane2));

        craftSubmenu.bottom().add(pane2).expand().fill().height(display.getHeight() / 2 - 10).pad(2f);
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
        lootPane.setHeight(display.getHeight() / 2);
        lootPane.setScrollingDisabled(true, false);
        lootPane.setScrollBarPositions(true, false);
        lootPane.setFadeScrollBars(false);
        lootPane.setScrollbarsOnTop(true);
        lootPane.addListener(new ScrollFocuser(lootPane));

        lootSubmenu.add(lootPane).expand().fill().pad(10f).padTop(0f);

        lootPane.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent evt, int keycode) {
                if (keycode == Keys.DOWN || keycode == Keys.S) {
                    lootPane.fling(.4f, 0, -64f / .4f);
                }
                if (keycode == Keys.UP || keycode == Keys.W) {
                    lootPane.fling(.4f, 0, 64f / .4f);
                }
                return false;
            }
        });

        display.addActor(lootSubmenu);

        craftGroup = new FocusGroup(buttonList, lootPane, craftMenu);
        craftGroup.addListener(focusListener);
    }

    /**
     * create explore submenu layout
     */
    private void makeExplore() {
        final TownUI ui = this;

        exploreSubmenu = new Table();
        exploreSubmenu.setWidth(250f);
        exploreSubmenu.setHeight(display.getHeight());
        exploreSubmenu.setPosition(-exploreSubmenu.getWidth(), 0);

        // pane for showing details about the selected file
        fileDetails = new Table();
        fileDetails.setSize(250f, display.getHeight());
        fileDetails.setPosition(display.getWidth(), 0);

        fileDetailsContent = new Table();
        fileDetailsPane = new ScrollPane(fileDetailsContent, skin);
        fileDetailsPane.setScrollingDisabled(true, true);
        fileDetails.add(fileDetailsPane).expand().fill();

        display.addActor(fileDetails);

        // list of required crafts
        fileList = new List<String>(skin);
        recentFileList = new List<String>(skin);
        recentFileList.setItems(historyPaths);

        // prep file browsing
        if (directory == null) {
            loadDir(Gdx.files.absolute(Gdx.files.external(".").file().getAbsolutePath()).parent());
        }
        else {
            loadDir(directory);
        }
        changeDir = false;

        exploreTabs = new ButtonGroup<Button>();

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
            fileList.addListener(new ChangeListener() {

                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (changeDir) {
                        changeDir = false;
                        event.cancel();
                        return;
                    }

                    fileDetails.addAction(Actions.moveTo(display.getWidth(), 0, .3f));

                    int listIndex;
                    final FileHandle selected;
                    try {
                        listIndex = fileList.getSelectedIndex();
                        selected = directoryList.get(listIndex);
                    }
                    catch (java.lang.IndexOutOfBoundsException e) {
                        listIndex = 0;
                        return;
                    }

                    if (selected != null && !selected.isDirectory()) {
                        MessageDispatcher.getInstance().dispatchMessage(0f, ui, ui, GameUI.Messages.Selected, selected);
                    } else {
                        fileDetails.clearActions();
                        fileDetails.addAction(Actions.moveTo(display.getWidth(), 0, .3f));
                    }

                    audio.playSfx(shared.getResource(DataDirs.tick, Sound.class));
                }
            });
            
            //handle double clicking on directories
            fileList.addListener(new InputListener(){
               private int lastIndex = -1;
               
               @Override
               public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                   int listIndex = fileList.getSelectedIndex();
                   
                   if (lastIndex == listIndex) {
                       FileHandle selected = directoryList.get(listIndex);
                       if (selected == null || selected.isDirectory()) {
                           if (selected == null) {
                               selected = directory.parent();
                           }
                           final FileHandle dest = selected;
                           changeDir = true;
                           fileList.setItems();
                           fileList.addAction(Actions.sequence(Actions.moveTo(-fileList.getWidth(), 0, .3f),
                                   Actions.run(new Runnable() {

                                       @Override
                                       public void run() {
                                           queueDir = dest;
                                       }

                                   }), Actions.moveTo(0, 0, .3f)));
                       }
                   } else {
                       lastIndex = listIndex;
                   }
                   return true;
               }
            });
        }

        {
            final TextButton recentButton = new TextButton("Recent Files", skin);
            recentButton.setName("history");
            exploreTabs.add(recentButton);

            recentFileList.addListener(new ChangeListener() {

                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    FileHandle selected = history.get(fileList.getSelectedIndex());
                    MessageDispatcher.getInstance().dispatchMessage(0f, ui, ui, GameUI.Messages.Selected, selected);
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

        exploreMenu.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent evt, int keycode) {

                if ((keycode == Keys.ENTER || keycode == Keys.SPACE) && exploreMenu.getOpenTabIndex() == 0) {
                    int listIndex = fileList.getSelectedIndex();
                    final FileHandle selected = directoryList.get(listIndex);
                    if (selected == null) {
                        // go to parent directory
                        queueDir = directory.parent();
                        audio.playSfx(shared.getResource(DataDirs.tick, Sound.class));
                        return true;
                    }
                    if (selected.isDirectory()) {
                        fileDetails.clearActions();
                        fileDetails.addAction(Actions.moveTo(display.getWidth(), 0, .3f));

                        changeDir = true;
                        fileList.setItems();
                        fileList.addAction(Actions.sequence(Actions.moveTo(-fileList.getWidth(), 0, .3f),
                                Actions.run(new Runnable() {

                                    @Override
                                    public void run() {
                                        queueDir = selected;
                                    }

                                }), Actions.moveTo(0, 0, .3f)));
                        return true;
                    }
                }

                return false;
            }
        });

        exploreMenu.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent evt, int keycode) {
                List<?> l;
                if (exploreMenu.getOpenTabIndex() == 0) {
                    l = fileList;
                }
                else {
                    l = recentFileList;
                }

                if (keycode == Keys.DOWN || keycode == Keys.S) {
                    l.setSelectedIndex(Math.min(l.getItems().size - 1, l.getSelectedIndex() + 1));
                }
                if (keycode == Keys.UP || keycode == Keys.W) {
                    l.setSelectedIndex(Math.max(0, l.getSelectedIndex() - 1));
                }

                return false;
            }
        });

        display.addActor(exploreSubmenu);

        exploreGroup = new FocusGroup(buttonList, exploreMenu);
        exploreGroup.addListener(focusListener);
    }

    /**
     * create explore submenu layout
     */
    private void makeQuest() {
        final TownUI ui = this;

        questSubmenu = new Table();
        questSubmenu.setWidth(250f);
        questSubmenu.setHeight(display.getHeight());
        questSubmenu.setPosition(-questSubmenu.getWidth(), 0);

        // pane for showing details about the selected file
        questDetails = new Table();
        questDetails.setSize(250f, display.getHeight());
        questDetails.setPosition(display.getWidth(), 0);

        questDetailsContent = new Table();
        questDetailsPane = new ScrollPane(questDetailsContent, skin);
        questDetailsPane.setScrollingDisabled(true, false);
        questDetailsPane.setFadeScrollBars(false);
        questDetails.add(questDetailsPane).expand().fill();

        display.addActor(questDetails);

        availableQuests = new List<Quest>(skin);
        acceptedQuests = new List<Quest>(skin);

        availableQuests.setItems(playerService.getQuestTracker().getQuests());
        acceptedQuests.setItems(playerService.getQuestTracker().getAcceptedQuests());

        ButtonGroup<Button> questTabs = new ButtonGroup<Button>();
        {
            final TextButton availableButton = new TextButton("Available", skin);
            availableButton.setName("available");
            questTabs.add(availableButton);

            final ScrollPane pane = new ScrollPane(availableQuests, skin);
            pane.setWidth(250f);
            pane.setHeight(display.getHeight());
            pane.setScrollingDisabled(true, false);
            pane.setFadeScrollBars(false);
            pane.setScrollBarPositions(true, false);
            pane.setScrollbarsOnTop(false);
            pane.addListener(new ScrollFocuser(pane));

            availableButton.setUserObject(pane);

            availableQuests.addListener(new ScrollFollower(pane, availableQuests));
            availableQuests.addListener(new ChangeListener() {

                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    Quest q = availableQuests.getSelected();
                    MessageDispatcher.getInstance().dispatchMessage(0, ui, ui, GameUI.Messages.Selected, q);
                }
            });
        }

        {
            final TextButton acceptedButton = new TextButton("Accepted", skin);
            acceptedButton.setName("history");
            questTabs.add(acceptedButton);

            acceptedQuests.addListener(new ChangeListener() {

                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    Quest q = acceptedQuests.getSelected();
                    MessageDispatcher.getInstance().dispatchMessage(0, ui, ui, GameUI.Messages.Selected, q);
                }

            });

            final ScrollPane pane = new ScrollPane(acceptedQuests, skin);
            pane.setWidth(250f);
            pane.setHeight(display.getHeight());
            pane.setScrollingDisabled(true, false);
            pane.setFadeScrollBars(false);
            pane.setScrollBarPositions(true, false);
            pane.setScrollbarsOnTop(false);
            pane.addListener(new ScrollFocuser(pane));

            acceptedButton.setUserObject(pane);
            acceptedQuests.addListener(new ScrollFollower(pane, acceptedQuests));
        }

        questMenu = new TabbedPane(questTabs, false);

        questSubmenu.add(questMenu).fill().expand();

        questMenu.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent evt, int keycode) {
                List<?> l;
                if (questMenu.getOpenTabIndex() == 0) {
                    l = availableQuests;
                }
                else {
                    l = acceptedQuests;
                }

                if (keycode == Keys.DOWN || keycode == Keys.S) {
                    l.setSelectedIndex(Math.min(l.getItems().size - 1, l.getSelectedIndex() + 1));
                }
                if (keycode == Keys.UP || keycode == Keys.W) {
                    l.setSelectedIndex(Math.max(0, l.getSelectedIndex() - 1));
                }

                return false;
            }
        });

        display.addActor(questSubmenu);

        questGroup = new FocusGroup(buttonList, questMenu);
        questGroup.addListener(focusListener);
    }

    /**
     * create data management submenu layout
     */
    private void makeSave() {
        final TownUI ui = this;

        Group window = saveWindow = super.makeWindow(skin, 600, 300, true);
        saveSlots = new Array<Table>();
        Table table = new Table();

        formFocus = new FocusGroup();

        formFocus.addListener(new ChangeListener() {

            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (ui.menu.isInState(TownState.Save)) {
                    ui.showPointer(formFocus.getFocused(), Align.left, Align.center);
                }
            }

        });

        table.pad(32f);
        table.setFillParent(true);

        table.add(new Label("Choose a Slot", skin, "prompt")).expandX().center();
        table.row();

        for (int i = 1; i <= playerService.slots(); i++) {
            Table row = new Table();
            row.setTouchable(Touchable.enabled);
            row.pad(0f);
            row.setBackground(skin.getDrawable("button_up"));
            SaveSummary s = playerService.summary(i);
            if (s == null) {
                row.add(new Label("No Data", skin, "prompt")).expandX().center();
            }
            else {
                Image icon = new Image(skin, s.gender);
                row.add(icon).expand().center().colspan(1).size(32f, 32f);

                row.add(new Label(s.date, skin, "prompt")).expand().colspan(1).center();

                Table info = new Table();
                info.add(new Label("Crafting Completed: " + s.progress, skin, "smaller")).expand().colspan(1).right()
                        .row();
                info.add(new Label("Time: " + s.time, skin, "smaller")).expand().colspan(1).right().row();
                info.add(new Label(new String(new char[s.diff]).replace('\0', '*') + " difficulty", skin, "smaller"))
                        .expand().colspan(1).right();

                row.add(info).colspan(1).expand().right();
            }
            row.addListener(new InputListener() {

                @Override
                public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                    if (button == Buttons.LEFT) {
                        MessageDispatcher.getInstance().dispatchMessage(0f, ui, ui, GameUI.Messages.Selected,
                                formFocus.getFocusedIndex());
                        audio.playSfx(shared.getResource(DataDirs.accept, Sound.class));
                        return true;
                    }
                    return false;
                }
            });
            formFocus.add(row);
            saveSlots.add(row);
            table.add(row).expandX().fillX().height(60);
            table.row();
        }

        window.addActor(table);
        window.setPosition(display.getWidth() / 2 - window.getWidth() / 2, display.getHeight());
        window.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent evt, int keycode) {
                if (keycode == Keys.DOWN || keycode == Keys.S) {
                    formFocus.next(true);
                }
                if (keycode == Keys.UP || keycode == Keys.W) {
                    formFocus.prev(true);
                }
                if (keycode == Keys.SPACE || keycode == Keys.ENTER) {
                    MessageDispatcher.getInstance().dispatchMessage(0f, ui, ui, GameUI.Messages.Selected,
                            formFocus.getFocusedIndex());
                }

                return false;
            }
        });
        ;

        display.addActor(window);
    }

    /**
     * Make the popup display for the downloader
     */
    private void makeDownload() {
        Group window = downloadWindow = super.makeWindow(skin, 600, 200, true);
        Table table = new Table();

        Label label = new Label("Downloading Daily Map", skin, "prompt");
        table.add(label).expand().align(Align.center);
        table.setFillParent(true);

        Image spinner = new Image(skin, playerService.getWorship());
        spinner.setSize(32f, 32f);
        spinner.setOrigin(spinner.getX(Align.center), spinner.getY(Align.center));
        spinner.addAction(Actions.forever(Actions.rotateBy(-360f, 2f)));

        spinner.setAlign(Align.center);
        spinner.setPosition(window.getX(Align.center), 60f, Align.center);

        window.setPosition(display.getWidth() / 2f - window.getWidth() / 2f, display.getHeight());
        window.addActor(table);
        window.addActor(spinner);

        display.addActor(window);
    }

    @Override
    public void extend() {
        makeMain();
        makeCraft();
        makeExplore();
        makeQuest();
        makeSave();
        makeDownload();

        goddess = new Image(skin.getRegion(playerService.getWorship()));
        goddess.setSize(128f, 128f);
        goddess.setScaling(Scaling.stretch);
        goddessDialog = makeWindow(skin, 500, 150, true);
        goddessDialog.setPosition(40f, display.getHeight() / 2f - goddessDialog.getHeight() / 2f);
        Table gMessage = new Table();
        gMessage.setFillParent(true);
        gMessage.pad(36f);
        gMsg = new Label("", skin, "small");
        gMsg.setWrap(true);
        gMessage.add(gMsg).expand().fill();
        goddessDialog.addActor(gMessage);

        display.addActor(goddess);
        display.addActor(goddessDialog);

        goddess.addAction(Actions.moveTo(display.getWidth(), display.getHeight() / 2 - 64f));
        goddessDialog.addAction(Actions.alpha(0f));
        goddessDialog.setVisible(false);

        setMessage("What're we doing next?");

        MessageDispatcher.getInstance().addListener(this, Quest.Actions.Expired);
    }

    private void loadDir(FileHandle external) {
        // disable input while loading directory
        InputProcessor input = Gdx.input.getInputProcessor();
        Gdx.input.setInputProcessor(null);

        FileHandle[] handles = external.list();
        Array<FileHandle> acceptable = new Array<FileHandle>();
        paths = new Array<String>();

        for (FileHandle handle : handles) {
            File f = handle.file();
            String path = handle.name();
            // make sure hidden files are not shown
            if (f.isHidden()) {
                continue;
            }
            if (f.isDirectory()) {
                path += "/";
            }

            acceptable.add(handle);
            paths.add(path);
        }

        paths.sort(new PathSort());
        acceptable.sort(new FileSort());

        if (external.parent() != null && !external.path().equals(external.parent().path())) {
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
    protected void extendAct(float delta) {
        if (queueDir != null) {
            loadDir(queueDir);
            queueDir = null;
        }

        menu.update();
    }

    @Override
    protected void triggerAction(int index) {
        MessageDispatcher.getInstance().dispatchMessage(0, this, this, index);
        menu.update();
        refreshButtons();
    }

    @Override
    public boolean handleMessage(Telegram msg) {
        // handle expiration of quests notification
        if (msg.message == Quest.Actions.Expired) {
            this.pushNotification("A quest has expired");
            return true;
        }
        return super.handleMessage(msg);
    }

    /**
     * restores the original positions of all the images
     */
    void restore() {
        exploreImg.clearActions();
        sleepImg.clearActions();
        craftImg.clearActions();
        character.clearActions();
        lootSubmenu.clearActions();
        craftSubmenu.clearActions();
        exploreSubmenu.clearActions();
        fileDetails.clearActions();
        questDetails.clearActions();
        questSubmenu.clearActions();
        downloadWindow.clearActions();

        saveWindow.clearActions();

        exploreImg.addAction(Actions.moveTo(getDisplayCenterX() - exploreImg.getWidth() / 2, 118f, .8f));
        sleepImg.addAction(Actions.moveTo(0, 0, .8f));
        craftImg.addAction(Actions.moveTo(getDisplayWidth() - craftImg.getWidth(), 0, .8f));
        character.addAction(Actions.moveTo(getDisplayCenterX() - character.getWidth() / 2, 18f, .8f));
        lootSubmenu.addAction(Actions.moveTo(-lootSubmenu.getWidth(), 0, .3f));
        craftSubmenu.addAction(Actions.moveTo(getDisplayWidth(), 0, .3f));
        exploreSubmenu.addAction(Actions.moveTo(-exploreSubmenu.getWidth(), 0, .3f));
        fileDetails.addAction(Actions.moveTo(getDisplayWidth(), 0, .3f));
        questSubmenu.addAction(Actions.moveTo(-questSubmenu.getWidth(), 0, .3f));
        questDetails.addAction(Actions.moveTo(getDisplayWidth(), 0, .3f));
        saveWindow.addAction(Actions.moveTo(getDisplayCenterX() - saveWindow.getWidth() / 2, getDisplayHeight(),
                .2f, Interpolation.circleOut));
        downloadWindow.addAction(Actions.moveTo(getDisplayCenterX() - downloadWindow.getWidth() / 2,
                getDisplayHeight(), .2f, Interpolation.circleOut));
        setMessage("What're we doing next?");

        enableMenuInput();
        refreshButtons();

        hidePointer();
    }

    @Override
    public String[] defineButtons() {
        return ((UIState) menu.getCurrentState()).defineButtons();
    }

    @Override
    protected FocusGroup focusList() {
        if (menu.isInState(TownState.Craft)) {
            return craftGroup;
        }
        else if (menu.isInState(TownState.Explore)) {
            return exploreGroup;
        }
        else if (menu.isInState(TownState.Save)) {
            return formFocus;
        }
        else if (menu.isInState(TownState.QuestMenu)) {
            return questGroup;
        }
        return null;
    }
}
