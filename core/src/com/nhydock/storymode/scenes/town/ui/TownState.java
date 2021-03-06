package com.nhydock.storymode.scenes.town.ui;

import github.nhydock.ssm.SceneManager;
import github.nhydock.ssm.ServiceManager;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Scanner;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.Net.HttpResponse;
import com.badlogic.gdx.ai.fsm.State;
import com.badlogic.gdx.ai.msg.MessageManager;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectIntMap;
import com.nhydock.gdx.scenes.scene2d.runnables.ChangeState;
import com.nhydock.gdx.scenes.scene2d.runnables.PlayBGM;
import com.nhydock.gdx.scenes.scene2d.runnables.PlaySound;
import com.nhydock.scenes.scene2d.ui.extras.TabbedPane;
import com.nhydock.storymode.DataDirs;
import com.nhydock.storymode.components.Stats;
import com.nhydock.storymode.datatypes.Craftable;
import com.nhydock.storymode.datatypes.Inventory;
import com.nhydock.storymode.datatypes.Item;
import com.nhydock.storymode.datatypes.QuestTracker.Reward;
import com.nhydock.storymode.datatypes.dungeon.Dungeon;
import com.nhydock.storymode.datatypes.npc.Trainer;
import com.nhydock.storymode.datatypes.quests.Quest;
import com.nhydock.storymode.scenes.Messages;
import com.nhydock.storymode.service.implementations.PageFile;
import com.nhydock.storymode.service.implementations.PageFile.NumberValues;
import com.nhydock.storymode.service.interfaces.IPlayerContainer.SaveSummary;

/**
 * Handles state based ui menu logic and switching
 * 
 * @author nhydock
 *
 */
enum TownState implements UIState<TownUI> {
    Main("Home", "Explore", "Town") {
        
        @Override
        public void enter(TownUI ui) {
            ui.restore();
            ui.transition(.5f);
            ui.resetFocus();
        }

        /**
         * Use on message to switch between menus based on button index
         */
        @Override
        public boolean onMessage(TownUI ui, Telegram t) {
            if (t.message == Messages.Interface.Button) {
                int button = (int)t.extraInfo;
                
                switch (button) {
                    case Messages.Town.Home:
                        ui.changeState(Home); break;
                    case Messages.Town.Explore:
                        ui.changeState(Explore); break;
                    case Messages.Town.Town:
                        ui.changeState(Town); break;
                    default:
                        return false;
                }
                return true;
            }

            return false;
        }
    },
    Town("Quest", "Craft", "Train"){

        @Override
        public void enter(TownUI entity) {
            entity.getPointer().setVisible(false);
            State prev = entity.getStateMachine().getPreviousState();
            if (prev != QuestMenu && prev != Craft && prev != Train) {
                entity.main.addAction(Actions.moveToAligned(0, 0, Align.bottomRight, .75f));
                entity.town.addAction(Actions.moveToAligned(0, 0, Align.bottomLeft, .75f));
                entity.transition(.75f);
            } else {
                entity.transition(0f);
            }
            entity.resetFocus();
        }

        @Override
        public boolean onMessage(TownUI ui, Telegram t) {
            if (t.message == Messages.Interface.Button) {
                int button = (int)t.extraInfo;
                
                switch (button) {
                    case Messages.Town.Quest:
                        ui.changeState(QuestMenu); break;
                    case Messages.Town.Train:
                        ui.changeState(Train); break;
                    case Messages.Town.Craft:
                        ui.changeState(Craft); break;
                    default:
                        return false;
                }
                return true;
            }
            else if (t.message == Messages.Interface.Close){
                ui.changeState(Main);
                return true;
            }
            return false;
        }
        
    },    
    Home ("Save Game", "Sleep", "Access PageFile") {
    
        @Override
        public void enter(TownUI entity) {
            entity.getPointer().setVisible(false);
            State prev = entity.getStateMachine().getPreviousState();
            if (prev != PageFile && prev != Sleep && prev != Save) {
                entity.main.addAction(Actions.moveToAligned(entity.getDisplayWidth(), 0, Align.bottomLeft, .75f));
                entity.home.addAction(Actions.moveToAligned(0, 0, Align.bottomLeft, .75f));
                entity.transition(.5f);
            } else {
                entity.transition(.1f);
            }
            entity.resetFocus();
        }

        @Override
        public boolean onMessage(TownUI ui, Telegram t) {
            if (t.message == Messages.Interface.Button) {
                int button = (int)t.extraInfo;
                
                switch (button) {
                    case Messages.Town.Sleep:
                        ui.changeState(Sleep); break;
                    case Messages.Town.Save:
                        ui.changeState(Save); break;
                    case Messages.Town.PageFile:
                        ui.changeState(PageFile); break;
                    default:
                        return false;
                }
                return true;
            }
            if (t.message == Messages.Interface.Close) {
                ui.changeState(Main);
                return true;
            }

            return false;
        }
        
    }, 
    Craft("Make Item") {
        
        private void populateLoot(TownUI ui) {
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

            ObjectIntMap<String> items = c.getRequirements();
            for (String name : items.keys()) {
                Label l = new Label(name, ui.getSkin(), "smallest");
                l.setAlignment(Align.left);
                ui.requirementList.add(l).expandX().fillX();
                String amount = ui.playerService.getInventory().genericCount(name) + "/" + items.get(name, 1);
                Label i = new Label(amount, ui.getSkin(), "smallest");
                i.setAlignment(Align.right);
                ui.requirementList.add(i).width(30f);
                ui.requirementList.row();
            }
            ui.requirementList.pack();
        }

        @Override
        public void enter(TownUI ui) {
            // populate the submenu's data
            ui.craftMenu.showTab(0, true);
            // create loot menu
            populateLoot(ui);

            ui.craftSubmenu.addAction(
                Actions.sequence(
                    Actions.moveTo(ui.getDisplayWidth(), 0),
                    Actions.delay(.2f),
                    Actions.moveToAligned(ui.getDisplayWidth(), 0, Align.bottomRight, .3f, Interpolation.circleOut)
                )
            );

            ui.lootSubmenu.addAction(
                Actions.sequence(
                    Actions.moveToAligned(0, 0, Align.bottomRight),
                    Actions.delay(.2f),
                    Actions.moveTo(0, 0, .3f, Interpolation.circleOut)
                )
            );

            ui.setMessage("Tink Tink");
            ui.transition(.1f);
            ui.resetFocus();
        }
        
        @Override
        public void exit(TownUI ui){
            ui.lootSubmenu.addAction(Actions.moveToAligned(0, 0, Align.bottomRight, .3f));
            ui.craftSubmenu.addAction(Actions.moveToAligned(ui.getDisplayWidth(), 0, Align.bottomLeft, .3f));
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
                    int count = ServiceManager.getService(PageFile.class).get(NumberValues.Items_Crafted);
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
            else if (t.message == Messages.Interface.Close) {
                ui.changeState(Town);
            }
            return false;
        }
    },
    Explore("Random Dungeon", "Daily Dungeon") {

        @Override
        public void enter(TownUI ui) {
            ui.main.addAction(Actions.moveToAligned(ui.getDisplayWidth() - 100f, 0, Align.bottom, .8f));
            ui.character.addAction(Actions.moveTo(ui.getDisplayWidth() - 180f, ui.character.getY(), .8f));
            
            ui.fileBrowser.addAction(
                Actions.sequence(
                    Actions.run(ui.getScene().getInput().disableMe),
                    Actions.moveToAligned(0, 0, Align.topLeft),
                    Actions.delay(.8f), 
                    Actions.moveToAligned(0, 16, Align.bottomLeft, .3f, Interpolation.circleOut),
                    Actions.run(ui.getScene().getInput().enableMe)
                )
            );

            ui.setMessage("Where to?");
            ui.transition(.6f);
            ui.resetFocus();
        }

        @Override
        public boolean onMessage(final TownUI ui, final Telegram t) {
            /**
             * Updates the information in the right side file panel to reflect
             * the metadata of the specified file
             */
            if (t.message == Messages.Interface.Close) {
                ui.changeState(Main);
                return true;
            }
            
            
            int button = -1;
            if (t.extraInfo != null && t.message == Messages.Interface.Button){
                button = (int)t.extraInfo; 
            }
            
            if (t.message == Messages.Town.SelectDungeon || button == Messages.Town.Random) {
                if (ui.playerService.getPlayer().getComponent(Stats.class).hp <= 0) {
                    ui.setMessage("You need to rest first!");
                }
                else {
                    Dungeon.Parameters params;
                    FileHandle f = null;
                    // load selected file dungeon
                    if (t.message == Messages.Town.SelectDungeon) {
                        FileHandle file = ui.fileBrowser.getSelectedFile();
                        if (file != null) {
                            params = Dungeon.Parameters.loadDataFromFile(file);
                            ui.fileBrowser.addToHistory(file);
                        } else {
                            return false;
                        }
                    }
                    // random dungeons
                    else {
                        params = Dungeon.Parameters.loadRandomDungeon();
                    }
                    com.nhydock.storymode.scenes.dungeon.Scene dungeon = (com.nhydock.storymode.scenes.dungeon.Scene) SceneManager.switchToScene("dungeon");
                    dungeon.setDungeon(params, f);
                    
                    return true;
                }
            }
            else if (t.message == Messages.Interface.Close) {
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

            ui.getDisplay().addAction(
                Actions.sequence(
                    Actions.run(ui.getScene().getInput().disableMe),
                    ui.fadeOutAction(.4f),
                    Actions.delay(.4f),
                    Actions.run(new Runnable() {

                        @Override
                        public void run() {
                            ui.playerService.rest();
                            ui.todayList.setItems(ui.playerService.getInventory().getTodaysCrafts());

                            MessageManager.getInstance().dispatchMessage(0, null,
                                    ui.playerService.getQuestTracker(), Quest.Actions.Advance);
                        }

                    }), 
                    ui.fadeInAction(.4f), 
                    Actions.delay(.4f), 
                    Actions.run(ui.getScene().getInput().enableMe),
                    Actions.run(new ChangeState(ui.getStateMachine(), TownState.Home))
                )
            );
                        
            ui.transition(0f);
            ui.setFocus(null);
        }
        
        @Override
        public boolean onMessage(TownUI ui, Telegram telegram) {
            return false;
        }

        @Override
        public void exit(TownUI ui) {
        }
    },
    GoddessDialog("Continue") {
        private String[] buttons2 = {"Good-bye!"};
        public Iterator<String> dialog;

        @Override
        public void enter(TownUI ui) {
            ui.goddessDialog.clearActions();
            ui.goddessDialog.addAction(
                    Actions.sequence(
                        Actions.moveToAligned(ui.getDisplayWidth()/2f, ui.getDisplayHeight()/2f-20, Align.center),
                        Actions.scaleTo(.7f, .7f),
                        Actions.alpha(0f),
                        Actions.parallel(
                            Actions.alpha(1f, .2f),
                            Actions.scaleTo(1f, 1f, .2f, Interpolation.circleOut),
                            Actions.moveBy(0, 20, .2f, Interpolation.circleOut)
                        )
                    )
                );
            ui.goddessDialog.setVisible(true);
            ui.restore();

            ui.transition(0f);
            ui.resetFocus();
        }

        @Override
        public void exit(final TownUI ui) {
            ui.goddessDialog.clearActions();
            ui.goddessDialog.addAction(Actions.sequence(Actions.alpha(0f, .2f), Actions.run(new Runnable() {
                @Override
                public void run() {
                    ui.goddessDialog.setVisible(false);
                }
            })));
        }

        @Override
        public void update(TownUI ui) {
            
        }

        @Override
        public String[] defineButtons() {
            if (dialog.hasNext()) {
                return buttons;
            }
            else {
                return buttons2;
            }
        }

        @Override
        public boolean onMessage(TownUI ui, Telegram t) {
            
            if (t.extraInfo instanceof String[]) {
                dialog = Arrays.asList((String[]) t.extraInfo).iterator();
                ui.gMsg.setText(dialog.next());
                return true;
            }
            if (t.message == Messages.Interface.Button) {
                if (dialog.hasNext()) {
                    ui.gMsg.setText(dialog.next());
                }
                else {
                    dialog = null;
                    ui.changeState(Main);
                }
                return true;
            }
            
            return false;
        }
    },
    Over() {

        @Override
        public void enter(TownUI ui) {
            ui.getScene().getInput().disable();
            ui.restore();
            ui.getRoot().clearListeners();
            ui.addAction(
                Actions.sequence(
                    Actions.run(PlayBGM.fadeOut),
                    Actions.delay(3f),
                    Actions.parallel(
                        Actions.forever(
                            Actions.sequence(
                                Actions.moveTo(5, 0, .1f, Interpolation.bounce),
                                Actions.moveTo(-5, 0, .1f, Interpolation.bounce)
                            )
                        ),
                        Actions.forever(Actions.sequence(Actions.run(new PlaySound(DataDirs.Sounds.explode)), Actions.delay(.2f)))
                    )
                )
            );
            ui.fader.addAction(
                Actions.sequence(
                    Actions.delay(2f), 
                    Actions.alpha(1f, 5f), 
                    Actions.run(new Runnable() {

                            @Override
                            public void run() {
                                SceneManager.switchToScene("endgame");
                            }
                        }
                    )
                )
            );
        }

        @Override
        public boolean onMessage(TownUI ui, Telegram telegram) {
            return false;
        }
    },
    Save() {

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
                    Actions.moveToAligned(ui.getDisplayCenterX(), ui.getDisplayCenterY(), Align.center, .3f, Interpolation.circleOut), 
                    Actions.run(new Runnable() {

                        @Override
                        public void run() {
                            ui.setFocus(ui.saveWindow);
                            ui.formFocus.setFocus(ui.formFocus.getActors().first());
                            ui.getPointer().setPosition(ui.saveSlots.first(), Align.left);
                            ui.getPointer().setVisible(true);
                        }

                    })));
            ui.transition(.1f);
            ui.resetFocus();
        }

        @Override
        public void exit(final TownUI ui) {
            ui.saveWindow.addAction(
                Actions.sequence(
                    Actions.moveToAligned(ui.getDisplayCenterX(), ui.getDisplayHeight(), Align.bottom, .3f, Interpolation.circleOut)
                )
            );
        }

        @Override
        public boolean onMessage(TownUI ui, Telegram telegram) {
            if (telegram.message == Messages.Interface.Selected) {
                ui.playerService.save((Integer) telegram.extraInfo + 1);
                ui.changeState(Home);
            }
            else if (telegram.message == Messages.Interface.Close) {
                ui.changeState(Home);
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
        public void enter(final TownUI entity) {
            Label prompt = entity.downloadWindow.findActor("downloadLabel");
            prompt.setText("Downloading Daily Map");
            
            entity.downloadWindow.clearActions();
            entity.downloadWindow.addAction(
                   Actions.sequence(
                       Actions.moveToAligned(
                           entity.getDisplayCenterX(), 
                           entity.getDisplayCenterY(),
                           Align.center,
                           .5f, Interpolation.circleOut),
                       Actions.run(new Runnable() {
                        
                        @Override
                        public void run() {
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
                                       Gdx.app.log("Daily Dungeon", output);
                                       dungeonData = output;
                       
                                       Dungeon.Parameters params = Dungeon.Parameters.loadFromSimpleData(dungeonData);
                                       com.nhydock.storymode.scenes.dungeon.Scene dungeon = (com.nhydock.storymode.scenes.dungeon.Scene) SceneManager.switchToScene("dungeon");
                                       dungeon.setDungeon(params, null);
                                   }
                                   catch (Exception e) {
                                       Gdx.app.error("Daily Dungeon", "Parse Failed", e);
                                       
                                       Label prompt = entity.downloadWindow.findActor("downloadLabel");
                                       prompt.setText("Could not establish a connection/failed to download dungeon");
                                       
                                       entity.downloadWindow.addAction(
                                           Actions.sequence(
                                               Actions.delay(1),
                                               Actions.run(new Runnable(){
                                                   @Override
                                                   public void run(){
                                                       entity.changeState(Explore);
                                                   }
                                               })
                                           )
                                       );
                                   }  
                                }
                                
                                @Override
                                public void failed(Throwable t) {
                                    Gdx.app.log("Daily Dungeon", "Could not establish a connection/failed to download dungeon");
                                    entity.changeState(Explore);
                                    entity.setMessage("Could not establish a connection");
                                }
                                
                                @Override
                                public void cancelled() {
                                    Gdx.app.log("Daily Dungeon", "Download cancelled");
                                    entity.changeState(Explore);
                                    entity.setMessage("Download cancelled");
                                }
                            });
                        }
                    })
                )
            );
            
            
            entity.resetFocus();
        }
        
        @Override
        public void exit(TownUI entity) {
            entity.downloadWindow.clearActions();
            entity.downloadWindow.addAction(
                    Actions.moveToAligned(entity.getDisplayCenterX(), entity.getDisplayHeight(), Align.bottom, .5f, Interpolation.circleOut));
            
        }

        @Override
        public boolean onMessage(TownUI entity, Telegram telegram) {
            if (telegram.message == Messages.Interface.Close) {
                if (connection != null) {
                    Gdx.net.cancelHttpRequest(connection);
                }
            }
            return false;
        }

    },
    QuestMenu("Complete Quest") {
        private String[] buttons2 = {"Accept Quest"};
        boolean completeView = false;

        @Override
        public String[] defineButtons() {
            if (completeView) {
                return buttons;
            }
            else {
                return buttons2;
            }
        }

        @Override
        public void enter(TownUI ui) {
            ui.town.addAction(Actions.moveBy(256, 90, .8f));
            ui.acceptedQuests.setItems(ui.playerService.getQuestTracker().getAcceptedQuests());
            ui.availableQuests.setItems(ui.playerService.getQuestTracker().getQuests());

            ui.questSubmenu.addAction(
                Actions.sequence(
                    Actions.moveTo(-ui.questSubmenu.getWidth(), 0),
                    Actions.delay(.8f), 
                    Actions.moveTo(0, 0, .3f, Interpolation.circleOut)
                )
            );

            fillDetails(ui, (ui.questMenu.getOpenTabIndex() == 0) ? ui.availableQuests.getSelected()
                    : ui.acceptedQuests.getSelected());

            ui.questDetails.addAction(
                Actions.sequence(
                    Actions.run(ui.getScene().getInput().disableMe),
                    Actions.moveTo(ui.getDisplayWidth(), 0), 
                    Actions.delay(.8f), 
                    Actions.moveToAligned(ui.getDisplayWidth(), 0, Align.bottomRight, .3f, Interpolation.circleOut),
                    Actions.run(ui.getScene().getInput().enableMe)
                )
            );
            ui.setMessage("Let's help people!");
            ui.transition(.6f);
            ui.resetFocus();
        }
        
        @Override
        public void exit(TownUI ui) {
            ui.town.addAction(Actions.moveBy(-256, -90, .8f));
            ui.questSubmenu.addAction(Actions.moveToAligned(0, 0, Align.bottomRight, .3f));
            ui.questDetails.addAction(Actions.moveToAligned(ui.getDisplayWidth(), 0, Align.bottomLeft, .3f));
            ui.addAction(
                Actions.sequence(
                    Actions.run(ui.getScene().getInput().disableMe), 
                    Actions.delay(.8f), 
                    Actions.run(ui.getScene().getInput().enableMe)
                )
            );
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
            else if (telegram.message == Messages.Interface.Button && (int)telegram.extraInfo == Messages.Town.AcceptQuest) {
                Quest selected;
                if (!completeView) {
                    selected = ui.availableQuests.getSelected();
                    if (selected != null){
                        ui.playerService.getQuestTracker().accept(selected);
                        ui.availableQuests.getItems().removeValue(selected, true);
                        ui.acceptedQuests.setItems(ui.playerService.getQuestTracker().getAcceptedQuests());
                        ui.audio.playSfx(DataDirs.Sounds.accept);
                    } else {
                        ui.audio.playSfx(DataDirs.Sounds.cancel);
                    }
                }
                // don't try to accept quests that have already been accepted
                else {
                    selected = ui.acceptedQuests.getSelected();
                    if (selected != null){
                        boolean completed = ui.playerService.getQuestTracker().complete(selected);
                        if (!completed) {
                            ui.setMessage("You can't complete that quest yet");
                            ui.audio.playSfx(DataDirs.Sounds.cancel);
                        }
                        else {
                            ui.audio.playSfx(DataDirs.Sounds.accept);
                            // reward the player for completing the quest
                            Inventory inv = ui.playerService.getInventory();
                            Craftable craftable = inv.getRequiredCrafts().random();
    
                            Reward reward = ui.playerService.getQuestTracker().getReward(craftable);
    
                            inv.pickup(reward.item, reward.count);
                            String[] message = {
                                    "As a reward for your hard work, the folk you helped out left you with this...",
                                    String.format("Received %d %s", reward.count, reward.item) 
                            };
                            Object info = telegram.extraInfo;
                            telegram.extraInfo = message;
                            GoddessDialog.onMessage(ui, telegram);
                            telegram.extraInfo = info;
                            
                            ui.changeState(GoddessDialog);
                        }
                        ui.acceptedQuests.setItems(ui.playerService.getQuestTracker().getAcceptedQuests());
                    }
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
            else if (telegram.message == Messages.Interface.Close) {
                ui.changeState(Town);
                return true;
            }
            return false;
        }

    }, 

    PageFile () {

        @Override
        public void enter(TownUI entity) {
            entity.pageFile.getWindow().addAction(
                Actions.moveToAligned(entity.getDisplayCenterX(), entity.getDisplayCenterY(), Align.center, .4f, Interpolation.circleOut)
            );
            entity.transition(0f);
            entity.resetFocus();
        }
        
        @Override
        public void exit(TownUI entity) {
            entity.pageFile.getWindow().addAction(
                Actions.moveToAligned(entity.getDisplayCenterX(), entity.getDisplayHeight(), Align.bottom, .4f, Interpolation.circleOut)
            );
        }


        @Override
        public boolean onMessage(TownUI ui, Telegram t) {

            if (t.message == Messages.Interface.Close){
                ui.changeState(Home);
                return true;
            }
            
            return false;
        }
        
    }, 
    Train(){

        @Override
        public void enter(TownUI entity) {
            entity.trainingMenu.show();
            entity.setKeyboardFocus(entity.trainingMenu.getGroup());
            entity.transition(0f);
        }
        
        @Override
        public void exit(TownUI entity) {
            entity.trainingMenu.hide();
        }

        @Override
        public boolean onMessage(TownUI entity, Telegram telegram) {
            if (telegram.message == Messages.Interface.Close){
                entity.changeState(Town);
                return true;
            }
            else if (telegram.message == Messages.Dungeon.Sacrifice) {
                Trainer t = entity.trainingMenu.getTrainer();
                Stats s = Stats.Map.get(entity.playerService.getPlayer());
                ObjectIntMap<Item> sacrifice = entity.sacrificeMenu.getSacrifice();
                if (t.sacrifice(sacrifice, s)){
                    t.train(s);
                    entity.sacrificeMenu.sacrifice();
                    entity.playerService.getInventory().sacrifice(sacrifice, 0);
                    entity.audio.playSfx(DataDirs.Sounds.accept);
                    entity.changeState(Town);
                } else {
                    entity.audio.playSfx(DataDirs.Sounds.cancel);
                }
                return true;
            }
            return false;
        }};

    final String[] buttons;
    
    TownState(String... buttons) {
        this.buttons = buttons;
    }
    
    @Override
    public String[] defineButtons() {
        return buttons;
    }
    
    @Override
    public void exit(TownUI ui) {
    }

    @Override
    public void update(TownUI ui) {
    }
}