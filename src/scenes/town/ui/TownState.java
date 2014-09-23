package scenes.town.ui;

import java.util.Iterator;

import github.nhydock.ssm.SceneManager;
import scenes.GameUI;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
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
import core.datatypes.Craftable;
import core.datatypes.Item;
import core.datatypes.quests.Quest;
import core.datatypes.FileType;
import core.service.interfaces.IPlayerContainer.SaveSummary;

/**
 * Handles state based ui menu logic and switching
 * @author nhydock
 *
 */
enum TownState implements UIState {	
	Main(){

		@Override
		public void enter(TownUI ui) {
			ui.restore();
			ui.refreshButtons();
			ui.acceptedQuests.setItems(ui.questService.getAcceptedQuests());
			ui.availableQuests.setItems(ui.questService.getQuests());
		}
		
		@Override
		public String[] defineButtons(){
			return new String[]{"Sleep", "Explore", "Craft", "Quest", "Save"};
		}
		
		/**
		 * Use on message to switch between menus based on button index
		 */
		@Override
		public boolean onMessage(TownUI ui, Telegram t)
		{
			if (t.message == MenuMessage.Sleep)
			{
				ui.changeState(Sleep);
				return true;
			}
			else if (t.message == MenuMessage.Explore)
			{
				ui.changeState(Explore);
				return true;
			}
			else if (t.message == MenuMessage.Craft)
			{
				ui.changeState(Craft);
				return true;
			}
			else if (t.message == MenuMessage.Quest)
			{
				ui.changeState(QuestMenu);
				return true;
			}
			else if (t.message == MenuMessage.Save)
			{
				ui.changeState(Save);
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
					Label l = new Label(item.toString(), ui.getSkin(), "smaller");
					l.setAlignment(Align.left);
					ui.lootList.add(l).expandX().fillX();
					Label i = new Label(""+loot.get(item), ui.getSkin(), "smaller");
					i.setAlignment(Align.right);
					ui.lootList.add(i).width(30f);
					ui.lootList.row();
					
				}
				ui.lootList.setTouchable(Touchable.disabled);
			}
			else
			{
				ui.lootList.center();
				Label l = new Label("Looks like you don't have any loot!  You should go exploring", ui.getSkin());
				l.setWrap(true);
				l.setAlignment(Align.center);
				ui.lootList.add(l).expandX().fillX();
			}
			ui.lootList.pack();
			
			int index = 0;
			if (ui.craftMenu.getOpenTabIndex() == 0)
			{
				index = ui.craftList.getSelectedIndex();
			}
			else
			{
				index = ui.todayList.getSelectedIndex();
			}
			
			ui.todayList.setItems(ui.playerService.getInventory().getTodaysCrafts());
			ui.craftList.setItems(ui.playerService.getInventory().getRequiredCrafts());
			
			if (ui.craftMenu.getOpenTabIndex() == 0)
			{
				ui.craftList.setSelectedIndex(index);
			}
			else
			{
				ui.todayList.setSelectedIndex(index);
			}
			
		}
		
		private void refreshRequirements(Craftable c, TownUI ui)
		{
			if (c == null)
			{
				throw new NullPointerException("Craftable object can not be null");
			}
			
			//build requirements list
			ui.requirementList.clear();
			
			ObjectMap<String, Integer> items = c.getRequirements();
			for (String name : items.keys())
			{
				Label l = new Label(name, ui.getSkin(), "smallest");
				l.setAlignment(Align.left);
				ui.requirementList.add(l).expandX().fillX();
				Label i = new Label(ui.playerService.getInventory().genericCount(name)+"/"+items.get(name), ui.getSkin(), "smallest");
				i.setAlignment(Align.right);
				ui.requirementList.add(i).width(30f);
				ui.requirementList.row();
			}	
			ui.requirementList.pack();
		}
		
		@Override
		public void enter(TownUI ui) {
			//populate the submenu's data
			ui.craftMenu.showTab(0);
			//create loot menu
			populateLoot(ui);
		
			ui.sleepImg.addAction(Actions.moveTo(-ui.sleepImg.getWidth(), 0, .8f));
			ui.exploreImg.addAction(Actions.moveTo(-ui.exploreImg.getWidth(), 118f, .8f));
			ui.craftImg.addAction(
				Actions.sequence(
					Actions.moveTo(ui.getDisplayWidth()-ui.craftImg.getWidth(), ui.getDisplayHeight() - ui.craftImg.getHeight(), .3f),
					Actions.moveTo(ui.getDisplayWidth()/2-ui.craftImg.getWidth()/2, 118f, .5f)
				)
			);
		
			ui.craftSubmenu.addAction(Actions.sequence(
				Actions.moveTo(ui.getDisplayWidth(), 0),
				Actions.delay(.8f),
				Actions.moveTo(ui.getDisplayWidth()-ui.craftSubmenu.getWidth(), 0, .3f, Interpolation.circleOut)
			));
			
			ui.lootSubmenu.addAction(Actions.sequence(
				Actions.moveTo(-ui.lootSubmenu.getWidth(), 0),
				Actions.delay(.8f),
				Actions.moveTo(0, 0, .3f, Interpolation.circleOut)
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
					c = ui.craftList.getSelected();
				}
				else
				{
					c = ui.todayList.getSelected();
				}
				
				if (c != null)
				{
					int count = Tracker.NumberValues.Items_Crafted.value();
					boolean made = ui.playerService.getInventory().makeItem(c);
					ui.setMessage((made)?"Crafted an item!":"Not enough materials");
					populateLoot(ui);
					
					if (made)
					{
						ui.playerService.getInventory().refreshRequirements();
						refreshRequirements(c, ui);
						
					
						if (ui.craftMenu.getOpenTabIndex() == 0)
						{
							c = ui.craftList.getSelected();
						}
						else
						{
							c = ui.todayList.getSelected();
						}
						
					}
					
					if (ui.playerService.getInventory().getProgressPercentage() >= 1.0f)
					{
						ui.changeState(Over);
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
						GoddessDialog.onMessage(ui, t);
						t.extraInfo = info;
						
						ui.changeState(GoddessDialog);
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
				ui.changeState(Main);
			}
			return false;
		}
	},
	Explore(){

		@Override
		public void enter(TownUI ui) {
			ui.sleepImg.addAction(Actions.moveTo(-ui.sleepImg.getWidth(), 0, .8f));
			ui.craftImg.addAction(Actions.moveTo(ui.getDisplayWidth(), 0, .8f));
		
			ui.exploreSubmenu.addAction(Actions.sequence(
				Actions.moveTo(-ui.exploreSubmenu.getWidth(), 0),
				Actions.delay(.8f),
				Actions.moveTo(0, 0, .3f, Interpolation.circleOut)
			));
			
			ui.setMessage("Where to?");
			ui.refreshButtons();
		}
	
		@Override
		public String[] defineButtons(){
			return new String[]{"Return", "Explore Dungeon", "Random Dungeon"};
		}
		
		@Override
		public boolean onMessage(final TownUI ui, final Telegram t)
		{
			if (t.message == MenuMessage.Explore || t.message == MenuMessage.Random)
			{
				if (ui.playerService.getPlayer().hp <= 0)
				{
					ui.setMessage("You need to rest first!");
				}
				else
				{
					FileType ext = FileType.Other;
					int diff = 1;
					
					scenes.dungeon.Scene dungeon = (scenes.dungeon.Scene)SceneManager.create("dungeon");
					//load selected file dungeon
					if (t.message == MenuMessage.Explore)
					{
						if (ui.exploreTabs.getChecked().getName().equals("history"))
						{
							FileHandle f = TownUI.history.get(ui.fileList.getSelectedIndex());
							ext = FileType.getType(f.extension());
							diff = ext.difficulty(f.length());
							dungeon.setDungeon(f, diff);
						}
						else
						{
							FileHandle f = ui.directoryList.get(ui.fileList.getSelectedIndex());
							if (f != null && !f.isDirectory())
							{
								ext = FileType.getType(f.extension());
								diff = ext.difficulty(f.length());
								dungeon.setDungeon(f, diff);
								TownUI.history.add(f);
								TownUI.historyPaths.add(f.name());
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
						TownUI.directory = null;
					}
				
					SceneManager.switchToScene(dungeon);
					return true;
				}
			}
			/**
			 * Updates the information in the right side file panel to reflect the metadata of the specified file
			 */
			else if (t.message == GameUI.Messages.Selected)
			{
				final FileHandle selected = (FileHandle)t.extraInfo;
				
				ui.fileDetails.addAction(
					Actions.sequence(
						Actions.moveTo(ui.getDisplayWidth(),  0, .3f),
						Actions.run(new Runnable(){
							
							@Override
							public void run() {
								//generate a details panel
								Table contents = ui.fileDetailsContent;
								contents.clear();
								
								FileType ext = FileType.getType(selected.extension());
								Image icon = new Image(ui.getSkin().getRegion(ext.toString()));
								Label type = new Label("File Type: " + ext, ui.getSkin());
								Label size = new Label("File Size: " + (selected.length()/1000f) + " kb", ui.getSkin());
								Label diff = new Label("Difficulty: " + new String(new char[ext.difficulty(selected.length())]).replace('\0', '*'), ui.getSkin());	
								
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
						}),
						Actions.moveTo(ui.getDisplayWidth()-ui.fileDetails.getWidth(), 0, .3f)
					)
				);
				return true;
			}
			else
			{
				ui.changeState(Main);
				return true;
			}
			
			return false;
		}
	},
	Sleep(){

		@Override
		public void enter(final TownUI ui) {
			ui.setMessage("Good night!");
			ui.sleepImg.addAction(Actions.sequence(
				Actions.moveTo(32f, ui.getDisplayHeight()/2 - ui.sleepImg.getHeight()/2, .3f),
				Actions.moveTo(ui.getDisplayWidth()/2 - ui.sleepImg.getWidth()/2, ui.getDisplayHeight()/2 - ui.sleepImg.getHeight()/2, .5f)
			));
			ui.exploreImg.addAction(Actions.moveTo(ui.getDisplayWidth(), 118f, .8f));
			ui.craftImg.addAction(Actions.moveTo(ui.getDisplayWidth(), 0f, .8f));
			
			ui.getDisplay().addAction(Actions.sequence(
				Actions.delay(1f),
				Actions.alpha(0f, .5f),
				Actions.delay(.4f),
				Actions.run(new Runnable(){

					@Override
					public void run() {
						ui.playerService.rest();
						
						//new crafts appear each day!
						ui.playerService.getInventory().refreshCrafts();
						
						ui.todayList.setItems(ui.playerService.getInventory().getTodaysCrafts());
					}
					
				}),
				Actions.alpha(1f, .5f),
				Actions.delay(.3f),
				Actions.run(new Runnable() {
					
					@Override
					public void run()
					{
						ui.changeState(TownState.Main);
					}
					
				})
			));
			ui.refreshButtons();
		}

		@Override
		public String[] defineButtons() {
			return null;
		}

		@Override
		public boolean onMessage(TownUI ui, Telegram telegram) { return false; }
		
		@Override
		public void exit(TownUI ui)
		{
			
			MessageDispatcher.getInstance().dispatchMessage(0, null, ui.questService, Quest.Actions.Advance);
		
		}
	},
	GoddessDialog(){

		public Iterator<String> dialog;
		
		@Override
		public void enter(TownUI ui) {
			ui.goddess.clearActions();
			ui.goddess.addAction(Actions.moveTo(ui.getDisplayWidth()-128f, ui.getDisplayHeight()/2-64f, .3f));
			
			ui.goddessDialog.clearActions();
			ui.goddessDialog.addAction(Actions.alpha(1f, .2f));
			ui.goddessDialog.setVisible(true);
			ui.restore();
			
			ui.refreshButtons();
		}
		
		@Override
		public void exit(final TownUI ui) {
			ui.goddess.clearActions();
			ui.goddessDialog.clearActions();
			ui.goddess.addAction(Actions.moveTo(ui.getDisplayWidth(), ui.getDisplayHeight()/2-64f, .3f));
			ui.goddessDialog.addAction(Actions.sequence(Actions.alpha(0f, .2f), Actions.run(new Runnable(){
				@Override
				public void run() {
					ui.goddessDialog.setVisible(false);
				}
			})));
		}

		@Override
		public void update(TownUI ui) {
			if (dialog.hasNext())
			{
				ui.gMsg.setText(dialog.next());
			}
			else
			{
				dialog = null;
				ui.changeState(Main);
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
		public boolean onMessage(TownUI ui, Telegram t)
		{
			if (t.extraInfo instanceof String[])
			{
				dialog = new Array<String>((String[])t.extraInfo).iterator();
				ui.gMsg.setText(dialog.next());
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
		public void enter(TownUI ui) {
			InputMultiplexer input = (InputMultiplexer)Gdx.input.getInputProcessor();
			input.removeProcessor(ui);
			
			ui.restore();
			ui.getRoot().clearListeners();
			ui.getRoot().addAction(
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
			ui.getFader().addAction(
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
		public boolean onMessage(TownUI ui, Telegram telegram) {
			return false;
		}
	},
	Save(){

		@Override
		public String[] defineButtons() {
			return new String[]{"Cancel"};
		}

		@Override
		public void enter(final TownUI ui) {
			
			for (int i = 1; i <= ui.playerService.slots(); i++)
			{
				Table row = ui.saveSlots.get(i-1);
			
				row.clearChildren();
				SaveSummary s = ui.playerService.summary(i);
				if (s == null)
				{
					row.add(new Label("No Data", ui.getSkin(), "prompt")).expandX().center();
				}
				else
				{
					Image icon = new Image(ui.getSkin(), s.gender);
					row.add(icon).expand().center().colspan(1).size(32f, 32f);
					
					row.add(new Label(s.date, ui.getSkin(), "prompt")).expand().colspan(1).center();
					
					Table info = new Table();
					info.add(new Label("Crafting Completed: " + s.progress, ui.getSkin(), "smaller")).expand().colspan(1).right().row();
					info.add(new Label("Time: " + s.time, ui.getSkin(), "smaller")).expand().colspan(1).right().row();
					info.add(new Label(new String(new char[s.diff]).replace('\0', '*') + " difficulty", ui.getSkin(), "smaller")).expand().colspan(1).right();
					
					row.add(info).colspan(1).expand().right();
				}	
			}
			
			ui.saveWindow.addAction(
				Actions.sequence(
					Actions.moveTo(ui.getDisplayWidth()/2-ui.saveWindow.getWidth()/2, ui.getDisplayHeight()/2 - ui.saveWindow.getHeight()/2, .3f, Interpolation.circleOut),
					Actions.run(new Runnable(){

						@Override
						public void run() {
							ui.setFocus(ui.saveWindow);
							ui.formFocus.setFocus(ui.formFocus.getActors().first());
							ui.showPointer(ui.saveSlots.first(), Align.left, Align.center);
						}
						
					})
				)
			);
			ui.refreshButtons();
		}

		@Override
		public void exit(TownUI ui)
		{
			ui.setFocus(ui.getButtonList());
			ui.hidePointer();
		}
		
		@Override
		public boolean onMessage(TownUI ui, Telegram telegram) {
			if (telegram.message == GameUI.Messages.Selected)
			{
				ui.playerService.save((Integer)telegram.extraInfo + 1);
				ui.changeState(Main);
			}
			else
			{
				ui.getManager().get(DataDirs.tick, Sound.class).play();
				ui.changeState(Main);
			}
			return true;
		}
	}, 
	QuestMenu(){

		@Override
		public String[] defineButtons() {
			return new String[]{"Return", "Accept Quest"};
		}

		@Override
		public void enter(TownUI ui) {
			ui.sleepImg.addAction(Actions.moveTo(-ui.sleepImg.getWidth(), 0, .8f));
			ui.craftImg.addAction(Actions.moveTo(ui.getDisplayWidth(), 0, .8f));
		
			ui.questSubmenu.addAction(Actions.sequence(
				Actions.moveTo(-ui.questSubmenu.getWidth(), 0),
				Actions.delay(.8f),
				Actions.moveTo(0, 0, .3f, Interpolation.circleOut)
			));
			
			ui.setMessage("Let's help people!");
			ui.refreshButtons();
		}

		@Override
		public boolean onMessage(final TownUI ui, Telegram telegram) {
			//change which quest is selected
			// update the quest details pane on the side
			if (telegram.message == GameUI.Messages.Selected)
			{
				final Quest selected = (Quest)telegram.extraInfo;
				
				ui.questDetails.addAction(
					Actions.sequence(
						Actions.moveTo(ui.getDisplayWidth(),  0, .3f),
						Actions.run(new Runnable(){
							
							@Override
							public void run() {
								//generate a details panel
								Table contents = ui.questDetailsContent;
								contents.clear();
								
								//Image icon = new Image(ui.getSkin().getRegion(ext.toString()));
								Label loc = new Label("Location: " + selected.getLocation(), ui.getSkin(), "smaller");
								Label prompt = new Label(selected.getPrompt(), ui.getSkin(), "smaller");
								prompt.setWrap(true);
								Label objective = new Label(selected.getObjectivePrompt(), ui.getSkin(), "smaller");
								objective.setWrap(true);
								
								int d = selected.getExpirationDate();
								String dayLabel = ((d == 1) ? "1 day" : d + " days") + " left to complete";
								
								Label days = new Label(dayLabel, ui.getSkin(), "smaller");
								days.setWrap(true);
								contents.pad(10f);
								
//									icon.setAlign(Align.center);
//									icon.setSize(96f, 96f);
//									icon.setScaling(Scaling.fit);
//									contents.add(icon).size(96f, 96f).expandX();
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
						}),
						Actions.moveTo(ui.getDisplayWidth()-ui.questDetails.getWidth(), 0, .3f)
					)
				);
				return true;
			
			}
			//accept a new quest
			else if (telegram.message == MenuMessage.Accept) {
				Quest selected;
				if (ui.questMenu.getOpenTabIndex() == 0)
				{
					selected = ui.availableQuests.getSelected();
				}
				//don't try to accept quests that have already been accepted
				else
				{
					return false;
				}
				
				ui.questService.accept(selected);
				ui.acceptedQuests.setItems(ui.questService.getAcceptedQuests());
				
				return true;
			} else {
				ui.changeState(Main);
				return true;
			}
		}
		
	};
	
	/**
	 * Message types for the main state
	 * @author nhydock
	 *
	 */
	static class MenuMessage
	{
		static final int Sleep = 0;
		static final int Explore = 1;
		static final int Craft = 2;
		static final int Quest = 3;
		static final int Save = 4;
		
		static final int Make = 1;
		static final int Accept = 1;
		static final int Refresh = 2;
		
		static final int Random = 2;
		
	}

	@Override
	public void exit(TownUI ui) {}

	@Override
	public void update(TownUI ui){}
}