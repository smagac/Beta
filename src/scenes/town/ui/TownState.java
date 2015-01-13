package scenes.town.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Scanner;

import github.nhydock.ssm.SceneManager;
import scene2d.InputDisabler;
import scene2d.ui.extras.TabbedPane;
import scenes.GameUI;
import scenes.Messages;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.Scaling;

import core.DataDirs;
import core.common.Tracker;
import core.components.Stats;
import core.datatypes.Craftable;
import core.datatypes.Inventory;
import core.datatypes.Item;
import core.datatypes.QuestTracker.Reward;
import core.datatypes.dungeon.DungeonParams;
import core.datatypes.quests.Quest;
import core.datatypes.FileType;
import core.service.interfaces.IPlayerContainer.SaveSummary;

/**
 * Handles state based ui menu logic and switching
 * 
 * @author nhydock
 *
 */
enum TownState implements UIState {
    Main() {

        @Override
        public void enter(TownUI ui) {
            ui.restore();
            ui.refreshButtons();
            ui.resetFocus();
        }

        @Override
        public String[] defineButtons() {
            return new String[] { "Sleep", "Explore", "Craft", "Quest", "Save" };
        }

        /**
         * Use on message to switch between menus based on button index
         */
        @Override
        public boolean onMessage(TownUI ui, Telegram t) {
            if (t.message == Messages.Interface.Button) {
                int button = (int)t.extraInfo;
                
                switch (button) {
                    case Messages.Town.Sleep:
                        ui.changeState(Sleep); break;
                    case Messages.Town.Explore:
                        ui.changeState(Explore); break;
                    case Messages.Town.Craft:
                        ui.changeState(Craft); break;
                    case Messages.Town.Quest:
                        ui.changeState(QuestMenu); break;
                    case Messages.Town.Save:
                        ui.changeState(Save); break;
                    default:
                        return false;
                }
                return true;
            }

            return false;
        }
    },
    Craft() {

        private void populateLoot(TownUI ui) {
            ui.lootList.clear();
            ui.lootList.top().left();

            ObjectMap<Item, Integer> loot = ui.playerService.getInventory().getLoot();
            if (loot.keys().hasNext) {
                ui.lootList.setWidth(ui.lootPane.getWidth());
                ui.lootList.pad(10f);
                for (Item item : loot.keys()) {
                    Label l = new Label(item.toString(), ui.getSkin(), "smaller");
                    l.setAlignment(Align.left);
                    ui.lootList.add(l).expandX().fillX();
                    Label i = new Label("" + loot.get(item), ui.getSkin(), "smaller");
                    i.setAlignment(Align.right);
                    ui.lootList.add(i).width(30f);
                    ui.lootList.row();

                }
                ui.lootList.setTouchable(Touchable.disabled);
            }
            else {
                ui.lootList.center();
                Label l = new Label("Looks like you don't have any loot!  You should go exploring", ui.getSkin());
                l.setWrap(true);
                l.setAlignment(Align.center);
                ui.lootList.add(l).expandX().fillX();
            }
            ui.lootList.pack();

            int index = 0;
            if (ui.craftMenu.getOpenTabIndex() == 0) {
                index = ui.craftList.getSelectedIndex();
            }
            else {
                index = ui.todayList.getSelectedIndex();
            }

            ui.todayList.setItems(ui.playerService.getInventory().getTodaysCrafts());
            ui.craftList.setItems(ui.playerService.getInventory().getRequiredCrafts());

            if (ui.craftMenu.getOpenTabIndex() == 0) {
                ui.craftList.setSelectedIndex(index);
            }
            else {
                ui.todayList.setSelectedIndex(index);
            }

        }

        private void refreshRequirements(Craftable c, TownUI ui) {
            if (c == null) {
                throw new NullPointerException("Craftable object can not be null");
            }

            // build requirements list
            ui.requirementList.clear();

            ObjectMap<String, Integer> items = c.getRequirements();
            for (String name : items.keys()) {
                Label l = new Label(name, ui.getSkin(), "smallest");
                l.setAlignment(Align.left);
                ui.requirementList.add(l).expandX().fillX();
                Label i = new Label(ui.playerService.getInventory().genericCount(name) + "/" + items.get(name),
                        ui.getSkin(), "smallest");
                i.setAlignment(Align.right);
                ui.requirementList.add(i).width(30f);
                ui.requirementList.row();
            }
            ui.requirementList.pack();
        }

        @Override
        public void enter(TownUI ui) {
            // populate the submenu's data
            ui.craftMenu.showTab(0);
            // create loot menu
            populateLoot(ui);

            ui.sleepImg.addAction(Actions.moveTo(-ui.sleepImg.getWidth(), 0, .8f));
            ui.exploreImg.addAction(Actions.moveTo(-ui.exploreImg.getWidth(), 118f, .8f));
            ui.craftImg.addAction(Actions.sequence(
                    Actions.moveTo(ui.getDisplayWidth() - ui.craftImg.getWidth(),
                            ui.getDisplayHeight() - ui.craftImg.getHeight(), .3f),
                    Actions.moveTo(ui.getDisplayWidth() / 2 - ui.craftImg.getWidth() / 2, 118f, .5f)));

            ui.craftSubmenu
                    .addAction(Actions.sequence(Actions.moveTo(ui.getDisplayWidth(), 0), Actions.delay(.8f), Actions
                            .moveTo(ui.getDisplayWidth() - ui.craftSubmenu.getWidth(), 0, .3f, Interpolation.circleOut)));

            ui.lootSubmenu.addAction(Actions.sequence(Actions.moveTo(-ui.lootSubmenu.getWidth(), 0),
                    Actions.delay(.8f), Actions.moveTo(0, 0, .3f, Interpolation.circleOut)));

            ui.setMessage("Tink Tink");
            ui.refreshButtons();
            ui.resetFocus();
        }

        @Override
        public String[] defineButtons() {
            return new String[] { "Return", "Make Item" };
        }

        @Override
        public boolean onMessage(TownUI ui, Telegram t) {
            if (t.message == Messages.Interface.Button && (int)t.extraInfo == Messages.Town.Make) {
                Craftable c;
                if (ui.craftMenu.getOpenTabIndex() == 0) {
                    c = ui.craftList.getSelected();
                }
                else {
                    c = ui.todayList.getSelected();
                }

                if (c != null) {
                    int count = Tracker.NumberValues.Items_Crafted.value();
                    boolean made = ui.playerService.getInventory().makeItem(c);
                    ui.setMessage((made) ? "Crafted an item!" : "Not enough materials");
                    populateLoot(ui);

                    if (made) {
                        ui.playerService.getInventory().refreshRequirements();
                        refreshRequirements(c, ui);

                        if (ui.craftMenu.getOpenTabIndex() == 0) {
                            c = ui.craftList.getSelected();
                        }
                        else {
                            c = ui.todayList.getSelected();
                        }

                    }

                    if (ui.playerService.getInventory().getProgressPercentage() >= 1.0f) {
                        ui.changeState(Over);
                    }
                    // show helpful tip after first item has been made!
                    else if (count == 0 && made) {
                        Array<FileHandle> files = DataDirs.getChildrenHandles(Gdx.files.classpath(DataDirs.GameData + "hints/"));
                        FileHandle file = files.random();
                        String[] hint = file.readString().split("\n");
                        
                        // modify telegram to include text
                        Object info = t.extraInfo;
                        t.extraInfo = hint;
                        GoddessDialog.onMessage(ui, t);
                        t.extraInfo = info;

                        ui.changeState(GoddessDialog);
                    }
                    return true;
                }
            }
            else if (t.message == Messages.Interface.Selected) {
                Craftable c = (Craftable) t.extraInfo;
                refreshRequirements(c, ui);
            }
            else if (t.message == Messages.Interface.Close || 
                     (t.message == Messages.Interface.Button && (int)t.extraInfo == Messages.Town.Close)) {
                
                ui.changeState(Main);
            }
            return false;
        }
    },
    Explore() {

        @Override
        public void enter(TownUI ui) {
            ui.sleepImg.addAction(Actions.moveTo(-ui.sleepImg.getWidth(), 0, .8f));
            ui.craftImg.addAction(Actions.moveTo(ui.getDisplayWidth(), 0, .8f));

            ui.exploreSubmenu.addAction(Actions.sequence(Actions.moveTo(-ui.exploreSubmenu.getWidth(), 0),
                    Actions.delay(.8f), Actions.moveTo(0, 0, .3f, Interpolation.circleOut)));

            ui.setMessage("Where to?");
            ui.refreshButtons();
            ui.resetFocus();
        }

        @Override
        public String[] defineButtons() {
            return new String[] { "Return", "Explore Dungeon", "Random Dungeon", "Daily Dungeon" };
        }

        @Override
        public boolean onMessage(final TownUI ui, final Telegram t) {
            /**
             * Updates the information in the right side file panel to reflect
             * the metadata of the specified file
             */
            if (t.message == Messages.Interface.Selected) {
                final FileHandle selected = (FileHandle) t.extraInfo;

                ui.fileDetails.addAction(Actions.sequence(Actions.moveTo(ui.getDisplayWidth(), 0, .3f),
                        Actions.run(new Runnable() {

                            @Override
                            public void run() {
                                // generate a details panel
                                Table contents = ui.fileDetailsContent;
                                contents.clear();

                                FileType ext = FileType.getType(selected.extension());
                                Image icon = new Image(ui.getSkin().getRegion(ext.toString()));
                                Label type = new Label("File Type: " + ext, ui.getSkin());
                                Label size = new Label("File Size: " + (selected.length() / 1000f) + " kb", ui
                                        .getSkin());
                                Label diff = new Label("Difficulty: "
                                        + new String(new char[ext.difficulty(selected.length())]).replace('\0', '*'),
                                        ui.getSkin());

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
                                ui.fileDetailsPane.pack();
                            }
                        }), Actions.moveTo(ui.getDisplayWidth() - ui.fileDetails.getWidth(), 0, .3f)));
                return true;
            }
            else if (t.message == Messages.Interface.Close) {
                ui.changeState(Main);
                return true;
            }
            else if (t.message != Messages.Interface.Button) {
                return false;
            }
            
            int button = (int)t.extraInfo;
            
            if (button == Messages.Town.Explore || button == Messages.Town.Random) {
                if (ui.playerService.getPlayer().getComponent(Stats.class).hp <= 0) {
                    ui.setMessage("You need to rest first!");
                }
                else {
                    DungeonParams params;
                    FileHandle f = null;
                    // load selected file dungeon
                    if (button == Messages.Town.Explore) {
                        if (ui.exploreTabs.getChecked().getName().equals("history")) {
                            f = TownUI.history.get(ui.fileList.getSelectedIndex());
                            if (f != null && !f.isDirectory()) {
                                params = DungeonParams.loadDataFromFile(f);
                            } else {
                                return false;
                            }
                        }
                        else {
                            f = ui.directoryList.get(ui.fileList.getSelectedIndex());
                            if (f != null && !f.isDirectory()) {
                                params = DungeonParams.loadDataFromFile(f);
                                TownUI.history.add(f);
                                TownUI.historyPaths.add(f.name());
                            }
                            else {
                                return false;
                            }
                        }
                    }
                    // random dungeons
                    else {
                        params = DungeonParams.loadRandomDungeon();
                        TownUI.directory = null;
                    }
                    scenes.dungeon.Scene dungeon = (scenes.dungeon.Scene) SceneManager.switchToScene("dungeon");
                    dungeon.setDungeon(params, f);
                    
                    return true;
                }
            }
            else if (button == Messages.Town.Close) {
                ui.changeState(Main);
                return true;
            }
            else if (button == Messages.Town.DailyDungeon) {
                ui.changeState(NetworkLoad);
                return true;
            }

            return false;
        }
    },
    Sleep() {

        @Override
        public void enter(final TownUI ui) {
            ui.setMessage("Good night!");
            ui.sleepImg.addAction(Actions.sequence(
                    Actions.moveTo(32f, ui.getDisplayHeight() / 2 - ui.sleepImg.getHeight() / 2, .3f),
                    Actions.moveTo(ui.getDisplayWidth() / 2 - ui.sleepImg.getWidth() / 2, ui.getDisplayHeight() / 2
                            - ui.sleepImg.getHeight() / 2, .5f)));
            ui.exploreImg.addAction(Actions.moveTo(ui.getDisplayWidth(), 118f, .8f));
            ui.craftImg.addAction(Actions.moveTo(ui.getDisplayWidth(), 0f, .8f));

            ui.getDisplay().addAction(
                    Actions.sequence(Actions.delay(1f), Actions.alpha(0f, .5f), Actions.delay(.4f),
                            Actions.run(new Runnable() {

                                @Override
                                public void run() {
                                    ui.playerService.rest();
                                    ui.todayList.setItems(ui.playerService.getInventory().getTodaysCrafts());

                                    MessageDispatcher.getInstance().dispatchMessage(0, null,
                                            ui.playerService.getQuestTracker(), Quest.Actions.Advance);
                                }

                            }), Actions.alpha(1f, .5f), Actions.delay(.3f), Actions.run(new Runnable() {

                                @Override
                                public void run() {
                                    ui.changeState(TownState.Main);
                                }

                            })));
            ui.refreshButtons();
            ui.setFocus(null);
        }

        @Override
        public String[] defineButtons() {
            return null;
        }

        @Override
        public boolean onMessage(TownUI ui, Telegram telegram) {
            return false;
        }

        @Override
        public void exit(TownUI ui) {
        }
    },
    GoddessDialog() {

        public Iterator<String> dialog;

        @Override
        public void enter(TownUI ui) {
            ui.goddess.clearActions();
            ui.goddess.addAction(Actions.moveTo(ui.getDisplayWidth() - 128f, ui.getDisplayHeight() / 2 - 64f, .3f));

            ui.goddessDialog.clearActions();
            ui.goddessDialog.addAction(Actions.alpha(1f, .2f));
            ui.goddessDialog.setVisible(true);
            ui.restore();

            ui.refreshButtons();
            ui.resetFocus();
        }

        @Override
        public void exit(final TownUI ui) {
            ui.goddess.clearActions();
            ui.goddessDialog.clearActions();
            ui.goddess.addAction(Actions.moveTo(ui.getDisplayWidth(), ui.getDisplayHeight() / 2 - 64f, .3f));
            ui.goddessDialog.addAction(Actions.sequence(Actions.alpha(0f, .2f), Actions.run(new Runnable() {
                @Override
                public void run() {
                    ui.goddessDialog.setVisible(false);
                }
            })));
        }

        @Override
        public void update(TownUI ui) {
            if (dialog.hasNext()) {
                ui.gMsg.setText(dialog.next());
            }
            else {
                dialog = null;
                ui.changeState(Main);
            }
        }

        @Override
        public String[] defineButtons() {
            if (dialog.hasNext()) {
                return new String[] { "Continue" };
            }
            else {
                return new String[] { "Good-bye" };
            }
        }

        @Override
        public boolean onMessage(TownUI ui, Telegram t) {
            if (t.extraInfo instanceof String[]) {
                dialog = new Array<String>((String[]) t.extraInfo).iterator();
                ui.gMsg.setText(dialog.next());
                return true;
            }
            return false;
        }
    },
    Over() {

        @Override
        public String[] defineButtons() {
            return null;
        }

        @Override
        public void enter(TownUI ui) {
            InputDisabler.swap();
            InputDisabler.clear();

            ui.restore();
            ui.getRoot().clearListeners();
            ui.getRoot().addAction(
                    Actions.sequence(
                            Actions.delay(3f),
                            Actions.forever(Actions.sequence(Actions.moveTo(5, 0, .1f, Interpolation.bounce),
                                    Actions.moveTo(-5, 0, .1f, Interpolation.bounce)))));
            ui.getFader().addAction(
                    Actions.sequence(Actions.delay(3f), Actions.alpha(1f, 5f), Actions.run(new Runnable() {

                        @Override
                        public void run() {
                            SceneManager.switchToScene("endgame");
                        }
                    })));
        }

        @Override
        public boolean onMessage(TownUI ui, Telegram telegram) {
            return false;
        }
    },
    Save() {

        @Override
        public String[] defineButtons() {
            return new String[] { "Cancel" };
        }

        @Override
        public void enter(final TownUI ui) {

            for (int i = 1; i <= ui.playerService.slots(); i++) {
                Table row = ui.saveSlots.get(i - 1);

                row.clearChildren();
                SaveSummary s = ui.playerService.summary(i);
                if (s == null) {
                    row.add(new Label("No Data", ui.getSkin(), "prompt")).expandX().center();
                }
                else {
                    Image icon = new Image(ui.getSkin(), s.gender);
                    row.add(icon).expand().center().colspan(1).size(32f, 32f);

                    row.add(new Label(s.date, ui.getSkin(), "prompt")).expand().colspan(1).center();

                    Table info = new Table();
                    info.add(new Label("Crafting Completed: " + s.progress, ui.getSkin(), "smaller")).expand()
                            .colspan(1).right().row();
                    info.add(new Label("Time: " + s.time, ui.getSkin(), "smaller")).expand().colspan(1).right().row();
                    info.add(
                            new Label(new String(new char[s.diff]).replace('\0', '*') + " difficulty", ui.getSkin(),
                                    "smaller")).expand().colspan(1).right();

                    row.add(info).colspan(1).expand().right();
                }
            }

            ui.saveWindow.addAction(Actions.sequence(
                    Actions.moveTo(ui.getDisplayWidth() / 2 - ui.saveWindow.getWidth() / 2, ui.getDisplayHeight() / 2
                            - ui.saveWindow.getHeight() / 2, .3f, Interpolation.circleOut), Actions.run(new Runnable() {

                        @Override
                        public void run() {
                            ui.setFocus(ui.saveWindow);
                            ui.formFocus.setFocus(ui.formFocus.getActors().first());
                            ui.showPointer(ui.saveSlots.first(), Align.left, Align.center);
                        }

                    })));
            ui.refreshButtons();
            ui.resetFocus();
        }

        @Override
        public void exit(TownUI ui) {
        }

        @Override
        public boolean onMessage(TownUI ui, Telegram telegram) {
            if (telegram.message == Messages.Interface.Selected) {
                ui.playerService.save((Integer) telegram.extraInfo + 1);
                ui.changeState(Main);
            }
            else {
                ui.audio.playSfx(DataDirs.Sounds.tick);
                ui.changeState(Main);
            }
            return true;
        }
    },
    // State used for waiting on the connection to be made with the server
    // to pull down a dungeon hash
    NetworkLoad() {
        private final String DAILY_DUNGEON_SERVER = "http://storymode.nhydock.me/daily";
        Net.HttpRequest connection;
        String dungeonData;
        
        @Override
        public String[] defineButtons() {
            return new String[] { "Cancel Download" };
        }

        @Override
        public void enter(final TownUI entity) {
            entity.downloadWindow.addAction(
                   Actions.moveTo(entity.getDisplayWidth() / 2 - entity.downloadWindow.getWidth()/2f, 
                                  entity.getDisplayHeight() / 2f - entity.downloadWindow.getHeight()/2f,
                                  .3f, Interpolation.circleOut)
            );
            
            connection = new Net.HttpRequest(Net.HttpMethods.GET);
            connection.setUrl(DAILY_DUNGEON_SERVER);
            connection.setTimeOut(5000);
            Gdx.net.sendHttpRequest(connection, new Net.HttpResponseListener() {
                
                @Override
                public void handleHttpResponse(HttpResponse httpResponse) {
                   try (InputStream content = httpResponse.getResultAsStream();
                        Scanner scanner = new Scanner(content))
                   {
                       String output = "";
                       while (scanner.hasNextLine()) {
                           output += scanner.nextLine();
                       }
                       System.out.println(output);
                       dungeonData = output;
       
                       scenes.dungeon.Scene dungeon = (scenes.dungeon.Scene) SceneManager.switchToScene("dungeon");
                       DungeonParams params = DungeonParams.loadFromSimpleData(dungeonData);
                       dungeon.setDungeon(params, null);
                   }
                   catch (IOException e) {
                       e.printStackTrace();
                       entity.changeState(Explore);
                   }  
                }
                
                @Override
                public void failed(Throwable t) {
                    entity.changeState(Explore);
                }
                
                @Override
                public void cancelled() {
                    entity.changeState(Explore);
                }
            });
            entity.resetFocus();
        }
        
        @Override
        public void exit(TownUI entity) {
            entity.downloadWindow.addAction(Actions.moveTo(entity.getDisplayCenterX() - entity.downloadWindow.getWidth() / 2,
                    entity.getDisplayHeight(), .2f, Interpolation.circleOut));
            
        }

        @Override
        public boolean onMessage(TownUI entity, Telegram telegram) {
            if (telegram.message == Messages.Town.CancelDownload) {
                if (connection != null) {
                    Gdx.net.cancelHttpRequest(connection);
                }
            }
            return false;
        }

    },
    QuestMenu() {

        boolean completeView = false;

        @Override
        public String[] defineButtons() {
            if (completeView) {
                return new String[] { "Return", "Complete Quest" };
            }
            else {
                return new String[] { "Return", "Accept Quest" };
            }
        }

        @Override
        public void enter(TownUI ui) {
            ui.acceptedQuests.setItems(ui.playerService.getQuestTracker().getAcceptedQuests());
            ui.availableQuests.setItems(ui.playerService.getQuestTracker().getQuests());

            ui.sleepImg.addAction(Actions.moveTo(-ui.sleepImg.getWidth(), 0, .8f));
            ui.craftImg.addAction(Actions.moveTo(ui.getDisplayWidth(), 0, .8f));

            ui.questSubmenu.addAction(Actions.sequence(Actions.moveTo(-ui.questSubmenu.getWidth(), 0),
                    Actions.delay(.8f), Actions.moveTo(0, 0, .3f, Interpolation.circleOut)));

            fillDetails(ui, (ui.questMenu.getOpenTabIndex() == 0) ? ui.availableQuests.getSelected()
                    : ui.acceptedQuests.getSelected());

            ui.questDetails
                    .addAction(Actions.sequence(Actions.moveTo(ui.getDisplayWidth(), 0), Actions.delay(.8f), Actions
                            .moveTo(ui.getDisplayWidth() - ui.questDetails.getWidth(), 0, .3f, Interpolation.circleOut)));
            ui.setMessage("Let's help people!");
            ui.refreshButtons();
            ui.setFocus(ui.getButtonList());
        }

        private void fillDetails(final TownUI ui, Quest selected) {

            // generate a details panel
            Table contents = ui.questDetailsContent;
            contents.clear();

            // don't populate if null
            if (selected == null) {
                return;
            }

            // Image icon = new Image(ui.getSkin().getRegion(ext.toString()));
            Label loc = new Label("Location: " + selected.getLocation(), ui.getSkin(), "smaller");
            Label prompt = new Label(selected.getPrompt(), ui.getSkin(), "smaller");
            prompt.setWrap(true);

            Label objective;
            if (ui.playerService.getQuestTracker().getAcceptedQuests().contains(selected, true)) {
                objective = new Label(selected.getObjectiveProgress(), ui.getSkin(), "smaller");
            }
            else {
                objective = new Label(selected.getObjectivePrompt(), ui.getSkin(), "smaller");
            }
            objective.setWrap(true);

            int d = selected.getExpirationDate();
            String dayLabel = ((d == 1) ? "1 day" : d + " days") + " left to complete";

            Label days = new Label(dayLabel, ui.getSkin(), "smaller");
            days.setWrap(true);
            contents.pad(10f);

            // icon.setAlign(Align.center);
            // icon.setSize(96f, 96f);
            // icon.setScaling(Scaling.fit);
            // contents.add(icon).size(96f, 96f).expandX();
            contents.top();
            contents.row();
            contents.add(loc).expandX().fillX().padBottom(10f);
            contents.row();
            contents.add(prompt).expandX().fillX();
            contents.row();
            contents.add(objective).expandX().fillX().padTop(20f);
            contents.row();
            contents.add(days).expandX().fillX().padTop(10f);

            contents.pack();
            ui.questDetailsPane.pack();
        }

        @Override
        public boolean onMessage(final TownUI ui, Telegram telegram) {
            // change which quest is selected
            // update the quest details pane on the side
            if (telegram.message == Messages.Interface.Selected) {
                final Quest selected = (Quest) telegram.extraInfo;

                ui.questDetails
                        .addAction(Actions.sequence(
                                Actions.moveTo(ui.getDisplayWidth(), 0, .3f, Interpolation.circleIn), Actions
                                        .run(new Runnable() {

                                            @Override
                                            public void run() {
                                                fillDetails(ui, selected);
                                            }
                                        }), Actions.moveTo(ui.getDisplayWidth() - ui.questDetails.getWidth(), 0, .3f,
                                        Interpolation.circleOut)));
                return true;

            }
            // accept a new quest
            else if (telegram.message == Messages.Interface.Button && (int)telegram.extraInfo == Messages.Town.Accept) {
                Quest selected;
                if (!completeView) {
                    selected = ui.availableQuests.getSelected();
                    ui.playerService.getQuestTracker().accept(selected);
                    ui.acceptedQuests.setItems(ui.playerService.getQuestTracker().getAcceptedQuests());
                }
                // don't try to accept quests that have already been accepted
                else {
                    selected = ui.acceptedQuests.getSelected();
                    boolean completed = ui.playerService.getQuestTracker().complete(selected);
                    if (!completed) {
                        ui.setMessage("You can't complete that quest yet");
                    }
                    else {
                        // reward the player for completing the quest
                        Inventory inv = ui.playerService.getInventory();
                        Craftable craftable = inv.getRequiredCrafts().random();

                        Reward reward = ui.playerService.getQuestTracker().getReward(craftable);

                        inv.pickup(reward.item, reward.count);

                        ui.changeState(GoddessDialog);
                    }
                    ui.acceptedQuests.setItems(ui.playerService.getQuestTracker().getAcceptedQuests());
                    return false;
                }

                return true;
            }
            else if (telegram.message == TabbedPane.Messages.ChangeTabs) {
                completeView = ui.questMenu.getOpenTabIndex() == 1;
                ui.questDetails.addAction(Actions.sequence(
                        Actions.moveTo(ui.getDisplayWidth(), 0, .3f, Interpolation.circleIn),
                        Actions.run(new Runnable() {

                            @Override
                            public void run() {
                                fillDetails(ui,
                                        (ui.questMenu.getOpenTabIndex() == 0) ? ui.availableQuests.getSelected()
                                                : ui.acceptedQuests.getSelected());
                            }
                        }), Actions.moveTo(ui.getDisplayWidth() - ui.questDetails.getWidth(), 0, .3f,
                                Interpolation.circleOut)));
                ui.refreshButtons();
                return true;
            }
            else {
                ui.changeState(Main);
                return true;
            }
        }

    };

    @Override
    public void exit(TownUI ui) {
    }

    @Override
    public void update(TownUI ui) {
    }
}