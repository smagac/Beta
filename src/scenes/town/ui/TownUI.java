package scenes.town.ui;

import github.nhydock.ssm.Inject;
import github.nhydock.ssm.ServiceManager;
import scene2d.ui.extras.FocusGroup;
import scene2d.ui.extras.ItemList;
import scene2d.ui.extras.ScrollFocuser;
import scene2d.ui.extras.ScrollFollower;
import scene2d.ui.extras.SimpleWindow;
import scene2d.ui.extras.TabbedPane;
import scenes.GameUI;
import scenes.Messages;
import scenes.Messages.Player.ItemMsg;
import scenes.Scene;

import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.ai.fsm.DefaultStateMachine;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
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
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.Scaling;

import core.DataDirs;
import core.common.Input;
import core.components.Stats;
import core.datatypes.Craftable;
import core.datatypes.quests.Quest;
import core.service.implementations.PageFile;
import core.service.interfaces.IPlayerContainer;
import core.service.interfaces.IPlayerContainer.SaveSummary;

@SuppressWarnings("unchecked")
public class TownUI extends GameUI {

    Image character;

    Group town;
    Group home;
    Group main;
    
    // explore display
    FileBrowser fileBrowser;
    
    // craft display
    private FocusGroup craftGroup;
    Table craftSubmenu;
    Table lootSubmenu;
    TabbedPane craftMenu;
    List<Craftable> craftList;
    List<Craftable> todayList;
    ScrollPane lootPane;
    ItemList lootList;
    Table requirementList;
    private ButtonGroup<Button> craftTabs;

    // quest display
    private FocusGroup questGroup;
    TabbedPane questMenu;
    Table questSubmenu;
    Table questDetails;
    ScrollPane questDetailsPane;
    Table questDetailsContent;
    List<Quest> availableQuests;
    List<Quest> acceptedQuests;

    // training display
    SacrificeSubmenu sacrificeMenu;
    TrainMenu trainingMenu;
    
    // download display
    SimpleWindow downloadWindow;

    SimpleWindow goddessDialog;
    Label gMsg;
    Image miniGoddess;

    Image fader;

    @Inject
    public IPlayerContainer playerService;

    SimpleWindow saveWindow;
    Array<Table> saveSlots;
    FocusGroup formFocus;
    FocusGroup defaultFocus;
    private FocusGroup exploreGroup;
    PageFileWindow pageFile;
    private FocusGroup pageFileFocus;
    private FocusGroup trainingFocus;

    @Override
    protected void listenTo(IntSet messages)
    {
        super.listenTo(messages);
        messages.addAll(
            Messages.Player.NewItem, 
            Messages.Player.RemoveItem, 
            Messages.Player.UpdateItem,
            Messages.Town.SelectDungeon,
            Messages.Dungeon.Sacrifice,
            Messages.PageFile.Changed
        );
    }
    
    public TownUI(Scene scene, AssetManager manager) {
        super(scene, manager);

        stateMachine = new DefaultStateMachine<TownUI>(this, TownState.Main);
    }

    private void makeMain() {
        main = new Group();
        main.setSize(getDisplayWidth(), getDisplayHeight());
        display.addActor(main);
        
        // explore icon
        {
            Group exploreImg = new Group();
            Image back = new Image(skin.getRegion("explore_back"));
            Image front = new Image(skin.getRegion("explore"));
            exploreImg.addActor(back);
            exploreImg.addActor(front);
            front.addAction(Actions.forever(Actions.sequence(Actions.moveTo(0, 0), Actions.moveTo(0, 10f, 1.5f),
                    Actions.moveTo(0, 0, 1.5f))));
            front.setTouchable(Touchable.disabled);
            back.setTouchable(Touchable.disabled);

            exploreImg.setSize(front.getWidth(), front.getHeight());
            exploreImg.setPosition(display.getWidth() / 2, display.getHeight(), Align.top);
            exploreImg.setTouchable(Touchable.enabled);
            exploreImg.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                    if (button == Buttons.LEFT) {
                        if (stateMachine.isInState(TownState.Main)) {
                            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Button, Messages.Town.Explore);
                            audio.playSfx(DataDirs.Sounds.accept);
                        }
                        return true;
                    }
                    return false;
                }
            });
            exploreImg.setOrigin(Align.center);
            
            main.addActor(exploreImg);
        }

        // sleep/home icon
        {
            Image homeImg = new Image(skin.getRegion("home"));
            homeImg.setPosition(0f, 0, Align.bottomLeft);
            homeImg.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                    if (button == Buttons.LEFT) {

                        if (stateMachine.isInState(TownState.Main)) {
                            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Button, Messages.Town.Home);
                            audio.playSfx(DataDirs.Sounds.accept);
                        }
                        
                        return true;
                    }
                    return false;
                }
            });
            main.addActor(homeImg);
        }

        // town icon
        {
            Image townImg = new Image(skin.getRegion("town"));
            townImg.setPosition(display.getWidth(), 0, Align.bottomRight);
            townImg.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                    if (button == Buttons.LEFT) {
                        if (stateMachine.isInState(TownState.Main)) {
                            MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Button, Messages.Town.Town);
                            audio.playSfx(DataDirs.Sounds.accept);
                        }
                        return true;
                    }
                    return false;
                }
            });
            main.addActor(townImg);
        }
    }
    
    /**
     * create the home submenu, where you can save, rest, or look at your data
     */
    private void makeHome() {
        home = new Group();
        home.setSize(getDisplayWidth(), getDisplayHeight());
        home.setPosition(0, 0, Align.bottomRight);
        display.addActor(home);
        
        // sleep icon
        {
            Image sleepImg = new Image(skin, "sleep");
            
            sleepImg.setPosition(display.getWidth() / 2, display.getHeight(), Align.top);
            sleepImg.setTouchable(Touchable.enabled);
            sleepImg.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                    if (button == Buttons.LEFT) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Button, Messages.Town.Sleep);
                        audio.playSfx(DataDirs.Sounds.accept);
                        return true;
                    }
                    return false;
                }
            });
            sleepImg.setOrigin(Align.center);
            
            home.addActor(sleepImg);
        }
        
        // save icon
        {
            Image saveImg = new Image(skin.getRegion("savedata"));
            saveImg.setPosition(0f, 40, Align.bottomLeft);
            saveImg.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                    if (button == Buttons.LEFT) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Button, Messages.Town.Save);
                        audio.playSfx(DataDirs.Sounds.accept);
                        return true;
                    }
                    return false;
                }
            });
            home.addActor(saveImg);
        }
        
        //pagefile icon
        {
            Image dictImg = new Image(skin.getRegion("pagefile"));
            dictImg.setPosition(display.getWidth(), 40, Align.bottomRight);
            dictImg.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                    if (button == Buttons.LEFT) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Button, Messages.Town.PageFile);
                        audio.playSfx(DataDirs.Sounds.accept);
                        return true;
                    }
                    return false;
                }
            });
            home.addActor(dictImg);
        }
    }

    
    /**
     * create the home submenu, where you can save, rest, or look at your data
     */
    private void makeTown() {
        town = new Group();
        town.setSize(getDisplayWidth(), getDisplayHeight());
        town.setPosition(getDisplayWidth(), 0, Align.bottomLeft);
        display.addActor(town);
        
        // Craft icon
        {
            Image craftImg = new Image(skin, "craft");
            
            craftImg.setPosition(display.getWidth() / 2, display.getHeight(), Align.top);
            craftImg.setTouchable(Touchable.enabled);
            craftImg.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                    if (button == Buttons.LEFT) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Button, Messages.Town.Craft);
                        audio.playSfx(DataDirs.Sounds.accept);
                        return true;
                    }
                    return false;
                }
            });
            craftImg.setOrigin(Align.center);
            
            town.addActor(craftImg);
        }
        
        // quest icon
        {
            Image questImg = new Image(skin.getRegion("quest"));
            questImg.setPosition(0f, 40, Align.bottomLeft);
            questImg.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                    if (button == Buttons.LEFT) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Button, Messages.Town.Quest);
                        audio.playSfx(DataDirs.Sounds.accept);
                        return true;
                    }
                    return false;
                }
            });
            town.addActor(questImg);
        }
        
        //train icon
        {
            Image trainImg = new Image(skin.getRegion("train"));
            trainImg.setPosition(display.getWidth(), 40, Align.bottomRight);
            trainImg.addListener(new InputListener() {
                @Override
                public boolean touchDown(InputEvent evt, float x, float y, int pointer, int button) {
                    if (button == Buttons.LEFT) {
                        MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Button, Messages.Town.Train);
                        audio.playSfx(DataDirs.Sounds.accept);
                        return true;
                    }
                    return false;
                }
            });
            town.addActor(trainImg);
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
        final TextButton myButton = new TextButton("My List", skin, "tab");
        myButton.setName("required");
        craftTabs.add(myButton);

        final TextButton todayButton = new TextButton("Today's Special", skin, "tab");
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
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Selected, list.getSelected());
                    audio.playSfx(DataDirs.Sounds.tick);
                }
            });
            p.addListener(new InputListener() {
                @Override
                public boolean keyDown(InputEvent evt, int keycode) {
                    if (Input.DOWN.match(keycode)) {
                        list.setSelectedIndex(Math.min(list.getItems().size - 1, list.getSelectedIndex() + 1));
                        float y = Math.max(0, (list.getSelectedIndex() * list.getItemHeight()) + p.getHeight() / 2);
                        p.scrollTo(0, list.getHeight() - y, p.getWidth(), p.getHeight());
                        return true;
                    }
                    if (Input.UP.match(keycode)) {
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
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Selected, list.getSelected());
                    audio.playSfx(DataDirs.Sounds.tick);
                }
            });

            p.addListener(new InputListener() {
                @Override
                public boolean keyDown(InputEvent evt, int keycode) {
                    if (Input.DOWN.match(keycode)) {
                        list.setSelectedIndex(Math.min(list.getItems().size - 1, list.getSelectedIndex() + 1));
                        float y = Math.max(0, (list.getSelectedIndex() * list.getItemHeight()) + p.getHeight() / 2);
                        p.scrollTo(0, list.getHeight() - y, p.getWidth(), p.getHeight());
                        return true;
                    }
                    if (Input.UP.match(keycode)) {
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
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Selected, craftList.getSelected());
                }
                else {
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Selected, todayList.getSelected());
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

                if (Input.DOWN.match(keycode)) {
                    l.setSelectedIndex(Math.min(l.getItems().size - 1, l.getSelectedIndex() + 1));
                }
                if (Input.UP.match(keycode)) {
                    l.setSelectedIndex(Math.max(0, l.getSelectedIndex() - 1));
                }
                if (Input.ACCEPT.match(keycode)) {
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Button, Messages.Town.Make);
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

        lootList = new ItemList(skin);
        lootList.setItems(playerService.getInventory().getLoot());
        
        lootPane = new ScrollPane(lootList.getList(), skin);
        lootPane.setHeight(display.getHeight() / 2);
        lootPane.setScrollingDisabled(true, false);
        lootPane.setScrollBarPositions(true, false);
        lootPane.setFadeScrollBars(false);
        lootPane.setScrollbarsOnTop(false);
        lootPane.addListener(new ScrollFocuser(lootPane));

        TextButton lootLabel = new TextButton("My Loot", skin, "tab");
        lootLabel.setUserObject(lootPane);
        lootSubmenu = new TabbedPane(new ButtonGroup<Button>(lootLabel), false);
        lootSubmenu.setWidth(250f);
        lootSubmenu.setHeight(display.getHeight());
        lootSubmenu.setPosition(-lootSubmenu.getWidth(), 0);

        lootPane.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent evt, int keycode) {
                if (Input.DOWN.match(keycode)) {
                    lootPane.fling(.4f, 0, -64f / .4f);
                }
                if (Input.UP.match(keycode)) {
                    lootPane.fling(.4f, 0, 64f / .4f);
                }
                return false;
            }
        });

        if (lootList.getItems().size == 0) {
            lootList.getList().center();
            Label l = new Label("Looks like you don't have any loot!  You should go exploring", ui.getSkin());
            l.setWrap(true);
            l.setAlignment(Align.center);
            lootList.getList().add(l).expandX().fillX();
        }
        
        display.addActor(lootSubmenu);

        craftGroup = new FocusGroup(lootPane, craftMenu);
        craftGroup.addListener(focusListener);
    }
    
    /**
     * create explore submenu layout
     */
    private void makeExplore() {
        fileBrowser = new FileBrowser(skin);
        fileBrowser.setWidth(getDisplayWidth() - 200);
        fileBrowser.setHeight(getDisplayHeight()-20);
        fileBrowser.setPosition(0, 0, Align.top);
        fileBrowser.init();
        display.addActor(fileBrowser);
        
        exploreGroup = new FocusGroup(fileBrowser, buttonList);
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
            final TextButton availableButton = new TextButton("Available", skin, "tab");
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
                    MessageDispatcher.getInstance().dispatchMessage(0, ui, ui, Messages.Interface.Selected, q);
                }
            });
        }

        {
            final TextButton acceptedButton = new TextButton("Accepted", skin, "tab");
            acceptedButton.setName("history");
            questTabs.add(acceptedButton);

            acceptedQuests.addListener(new ChangeListener() {

                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    Quest q = acceptedQuests.getSelected();
                    MessageDispatcher.getInstance().dispatchMessage(0, ui, ui, Messages.Interface.Selected, q);
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

                if (Input.DOWN.match(keycode)) {
                    l.setSelectedIndex(Math.min(l.getItems().size - 1, l.getSelectedIndex() + 1));
                }
                if (Input.UP.match(keycode)) {
                    l.setSelectedIndex(Math.max(0, l.getSelectedIndex() - 1));
                }
                
                if (Input.ACCEPT.match(keycode)){
                    MessageDispatcher.getInstance().dispatchMessage(null, Messages.Interface.Button, Messages.Town.AcceptQuest);
                }

                return false;
            }
        });

        display.addActor(questSubmenu);

        questGroup = new FocusGroup(questMenu);
        questGroup.addListener(focusListener);
    }

    /**
     * create data management submenu layout
     */
    private void makeSave() {
        final TownUI ui = this;

        SimpleWindow window = saveWindow = new SimpleWindow(skin, "square");
        window.setSize(600, 300);
        saveSlots = new Array<Table>();
        Table table = new Table();

        formFocus = new FocusGroup();

        formFocus.addListener(new ChangeListener() {

            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (ui.stateMachine.isInState(TownState.Save)) {
                    ui.getPointer().setPosition(formFocus.getFocused(), Align.left);
                    ui.getPointer().setVisible(true);
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
                        MessageDispatcher.getInstance().dispatchMessage(0f, ui, ui, Messages.Interface.Selected,
                                formFocus.getFocusedIndex());
                        audio.playSfx(DataDirs.Sounds.accept);
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
                if (Input.DOWN.match(keycode)) {
                    formFocus.next(true);
                }
                if (Input.UP.match(keycode)) {
                    formFocus.prev(true);
                }
                if (Input.ACCEPT.match(keycode)) {
                    MessageDispatcher.getInstance().dispatchMessage(0f, ui, ui, Messages.Interface.Selected,
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
        downloadWindow = new SimpleWindow(skin, "round");
        downloadWindow.setSize(600, 200);
        

        Label label = new Label("Downloading Daily Map", skin, "prompt");
        label.setAlignment(Align.center);
        label.setWrap(true);
        label.setSize(550, 100);
        label.setPosition(300, 140, Align.center);
        label.setName("downloadLabel");

        Image spinner = new Image(skin, playerService.getWorship());
        spinner.setSize(32f, 32f);
        spinner.setOrigin(spinner.getX(Align.center), spinner.getY(Align.center));
        spinner.addAction(Actions.forever(Actions.rotateBy(-360f, 2f)));

        spinner.setAlign(Align.center);
        spinner.setPosition(downloadWindow.getX(Align.center), 50f, Align.center);

        downloadWindow.setPosition(getDisplayCenterX(), getDisplayHeight() + 48f, Align.bottom);
        downloadWindow.addActor(label);
        downloadWindow.addActor(spinner);

        display.addActor(downloadWindow);
    }

    private void makeTraining() {

        sacrificeMenu = new SacrificeSubmenu(skin, playerService, this);
        trainingMenu = new TrainMenu(skin, sacrificeMenu);
        trainingMenu.getGroup().setPosition(getDisplayCenterX(), getDisplayCenterY(), Align.center);
        display.addActor(trainingMenu.getGroup());
        
        trainingFocus = new FocusGroup(trainingMenu.getGroup());
    }
    
    private void makePagefile() {
        pageFile = new PageFileWindow(skin, playerService, ServiceManager.getService(PageFile.class));
        pageFile.getWindow().setPosition(getDisplayCenterX(), getDisplayHeight(), Align.bottom);
        display.addActor(pageFile.getWindow());
        
        pageFileFocus = new FocusGroup(pageFile.getWindow());
        pageFileFocus.setFocus(pageFile.getWindow());
    }
    
    @Override
    public void extend() {
        makeMain();
        makeHome();
        makeTown();
        
        //draw you
        {
            character = new Image(skin.getRegion(playerService.getGender()));
            character.setSize(96f, 96f);
            character.setPosition(display.getWidth() / 2 - character.getWidth() / 2, 18f);
            display.addActor(character);
        }
        
        makeCraft();
        makeExplore();
        makeQuest();
        makeTraining();
        makePagefile();
        makeSave();
        makeDownload();

        Image goddess = new Image(skin.getRegion(playerService.getWorship()));
        goddess.setSize(128f, 128f);
        goddess.setPosition(620, 75, Align.center);
        goddess.setScaling(Scaling.stretch);
        goddessDialog = new SimpleWindow(skin, "round");
        goddessDialog.setSize(700, 150);
        goddessDialog.setOrigin(Align.center);
        goddessDialog.setPosition(display.getWidth()/2f, display.getHeight() / 2f, Align.center);
        gMsg = new Label("", skin, "small");
        gMsg.setWrap(true);
        gMsg.setWidth(500);
        gMsg.setPosition(20, 75, Align.left);
        goddessDialog.addActor(gMsg);

        goddessDialog.addActor(goddess);
        display.addActor(goddessDialog);

        goddessDialog.addAction(Actions.alpha(0f));
        goddessDialog.setVisible(false);

        setMessage("What're we doing next?");
        
        miniGoddess = new Image(skin.getRegion(playerService.getWorship()));
        miniGoddess.setSize(32f, 32f);
        miniGoddess.setScaling(Scaling.stretch);
        miniGoddess.setPosition(messageWindow.getWidth() - 16f, messageWindow.getHeight() / 2f, Align.right);
        miniGoddess.setOrigin(Align.center);
        miniGoddess.addAction(
            Actions.forever(
                Actions.sequence(
                    Actions.moveBy(0, 3f, .5f), 
                    Actions.moveBy(0, -6f, 1f), 
                    Actions.moveBy(0, 3f, .5f),
                    Actions.run(new Runnable(){

                        @Override
                        public void run() {
                            boolean flip = MathUtils.randomBoolean(.1f);
                            if (flip) {
                                miniGoddess.addAction(Actions.rotateBy(-360f, 1f));
                            }
                        }
                        
                    })
                )
            )
        );
        messageWindow.addActor(miniGoddess);
        
        MessageDispatcher.getInstance().addListener(this, Quest.Actions.Expired);
        
        defaultFocus = new FocusGroup(buttonList);
        defaultFocus.addListener(focusListener);
        
        fader = new Image(skin, "wfill");
        fader.setColor(1, 1, 1, 0);
        fader.setSize(getWidth(), getHeight());
        fader.setTouchable(Touchable.disabled);
        addActor(fader);
    }

    @Override
    protected void extendAct(float delta) {
        stateMachine.update();
    }

    @Override
    public boolean handleMessage(Telegram telegram) {
        // handle expiration of quests notification
        if (telegram.message == Quest.Actions.Expired) {
            this.pushNotification("A quest has expired");
            return true;
        }
        if (telegram.message == Messages.Player.NewItem) {
            ItemMsg msg = (ItemMsg)telegram.extraInfo;
            lootList.updateLabel(msg.item, msg.amount);
            sacrificeMenu.updateLabel(msg.item, msg.amount);
            return true;
        }
        if (telegram.message == Messages.Player.UpdateItem || 
            telegram.message == Messages.Player.RemoveItem ) {
            ItemMsg msg = (ItemMsg)telegram.extraInfo;
            lootList.updateLabel(msg.item, msg.amount);
            sacrificeMenu.updateLabel(msg.item, msg.amount);
            return true;
        }
        if (telegram.message == Messages.Player.Stats){
            pageFile.statusPane.updateStats(Stats.Map.get(playerService.getPlayer()));
        }
        if (telegram.message == Messages.PageFile.Changed){
            if (telegram.extraInfo instanceof PageFile.NumberValues){
                pageFile.statusPane.updateScore((PageFile.NumberValues)telegram.extraInfo);
            }
            if (telegram.extraInfo instanceof PageFile.StringValues){
                pageFile.statusPane.updateScore((PageFile.StringValues)telegram.extraInfo);
            }
        }
        return super.handleMessage(telegram);
    }

    /**
     * restores the original positions of all the images
     */
    void restore() {
        main.clearActions();
        home.clearActions();
        town.clearActions();
        character.clearActions();
        lootSubmenu.clearActions();
        craftSubmenu.clearActions();
        fileBrowser.clearActions();
        questDetails.clearActions();
        questSubmenu.clearActions();
        downloadWindow.clearActions();
        pageFile.getWindow().clearActions();

        saveWindow.clearActions();

        main.addAction(Actions.moveTo(0, 0, .8f));
        home.addAction(Actions.moveToAligned(0, 0, Align.bottomRight, .6f));
        town.addAction(Actions.moveToAligned(getDisplayWidth(), 0, Align.bottomLeft, .6f));
        character.addAction(Actions.moveTo(getDisplayCenterX() - character.getWidth() / 2, 18f, .8f));
        lootSubmenu.addAction(Actions.moveTo(-lootSubmenu.getWidth(), 0, .3f));
        craftSubmenu.addAction(Actions.moveTo(getDisplayWidth(), 0, .3f));
        fileBrowser.addAction(Actions.moveTo(0, -fileBrowser.getHeight(), .3f));
        saveWindow.addAction(Actions.moveToAligned(getDisplayCenterX(), getDisplayHeight(),Align.bottom,.2f, Interpolation.circleOut));
        downloadWindow.addAction(Actions.moveToAligned(getDisplayCenterX(), getDisplayHeight() + 100f, Align.bottom, .2f, Interpolation.circleOut));
        pageFile.getWindow().addAction(Actions.moveToAligned(getDisplayCenterX(), getDisplayHeight(), Align.bottom, .3f));
        setMessage("What're we doing next?");
        
        
        enableMenuInput();
        refreshButtons();

        pointer.setVisible(false);
    }

    @Override
    public String[] defineButtons() {
        return ((UIState) stateMachine.getCurrentState()).defineButtons();
    }

    @Override
    protected FocusGroup focusList() {
        if (stateMachine.isInState(TownState.Craft)) {
            return craftGroup;
        }
        else if (stateMachine.isInState(TownState.Explore)) {
            return exploreGroup;
        }
        else if (stateMachine.isInState(TownState.Save)) {
            return formFocus;
        }
        else if (stateMachine.isInState(TownState.QuestMenu)) {
            return questGroup;
        }
        else if (stateMachine.isInState(TownState.PageFile)) {
            return pageFileFocus;
        }
        else if (stateMachine.isInState(TownState.Train)){
            return trainingFocus;
        }
        return defaultFocus;
    }
}
